package com.avayaspacesproject.ui.conference.members

import android.content.Context
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.avaya.spacescsdk.services.call.model.Member
import com.avayaspacesproject.R

internal class TopicMemberPopupMenu(context: Context, anchor: View, val member: Member, val listener: TopicMemberMenuListener) : PopupMenu(context, anchor) {
    init {
        getMenuInflater().inflate(R.menu.menu_member, menu)

        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_allow_unmute -> {
                    listener.onMemberAllowMute(member)
                    return@setOnMenuItemClickListener true
                }
                R.id.item_remove -> {
                    listener.onMemberRemove(member)
                    return@setOnMenuItemClickListener true
                }
                R.id.item_leave -> {
                    listener.onMemberLeave(member)
                    return@setOnMenuItemClickListener true
                }
                else -> false
            }
        }
    }

    fun isForMemberId(memberId: String): Boolean =
            member.identity == memberId

    fun updateMenuOptions(itsMe: Boolean, canChat: Boolean, canMakeAdmin: Boolean, canMakeMember: Boolean, canRemove: Boolean, canLeave: Boolean, canLowerHand: Boolean, canRaiseHand: Boolean, canAllowUnmute: Boolean) {
        menu.findItem(R.id.item_allow_unmute).isVisible = canAllowUnmute
        menu.findItem(R.id.item_remove).isVisible = canRemove
        menu.findItem(R.id.item_leave).isVisible = canLeave
        val showItsMe = itsMe && !canAllowUnmute && !canRaiseHand && !canLowerHand && !canMakeAdmin && !canMakeMember && !canChat && !canLeave
        menu.findItem(R.id.item_its_me).isVisible = showItsMe
    }
}

interface TopicMemberMenuListener {
    fun onMemberAllowMute(member: Member)
    fun onMemberRaiseHand(member: Member)
    fun onMemberLowerHand(member: Member)
    fun onMemberMakeAdmin(member: Member)
    fun onMemberMakeMember(member: Member)
    fun onMemberChat(member: Member)
    fun onMemberRemove(member: Member)
    fun onMemberLeave(member: Member)
}
