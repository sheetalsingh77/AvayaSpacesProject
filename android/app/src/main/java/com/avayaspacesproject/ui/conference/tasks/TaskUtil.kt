package io.zang.spaces.util

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.avayaspacesproject.R
import io.zang.spaces.api.LoganMessage
import util.DateWorks
import java.util.*

object TaskUtil {
    @JvmStatic
    fun isTaskOverdue(task: LoganMessage?, today: Boolean): Boolean {
        if (task == null) {
            return false
        }
        val completed = isTaskCompleted(task)
        return !today && !completed && task.contentDueDate != null && task.contentDueDate.time < System.currentTimeMillis()
    }

    @JvmStatic
    fun isTaskCompleted(task: LoganMessage?): Boolean {
        if (task == null) {
            return false
        }
        return LoganMessage.LM_TASK_STA_COMPLETED == task.contentStatus
    }

    @JvmStatic
    fun isTaskDueToday(task: LoganMessage?): Boolean {
        if (task == null) {
            return false
        }

        val completed = isTaskCompleted(task)
        if (!completed && task.contentDueDate != null) {
            val cal = Calendar.getInstance()
            cal.time = task.contentDueDate
            return DateWorks.today(cal)
        }
        return false
    }

    @JvmStatic
    @ColorRes
    fun getTaskStatusColor(completed: Boolean, overdue: Boolean, today: Boolean): Int {
        return when {
            completed -> {
                R.color.task_complete
            }
            overdue -> {
                R.color.task_overdue
            }
            today -> {
                R.color.task_today
            }
            else -> R.color.task_normal
        }
    }

    @JvmStatic
    @DrawableRes
    fun getTaskDrawable(task: LoganMessage?): Int {
        if (task == null) {
            R.drawable.ic_circle_ring
        }
        val completed = isTaskCompleted(task)
        return when {
            completed -> {
                R.drawable.ic_check_circle_black
            }
            else -> {
                R.drawable.ic_circle_ring
            }
        }
    }

    @JvmStatic
    @ColorRes
    fun getTaskIndicatorColor(completed: Boolean, today: Boolean, overdue: Boolean): Int {
        return when {
            completed -> {
                R.color.task_complete
            }
            today -> {
                R.color.task_today
            }
            overdue -> {
                R.color.task_overdue
            }
            else -> {
                R.color.color_background
            }
        }
    }
}