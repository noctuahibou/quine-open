package com.thatdot.quine.persistor.cassandra.vanilla

import com.datastax.oss.driver.api.core.cql.SimpleStatement

import com.thatdot.quine.persistor.cassandra.SnapshotsTableDefinition

object Snapshots extends SnapshotsTableDefinition {
  protected val selectAllQuineIds: SimpleStatement = select.distinct.column(quineIdColumn.name).build
}
