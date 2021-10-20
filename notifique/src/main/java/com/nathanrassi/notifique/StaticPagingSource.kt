package com.nathanrassi.notifique

import androidx.paging.PagingSource
import androidx.paging.PagingState

internal class StaticPagingSource<RowType : Any>(
  private val items: List<RowType>
) : PagingSource<Int, RowType>() {
  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RowType> {
    return LoadResult.Page(items, null, null, 0, 0) // TODO
  }

  override fun getRefreshKey(state: PagingState<Int, RowType>): Int? {
    return state.anchorPosition
  }

  override val jumpingSupported = true
}