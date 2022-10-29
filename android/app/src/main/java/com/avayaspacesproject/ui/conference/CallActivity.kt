package com.avayaspacesproject.ui.conference

import android.content.Intent
import android.os.Build.ID
import android.view.WindowManager
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.avaya.spacescsdk.services.call.*
import com.avaya.spacescsdk.services.call.listeners.MeetingAttendeeListener
import com.avaya.spacescsdk.services.call.model.JoinMeetingOptions
import com.avaya.spacescsdk.services.call.model.Member
import com.avaya.spacescsdk.services.spaces.listeners.SpacesAPITopicReadyListener
import com.avaya.spacescsdk.services.spaces.listeners.TopicListener
import com.avaya.spacescsdk.services.spaces.model.SpacesTopic
import com.avaya.spacescsdk.services.user.listeners.SpacesAPIUserOnlineListener
import com.avaya.spacescsdk.services.user.model.SpacesUserInfo
import com.avaya.spacescsdk.utils.*
import com.avayaspacesproject.ReferenceApp
import com.avayaspacesproject.databinding.ActivityCallBinding
import com.avayaspacesproject.di.component.ActivityComponent
import com.avayaspacesproject.di.module.ActivityModule
import com.avayaspacesproject.ui.base.BaseActivity
import com.avayaspacesproject.ui.conference.members.LeaveSpaceDialogListener
import com.avayaspacesproject.ui.conference.members.MembersTabFragment
import com.avayaspacesproject.ui.conference.members.RemoveParticipantDialogListener
import com.avayaspacesproject.ui.conference.members.TopicMemberPopupMenu
import com.avayaspacesproject.ui.login.LoginActivity
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject


