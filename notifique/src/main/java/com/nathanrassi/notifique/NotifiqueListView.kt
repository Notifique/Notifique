package com.nathanrassi.notifique

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import android.content.Context
import android.graphics.Rect
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import javax.inject.Inject

internal class NotifiqueListView(
  context: Context,
  attributeSet: AttributeSet
) : RecyclerView(context, attributeSet) {
  @Inject internal lateinit var dao: Notifique.Dao
  private val listAdapter: Adapter
  private lateinit var liveData: LiveData<PagedList<Notifique>>
  private lateinit var observer: Observer<PagedList<Notifique>>

  init {
    context.appComponent.inject(this)
    val inflater = LayoutInflater.from(context)
    layoutManager = LinearLayoutManager(context)
    listAdapter = Adapter(inflater)
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
    liveData = LivePagedListBuilder(dao.sourceFactory(), 20).build()
    observer = Observer { listAdapter.submitList(it) }
    liveData.observeForever(observer)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    liveData.removeObserver(observer)
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
    }

    internal fun setNotifique(notifique: Notifique) {
      appName.text = notifique.notifPackage
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

      override fun areContentsTheSame(
        oldItem: Notifique,
        newItem: Notifique
      ) = oldItem == newItem
    }
  }
}
