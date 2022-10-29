package com.avayaspacesproject.ui.login

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.avaya.spaces.*
import com.avaya.spacescsdk.exceptions.CSDKResponseException
import com.avaya.spacescsdk.services.auth.model.Credential
import com.avaya.spacescsdk.services.auth.model.GuestCredential
import com.avaya.spacescsdk.services.auth.model.OAuthCredential
import com.avaya.spacescsdk.services.auth.model.UsernamePasswordCredential
import com.avaya.spacescsdk.services.spaces.enums.ResponseErrorCode
import com.avaya.spacescsdk.services.user.SpacesUser
import com.avaya.spacescsdk.services.user.model.SpacesUserInfo
import com.avaya.spacescsdk.utils.*
import com.avayaspacesproject.R
import com.avayaspacesproject.ReferenceApp
import com.avayaspacesproject.databinding.ActivityLoginBinding
import com.avayaspacesproject.di.component.ActivityComponent
import com.avayaspacesproject.di.module.ActivityModule
import com.avayaspacesproject.ui.base.BaseActivity
import com.avayaspacesproject.ui.events.GoToDashboardEvent
import com.avayaspacesproject.ui.events.PasswordRequiredEvent
import com.avayaspacesproject.ui.events.SingleLiveEvent
import com.avayaspacesproject.ui.events.TopicFetchedEvent
import com.avayaspacesproject.ui.home.HomeActivity
import com.avayaspacesproject.ui.join.JoinMeetingActivity
import javax.inject.Inject


open class LoginActivity : BaseActivity<ActivityLoginBinding>(), View.OnClickListener {

