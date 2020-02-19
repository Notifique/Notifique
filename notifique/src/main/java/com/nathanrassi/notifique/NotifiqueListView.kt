package com.nathanrassi.notifique

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
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
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.MutableSelection
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
  lateinit var onSelectionStateChangedListener: OnSelectionStateChangedListener
  private val allNotifiques: Query<Notifique>
  private val dataSourceFactory: QueryDataSourceFactory<Notifique>
  private lateinit var liveData: LiveData<PagedList<Notifique>>
  private val observer: Observer<PagedList<Notifique>>
  private val listAdapter: Adapter
  private val selectionTracker: SelectionTracker<Long>
  private lateinit var scope: CoroutineScope

  interface OnSelectionStateChangedListener {
    fun onSelectionStateChanged(selected: Boolean)
  }

  fun deleteSelected() {
    val mutableSelection = MutableSelection<Long>().also {
      selectionTracker.copySelection(it)
    }
    GlobalScope.launch {
      for (id in mutableSelection.iterator()) {
        notifiqueQueries.delete(id)
      }
      withContext(Dispatchers.Main) {
        selectionTracker.clearSelection()
      }
    }
  }

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

    selectionTracker = SelectionTracker.Builder(
        "list-selection-id",
        this,
        object : ItemKeyProvider<Long>(SCOPE_MAPPED) {
          override fun getKey(position: Int): Long? {
            return listAdapter.getKey(position)
          }

          override fun getPosition(key: Long): Int {
            val index = listAdapter.currentList!!.indexOfFirst {
              key == it.id
            }
            return if (index == -1) NO_POSITION else index
          }
        },
        object : ItemDetailsLookup<Long>() {
          override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
            val childView = findChildViewUnder(e.x, e.y) ?: return null
            val viewHolder = getChildViewHolder(childView)
            val position = viewHolder.adapterPosition
            return object : ItemDetails<Long>() {
              override fun getSelectionKey(): Long? {
                return listAdapter.getKey(position)
              }

              override fun getPosition(): Int {
                return position
              }
            }
          }
        },
        StorageStrategy.createLongStorage()
    )
        .build()
    selectionTracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
      var hasSelection = false

      override fun onItemStateChanged(
        key: Long,
        selected: Boolean
      ) {
        if (selectionTracker.hasSelection()) {
          if (!hasSelection) {
            hasSelection = true
            onSelectionStateChangedListener.onSelectionStateChanged(true)
          }
        } else if (hasSelection) {
          hasSelection = false
          onSelectionStateChangedListener.onSelectionStateChanged(false)
        }
      }
    })
    listAdapter.selectionTracker = selectionTracker
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

  override fun onSaveInstanceState(): Parcelable {
    val savedState = super.onSaveInstanceState()
    return Bundle(2).apply {
      putParcelable("savedState", savedState)
      selectionTracker.onSaveInstanceState(this)
    }
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    (state as Bundle).apply {
      super.onRestoreInstanceState(getParcelable("savedState"))
      selectionTracker.onRestoreInstanceState(this)
    }
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
      foreground = context.getDrawable(R.drawable.item_view_foreground)
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
    lateinit var selectionTracker: SelectionTracker<Long>

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int
    ) = ViewHolder(
        inflater.inflate(R.layout.list_item, parent, false) as ItemView
    )

    fun getKey(position: Int): Long? {
      return getItem(position)?.id
    }

    override fun onBindViewHolder(
      holder: ViewHolder,
      position: Int
    ) {
      val notifique = getItem(position)
      if (notifique == null) {
        holder.root.setPlaceholder()
        holder.root.isSelected = false
      } else {
        holder.root.setNotifique(notifique)
        holder.root.isSelected = selectionTracker.isSelected(notifique.id)
      }
    }

    override fun onBindViewHolder(
      holder: ViewHolder,
      position: Int,
      payloads: List<Any>
    ) {
      if (payloads.isEmpty()) {
        onBindViewHolder(holder, position)
      } else if (
          payloads.size == 1 && payloads.contains(SelectionTracker.SELECTION_CHANGED_MARKER)
      ) {
        val notifique = getItem(position)!!
        holder.root.isSelected = selectionTracker.isSelected(notifique.id)
      } else {
        throw IllegalStateException("Unhandled payloads: $payloads")
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

