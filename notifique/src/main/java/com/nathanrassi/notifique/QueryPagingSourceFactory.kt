package com.nathanrassi.notifique

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal class QueryPagingSourceFactory<RowType : Any>(
  private val fetchDispatcher: CoroutineDispatcher,
  private val queryProvider: (limit: Long, offset: Long) -> Query<RowType>,
  private val countQueryProvider: () -> Query<Long>,
  private val transacter: Transacter
) : () -> PagingSource<Int, RowType> {
  override fun invoke(): PagingSource<Int, RowType> {
    return QueryPagingSource(fetchDispatcher, queryProvider, countQueryProvider, transacter)
  }
}

private class QueryPagingSource<RowType : Any>(
  private val fetchDispatcher: CoroutineDispatcher,
  private val queryProvider: (limit: Long, offset: Long) -> Query<RowType>,
  private val countQueryProvider: () -> Query<Long>,
  private val transacter: Transacter
) : PagingSource<Int, RowType>(), Query.Listener {
  private var query: Query<RowType>? = null

  override fun queryResultsChanged() {
    invalidate()
  }

  init {
    registerInvalidatedCallback {
      query?.removeListener(this)
      query = null
    }
  }

  override suspend fun load(params: LoadParams<Int>): LoadResult.Page<Int, RowType> {
    return withContext(fetchDispatcher) {
      suspendCancellableCoroutine { cont ->
        // TODO: Try to center this position when it's a refresh and not null key.
        var startPosition = params.key ?: 0
        var requestedLoadSize = params.loadSize
        if (params is LoadParams.Prepend) {
          // Clamp load size to positive indices only, and shift start index by load size.
          requestedLoadSize = minOf(requestedLoadSize, startPosition)
          startPosition -= requestedLoadSize
        }
        query?.removeListener(this@QueryPagingSource)
        queryProvider(requestedLoadSize.toLong(), startPosition.toLong()).let { query ->
          query.addListener(this@QueryPagingSource)
          this@QueryPagingSource.query = query
          val countQuery = countQueryProvider()
          if (!invalid) {
            transacter.transaction {
              val data = query.executeAsList()
              val totalCount = countQuery.executeAsOne().toInt()
              val dataSize = data.size
              val lastPosition = startPosition + dataSize
              val previousKey = if (startPosition == 0) null else startPosition
              val nextKey = if (lastPosition == totalCount) null else lastPosition
              val itemsBefore = startPosition
              val itemsAfter = totalCount - lastPosition
              val result = LoadResult.Page(
                data,
                previousKey,
                nextKey,
                itemsBefore,
                itemsAfter
              )
              cont.resume(result)
            }
          }
        }
      }
    }
  }

  override fun getRefreshKey(state: PagingState<Int, RowType>): Int? {
    return state.anchorPosition
  }

  override val jumpingSupported = true
}
