package com.thatdot.quine.persistor.cassandra.vanilla

import com.datastax.oss.driver.api.core.cql.SimpleStatement

import com.thatdot.quine.persistor.cassandra.JournalsTableDefinition

object Journals extends JournalsTableDefinition {
  protected val selectAllQuineIds: SimpleStatement = select.distinct
    .column(quineIdColumn.name)
    .build
}
