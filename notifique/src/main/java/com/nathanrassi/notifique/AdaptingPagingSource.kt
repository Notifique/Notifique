package com.nathanrassi.notifique

import androidx.paging.PagingSource
import androidx.paging.PagingState

internal class AdaptingPagingSourceFactory<OriginalValue : Any, AdaptedValue : Any>(
  private val delegate: () -> PagingSource<Int, OriginalValue>,
  private val adapter: (OriginalValue) -> AdaptedValue
) : () -> PagingSource<Int, AdaptedValue> {
  override fun invoke(): PagingSource<Int, AdaptedValue> {
    return AdaptingPagingSource(delegate(), adapter)
  }
}

private class AdaptingPagingSource<OriginalValue : Any, AdaptedValue : Any>(
  private val delegate: PagingSource<Int, OriginalValue>,
  private val adapter: (OriginalValue) -> AdaptedValue
) : PagingSource<Int, AdaptedValue>() {
  init {
    delegate.registerInvalidatedCallback(::invalidate)
  }

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AdaptedValue> {
    return when (val result = delegate.load(params)) {
      is LoadResult.Page<Int, OriginalValue> -> {
        val items = result.data
        val adaptedItems = ArrayList<AdaptedValue>(items.size)
        for (i in items.indices) {
          val item = items[i]
          adaptedItems += adapter(item)
        }
        LoadResult.Page(
          adaptedItems,
          result.prevKey,
          result.nextKey,
          result.itemsBefore,
          result.itemsAfter
        )
      }
      is LoadResult.Error -> {
        LoadResult.Error(result.throwable)
      }
    }
  }

  override fun getRefreshKey(state: PagingState<Int, AdaptedValue>): Int? {
    // This could delegate if we had an adapter backwards, but the abstraction isn't worth it here.
    return state.anchorPosition
  }

  override val jumpingSupported: Boolean
    get() = delegate.jumpingSupported

  override val keyReuseSupported: Boolean
    get() = delegate.keyReuseSupported
}