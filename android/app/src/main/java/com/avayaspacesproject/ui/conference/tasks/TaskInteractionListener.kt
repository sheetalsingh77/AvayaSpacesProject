package com.avayaspacesproject.ui.conference.tasks

import android.view.View
import io.zang.spaces.api.LoganMessage

interface TaskInteractionListener {
    fun onTaskSelected(message: LoganMessage, v: View)
    fun onTaskActions(message: LoganMessage, v: View)
}