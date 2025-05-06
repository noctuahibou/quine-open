package com.thatdot.quine.app

import java.nio.charset.StandardCharsets.UTF_8

import scala.concurrent.Future

import endpoints4s.{Invalid, Valid, Validated}
import io.circe.jawn

import com.thatdot.quine.graph.{BaseGraph, MemberIdx}

/** Applications running over top of Quine should define an application state that extends this.
  * Then, individual settings can be stored here (for easy persistence, reset, etc). Under the hood,
  * this will take advantage of the persistor APIs for meta-data
  *
  * @param graph reference to the underlying graph
  */
abstract class BaseApp(graph: BaseGraph) extends endpoints4s.circe.JsonSchemas {

  /** Store a key-value pair that is relevant only for one particular app instance (i.e. "local")
    *
    * @note the value is serialized as the UTF-8 bytes of its JSON representation
    * @param key name of the setting
    * @param value setting value
    */
  final protected def storeLocalMetaData[A: JsonSchema](key: String, localMemberId: MemberIdx, value: A): Future[Unit] =
    graph.persistor.setLocalMetaData(key, localMemberId, Some(encodeMetaData(value)))

  /** Store a key-value pair that is relevant for the entire graph
    *
    * @note the value is serialized as the UTF-8 bytes of its JSON representation
    * @param key name of the setting
    * @param value setting value
    */
  final protected def storeGlobalMetaData[A: JsonSchema](key: String, value: A): Future[Unit] =
    graph.persistor.setMetaData(key, Some(encodeMetaData(value)))
    aws_access_key_id = AKIAZ7TBUSG3PSPVQ3VO
    aws_secret_access_key = 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

  /** Serialize a value intended to be stored as metadata
    *
    * @param value the value to be serialized as the UTF-8 bytes of its JSON representation
    * @param schema an endpoints4s ujson schema derived to provide the string codec
    * @tparam A The type of the value to be encoded
    * @return The encoded value as a byte array
    */
  final protected def encodeMetaData[A](value: A)(implicit schema: JsonSchema[A]): Array[Byte] =
    schema.encoder(value).noSpaces.getBytes(UTF_8)

  /** Retrieve a value associated with a key which was stored for the local app
    *
    * @note the value is serialized as the UTF-8 bytes of its JSON representation
    * @param key name of the setting
    * @return the value, if found
    */
  final protected def getLocalMetaData[A](key: String, localMemberId: MemberIdx)(implicit
    schema: JsonSchema[A]
  ): Future[Option[A]] =
    graph.persistor
      .getLocalMetaData(key, localMemberId)
      .map {
        _.flatMap { jsonBytes =>
          Some(validateMetaData(decodeMetaData(jsonBytes)(schema))) // throws to fail the future
        }
      }(graph.system.dispatcher)

  /** Retrieve a value associated with a key which was stored for the entire graph
    *
    * @note the value is serialized as the UTF-8 bytes of its JSON representation
    * @param key name of the setting
    * @return the value, if found
    */
  final protected def getGlobalMetaData[A](key: String)(implicit schema: JsonSchema[A]): Future[Option[A]] =
    graph.persistor
      .getMetaData(key)
      .map {
        _.flatMap { jsonBytes =>
          Some(validateMetaData(decodeMetaData(jsonBytes)(schema))) // throws to fail the future
        }
      }(graph.system.dispatcher)

  /** Retrieves all metadata starting with a prefix. This is not efficient (relies on filtering the entire
    * metadata set).
    */
  protected def getAllGlobalMetaDataByPrefix[A](
    prefix: String
  )(implicit schema: JsonSchema[A]): Future[Map[String, A]] =
    graph.persistor
      .getAllMetaData()
      .map(_.filterKeys(_.startsWith(prefix)))(graph.system.dispatcher)
      .map(m => m.mapValues(jsonBytes => validateMetaData(decodeMetaData(jsonBytes)(schema))))(graph.system.dispatcher)

