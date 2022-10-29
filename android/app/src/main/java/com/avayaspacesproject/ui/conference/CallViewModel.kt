package com.avayaspacesproject.ui.conference

import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.avaya.spacescsdk.services.call.CallService
import com.avaya.spacescsdk.services.call.Conference
import com.avaya.spacescsdk.services.call.model.JoinMeetingOptions
import com.avaya.spacescsdk.services.spaces.Spaces
import com.avaya.spacescsdk.services.spaces.listeners.SpacesAPIGetTopicListener
import com.avaya.spacescsdk.services.spaces.model.SpacesTopic
import com.avaya.spacescsdk.services.user.SpacesUser
import com.avaya.spacescsdk.services.user.model.SpacesUserInfo
import com.avayaspacesproject.ui.conference.members.TopicViewModel
import javax.inject.Inject

class CallViewModel @Inject constructor(
    private val spacesUser: SpacesUser,
    private val spaces: Spaces
) : TopicViewModel(spacesUser, spaces) {

    private val logTag: String = "CallViewModel"

    val conferenceLiveData: MutableLiveData<Conference?> = MutableLiveData()

    var conference: Conference? = null
        get() = conferenceLiveData.value

    @UiThread
    fun clearConference() {
        conferenceLiveData.value = null
    }


    val hasConference: Boolean
        @JvmName("hasConference")
        get() = conference != null



    val startingAnimationShownLiveData: MutableLiveData<Boolean> = MutableLiveData()

    val startingAnimationShown: Boolean
        get() {
            val value = startingAnimationShownLiveData.value
            return value != null && value
        }

    fun subscribeTopicById(topicID: String) {
        spaces.subscribeTopicById(topicID, object : SpacesAPIGetTopicListener {
            override fun onTopicFetched(topic: SpacesTopic) {
                _topicLiveData.postValue(topic)
            }
        })

    }

    fun joinMeetingForTopic(topic: SpacesTopic, meetingOptions: JoinMeetingOptions){
        val callService: CallService = spacesUser.getCallService()
        val conferenceTemp = callService.getCallForTopic(topic, meetingOptions)
        Log.d(logTag, "Conference:${conferenceTemp}")
        conferenceLiveData.postValue(conferenceTemp)

    }

    fun getUserInfo(): SpacesUserInfo {
        return spacesUser.getUserInfo()
    }

    fun getUserIdentity(): String {
        return spacesUser.getUserIdentity()
    }

    fun isGuestUser(): Boolean {
        return spacesUser.isGuestUser()
    }

    fun logoutUser() {
        spacesUser.signOut()
    }
    fun onSignoutConfirmed(){
        spacesUser.signOut()
    }



}