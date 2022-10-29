package com.avayaspacesproject.ui.conference

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.avayaspacesproject.R

class EndMeetingDialogFragment : DialogFragment() {

    private var listener: EndMeetingDialogListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.end_meeting_dialog_message))
            .setPositiveButton(R.string.ok) { _, _ -> listener?.onEndMeetingConfirmed() }
            .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            .create()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        listener = try {
            context as EndMeetingDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException("$context must implement EndMeetingDialogListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        const val TAG: String = "EndMeetingDialogFragment"

        @JvmStatic
        fun newInstance(): EndMeetingDialogFragment =
            EndMeetingDialogFragment()
    }
}

@FunctionalInterface
interface EndMeetingDialogListener {
    fun onEndMeetingConfirmed()
}
