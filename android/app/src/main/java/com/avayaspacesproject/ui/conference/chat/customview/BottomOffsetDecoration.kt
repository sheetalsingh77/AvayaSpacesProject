package com.avayaspacesproject.ui.conference.chat.customview

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class BottomOffsetDecoration(private val offsetPx: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        if (offsetPx <= 0) return
        val itemCount = parent.adapter?.itemCount
        if (itemCount == null || itemCount <= 0) return
        val position = parent.getChildAdapterPosition(view)
        outRect.bottom = if (position == itemCount - 1) offsetPx else 0
    }
}