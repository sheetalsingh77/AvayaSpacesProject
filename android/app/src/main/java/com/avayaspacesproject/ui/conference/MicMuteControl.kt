package com.avayaspacesproject.ui.conference

import android.content.Context
import android.util.AttributeSet
import com.avaya.spacescsdk.services.call.ConferenceMuteState
import com.avaya.spacescsdk.services.call.ui.ConferenceButton
import com.avayaspacesproject.R

class MicMuteControl @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConferenceButton(context, attrs, defStyleAttr) {

    fun setMuteState(state: ConferenceMuteState) {
        when (state) {
            ConferenceMuteState.MUTED, ConferenceMuteState.MUTING -> setMuted()
            ConferenceMuteState.UNMUTED, ConferenceMuteState.UNMUTING -> setUnmuted()
//            HAND_LOWERED, RAISING_HAND -> setRaiseHand()
//            HAND_RAISED, LOWERING_HAND -> setHandRaised()
            else -> {}
        }
        isEnabled = !state.transitional
    }

    private fun setMuted() {
        setImageResource(R.drawable.ic_mic_muted)
        setText(R.string.unmute)
        contentDescription = resources.getString(R.string.unmute_audio)
        tag = "mic_muted"
    }

    private fun setUnmuted() {
        setImageResource(R.drawable.ic_mic_unmuted)
        setText(R.string.mute)
        contentDescription = resources.getString(R.string.mute_audio)
        tag = "mic_unmuted"
    }

//    private fun setRaiseHand() {
//        setImageResource(R.drawable.av_control_raise_hand)
//        setText(R.string.raise)
//        contentDescription = resources.getString(R.string.raise_hand)
//        tag = "raise_hand"
//    }
//
//    private fun setHandRaised() {
//        setImageResource(R.drawable.av_control_lower_hand)
//        setText(R.string.lower)
//        contentDescription = resources.getString(R.string.lower_hand)
//        tag = "lower_hand"
//    }
}
