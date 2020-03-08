package com.nathanrassi.notifique

import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement
import javax.inject.Inject

@AppScope
internal class DatabaseDelayer @Inject constructor() {
  @Volatile var databaseDelayMillis: Long = 0L
}

internal class DatabaseDelayerSqlDriver(
  private val delegate: SqlDriver,
  private val databaseDelayer: DatabaseDelayer
) : SqlDriver by delegate {
  override fun executeQuery(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ): SqlCursor {
    Thread.sleep(databaseDelayer.databaseDelayMillis)
    return delegate.executeQuery(identifier, sql, parameters, binders)
  }

  override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ) {
    Thread.sleep(databaseDelayer.databaseDelayMillis)
    delegate.execute(identifier, sql, parameters, binders)
  }
}
