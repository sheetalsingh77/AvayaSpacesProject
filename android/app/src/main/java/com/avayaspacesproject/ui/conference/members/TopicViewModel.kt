package com.avayaspacesproject.ui.conference.members

import androidx.annotation.NonNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.avaya.spacescsdk.services.call.listeners.MeetingAttendeeListener
import com.avaya.spacescsdk.services.call.model.Member
import com.avaya.spacescsdk.services.spaces.Spaces
import com.avaya.spacescsdk.services.spaces.listeners.SpacesAPITopicReadyListener
import com.avaya.spacescsdk.services.spaces.listeners.TopicListener
import com.avaya.spacescsdk.services.spaces.model.SpacesTopic
import com.avaya.spacescsdk.services.user.SpacesUser
import com.avaya.spacescsdk.services.user.listeners.SpacesAPIUserOnlineListener
import com.avaya.spacescsdk.services.user.model.SpacesUserInfo
import com.avaya.spacescsdk.utils.objectIdentity
import javax.inject.Inject

open class TopicViewModel @Inject constructor(
    private val spacesUser: SpacesUser,
    private val spaces: Spaces
) : ViewModel() {

    private val logTag: String = "TopicViewModel"

    open val _topicLiveData = MutableLiveData<SpacesTopic?>()
    open val topicLiveData: LiveData<SpacesTopic?> = _topicLiveData

    var topic: SpacesTopic?
        get() = topicLiveData.value
        set(value) {
            if (value != topicLiveData.value) {
                _topicLiveData.postValue(value)
            }

        }

    fun getCurrentTopic():SpacesTopic?{

        return spaces.getCurrentTopic()
    }
    val hasTopic: Boolean
        get() = topicLiveData.value != null

    fun matchesTopicId(id: String?): Boolean {
        val maybeTopic = topicLiveData.value
        return (maybeTopic != null) && (maybeTopic.topicId == id)
    }

    val topicStateSummary: String
        get() = when (val actualTopic = topicLiveData.value) {
            null -> "no topic"
            else -> actualTopic.summary()
        }
    override fun toString(): String =
        "$objectIdentity ($topicStateSummary)"


    fun addUserOnlineListener(spacesAPIUserOnlineListener: SpacesAPIUserOnlineListener) {
        spaces.addUserOnlineListener(spacesAPIUserOnlineListener)
    }

    fun addTopicListener(topicListener: TopicListener) {
        spaces.addTopicListener(topicListener)
    }

    fun addTopicReadyListener(topicReadyListener: SpacesAPITopicReadyListener) {
        spaces.addTopicReadyListener(topicReadyListener)
    }

    fun addMeetingAttendeeListener(addMeetingAttendeeListener: MeetingAttendeeListener){
        spaces.addMeetingAttendeeListener(addMeetingAttendeeListener)
    }

    fun removeUserOnlineListener(spacesAPIUserOnlineListener: SpacesAPIUserOnlineListener) {
        spaces.removeUserOnlineListener(spacesAPIUserOnlineListener)
    }

    fun removeTopicListener(topicListener: TopicListener) {
        spaces.removeTopicListener(topicListener)
    }

    fun removeTopicReadyListener(topicReadyListener: SpacesAPITopicReadyListener) {
        spaces.removeTopicReadyListener(topicReadyListener)
    }

    fun removeMeetingAttendeeListener(meetingAttendeeListener: MeetingAttendeeListener){
        spaces.removeMeetingAttendeeListener(meetingAttendeeListener)
    }

    fun requestAttendeeToggleAudioUnmute(@NonNull topicId:String, @NonNull member: Member){
        spaces.requestAttendeeToggleAudioUnmute(topicId,member)
    }

    fun requestAttendeeAudioMute(@NonNull topicId:String, @NonNull member: Member){
        spaces.requestAttendeeAudioMute(topicId,member)
    }

    fun currentTopicCanRemoveMember(member: Member):Boolean{
        if (member == null) {
            return false
        }

        return topic != null && topic!!.canRemoveMember(member)
    }

    fun currentTopicCanQuit(): Boolean {
        return topic != null && topic!!.canQuit()
    }

    fun topicById(topicId: String): SpacesTopic? {
        return spaces.topicById(topicId)
    }

    fun topicRemoveUser(@NonNull topic:SpacesTopic, @NonNull user: SpacesUserInfo){
        spaces.topicRemoveUser(topic,user)
    }

}