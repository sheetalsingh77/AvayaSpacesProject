package com.avayaspacesproject.ui.conference.tasks

import android.content.res.ColorStateList
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.avayaspacesproject.R
import io.zang.spaces.api.LoganMessage
import io.zang.spaces.util.TaskUtil.getTaskDrawable
import io.zang.spaces.util.TaskUtil.getTaskIndicatorColor
import io.zang.spaces.util.TaskUtil.getTaskStatusColor

class TopicTasksTabAdapter(diffCallback: DiffUtil.ItemCallback<TopicTaskModel>, private val listener: TaskInteractionListener) :
    ListAdapter<TopicTaskModel, TopicTasksTabAdapter.TaskViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_task, parent, false)
        return TaskViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    class TaskViewHolder(
        view: View,
        listener: TaskInteractionListener
    ) :
        RecyclerView.ViewHolder(view) {
        private val overdueMark: View
        private val title: TextView
        private val creationDate: TextView
        private val dueDate: TextView
        private val statusIcon: ImageView
        private val listener: TaskInteractionListener = listener
        fun bindTo(model: TopicTaskModel) {
            val msg: LoganMessage = model.message
            val context = itemView.context
//            itemView.setOnClickListener { v: View? ->
//                listener.onTaskSelected(
//                    msg,
//                    v!!
//                )
//            }
            itemView.setOnLongClickListener { v: View? ->
                listener.onTaskActions(msg, v!!)
                true
            }
            title.setText(model.title)
            val completed: Boolean = model.isCompleted
            val today: Boolean = model.isToday
            val overdue: Boolean = model.isOverdue
            @ColorRes val indicatorColor: Int = getTaskIndicatorColor(completed, today, overdue)
            overdueMark.setBackgroundResource(indicatorColor)
            overdueMark.visibility =
                if (completed || overdue || today) View.VISIBLE else View.INVISIBLE
            dueDate.visibility =
                if (TextUtils.isEmpty(model.dueDate)) View.GONE else View.VISIBLE
            dueDate.text = model.dueDate
            @ColorRes val statusColor: Int = getTaskStatusColor(completed, overdue, today)
            statusIcon.setImageResource(getTaskDrawable(msg))
            statusIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    statusColor
                )
            )
            statusIcon.visibility = View.VISIBLE
            val createdDateLabel: String = model.createdDate!!
            if (TextUtils.isEmpty(createdDateLabel)) {
                creationDate.visibility = View.GONE
            } else {
                creationDate.text = createdDateLabel
                creationDate.visibility = View.VISIBLE
            }
        }

        init {
            overdueMark = view.findViewById(R.id.overdue)
            title = view.findViewById(R.id.title)
            creationDate = view.findViewById(R.id.created_date)
            dueDate = view.findViewById(R.id.due_date)
            statusIcon = view.findViewById(R.id.icon)
        }
    }
}