package com.avayaspacesproject.ui.conference.chat

import android.content.Context
import android.os.Build.ID
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.avaya.spacescsdk.services.messaging.enums.MessageRealm
import com.avaya.spacescsdk.services.messaging.listeners.MessageListener
import com.avaya.spacescsdk.services.messaging.listeners.MessageModifiedListener
import com.avaya.spacescsdk.services.messaging.listeners.PagingListener
import com.avaya.spacescsdk.services.messaging.listeners.SendMessageListener
import com.avaya.spacescsdk.services.messaging.model.AttachmentToUpload

import com.avaya.spacescsdk.services.spaces.listeners.SpacesAPITopicReadyListener
import com.avaya.spacescsdk.services.spaces.listeners.TopicListener
import com.avaya.spacescsdk.services.spaces.model.SpacesTopic
import com.avaya.spacescsdk.services.user.SpacesUser
import com.avaya.spacescsdk.utils.UcLog
import io.zang.spaces.api.LoganMessage
import javax.inject.Inject

class ChatViewModel @Inject constructor(private val spacesUser: SpacesUser) : ViewModel() {
    val hasScrolled = MutableLiveData(false)
    val spaces = spacesUser.getSpaces()
    val messagingService = spacesUser.getMessagingService()
    fun sendMessage(topic: SpacesTopic?, text: String, attachment: AttachmentToUpload?) {
        messagingService.sendMessageWithAttachment(
            topic!!,
            text, null,
            attachment,
            object : SendMessageListener {
                override fun onMessageSent() {

                }

                override fun onFileUploadFailed() {
                    UcLog.w(ID, "File uploading failed during sending message with attachment")
                }
            })
    }

    fun addTopicReadyListener(topicReadyListener: SpacesAPITopicReadyListener) {
        spaces.addTopicReadyListener(topicReadyListener)
    }

    fun removeTopicReadyListener(topicReadyListener: SpacesAPITopicReadyListener) {
        spaces.removeTopicReadyListener(topicReadyListener)

    }

    fun addMessageListener(spacesAPIMessageListener: MessageListener) {
        messagingService.addMessageListener(spacesAPIMessageListener)
    }

    fun addMessageModifiedListener(messageModifiedListener: MessageModifiedListener) {
        messagingService.addMessageModifiedListener(messageModifiedListener)
    }

    fun removeMessageListener(spacesAPIMessageListener: MessageListener) {
        messagingService.removeMessageListener(spacesAPIMessageListener)
    }

    fun removeMessageModifiedListener(messageModifiedListener: MessageModifiedListener) {
        messagingService.removeMessageModifiedListener(messageModifiedListener)
    }

    fun getCurrentTopic(): SpacesTopic? {
        return spaces.getCurrentTopic()
    }

    fun getTopicById(topicId: String?): SpacesTopic? {
        return spaces.topicById(topicId!!)
    }

    fun pageTopicMessages(spacesAPIRealm: MessageRealm, topic: SpacesTopic) {
        messagingService.getNextPage(spacesAPIRealm, topic)
    }

    fun addTopicListener(topicListener: TopicListener) {
        spaces.addTopicListener(topicListener)
    }

    fun addPagingListener(listener: PagingListener) {
        messagingService.addPagingListener(listener)
    }

    fun removeTopicListener(listener: TopicListener) {
        spaces.removeTopicListener(listener)
    }

    fun removePagingListener(listener: PagingListener) {
        messagingService.removePagingListener(listener)

    }

    fun topicById(topicId: String): SpacesTopic? {
        return spaces.topicById(topicId)
    }

    fun copyMessageToClipboard(context: Context, message: LoganMessage) {
        messagingService.copySelectedMessageToClipboard(context,message)
    }

    fun topicCanRemoveMessage(topic: SpacesTopic, message: LoganMessage): Boolean {
        return messagingService.topicCanRemoveMessage(topic, message)
    }

    fun deleteMessage(message: LoganMessage) {
        messagingService.deleteMessage( message)
    }


}
