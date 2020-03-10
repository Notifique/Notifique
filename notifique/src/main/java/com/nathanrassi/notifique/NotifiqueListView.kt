package com.nathanrassi.notifique

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.MutableSelection
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
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
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.math.roundToInt

internal class NotifiqueListView(
  context: Context,
  attributeSet: AttributeSet
) : RecyclerView(context, attributeSet) {
  @Inject lateinit var notifiqueQueries: NotifiqueQueries
  lateinit var onSelectionStateChangedListener: OnSelectionStateChangedListener
  private var deleteIcon = ContextCompat.getDrawable(context, R.drawable.toolbar_delete)!!
  private val allNotifiques: Query<Notifique>
  private val dataSourceFactory: QueryDataSourceFactory<Notifique>
  private lateinit var liveData: LiveData<PagedList<Notifique>>
  private val observer: Observer<PagedList<Notifique>>
  private val listAdapter: Adapter
  private val selectionTracker: SelectionTracker<Long>
  private val swipeBackground = ColorDrawable(context.getColor(R.color.list_item_swipe_background))
  // Consider "now" check from time of this list view's creation.
  private val dateFormatter = DateFormatter(
      TimeZone.getDefault(),
      resources.configuration.primaryLocale,
      DateFormat.is24HourFormat(context)
  )
  private lateinit var scope: CoroutineScope
  private var savedState: Parcelable? = null

  interface OnSelectionStateChangedListener {
    fun onSelectionStateChanged(selected: Boolean)
  }

  fun deleteSelected() {
    val selection = MutableSelection<Long>().also {
      selectionTracker.copySelection(it)
    }
    GlobalScope.launch {
      for (id in selection.iterator()) {
        notifiqueQueries.delete(id)
      }
      withContext(Dispatchers.Main) {
        selectionTracker.clearSelection()
      }
    }
  }

  fun deselectSelected() {
    selectionTracker.clearSelection()
  }

  init {
    context.appComponent.inject(this)

    val inflater = LayoutInflater.from(context)
    layoutManager = LinearLayoutManager(context)
    listAdapter = Adapter(inflater, dateFormatter).apply {
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
    adapter = listAdapter
    addItemDecoration(DividerItemDecoration(context.getDrawable(R.drawable.divider)!!))

    selectionTracker = SelectionTracker.Builder(
        "list-selection-id",
        this,
        object : ItemKeyProvider<Long>(SCOPE_MAPPED) {
          override fun getKey(position: Int): Long? {
            return listAdapter.getNotifiqueId(position)
          }

          override fun getPosition(key: Long): Int {
            val index = listAdapter.currentList!!.indexOfFirst {
              // Notifique object is null during deletion.
              it != null && key == it.id
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
                return listAdapter.getNotifiqueId(position)
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

    ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, LEFT or RIGHT) {
      override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder
      ): Int {
        if (listAdapter.getNotifiqueId(viewHolder.adapterPosition) == null) {
          // Placeholder.
          return 0
        }
        return LEFT or RIGHT
      }

      override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        target: ViewHolder
      ): Boolean {
        return false
      }

      override fun onSwiped(
        viewHolder: ViewHolder,
        direction: Int
      ) {
        GlobalScope.launch {
          notifiqueQueries.delete(listAdapter.getNotifiqueId(viewHolder.adapterPosition)!!)
        }
      }

      override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
      ) {
        val itemView = viewHolder.itemView
        val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2
        if (dX > 0) {
          swipeBackground.setBounds(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
          deleteIcon.setBounds(itemView.left + (iconMargin/2), itemView.top + iconMargin, itemView.left + (iconMargin/2) + deleteIcon.intrinsicWidth, itemView.bottom - iconMargin)
        } else {
          swipeBackground.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
          deleteIcon.setBounds(itemView.right - (iconMargin/2) - deleteIcon.intrinsicWidth, itemView.top + iconMargin, itemView.right - (iconMargin/2), itemView.bottom - iconMargin)
        }
        swipeBackground.draw(c)
        if (dX > 0) {
          c.clipRect(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
        } else {
          c.clipRect(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
        }
        deleteIcon.draw(c)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
      }
    }).apply {
      attachToRecyclerView(this@NotifiqueListView)
    }

    observer = Observer {
      listAdapter.submitList(it)
      // Hack to restore position after first load.
      if (savedState != null) {
        (savedState as Bundle).apply {
          super.onRestoreInstanceState(getParcelable("savedState"))
          selectionTracker.onRestoreInstanceState(this)
        }
        savedState = null
      }
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    scope = MainScope()
    liveData = LivePagedListBuilder(
        dataSourceFactory,
        PagedList.Config.Builder()
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
    super.onRestoreInstanceState((state as Bundle).getParcelable("savedState"))
    savedState = state
  }

  private val Configuration.primaryLocale: Locale
    get() = if (SDK_INT >= 24) {
      locales[0]!!
    } else {
      @Suppress("Deprecation") locale
    }

  private class ItemView(
    context: Context,
    attributeSet: AttributeSet
  ) : LinearLayout(context, attributeSet) {
    private val appName: TextView
    private val timestamp: TextView
    private val title: TextView
    private val message: TextView
    private val appPicture: ImageView
    private val date = Date()

    init {
      orientation = VERTICAL
      foreground = context.getDrawable(R.drawable.item_view_foreground)
      val inflater = LayoutInflater.from(context)
      inflater.inflate(R.layout.list_item_children, this, true)
      appName = findViewById(R.id.app_name)
      timestamp = findViewById(R.id.timestamp)
      title = findViewById(R.id.title)
      message = findViewById(R.id.message)
      appPicture = findViewById(R.id.icon_picture)
    }

    fun setNotifique(
      notifique: Notifique,
      dateFormatter: DateFormatter
    ) {
      appName.visibility = VISIBLE
      timestamp.visibility = VISIBLE
      title.visibility = VISIBLE
      message.visibility = VISIBLE
      appPicture.visibility = VISIBLE
      appName.text = notifique.app
      timestamp.text = dateFormatter.format(date.apply { time = notifique.timestamp })
      title.text = notifique.title
      message.text = notifique.message
      try {
        appPicture.setImageDrawable(context.packageManager.getApplicationIcon(notifique.package_))
      } catch (e: PackageManager.NameNotFoundException) {
        appPicture.setImageResource(R.drawable.toolbar_delete)
      }
    }

    fun setPlaceholder() {
      appName.visibility = INVISIBLE
      timestamp.visibility = INVISIBLE
      title.visibility = INVISIBLE
      message.visibility = INVISIBLE
      appPicture.visibility = INVISIBLE
    }
  }

  private class Adapter(
    private val inflater: LayoutInflater,
    private val dateFormatter: DateFormatter
  ) :
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

    fun getNotifiqueId(position: Int): Long? {
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
        holder.root.setNotifique(notifique, dateFormatter)
        holder.root.isSelected = selectionTracker.isSelected(notifique.id)
      }
    }

    override fun onBindViewHolder(
      holder: ViewHolder,
      position: Int,
      payloads: List<Any>
    ) {
      when {
        payloads.isEmpty() -> {
          onBindViewHolder(holder, position)
        }
        payloads.containsOnly(SelectionTracker.SELECTION_CHANGED_MARKER) -> {
          val notifique = getItem(position)!!
          holder.root.isSelected = selectionTracker.isSelected(notifique.id)
        }
        else -> {
          throw IllegalStateException("Unhandled payloads: $payloads")
        }
      }
    }

    private fun List<Any>.containsOnly(element: Any): Boolean {
      for (i in indices) {
        if (this[i] != element) {
          return false
        }
      }
      return true
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

/**
 * Copied from [androidx.recyclerview.widget.DividerItemDecoration]
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
      val bottom = bounds.bottom + child.translationY.roundToInt()
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
