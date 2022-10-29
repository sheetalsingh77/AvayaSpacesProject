package com.avayaspacesproject.ui.conference.ideas

import android.view.View
import io.zang.spaces.api.LoganMessage

interface PostInteractionListener {
    fun onPostSelected(message: LoganMessage, v: View)
    fun onPostActions(message: LoganMessage, v: View)
}