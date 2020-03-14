package com.nathanrassi.notifique

import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter

// TODO: https://github.com/cashapp/sqldelight/issues/1628/
internal class QueryDataSourceFactory<RowType : Any>(
  private val queryProvider: (limit: Long, offset: Long) -> Query<RowType>,
  private val countQuery: Query<Long>,
  private val transacter: Transacter
) : DataSource.Factory<Int, RowType>() {
  override fun create(): PositionalDataSource<RowType> =
    QueryDataSource(queryProvider, countQuery, transacter)
}

private class QueryDataSource<RowType : Any>(
  private val queryProvider: (limit: Long, offset: Long) -> Query<RowType>,
  private val countQuery: Query<Long>,
  private val transacter: Transacter
) : PositionalDataSource<RowType>() {
  override fun loadRange(
    params: LoadRangeParams,
    callback: LoadRangeCallback<RowType>
  ) {
    queryProvider(params.loadSize.toLong(), params.startPosition.toLong()).let { query ->
      if (!isInvalid) {
        callback.onResult(query.executeAsList())
      }
    }
  }

  override fun loadInitial(
    params: LoadInitialParams,
    callback: LoadInitialCallback<RowType>
  ) {
    queryProvider(params.requestedLoadSize.toLong(), params.requestedStartPosition.toLong()).let { query ->
      if (!isInvalid) {
        transacter.transaction {
          callback.onResult(
              /* data = */ query.executeAsList(),
              /* position = */ params.requestedStartPosition,
              /* totalCount = */ countQuery.executeAsOne().toInt()
          )
        }
      }
    }
  }
}
