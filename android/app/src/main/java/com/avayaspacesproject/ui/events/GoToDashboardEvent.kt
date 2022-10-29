package com.avayaspacesproject.ui.events

import com.avaya.spacescsdk.services.call.model.ExternalIntentAction

data class GoToDashboardEvent(
    val action: ExternalIntentAction,

    ) : SingleLiveEvent() {
    override fun toString(): String {
        return "SignInSucceededEvent(action=$action)"
    }

}