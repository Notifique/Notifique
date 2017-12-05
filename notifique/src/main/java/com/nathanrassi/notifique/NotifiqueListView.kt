package com.nathanrassi.notifique

import android.arch.paging.PagedList
import android.arch.paging.PagedListAdapter
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v7.recyclerview.extensions.DiffCallback
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.Executor
import javax.inject.Inject

private class NotifiqueListView(
    context: Context, attributeSet: AttributeSet) : RecyclerView(context, attributeSet) {
  @Inject internal lateinit var dao: Notifique.Dao
  private val listAdapter = Adapter(LayoutInflater.from(context))
  private lateinit var databaseJob: Job

  init {
    context.appComponent.inject(this)
    layoutManager = LinearLayoutManager(context)
    adapter = listAdapter
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    fun setList() {
      databaseJob = launch(UI) {
        val pagedList = async {
          // TODO: If insertion soon before the source creation, we're boned.
          // https://issuetracker.google.com/issues/69752873
          val source = dao.source()
          source.addInvalidatedCallback {
            launch(UI) {
              setList()
            }
          }
          PagedList.Builder<Int, Notifique>()
              .setDataSource(source)
              .setMainThreadExecutor(MainThreadExecutor)
              .setBackgroundThreadExecutor(BackgroundThreadExecutor)
              .setConfig(PagedList.Config.Builder()
                  .setPageSize(20)
                  .build())
              .build()
        }.await()
        listAdapter.setList(pagedList)
      }
    }
    setList()
  }

  object MainThreadExecutor : Executor {
    private val main = Handler(Looper.getMainLooper())

    override fun execute(command: Runnable) {
      main.post(command)
    }
  }

  object BackgroundThreadExecutor : Executor {
    override fun execute(command: Runnable) {
      launch { command.run() }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    databaseJob.cancel()
  }

  private class ItemView(context: Context, attributeSet: AttributeSet) : LinearLayout(context,
      attributeSet) {
    private val appName: TextView
    private val title: TextView
    private val message: TextView

    init {
      orientation = VERTICAL
      val inflater = LayoutInflater.from(context)
      inflater.inflate(R.layout.list_item_children, this, true)
      appName = findViewById(R.id.app_name)
      title = findViewById(R.id.title)
      message = findViewById(R.id.message)
    }

    internal fun setNotifique(notifique: Notifique) {
      appName.text = notifique.notifPackage
      title.text = notifique.title
      message.text = notifique.message
    }

    internal fun setPlaceholder() {
      appName.text = "PLACEHOLDER TODO"
      title.text = "PLACEHOLDER TODO"
      message.text = "PLACEHOLDER TODO"
    }
  }

  private class Adapter(
      private val inflater: LayoutInflater) : PagedListAdapter<Notifique, Adapter.ViewHolder>(
      NotifiqueDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        inflater.inflate(R.layout.list_item, parent, false) as ItemView)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      val notifique = getItem(position)
      if (notifique == null) {
        // Placeholder.
        holder.root.setPlaceholder()
      } else {
        holder.root.setNotifique(notifique)
      }
    }

    private class ViewHolder(internal val root: ItemView) : RecyclerView.ViewHolder(root)

    object NotifiqueDiffCallback : DiffCallback<Notifique>() {
      override fun areItemsTheSame(
          oldItem: Notifique, newItem: Notifique) = oldItem.id == newItem.id

      override fun areContentsTheSame(
          oldItem: Notifique, newItem: Notifique) = oldItem == newItem
    }
  }
}