class CallActivity : BaseActivity<ActivityCallBinding>(), EndMeetingDialogListener,
    ConferenceFinishedListener, HasAndroidInjector, RemoveParticipantDialogListener,
    LeaveSpaceDialogListener {

    private val logTAG: String = "CallActivity"

    override fun getViewBinding(): ActivityCallBinding = ActivityCallBinding.inflate(layoutInflater)

    lateinit var viewModel: CallViewModel

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    private lateinit var activityComponent: ActivityComponent

    private lateinit var topicID: String

    private lateinit var meetingOptions: JoinMeetingOptions

    var confViewController: ConferenceViewController? = null

    private lateinit var tabsAdapter: SpaceTabPagerAdapter


    @Inject
    protected lateinit var audioDeviceManager: AudioDeviceManager

    private var memberContextPopupMenu: TopicMemberPopupMenu? = null

    private fun hasConference(): Boolean {
        return viewModel.hasConference
    }


    override fun initializeViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory).get(CallViewModel::class.java)
    }

    override fun injectDependency() {
        activityComponent = ReferenceApp.appComponent.buildActivityComponent(ActivityModule(this))
        activityComponent.inject(this)
    }

    override fun initializeView() {

        viewModel.topicLiveData.observe(this, Observer(::handleTopicFetchedEvent))
        viewModel.conferenceLiveData.observe(this, Observer(::onConferenceChanged))

        if (intent != null) {
            topicID = intent.getStringExtra("TOPIC_ID").toString()

            if (intent.action == ACTION_REJOIN_CONFERENCE) {
                meetingOptions = JoinMeetingOptions()
            } else {
                meetingOptions = intent.getParcelableExtra(EXTRA_MEETING_OPTIONS)!!
            }
            viewModel.subscribeTopicById(topicID)


        }

        viewModel.addTopicListener(topicListener)
        viewModel.addMeetingAttendeeListener(participantConferenceUpdateListener)
        viewModel.addUserOnlineListener(userOnlineListener)

        addMeetingRoomView()
    }

    private fun addMeetingRoomView() {

        tabsAdapter =
            SpaceTabPagerAdapter(
                resources,
                supportFragmentManager
            )


        binding.viewPager.offscreenPageLimit = 5
        binding.viewPager.adapter = tabsAdapter

        binding.tabLayout.setupWithViewPager(binding.viewPager)

        tabsAdapter.setGuestMode(viewModel.isGuestUser())

    }

    private val topicListener: TopicListener = object : TopicListener {
        @AnyThread
        override fun onGroupTopicsUpdated() {

        }

        @AnyThread
        override fun onDirectTopicsUpdated() {

        }

        @AnyThread
        override fun onTopicUpdated(topic: SpacesTopic) {
            handleTopicUpdated(topic)
        }

        @AnyThread
        override fun onTopicDeleted(topic: SpacesTopic) {

        }

        @AnyThread
        override fun onTopicArchived(topic: SpacesTopic) {

        }

        override fun onTopicRoleChanged(topic: SpacesTopic, attendeeId: String) {

        }
    }

    private val participantConferenceUpdateListener: MeetingAttendeeListener =
        object : MeetingAttendeeListener {
            override fun onMeetingAttendeeAdded(member: Member) {}
            override fun onMeetingAttendeeUpdate(member: Member) {
                updateMemberPopupMenu(member.identity)
            }

            override fun onMeetingAttendeeLeft(member: Member) {
                updateMemberPopupMenu(member.identity)
            }
        }

    private val userOnlineListener: SpacesAPIUserOnlineListener =
        object : SpacesAPIUserOnlineListener {

            override fun onUserOnlineForTopic(
                user: SpacesUserInfo,
                online: Boolean,
                topic: SpacesTopic
            ) {
                updateMemberPopupMenu(user.identity)
            }
        }

    private val topicReadyListener: SpacesAPITopicReadyListener =
        SpacesAPITopicReadyListener { topic ->
            UcLog.d(ID, "on topic ready: $topic")

            viewModel.topic = topic
        }

    private fun updateMemberPopupMenu(memberId: String) {
        com.avaya.spaces.util.runOnUiThread {
            val topic: SpacesTopic? = viewModel.topic
            val conference: Conference? = viewModel.conference
            val member: Member? = topic?.memberByUserId(memberId)
            if (member == null) {
                // If the member left then dismiss their context menu
                if (memberContextPopupMenu != null && memberContextPopupMenu!!.isForMemberId(
                        memberId
                    )
                ) {
                    memberContextPopupMenu!!.dismiss()
                }
                return@runOnUiThread
            }
            if (memberContextPopupMenu != null && memberContextPopupMenu!!.isForMemberId(memberId)) {
                val fragment = MembersTabFragment()
                fragment.updateMemberPopupMenu(member, topic, conference)
            }
        }
    }

    private fun handleTopicUpdated(topic: SpacesTopic) {
        val currentTopic: SpacesTopic? = currentTopic()
        if (topic != null && topic.isEqualTo(currentTopic)) {
            viewModel.topic = topic
        }
    }

    private fun currentTopic(): SpacesTopic? {
        return viewModel.topic
    }


    private fun handleTopicFetchedEvent(topic: SpacesTopic?) {
        topic?.let {
            showLogs(logTAG, "topic:${topic}")
            viewModel.joinMeetingForTopic(it, meetingOptions)

        }
    }

    private fun handleToolBarTitle(title: String?) {
        supportActionBar!!.title = title
    }

    private fun onConferenceChanged(conference: Conference?) {
        assert(viewModel.conference === conference)
        conference?.let {
            showLogs(logTAG, "conference:${conference}")
            startConference(it)
        }
    }

    private fun startConference(conference: Conference) {

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        conference.start(this)
        attachConference()
    }


    private fun attachConference() {
        showLogs(logTAG, "in attachConference")
        val conference: Conference = viewModel.conference!!

        handleToolBarTitle(conference.title)

        conference.addFinishedListener(this)
        conference.errorCode.observe(this,
            Observer { error: ConferenceError ->
                this.onConferenceError(
                    error
                )
            }
        )

        if (confViewController == null) {
            confViewController = ConferenceViewControllerImpl(
                null,
                audioDeviceManager,
                viewModel,
                binding.callScreenView
            )
            UcLog.d(ID, "Joining $conference with $confViewController")
            conference.attach(confViewController!!, ScreenModeRequest.FULL_SCREEN)
        } else {
            UcLog.d(ID, "Rejoining $conference with $confViewController")
        }
        confViewController!!.startUi()
    }

    private fun determineConferenceScreenMode(): ScreenModeRequest {
        val intent = intent
        if (intent != null && intent.action != null) {
            when (intent.action) {
                ACTION_REJOIN_CONFERENCE -> {
                    intent.clearIntent()
                    return ScreenModeRequest.FULL_SCREEN
                }
                ACTION_VIEW_MESSAGE -> {
                    intent.clearIntent()
                    return ScreenModeRequest.MINIMIZED
                }
            }
        }
        return ScreenModeRequest.UNCHANGED
    }


    @MainThread
    private fun onConferenceError(error: ConferenceError) {
        if (error != ConferenceError.NO_ERROR) {
            UcLog.w(ID, "Terminating conference due to $error")
            shutdownConference()
        }
    }

    @UiThread
    fun shutdownConference() {
        val conference: Conference = viewModel.conference!!
        handleToolBarTitle(null)
        conference?.hangupConference()
    }

    fun moveToLoginScreen() {
        if (viewModel.isGuestUser()) {
            viewModel.logoutUser()
        }

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    fun toggleFullscreen(fullscreen: Boolean) {
        window.fullscreen = fullscreen
    }

    fun showHideToolBar(isShow: Boolean) {
        if (isShow) {
            supportActionBar!!.show()
        } else {
            supportActionBar!!.hide()
        }
    }

    @UiThread
    protected fun pauseConference() {
        val conference: Conference? = viewModel.conference
        conference?.onPause()
    }

    override fun onPause() {
        pauseConference()
        if (hasConference()) {
            detachConference()
        }
        super.onPause()
    }

    private fun detachConference() {
        val conference: Conference? = viewModel.conference
        if (conference != null) {
            conference.detach()
            conference.errorCode.removeObservers(this)
        }
        confViewController = null
    }

    override fun onBackPressed() {
        if (hasConference()) {
            showEndMeetingDialog()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        onDestroyConfInternal()
        //viewModel.removeTopicReadyListener(topicReadyListener)
        viewModel.removeTopicListener(topicListener)
        viewModel.removeMeetingAttendeeListener(participantConferenceUpdateListener)
        viewModel.removeUserOnlineListener(userOnlineListener)
        super.onDestroy()
    }

    @AnyThread
    private fun onDestroyConf() {
        runOnUiThread { onDestroyConfInternal() }
    }

    @UiThread
    private fun onDestroyConfInternal() {
        window.keepScreenOn = false
        toggleFullscreen(false)
        detachConference()
        val conference: Conference? = viewModel.conference
        if (conference != null) {
            conference.removeFinishedListener(this)
            viewModel.clearConference()
            UcLog.d(ID, "Finished destroying conference $conference")
            moveToLoginScreen()
        }
    }

    @UiThread
    fun showEndMeetingDialog() {
        val dialogFragment: EndMeetingDialogFragment = EndMeetingDialogFragment.newInstance()
        dialogFragment.show(supportFragmentManager, EndMeetingDialogFragment.TAG)
    }

    override fun onEndMeetingConfirmed() {
        if (confViewController != null) {
            confViewController!!.onHangupConfirmed()

        }
    }

    override fun onConferenceFinished() {
        onDestroyConf()
    }

    override fun onResumeImpl() {
        super.onResumeImpl()
        resumeConference()
        reattachConf()
    }

    @UiThread
    protected fun resumeConference() {
        val conference: Conference? = viewModel.conference
        conference?.onResume()
    }


    @UiThread
    protected fun reattachConf() {
//        if (!hasConference()) {
//            // See if we missed the start of a conference somehow.
//            val conference: Conference? =
//               viewModel.conference
//            if (conference != null) {
//                viewModel.conference=conference
//            }
//        }

        // Rejoin the conference, if there is one.
        if (hasConference() && viewModel.conference?.active!!) {
            attachConference()
        }
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return fragmentDispatchingAndroidInjector
    }

    override fun onRemoveParticipantConfirmed(memberId: String, topicId: String) {
        val topic: SpacesTopic? = viewModel.topicById(topicId)
        if (topic == null) {
            UcLog.d(ID, "onRemoveParticipantConfirmed but topic was null, not found by Id")
            return
        }
        val member: Member? = topic.getMemberById(memberId)
        if (member == null) {
            UcLog.d(
                ID,
                "onRemoveParticipantConfirmed but member was null, not found by Id, potentially already removed."
            )
            return
        }

        viewModel.topicRemoveUser(topic, member.userInfo)
    }

    override fun onLeaveSpaceConfirmed(topicId: String?) {
        val topic: SpacesTopic = viewModel.topicById(topicId!!) ?: return



        if (topic.isEqualTo(currentTopic())) {
            if (viewModel.hasConference) {
                UcLog.d(ID, "Terminating conference due to unsubscribing topic")
                shutdownConference()

            }
        }
        // Guest users treat Leave as Signout
        if (viewModel.isGuestUser()) {
            viewModel.onSignoutConfirmed()
            moveToLoginScreen()
            return
        }
        viewModel.topicRemoveUser(topic, viewModel.getUserInfo())
    }

    val Intent.isRejoinConference: Boolean
        @JvmName("isRejoinConferenceIntent")
        get() = action == ACTION_REJOIN_CONFERENCE
}
