package com.avayaspacesproject.ui.conference.members

import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.avayaspacesproject.R
import com.avayaspacesproject.databinding.ListItemMemberBinding
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.avaya.spacescsdk.services.call.model.MemberAudioState
import com.avaya.spacescsdk.services.call.model.MemberVideoState
import com.avayaspacesproject.ReferenceApp.Companion.context
import com.bumptech.glide.Glide

internal class MembersListAdapter(
    diffCallback: DiffUtil.ItemCallback<MemberModel>,
    private val listener: MembersFragmentListener
) : ListAdapter<MemberModel, MemberViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder =
        MemberViewHolder(parent.createMemberView(parent), listener)

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bindTo(getItem(position)!!)
    }
}

private val ViewGroup.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(context)


private fun ViewGroup.createMemberView(parent: ViewGroup): ListItemMemberBinding {
    return ListItemMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
}

data class MemberViewHolder(
    private val itemBinding: ListItemMemberBinding,
    private val listener: MembersFragmentListener
) : RecyclerView.ViewHolder(itemBinding.root) {

    override fun toString(): String = "${super.toString()} '${itemBinding.userName.text}'"

    fun bindTo(model: MemberModel) {
        val member = model.member
        val online = model.isOnline
        val alpha = if (online) 1.0f else 0.5f

        itemBinding.memberItemView.setBackgroundColor(
            if (model.audioState == MemberAudioState.AUDIO_STATE_RAISED_HAND) {
                ContextCompat.getColor(
                    itemBinding.memberItemView.context,
                    R.color.member_raised_hand_bg
                )
            } else {
                0
            }
        )
        itemBinding.memberItemView.setOnClickListener { listener.onMemberSelected(member) }

        itemBinding.moreIcon.visibility = VISIBLE
        itemBinding.moreIcon.setOnClickListener { v: View -> listener.onMemberActions(member, v) }

        itemBinding.onlineGreenDot.visibility = if (online) VISIBLE else View.GONE

        itemBinding.userName.alpha = alpha
        itemBinding.userName.text = model.displayName

        itemBinding.email.alpha = alpha
        itemBinding.email.text = model.subtitle(itemBinding.root.context.resources)

        itemBinding.screenShare.visibility =
            if (model.isMediaSessionScreenshare) VISIBLE else View.GONE

        val memberVideoState = model.videoState
        if (model.isMe && member.mediaSessionConnected && !model.isVideoLicensed) {
            itemBinding.videoState.visibility = VISIBLE
            itemBinding.videoState.setImageResource(R.drawable.ic_video_off)
            itemBinding.videoState.isEnabled = false
        } else {
            itemBinding.videoState.visibility =
                if (memberVideoState === MemberVideoState.VIDEO_STATE_NONE) View.GONE else VISIBLE
            itemBinding.videoState.setImageResource(if (memberVideoState === MemberVideoState.VIDEO_STATE_MUTE) R.drawable.ic_video_off else R.drawable.ic_video_on)
            itemBinding.videoState.isEnabled = true
        }
        itemBinding.videoState.setOnClickListener { listener.onMemberVideoToggled(member) }


        itemBinding.audioState.visibility =
            if (model.audioState === MemberAudioState.AUDIO_STATE_NONE) View.GONE else VISIBLE

        when (model.audioState) {
            MemberAudioState.AUDIO_STATE_ACTIVE_TALKER, MemberAudioState.AUDIO_STATE_NOISY -> {
                AnimatedVectorDrawableCompat
                    .create(itemBinding.root.context, model.audioStateImageResource)
                    ?.let {
                        itemBinding.audioState.setImageDrawable(it)
                        it.start()
                    }
            }
            else -> itemBinding.audioState.setImageResource(model.audioStateImageResource)
        }

        itemBinding.audioState.setOnClickListener {
            if (model.audioState !== MemberAudioState.AUDIO_STATE_COLLAB_ONLY) {
                listener.onMemberAudioToggled(member)
            }
        }

        itemBinding.userpic.alpha = alpha
        Glide.with(itemBinding.root.context)
            .load(model.pictureUrl)
            .error(R.drawable.ic_baseline_person_24)
            .circleCrop()
            .into(itemBinding.userpic)
    }
}

private val MemberModel.audioStateImageResource: Int
    @DrawableRes
    get() = when (audioState) {
        MemberAudioState.AUDIO_STATE_RAISED_HAND -> R.drawable.ic_raise_hand
        MemberAudioState.AUDIO_STATE_MUTE -> R.drawable.ic_mic_muted
        MemberAudioState.AUDIO_STATE_MUTE_LOCK ->  R.drawable.ic_audio_muted_lock_24dp
        MemberAudioState.AUDIO_STATE_ACTIVE_TALKER -> R.drawable.ic_active_speaker_24dp_anim_blink
        MemberAudioState.AUDIO_STATE_NOISY -> R.drawable.ic_background_noise_24dp_anim_blink
        MemberAudioState.AUDIO_STATE_COLLAB_ONLY -> R.drawable.ic_screen_share_on
        MemberAudioState.AUDIO_STATE_NORMAL -> if (isMediaSessionPhone) R.drawable.ic_phone else R.drawable.ic_mic_unmuted
        else -> if (isMediaSessionPhone) R.drawable.ic_phone else R.drawable.ic_mic_unmuted
    }