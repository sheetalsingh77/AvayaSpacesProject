package com.avayaspacesproject.ui.conference.members

import android.view.View
import com.avaya.spacescsdk.services.call.model.Member

interface MembersFragmentListener {
    fun onMemberSelected(member: Member)
    fun onMemberActions(member: Member, view: View)
    fun onMemberAudioToggled(member: Member)
    fun onMemberVideoToggled(member: Member)
}
