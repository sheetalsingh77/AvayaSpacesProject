package com.avayaspacesproject.ui.conference.members

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.avayaspacesproject.R
import com.avaya.spaces.*
import com.avaya.spacescsdk.utils.EXTRA_MEMBER_ID
import com.avaya.spacescsdk.utils.EXTRA_MEMBER_NAME
import com.avaya.spacescsdk.utils.EXTRA_TOPIC_ID
import com.avaya.spacescsdk.utils.EXTRA_TOPIC_TITLE

class RemoveParticipantDialogFragment : DialogFragment() {

    private var listener: RemoveParticipantDialogListener? = null
    private lateinit var participantName: String
    private lateinit var participantId: String
    private lateinit var topicId: String
    private lateinit var topicTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        participantId = requireArguments().getString(EXTRA_MEMBER_ID)!!
        participantName = requireArguments().getString(EXTRA_MEMBER_NAME)!!
        topicId = requireArguments().getString(EXTRA_TOPIC_ID)!!
        topicTitle = requireArguments().getString(EXTRA_TOPIC_TITLE)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.remove_member_confirmation, participantName, topicTitle))
                    .setPositiveButton(R.string.remove) { _, _ -> listener?.onRemoveParticipantConfirmed(participantId, topicId) }
                    .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
                    .create()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        listener = try {
            context as RemoveParticipantDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException("$context must implement RemoveParticipantDialogListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        const val TAG: String = "RemoveParticipantDialogFragment"

        @JvmStatic
        fun newInstance(participantId: String, participantName: String, topicId: String, topicTitle: String): RemoveParticipantDialogFragment =
                RemoveParticipantDialogFragment().apply {
                    arguments = bundleOf(
                            EXTRA_MEMBER_ID to participantId,
                            EXTRA_MEMBER_NAME to participantName,
                            EXTRA_TOPIC_ID to topicId,
                            EXTRA_TOPIC_TITLE to topicTitle)
                }
    }
}

@FunctionalInterface
interface RemoveParticipantDialogListener {
    fun onRemoveParticipantConfirmed(memberId: String, topicId: String)
}
