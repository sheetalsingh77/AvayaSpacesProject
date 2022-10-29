package com.avayaspacesproject.ui.events

import com.avaya.spacescsdk.services.call.model.ExternalIntentAction
import com.avayaspacesproject.BuildConfig

//ToDo Need to add id to toString method
data class TopicFetchedEvent(
    val action: ExternalIntentAction,
    val topicTitle: String,
    val password: String?,
    val id: String?
) : SingleLiveEvent() {

    override fun toString(): String =
        StringBuilder(128).apply {
            append(this@TopicFetchedEvent.javaClass.simpleName)
            append('{')
            append(action)
            if (BuildConfig.DEBUG) {
                append(" \"")
                append(topicTitle)
                append('"')
            }
            if (password != null) {
                append(" has password")
            }
            append('}')
        }.toString()
}
