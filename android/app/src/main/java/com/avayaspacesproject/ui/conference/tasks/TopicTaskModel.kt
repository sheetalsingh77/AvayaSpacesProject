package com.avayaspacesproject.ui.conference.tasks

import android.content.Context
import android.text.format.DateFormat
import io.zang.spaces.api.LoganMessage
import io.zang.spaces.util.TaskUtil.isTaskCompleted
import io.zang.spaces.util.TaskUtil.isTaskDueToday
import io.zang.spaces.util.TaskUtil.isTaskOverdue
import java.util.*

class TopicTaskModel(message: LoganMessage, context: Context?) {
    val id: String
    val message: LoganMessage
    val title: CharSequence
    var createdDate: String? = null
    var dueDate: String? = null
    val isCompleted: Boolean
    val isToday: Boolean
    val isOverdue: Boolean

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as TopicTaskModel
        return id == that.id && title == that.title && createdDate == that.createdDate && dueDate == that.dueDate && isCompleted == that.isCompleted && isToday == that.isToday && isOverdue == that.isOverdue
    }

    override fun hashCode(): Int {
        return Objects.hash(id, title, createdDate, dueDate, isCompleted, isToday, isOverdue)
    }

    init {
        id = message.iden
        this.message = message
        title = message.contentBodyTextAsText
        val contentDueDate = message.contentDueDate
        dueDate = if (contentDueDate == null) {
            ""
        } else {
            DateFormat.getDateFormat(context).format(contentDueDate)
        }
        val createdSafe = message.createdSafe()
        createdDate = if (createdSafe == null) {
            ""
        } else {
            DateFormat.getDateFormat(context).format(createdSafe)
        }
        isCompleted = isTaskCompleted(message)
        isToday = isTaskDueToday(message)
        isOverdue = isTaskOverdue(message, isToday)
    }
}