package com.avayaspacesproject.ui.conference.chat

import android.content.Context
import android.text.format.DateFormat
import com.avayaspacesproject.ui.conference.chat.MessageModel
import io.zang.spaces.api.LoganMessage
import util.DateWorks
import java.util.*

class MessageChatModel(message: LoganMessage, context: Context?) :
    MessageModel {
    private val id: String
    val message: LoganMessage
    private val ours: Boolean
    var pictureUrl: String? = null
    private val showPicture: Boolean
    val senderName: CharSequence
    private val showSenderName: Boolean
    private val createdDate: String
    var modifiedDate: String? = null
    val body: String
    override fun getId(): String {
        return id
    }

    override fun getType(): Int {
        return MessageModel.VIEW_TYPE_MESSAGE
    }

    override fun getCreatedDate(): String {
        return createdDate
    }

    fun canShowSenderName(): Boolean {
        return showSenderName
    }

    fun canShowPicture(): Boolean {
        return showPicture
    }

    override fun isOurs(): Boolean {
        return ours
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as MessageChatModel
        return id == that.id && body == that.body && pictureUrl == that.pictureUrl && showPicture == that.showPicture && senderName == that.senderName && showSenderName == that.showSenderName && createdDate == that.createdDate && modifiedDate == that.modifiedDate && ours == that.ours
    }

    override fun hashCode(): Int {
        return Objects.hash(id, body, senderName, modifiedDate)
    }

    init {
        id = message.iden
        this.message = message
        ours = message.isFromMe
        val user = message.sender
        pictureUrl = if (user != null) {
            user.pictureUrl
        } else {
            ""
        }
        showPicture = message.zShowUserpic
        senderName = message.senderNameSafe()
        showSenderName = message.zShowUsername
        createdDate =
            DateFormat.getLongDateFormat(context).format(message.createdSafe())
        body = message.contentBodyTextAsHtml
        val modifiedSafe = message.modifiedSafe()
        modifiedDate = if (message.zShowTimeOnBottom) {
            DateWorks.formatTime(context, modifiedSafe.time, false)
        } else {
            ""
        }
    }
}