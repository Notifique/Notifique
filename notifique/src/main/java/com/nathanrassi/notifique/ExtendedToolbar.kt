package com.nathanrassi.notifique

import android.content.Context
import android.util.AttributeSet
import android.view.WindowInsets
import androidx.appcompat.widget.Toolbar

private class ExtendedToolbar(
  context: Context,
  attributes: AttributeSet
) : Toolbar(context, attributes) {
  private var extraHeight = 0
  private var initialHeight = 0
  private var initialPadding = 0
  private var initialMeasure = true

  override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
    extraHeight = insets.systemWindowInsetTop
    return super.onApplyWindowInsets(insets)
  }

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int
  ) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    if (initialMeasure) {
      initialHeight = measuredHeight
      initialPadding = paddingTop
      initialMeasure = false
    }
    layoutParams.height = initialHeight + extraHeight
    setPadding(paddingLeft, initialPadding + extraHeight, paddingRight, paddingBottom)
  }
}