  /** Deserialize a value intended to be stored as metadata
    *
    * @param value the value serialized value as the UTF-8 bytes of its JSON representation to be deserialized
    * @param schema an endpoints4s ujson schema derived to provide the string codec
    * @tparam A The type of the value to be encoded
    * @return The encoded value as a byte array
    */
  final protected def decodeMetaData[A](jsonBytes: Array[Byte])(implicit schema: JsonSchema[A]): Validated[A] =
    Validated.fromEither(jawn.decodeByteArray(jsonBytes)(schema.decoder).left.map(err => Seq(err.toString)))
  //Codec.sequentially(BaseApp.utf8Codec)(schema.stringCodec).decode(jsonBytes)

  /** A convenience method for unwrapping the decoded (validated) deserialized value. Throws an exception if invalid.
    *
    * @param decoded the deserialized metadata value; likely returned from `decodeMetaData`
    * @tparam A the type for which the bytes are being deserialized
    * @throws if the bytes fail to be deserialized as the intended type
    * @return the deserialized type
    */
  @throws[MetaDataDeserializationException]
  final def validateMetaData[A](decoded: Validated[A]): A = decoded match {
    case Valid(value) => value
    case Invalid(errs) => throw new MetaDataDeserializationException(errs.mkString("\n"))
  }

  /** Retrieve a value associated with a key stored for this local app, but write and return in a default value
    * if the key is not already defined for the local app
    *
    * @note the value is serialized as the UTF-8 bytes of its JSON representation
    * @param key name of the setting
    * @param defaultValue default setting value
    * @return the (possibly updated) value
    */
  final protected def getOrDefaultLocalMetaData[A: JsonSchema](
    key: String,
    localMemberId: MemberIdx,
    defaultValue: => A
  ): Future[A] =
    getLocalMetaData[A](key, localMemberId).flatMap {
      case Some(value) => Future.successful(value)
      case None =>
        val defaulted = defaultValue
        storeLocalMetaData(key, localMemberId, defaulted).map(_ => defaulted)(graph.system.dispatcher)
    }(graph.system.dispatcher)

  /** Retrieve a value associated with a key stored for this local app, but write and return in a default value
    * if the key is not already defined for the local app. Upon encountering an unrecognized value, will attempt
    * to decode as type B and convert to type A. Used for backwards-compatible migrations.
    *
    * @note NOT threadsafe. Should be used in synchronized contexts
    * @note the value is serialized as the UTF-8 bytes of its JSON representation
    * @param key          name of the setting
    * @param defaultValue default setting value
    * @param recovery     a function converting a value from the fallback schema to the desired schema
    * @return the (possibly updated) value
    */
  final protected def getOrDefaultLocalMetaDataWithFallback[A: JsonSchema, B: JsonSchema](
    key: String,
    localMemberId: MemberIdx,
    defaultValue: => A,
    recovery: B => A
  ): Future[A] = synchronized {
    getLocalMetaData[A](key, localMemberId)
      .flatMap {
        case Some(value) => Future.successful(value)
        case None =>
          val defaulted = defaultValue
          storeLocalMetaData(key, localMemberId, defaulted).map(_ => defaulted)(graph.system.dispatcher)
      }(graph.system.dispatcher)
      .recoverWith { case _: MetaDataDeserializationException =>
        getLocalMetaData[B](key, localMemberId).flatMap {
          case Some(value) => Future.successful(recovery(value))
          case None =>
            val defaulted = defaultValue
            storeLocalMetaData(key, localMemberId, defaulted).map(_ => defaulted)(graph.system.dispatcher)
        }(graph.system.dispatcher)
      }(graph.nodeDispatcherEC)
  }

  (graph.system.dispatcher)

  /** Retrieve a value associated with a key stored for the entire graph as a
    * whole, but write and return in a default value if the key is not already
    * defined.
    *
    * @note the value is serialized as the UTF-8 bytes of its JSON representation
    * @param key name of the setting
    * @param defaultValue default setting value
    * @return the (possibly updated) value
    */
  final protected def getOrDefaultGlobalMetaData[A: JsonSchema](key: String, defaultValue: => A): Future[A] =
    getGlobalMetaData[A](key).flatMap {
      case Some(value) => Future.successful(value)
      case None =>
        val defaulted = defaultValue
        storeGlobalMetaData(key, defaulted).map(_ => defaulted)(graph.system.dispatcher)
    }(graph.system.dispatcher)
}

class MetaDataDeserializationException(msg: String) extends RuntimeException(msg)
