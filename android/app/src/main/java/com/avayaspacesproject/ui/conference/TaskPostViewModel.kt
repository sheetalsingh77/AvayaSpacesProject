package com.avayaspacesproject.ui.conference

import android.content.Context
import androidx.lifecycle.ViewModel
import com.avaya.spacescsdk.services.messaging.listeners.MessageListener
import com.avaya.spacescsdk.services.messaging.listeners.MessageModifiedListener
import com.avaya.spacescsdk.services.messaging.listeners.PagingListener
import com.avaya.spacescsdk.services.spaces.Spaces
import com.avaya.spacescsdk.services.spaces.listeners.SpacesAPITopicReadyListener
import com.avaya.spacescsdk.services.spaces.model.SpacesTopic
import com.avaya.spacescsdk.services.user.SpacesUser
import io.zang.spaces.api.LoganMessage
import javax.inject.Inject

open class TaskPostViewModel  @Inject constructor(
    private val spacesUser: SpacesUser,
    private val spaces: Spaces
) : ViewModel(){

    val messagingService = spacesUser.getMessagingService()

    fun getCurrentTopic(): SpacesTopic? {

        return spaces.getCurrentTopic()
    }

    fun getUserIdentity(): String {
        return spacesUser.getUserIdentity()
    }

    fun getTopicId(): String? {
        return spaces.getTopicId()
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

    fun addPagingListener(listener: PagingListener) {
        messagingService.addPagingListener(listener)
    }

    fun removePagingListener(listener: PagingListener) {
        messagingService.removePagingListener(listener)

    }

    fun topicCanRemoveMessage(topic: SpacesTopic, message: LoganMessage): Boolean {
        return messagingService.topicCanRemoveMessage(topic, message)
    }

    fun deleteMessage(message: LoganMessage) {
        messagingService.deleteMessage(message)
    }

    fun copyMessageToClipboard(context: Context, message: LoganMessage){
        messagingService.copySelectedMessageToClipboard(context, message)
    }
}