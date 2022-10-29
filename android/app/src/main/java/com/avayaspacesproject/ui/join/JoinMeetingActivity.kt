package com.avayaspacesproject.ui.join

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.SharedPreferences
import android.view.View
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.ViewModelProvider
import com.avaya.spaces.*
import com.avaya.spacescsdk.services.call.model.ExternalIntentAction
import com.avaya.spacescsdk.services.call.model.JoinMeetingOptions
import com.avaya.spacescsdk.services.call.model.externalIntentActionExtra
import com.avaya.spacescsdk.services.spaces.Spaces
import com.avaya.spacescsdk.utils.*
import com.avayaspacesproject.R
import com.avayaspacesproject.ReferenceApp
import com.avayaspacesproject.ReferenceApp.Companion.context
import com.avayaspacesproject.databinding.ActivityJoinMeetingBinding
import com.avayaspacesproject.di.component.ActivityComponent
import com.avayaspacesproject.di.module.ActivityModule
import com.avayaspacesproject.di.module.DefaultSharedPreferences
import com.avayaspacesproject.ui.base.BaseActivity
import com.avayaspacesproject.ui.conference.CallActivity
import io.zang.spaces.*
import javax.inject.Inject

private const val GUEST_START_VIDEO = "guest_start_video"
private const val GUEST_BLOCK_VIDEO = "guest_block_video"
private const val GUEST_MUTE_MICROPHONE = "guest_mute_microphone"

class JoinMeetingActivity : BaseActivity<ActivityJoinMeetingBinding>(), View.OnClickListener {
    private val logTAG: String = "JoinMeetingActivity"

    override fun getViewBinding(): ActivityJoinMeetingBinding =
        ActivityJoinMeetingBinding.inflate(layoutInflater)

    private lateinit var activityComponent: ActivityComponent

    lateinit var viewModel: JoinViewModel

    @SuppressLint("FieldSiteTargetOnQualifierAnnotation")
    @field:DefaultSharedPreferences
    @Inject
    protected lateinit var sharedPreferences: SharedPreferences

    @Inject
    protected lateinit var spaces: Spaces

    private var joinAsGuestChosen = false
    private var meetingPassword: String? = null

    private val externalIntentAction: ExternalIntentAction?
        get() = viewModel.externalIntentAction.value

    private val meetingOptions: JoinMeetingOptions
        get() = JoinMeetingOptions(
            if (joinAsGuestChosen) true else null,
            binding.blockVideo.isChecked, binding.muteMicrophone.isChecked, meetingPassword
        )

    private var topicID: String? = null

    override fun initializeView() {

        if (intent != null) {
            val action = intent.externalIntentActionExtra
            viewModel.externalIntentAction.value = action

            val meetingTitle = context.getString(
                R.string.possessive_personal_meeting_room,
                intent.getStringExtra(EXTRA_TOPIC_TITLE)
            )
            showLogs(logTAG, "meetingName:" + meetingTitle)

            binding.meetingName.text = meetingTitle
            binding.displayName.text = intent.getStringExtra(EXTRA_DISPLAY_NAME)
            meetingPassword = intent.getStringExtra(EXTRA_MEETING_PASSWORD)
            joinAsGuestChosen = intent.getBooleanExtra(EXTRA_IS_GUEST_USER, false)
            topicID = intent.getStringExtra(EXTRA_TOPIC_ID)

            viewModel.loginDataChanged(
                binding.displayName.text.toString(),
                binding.meetingName.text.toString()
            )


        }

        binding.autoJoinMeeting.setOnCheckedChangeListener { _, isChecked ->
            saveStartVideoPreference(
                isChecked
            )
        }

        binding.blockVideo.isChecked = getBlockVideoPreference()
        binding.blockVideo.setOnCheckedChangeListener { _, isChecked ->
            saveBlockVideoPreference(
                isChecked
            )
        }

        binding.muteMicrophone.isChecked = getMuteMicrophonePreference()
        binding.muteMicrophone.setOnCheckedChangeListener { _, isChecked ->
            saveMuteMicrophonePreference(
                isChecked
            )
        }
    }

    override fun initializeViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory).get(JoinViewModel::class.java)
    }


    override fun injectDependency() {
        activityComponent = ReferenceApp.appComponent.buildActivityComponent(ActivityModule(this))
        activityComponent.inject(this)
    }

    private fun saveStartVideoPreference(isChecked: Boolean) {
        sharedPreferences.edit().putBoolean(GUEST_START_VIDEO, isChecked).apply()
    }

    private fun getBlockVideoPreference(): Boolean =
        sharedPreferences.getBoolean(GUEST_BLOCK_VIDEO, true)

    private fun saveBlockVideoPreference(isChecked: Boolean) {
        sharedPreferences.edit().putBoolean(GUEST_BLOCK_VIDEO, isChecked).apply()
    }

    private fun getMuteMicrophonePreference(): Boolean =
        sharedPreferences.getBoolean(GUEST_MUTE_MICROPHONE, true)

    private fun saveMuteMicrophonePreference(isChecked: Boolean) {
        sharedPreferences.edit().putBoolean(GUEST_MUTE_MICROPHONE, isChecked).apply()
    }

    private fun navigateToHomeScreen() {
        val uri = externalIntentAction?.uri
        val intent = Intent(this, CallActivity::class.java).apply {
            action = ACTION_VIEW
            data = uri
            putExtra(EXTRA_MEETING_OPTIONS, meetingOptions)
        }
        if (meetingPassword != null) {
            intent.putExtra(EXTRA_MEETING_PASSWORD, meetingPassword)
        }
        if (topicID != null) {
            intent.putExtra("TOPIC_ID", topicID)
        }
        intent.putExtra(EXTRA_IS_GUEST_USER,joinAsGuestChosen)

        TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(intent)
            .startActivities()
        finish()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.join_meeting_as_guest -> {
                showLogs(logTAG, "on join_meeting_as_guest click")
                navigateToHomeScreen()
            }
        }
    }


}

