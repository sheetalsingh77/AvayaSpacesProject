package com.avayaspacesproject.ui.conference.tasks

import android.content.Context
import androidx.annotation.NonNull
import androidx.lifecycle.ViewModel
import com.avaya.spacescsdk.services.messaging.listeners.MessageListener
import com.avaya.spacescsdk.services.messaging.listeners.MessageModifiedListener
import com.avaya.spacescsdk.services.messaging.listeners.PagingListener
import com.avaya.spacescsdk.services.spaces.Spaces
import com.avaya.spacescsdk.services.spaces.listeners.SpacesAPITopicReadyListener
import com.avaya.spacescsdk.services.spaces.model.SpacesTopic
import com.avaya.spacescsdk.services.user.SpacesUser
import com.avayaspacesproject.ui.conference.TaskPostViewModel
import com.avayaspacesproject.ui.conference.chat.ChatViewModel
import io.zang.spaces.api.LoganMessage
import javax.inject.Inject

class TaskViewModel @Inject constructor(
    private val spacesUser: SpacesUser,
    private val spaces: Spaces
) : TaskPostViewModel(spacesUser,spaces) {

//    fun getUserIdentity(): String {
//        return spacesUser.getUserIdentity()
//    }
//
//    fun getTopicId(): String? {
//        return spaces.getTopicId()
//    }


}