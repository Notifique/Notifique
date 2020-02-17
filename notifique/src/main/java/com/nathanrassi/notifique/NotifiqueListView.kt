package com.nathanrassi.notifique

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Rect
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.android.paging.QueryDataSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.properties.Delegates.notNull

internal class NotifiqueListView(
  context: Context,
  attributeSet: AttributeSet
) : RecyclerView(context, attributeSet) {
  @Inject lateinit var notifiqueQueries: NotifiqueQueries
  private val allNotifiques: Query<Notifique>
  private val dataSourceFactory: QueryDataSourceFactory<Notifique>
  private val listener: Query.Listener
  private val listAdapter: Adapter
  private var scope: CoroutineScope by notNull()

  init {
    context.appComponent.inject(this)

    val inflater = LayoutInflater.from(context)
    layoutManager = LinearLayoutManager(context)
    listAdapter = Adapter(inflater).apply {
      registerAdapterDataObserver(object : AdapterDataObserver() {
        override fun onChanged() {
          android.util.Log.d("eric", "eric CHANGED")
        }

        override fun onItemRangeChanged(
          positionStart: Int,
          itemCount: Int
        ) {
          android.util.Log.d("eric", "eric ITEM RANGE CHANGED")
        }

        override fun onItemRangeInserted(
          positionStart: Int,
          itemCount: Int
        ) {
          android.util.Log.d(
              "eric", "eric ITEM RANGE INSERTED " + Thread.currentThread()
              + " " + itemCount +
              " " + this@apply.itemCount
          )
          if (!canScrollVertically(-1) && positionStart == 0) { // TODO: still needed?
            android.util.Log.d(
                "eric", "eric scrolling to top " + Thread.currentThread()
                + " " + itemCount +
                " " + this@apply.itemCount
            )
            scrollToPosition(0)
          }
        }
      })
    }

    allNotifiques = notifiqueQueries.allNotifiques() // TODO: just notifiques() ?
    dataSourceFactory = QueryDataSourceFactory(
        queryProvider = notifiqueQueries::notifiques,
        countQuery = notifiqueQueries.countNotifiques(),
        transacter = notifiqueQueries
    )
    listener = object : Query.Listener {
      override fun queryResultsChanged() {
        android.util.Log.d("eric", "eric query results changed " + Thread.currentThread())

        setData()
      }
    }
    setHasFixedSize(true)
    adapter = listAdapter
    addItemDecoration(object : ItemDecoration() {
      override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: State
      ) {
        val top = if (parent.getChildAdapterPosition(view) == 0) 40 else 0
        // TODO
        outRect.set(0, top, 0, 40)
      }
    })
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    scope = MainScope()
    scope.launch(Dispatchers.IO) {
      setData()
    }
    allNotifiques.addListener(listener)
  }

  @SuppressLint("WrongThread") // Called on I/O Dispatcher worker thread.
  private fun setData() {
    val dataSource = dataSourceFactory.create()
    val pagedList = PagedList.Builder(
        dataSource, PagedList.Config.Builder()
        .setEnablePlaceholders(true)
        .setInitialLoadSizeHint(25)
        .setPageSize(15)
        //.setInitialLoadSizeHint(5)
        //.setPageSize(5)
        .build()
    )
        .setFetchExecutor(FetchExecutor())
        .setNotifyExecutor(MainExecutor())
        .build()

    scope.launch(Dispatchers.Main) { // TODO
      android.util.Log.d("eric", "eric 1 " + Thread.currentThread())
      listAdapter.submitList(pagedList)
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    scope.cancel()
    allNotifiques.removeListener(listener)
  }

  private class ItemView(
    context: Context,
    attributeSet: AttributeSet
  ) : LinearLayout(context, attributeSet) {
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

      setOnClickListener {
        val notificationManager = context.getSystemService(NotificationManager::class.java)!!
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
          notificationManager.createNotificationChannel(
              NotificationChannel(
                  "test notification", "test notification", NotificationManager.IMPORTANCE_LOW
              )
          )
        }
        notificationManager.notify(
            1, NotificationCompat.Builder(context, "test notification").setSmallIcon(
            R.mipmap.ic_launcher
        ).setContentTitle(
            "Test"
        ).setContentText("this is the message.").build()
        )
        notificationManager.notify(
            2, NotificationCompat.Builder(context, "test notification").setSmallIcon(
            R.mipmap.ic_launcher
        ).setContentTitle(
            "Test #2"
        ).setContentText("this is the SECOND message.").build()
        )
      }
    }

    internal fun setNotifique(notifique: Notifique) {
      appName.text = notifique.app
      title.text = notifique.title
      message.text = notifique.message
    }

    @SuppressLint("SetTextI18n") // TODO
    internal fun setPlaceholder() {
      appName.text = "PLACEHOLDER TODO"
      title.text = "PLACEHOLDER TODO"
      message.text = "PLACEHOLDER TODO"
    }
  }

  private class Adapter(private val inflater: LayoutInflater) :
      PagedListAdapter<Notifique, Adapter.ViewHolder>(
          NotifiqueDiffCallback
      ) {
    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int
    ) = ViewHolder(
        inflater.inflate(R.layout.list_item, parent, false) as ItemView
    )

    override fun onBindViewHolder(
      holder: ViewHolder,
      position: Int
    ) {
      val notifique = getItem(position)
      if (notifique == null) {
        holder.root.setPlaceholder()
      } else {
        holder.root.setNotifique(notifique)
      }
    }

    private class ViewHolder(val root: ItemView) : RecyclerView.ViewHolder(root)

    object NotifiqueDiffCallback : DiffUtil.ItemCallback<Notifique>() {
      override fun areItemsTheSame(
        oldItem: Notifique,
        newItem: Notifique
      ) = oldItem.id == newItem.id

      @SuppressLint("DiffUtilEquals") // The implementation of Notifique is a data class.
      override fun areContentsTheSame(
        oldItem: Notifique,
        newItem: Notifique
      ) = oldItem == newItem
    }
  }

  private inner class FetchExecutor : Executor {
    override fun execute(command: Runnable) {
      scope.launch(Dispatchers.IO) {
        command.run()
      }
    }
  }

  private inner class MainExecutor : Executor {
    override fun execute(command: Runnable) {
      scope.launch(Dispatchers.Main) {
        command.run()
      }
    }
  }
}
