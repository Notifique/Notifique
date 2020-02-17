package com.nathanrassi.notifique

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PagedList.Config.Builder
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.android.paging.QueryDataSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import javax.inject.Inject

internal class NotifiqueListView(
  context: Context,
  attributeSet: AttributeSet
) : RecyclerView(context, attributeSet) {
  @Inject lateinit var notifiqueQueries: NotifiqueQueries
  private val allNotifiques: Query<Notifique>
  private val dataSourceFactory: QueryDataSourceFactory<Notifique>
  private lateinit var liveData: LiveData<PagedList<Notifique>>
  private val observer: Observer<PagedList<Notifique>>
  private val listAdapter: Adapter
  private lateinit var scope: CoroutineScope

  init {
    context.appComponent.inject(this)

    val inflater = LayoutInflater.from(context)
    layoutManager = LinearLayoutManager(context)
    listAdapter = Adapter(inflater).apply {
      registerAdapterDataObserver(object : AdapterDataObserver() {
        override fun onItemRangeInserted(
          positionStart: Int,
          itemCount: Int
        ) {
          if (!canScrollVertically(-1) && positionStart == 0) {
            scrollToPosition(0)
          }
        }
      })
    }

    allNotifiques = notifiqueQueries.allNotifiques()
    dataSourceFactory = QueryDataSourceFactory(
        queryProvider = notifiqueQueries::notifiques,
        countQuery = notifiqueQueries.countNotifiques(),
        transacter = notifiqueQueries
    )
    observer = Observer {
      listAdapter.submitList(it)
    }
    setHasFixedSize(true)
    adapter = listAdapter
    addItemDecoration(DividerItemDecoration(context.getDrawable(R.drawable.divider)!!))
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    scope = MainScope()
    liveData = LivePagedListBuilder(
        dataSourceFactory,
        Builder()
            .setEnablePlaceholders(true)
            .setInitialLoadSizeHint(25)
            .setPageSize(15)
            .build()
    )
        .setFetchExecutor(FetchExecutor(scope))
        .build()
    liveData.observeForever(observer)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    scope.cancel()
    liveData.removeObserver(observer)
  }

  private class ItemView(
    context: Context,
    attributeSet: AttributeSet
  ) : LinearLayout(context, attributeSet) {
    private val appName: TextView
    private val timestamp: TextView
    private val title: TextView
    private val message: TextView

    init {
      orientation = VERTICAL
      val inflater = LayoutInflater.from(context)
      inflater.inflate(R.layout.list_item_children, this, true)
      appName = findViewById(R.id.app_name)
      timestamp = findViewById(R.id.timestamp)
      title = findViewById(R.id.title)
      message = findViewById(R.id.message)
    }

    internal fun setNotifique(notifique: Notifique) {
      appName.text = notifique.app
      timestamp.text = timestampFormat.format(Date(notifique.timestamp))
      title.text = notifique.title
      message.text = notifique.message
    }

    @SuppressLint("SetTextI18n") // TODO
    internal fun setPlaceholder() {
      appName.text = "PLACEHOLDER TODO"
      timestamp.text = "PLACEHOLDER TODO"
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

  private class FetchExecutor(private val scope: CoroutineScope) : Executor {
    override fun execute(command: Runnable) {
      scope.launch(Dispatchers.IO) {
        command.run()
      }
    }
  }
}

private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

/**
 * Copied from [DividerItemDecoration]
 */
private class DividerItemDecoration(
  private val divider: Drawable
) : ItemDecoration() {
  private val bounds = Rect()

  override fun onDraw(
    c: Canvas,
    parent: RecyclerView,
    state: State
  ) {
    drawVertical(c, parent)
  }

  private fun drawVertical(
    canvas: Canvas,
    parent: RecyclerView
  ) {
    canvas.save()
    val left: Int
    val right: Int
    if (parent.clipToPadding) {
      left = parent.paddingLeft
      right = parent.width - parent.paddingRight
      canvas.clipRect(
          left, parent.paddingTop, right,
          parent.height - parent.paddingBottom
      )
    } else {
      left = 0
      right = parent.width
    }
    val childCount = parent.childCount
    for (i in 0 until childCount) {
      val child = parent.getChildAt(i)
      parent.getDecoratedBoundsWithMargins(child, bounds)
      val bottom = bounds.bottom + Math.round(child.translationY)
      val top = bottom - divider.intrinsicHeight
      divider.setBounds(left, top, right, bottom)
      divider.draw(canvas)
    }
    canvas.restore()
  }

  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: State
  ) {
    val dividerHeight = if (parent.getChildAdapterPosition(
            view
        ) == parent.adapter!!.itemCount - 1
    ) 0 else divider.intrinsicHeight
    outRect[0, 0, 0] = dividerHeight
  }
}

