package com.avayaspacesproject.ui.conference.chat

import android.view.View
import io.zang.spaces.api.LoganMessage

interface MessageFragmentListener {
    fun onMessageSelected(message: LoganMessage?, v: View?) {}
    fun onMessageActions(message: LoganMessage?, v: View?) {}
}