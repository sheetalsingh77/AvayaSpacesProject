package com.avayaspacesproject.ui.join

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.avaya.spacescsdk.services.call.model.ExternalIntentAction
import com.avayaspacesproject.R
import javax.inject.Inject

class JoinViewModel @Inject constructor() : ViewModel() {
    private val logTag: String = "JoinViewModel"

    private val _loginForm = MutableLiveData<JoinMeetingFormState>()
    val loginFormState: LiveData<JoinMeetingFormState> = _loginForm

    val externalIntentAction = MutableLiveData<ExternalIntentAction>()

    fun loginDataChanged(username: String, meetingUrl: String) {
        if (!isDisplayNameValid(username)) {
            _loginForm.value = DisplayNameError(R.string.invalid_username)
        } else {
            _loginForm.value = JoinMeetingValidData
        }
    }

    private fun isDisplayNameValid(username: String): Boolean = username.isNotBlank()

}

/**
 * Data validation state of the login form.
 */
sealed class JoinMeetingFormState

data class DisplayNameError(
    @StringRes val stringId: Int
) : JoinMeetingFormState()

object JoinMeetingValidData : JoinMeetingFormState()