    companion object {
        var PERMISSION_ALL = 1
        val PERMISSIONS: Array<String> = listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO

        ).toTypedArray()
    }



    private val logTAG: String = "LoginActivity"
    private var joinAsGuestChosen = false

    private lateinit var activityComponent: ActivityComponent

    override fun getViewBinding(): ActivityLoginBinding =
        ActivityLoginBinding.inflate(layoutInflater)

    @Inject
    protected lateinit var spacesUser: SpacesUser

    private lateinit var viewModel: LoginViewModel

    private lateinit var spacesUserInfo: SpacesUserInfo

    private lateinit var meetingUrl: String

    private lateinit var meetingPassword: String


    override fun initializeViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory).get(LoginViewModel::class.java)

    }

    override fun injectDependency() {
        activityComponent = ReferenceApp.appComponent.buildActivityComponent(ActivityModule(this))
        activityComponent.inject(this)
    }

    override fun initializeView() {

//        for (permission in STORAGE_PERMISSIONS_REQUIRED) {
//            val access = ContextCompat.checkSelfPermission(this, permission)
//            if (access != PackageManager.PERMISSION_GRANTED) {
//                UcLog.l(Build.ID, "Requesting permission: $permission")
//                requestPermission(permission, "")
//                return
//            }
//        }

        if (!hasPermissions(this, *PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }else{
            enableView()
        }
//        enableView()
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableView() {
        viewModel.eventLiveData.observe(this, Observer(::onEvent))
        viewModel.responseErrorCode.observe(this, Observer(::handleResponseError))
        viewModel.loginFormState.observe(this, Observer(::handleLoginFormStateChange))
        viewModel.handleError.observe(this, Observer(::handleCSDKException))

        viewModel.loginDataChanged(
            binding.displayName.text.toString(),
            binding.meetingUrl.text.toString()
        )

        binding.meetingUrl.afterTextChanged {
            viewModel.loginDataChanged(
                binding.displayName.text.toString(),
                binding.meetingUrl.text.toString()
            )
        }

        binding.displayName.afterTextChanged {
            viewModel.loginDataChanged(
                binding.displayName.text.toString(),
                binding.meetingUrl.text.toString()
            )
        }
        binding.username.afterTextChanged {
            viewModel.usernamePasswordLoginChanged(binding.username.text.toString(),binding.password.text.toString())
        }

        binding.password.afterTextChanged {
            viewModel.usernamePasswordLoginChanged(binding.username.text.toString(),binding.password.text.toString())
        }

        if (spacesUser.isSessionActive()) {
            toggleSignInView(false)
            toggleMeetingView(true)
        } else if (spacesUser.hasSavedUser()) {
            //If there is a saved user, we do not have the credentials stored.
            performLogin(null)

        } else {
            toggleSignInView(true)
            toggleMeetingView(false)
        }
    }

//    override fun onRequestPermissionResult(
//        permission: String?,
//        tag: Any?,
//        success: Boolean
//    ) {
//        if (tag is String) {
//            if (success) {
//                enableView()
//            } else {
//                UcLog.w(Build.ID, "User has rejected permission")
//            }
//        } else {
//            super.onRequestPermissionResult(permission, tag, success)
//        }
//    }

     override fun onRequestPermissionsResult(
         requestCode: Int,
         permissions: Array<String>,
         grantResults: IntArray
     ) {
         super.onRequestPermissionsResult(requestCode, permissions, grantResults)
         when (requestCode) {
            PERMISSION_ALL -> {

                // When request is cancelled, the results array are empty
                if (grantResults.size > 0 &&
                    ((grantResults[0]
                            + grantResults[1]
                            + grantResults[2])
                            == PackageManager.PERMISSION_GRANTED)
                ) {
                    // Permissions are granted
                    Toast.makeText(this, "Permissions granted.", Toast.LENGTH_SHORT).show()
                    enableView()
                } else {
                    // Permissions are denied
                    Toast.makeText(this, "Permissions denied.", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }
    private fun handleCSDKException(csdkResponseException: CSDKResponseException?) {
        csdkResponseException?.message?.let {
            hideProgress()
            showMessage(it)
        }

    }

    private fun handleResponseError(responseErrorCode: ResponseErrorCode?) {
        responseErrorCode?.name?.let {
            hideProgress()
            showMessage(it)
        }

    }

    private fun onEvent(event: SingleLiveEvent?) {
        when (event) {
            is TopicFetchedEvent -> handleTopicFetchedEvent(event)
            is GoToDashboardEvent -> goToHomeScreen(event)
            is PasswordRequiredEvent -> handlePasswordRequiredEvent(event.invalid)
        }
    }

    private fun handlePasswordRequiredEvent(invalid: Boolean) {
        if (invalid) {
            showMessage(getString(R.string.password_required))
        } else {
            showMessage(getString(R.string.wrong_password))
        }
    }

    private fun goToHomeScreen(event: GoToDashboardEvent) {
        val intent = Intent(this, HomeActivity::class.java).apply {
            action = Intent.ACTION_VIEW
//            data = viewModel.nextUri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
        }
        intent.putExtra(EXTRA_IS_GUEST_USER, joinAsGuestChosen)
        startActivity(intent)
        finish()
    }

    private fun handleTopicFetchedEvent(event: TopicFetchedEvent) {

        val intent = Intent(this, JoinMeetingActivity::class.java)
            .putExtra(EXTRA_MEETING_ACTION, event.action)
            .putExtra(EXTRA_TOPIC_TITLE, event.topicTitle)
            .putExtra(EXTRA_DISPLAY_NAME, spacesUserInfo.displayName)
            .putExtra(EXTRA_IS_GUEST_USER, joinAsGuestChosen)
            .putExtra(EXTRA_TOPIC_ID, event.id)
        if (event.password != null) {
            intent.putExtra(EXTRA_MEETING_PASSWORD, event.password)
        }
        startActivity(intent)
        finish()
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.join_as_guest_button -> {

                showLogs(logTAG, "clicked join as guest")
                joinAsGuestChosen = true
                val guestUsername = binding.displayName.text.toString()
                val guestCredential = GuestCredential(guestUsername)
                performLogin(guestCredential)
            }
            R.id.sign_in_button -> {
                showLogs(logTAG, "clicked on Sign in / Create account button")
                joinAsGuestChosen = false

                val oAuthCredential = OAuthCredential(
                    "58f62bb6a31329531a98",
                    "1852cd814e261c27b30337ab2698c6df0922fee0",
                    "spacesmobilesdk://oauth2"
                )

                performLogin(oAuthCredential)
            }
            R.id.user_pass_login_button -> {
                showLogs(logTAG, "clicked on Sign in button")
                val username = binding.username.text.toString()
                val password = binding.password.text.toString()
                val userPassCredential = UsernamePasswordCredential(
                    "58f62bb6a31329531a98",
                    "1852cd814e261c27b30337ab2698c6df0922fee0",
                    username, password

                )
                performLogin(userPassCredential)
            }
            R.id.connect_button -> {
                meetingUrl = binding.meetingUrl.text.toString()
                meetingPassword = binding.meetingPassword.text.toString()
                spacesUserInfo = spacesUser.getUserInfo()!!
                viewModel.performActionOnUri(meetingUrl, meetingPassword)

            }
            R.id.sign_out -> {
                viewModel.signOut();
                toggleSignInView(true)
                toggleMeetingView(false)
            }

        }
    }

    private fun performLogin(credential: Credential?) {
        showProgress(this)

        viewModel.login(credential)
            .observe(this, Observer<SpacesUserInfo?> {
                it?.let {
                    hideProgress()
                    spacesUserInfo = it
                    toggleSignInView(false)
                    toggleMeetingView(true)

                }
            })
    }

    private fun toggleMeetingView(show: Boolean) {
        when (show) {
            true -> binding.meetingDetailsLayout.visibility = View.VISIBLE
            false -> binding.meetingDetailsLayout.visibility = View.GONE
        }
    }

    private fun toggleSignInView(show: Boolean) {
        when (show) {
            true -> binding.loginLayout.visibility = View.VISIBLE
            false -> binding.loginLayout.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        viewModel.exit()
        super.onBackPressed()

    }

    private fun handleLoginFormStateChange(newState: JoinMeetingFormState?) {
        val loginState = newState ?: return

        // Disable login button unless both display name / meeting URL is valid
        when (loginState) {
            is DisplayNameError -> {
                binding.joinAsGuestButton.isEnabled = false
                binding.displayName.error = getString(loginState.stringId)
            }
            is MeetingUrlError -> {
                binding.joinAsGuestButton.isEnabled = false
                binding.signInButton.isEnabled = false
                binding.meetingUrl.error = getString(loginState.stringId)
            }
            is JoinMeetingValidData -> {
                binding.joinAsGuestButton.isEnabled = true
                binding.signInButton.isEnabled = true
                binding.displayName.error = null
                binding.meetingUrl.error = null
            }
            is DisplayUserNameError -> {
                binding.userPassLoginButton.isEnabled = false
                binding.username.error = getString(loginState.stringId)
            }
            is DisplayPasswordError -> {
                binding.userPassLoginButton.isEnabled = false
                binding.password.error = getString(loginState.stringId)
            }
            is ValidUsernamePassword -> {
                binding.userPassLoginButton.isEnabled = true
                binding.username.error = null
                binding.password.error = null
            }
        }
    }


}

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
