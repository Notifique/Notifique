package com.nathanrassi.notifique

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

internal class NotifiqueListView(
    context: Context, attributeSet: AttributeSet) : RecyclerView(context, attributeSet) {
  @Inject internal lateinit var dao: Notifique.Dao
  private lateinit var databaseJob: Job

  init {
    context.appComponent.inject(this)
    layoutManager = LinearLayoutManager(context)
    // TODO: Add the adapter to show list items.
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    databaseJob = launch(UI) {
      val notifiques = async { dao.all }.await()
      // TODO: Bind the data to the view.
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    databaseJob.cancel()
  }
}
