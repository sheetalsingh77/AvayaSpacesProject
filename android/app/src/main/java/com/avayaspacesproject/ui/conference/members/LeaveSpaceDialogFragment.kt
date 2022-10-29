package com.avayaspacesproject.ui.conference.members

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.avaya.spacescsdk.utils.EXTRA_TOPIC_ID
import com.avaya.spacescsdk.utils.EXTRA_TOPIC_TITLE
import com.avayaspacesproject.R



class LeaveSpaceDialogFragment : DialogFragment() {

    private var listener: LeaveSpaceDialogListener? = null
    private var topicId: String? = null
    private var topicTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        topicId = arguments?.getString(EXTRA_TOPIC_ID)
        topicTitle = arguments?.getString(EXTRA_TOPIC_TITLE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.leave_space_dialog_message, topicTitle))
                    .setPositiveButton(R.string.leave_space) { _, _ -> listener?.onLeaveSpaceConfirmed(topicId) }
                    .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
                    .create()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        listener = try {
            context as LeaveSpaceDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException("$context must implement LeaveSpaceDialogListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        const val TAG: String = "LeaveSpaceDialogFragment"

        @JvmStatic
        fun newInstance(topicId: String, topicTitle: String?): LeaveSpaceDialogFragment =
                LeaveSpaceDialogFragment().apply {
                    arguments = bundleOf(
                            EXTRA_TOPIC_ID to topicId,
                            EXTRA_TOPIC_TITLE to topicTitle)
                }
    }
}

@FunctionalInterface
interface LeaveSpaceDialogListener {
    fun onLeaveSpaceConfirmed(topicId: String?)
}
