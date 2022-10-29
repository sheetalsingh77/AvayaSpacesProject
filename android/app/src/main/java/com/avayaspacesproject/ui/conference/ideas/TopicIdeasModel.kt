package com.avayaspacesproject.ui.conference.ideas

import android.content.Context
import android.text.TextUtils
import android.text.format.DateFormat
import io.zang.spaces.api.LoganMessage
import java.util.*

class TopicIdeasModel(message: LoganMessage, context: Context?) {
    val id: String
    val message: LoganMessage
    val title: CharSequence
    val subtitle: CharSequence
    var createdDate: String? = null

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as TopicIdeasModel
        return id == that.id && title == that.title && subtitle == that.subtitle && createdDate == that.createdDate
    }

    override fun hashCode(): Int {
        return Objects.hash(id, title, subtitle, createdDate)
    }

    companion object {
        private fun determineSubtitle(message: LoganMessage): String {
            if (message.sender != null) {
                val displayName = message.sender.displayName
                if (!TextUtils.isEmpty(displayName)) {
                    return displayName
                }
            }
            return ""
        }
    }

    init {
        id = message.iden
        this.message = message
        title = message.contentBodyTextAsText
        val createdSafe = message.createdSafe()
        createdDate = if (createdSafe == null) {
            ""
        } else {
            DateFormat.getDateFormat(context).format(createdSafe)
        }
        val newSubtitle = determineSubtitle(message)
        subtitle = newSubtitle
    }
}