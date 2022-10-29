package com.avayaspacesproject.ui.conference.members

import android.content.res.Resources
import android.graphics.Typeface.ITALIC
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.StyleSpan
import com.avaya.spacescsdk.services.call.model.Member
import com.avaya.spacescsdk.services.call.model.MemberAudioState
import com.avaya.spacescsdk.services.call.model.MemberVideoState
import com.avaya.spacescsdk.services.spaces.model.SpacesTopic
import com.avaya.spacescsdk.services.user.model.SpacesUserInfo
import com.avaya.spacescsdk.utils.UcLog
import com.avayaspacesproject.BuildConfig
import com.avayaspacesproject.R

import java.util.*

data class MemberModel(
    val member: Member,
    val displayName: String,
    val pictureUrl: String?,
    val role: String,
    val audioState: MemberAudioState,
    val videoState: MemberVideoState,
    val isOnline: Boolean,
    val isConnected: Boolean,
    val isMediaSessionPhone: Boolean,
    val isMediaSessionScreenshare: Boolean,
    val isMe: Boolean,
    val isVideoLicensed: Boolean
) {
    val id: String
        get() = member.identity

    val joinTime: Date
        get() = member.joinTime

    fun subtitle(resources: Resources): CharSequence {
        return determineSubtitle(member, resources)
    }
}

fun buildTopicMemberModels(
    topic: SpacesTopic
): List<MemberModel> {
    val members: MutableList<Member> = topic.members
    if (members.isEmpty()) {
        return emptyList()
    }

    val numMembers = members.size
    // Don't log details of the members in release builds
    val membersDetail = if (BuildConfig.DEBUG && (numMembers <= 12)) {
        members.toString()
    } else {
        "$numMembers members"
    }
    UcLog.d("TopicMembers", "Building roster for $topic with $membersDetail")

    val models: MutableList<MemberModel> = ArrayList(numMembers)
    members.mapTo(models) { createTopicMemberModel(it, topic) }
    Collections.sort(models, MEMBER_COMPARATOR)
    return models
}

fun createTopicMemberModel(member: Member, topic: SpacesTopic): MemberModel =
    MemberModel(
        member, member.userInfo.displayName,
        member.userInfo.pictureUrl, member.role, member.audioState, member.videoState,
        member.online, member.mediaSessionConnected, member.mediaSessionPhone,
        member.mediaSessionScreenshare, member.isMe, topic.isVideoLicensed
    )

private fun determineSubtitle(member: Member, resources: Resources): CharSequence {
    val role: String = member.role
    val username: String = member.userInfo.getUsername(resources)
    return if (role == null) username else buildSubtitleWithRole(username, role)
}

private fun SpacesUserInfo.getUsername(resources: Resources): String =
        if (isAnonymous) {
            resources.getString(R.string.anonymous)
        } else {
            username
        }

private fun buildSubtitleWithRole(
    tempSubtitle: String, role: String
): SpannableStringBuilder {
    val sb = SpannableStringBuilder()
    sb.append(tempSubtitle)
    val len = sb.length
    sb.append(" (").append(role).append(")")
    sb.setSpan(StyleSpan(ITALIC), len, sb.length, SPAN_INCLUSIVE_EXCLUSIVE)
    return sb
}

val MEMBER_COMPARATOR: Comparator<MemberModel> = object : Comparator<MemberModel> {
    override fun compare(o1: MemberModel, o2: MemberModel): Int {
        if (o1.isMe) return -1
        if (o2.isMe) return 1
        var result = java.lang.Boolean.compare(o2.isConnected, o1.isConnected)
        if (result == 0) {
            result = java.lang.Boolean.compare(o2.isOnline, o1.isOnline)
            if (result == 0) {
                result = java.lang.Boolean.compare(
                    o2.isMediaSessionScreenshare,
                    o1.isMediaSessionScreenshare
                )
                if (result == 0) {
                    return o1.joinTime.compareTo(o2.joinTime)
                }
            }
        }
        return result
    }
}
