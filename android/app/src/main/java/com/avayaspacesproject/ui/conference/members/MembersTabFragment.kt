package com.avayaspacesproject.ui.conference.members

import android.content.Context
import android.os.Build.ID
import android.view.Menu
import android.view.View
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.avaya.spaces.util.TaskThrottle
import com.avaya.spaces.util.runOnUiThread
import com.avaya.spacescsdk.services.call.Conference
import com.avaya.spacescsdk.services.call.SelfMuteToggleResult
import com.avaya.spacescsdk.services.call.listeners.MeetingAttendeeListener
import com.avaya.spacescsdk.services.spaces.listeners.SpacesAPITopicReadyListener
import com.avaya.spacescsdk.services.spaces.listeners.TopicListener
import com.avaya.spacescsdk.services.spaces.model.SpacesTopic
import com.avaya.spacescsdk.services.user.listeners.SpacesAPIUserOnlineListener
import com.avaya.spacescsdk.services.user.model.SpacesUserInfo
import com.avayaspacesproject.R
import com.avayaspacesproject.databinding.FragmentMembersTabBinding
import com.avayaspacesproject.ui.base.BaseFragment
import com.avayaspacesproject.ui.conference.CallActivity
import com.avayaspacesproject.ui.conference.CallViewModel
import kotlinx.coroutines.*
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import com.avaya.spacescsdk.services.call.model.Member
import com.avaya.spacescsdk.utils.UcLog
import com.avaya.spacescsdk.utils.isValidEmailAddress
import com.avayaspacesproject.ui.conference.chat.customview.BottomOffsetDecoration

class MembersTabFragment :
    BaseFragment<FragmentMembersTabBinding>(FragmentMembersTabBinding::inflate),
    MembersFragmentListener, SpacesAPIUserOnlineListener {

    private val logTag: String = "MembersTabFragment"

    override fun getViewBinding(): FragmentMembersTabBinding =
        FragmentMembersTabBinding.inflate(layoutInflater)

    private lateinit var adapter: MembersListAdapter

    lateinit var viewModel: CallViewModel

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var topic: SpacesTopic? = null

    private val throttle = TaskThrottle(lifecycleScope, ::refresh, 100)

    private var memberContextPopupMenu: TopicMemberPopupMenu? = null

    private val topicListener: TopicListener = object : TopicListener {
        @AnyThread
        override fun onTopicUpdated(topicUpdate: SpacesTopic) {

            if (topicUpdate.isEqualTo(viewModel.getCurrentTopic())) {
                topic = topicUpdate
                onTopicUpdate()
            } else {
                (activity as CallActivity)?.showLogs(logTag, "onTopicUpdated else" + topicUpdate)
            }
        }

        override fun onTopicRoleChanged(topicChanged: SpacesTopic, attendeeId: String) {
            if (topicChanged.isEqualTo(viewModel.getCurrentTopic())) {
                onTopicUpdate()
            }
        }

        //called this topic Deleted method if admin removes member/participant
        override fun onTopicDeleted(topicDeleted: SpacesTopic) {
            //super.onTopicDeleted(topic)
            if (topicDeleted.isEqualTo(viewModel.getCurrentTopic())) {
                (activity as CallActivity)?.showLogs(logTag, "onTopicDeleted topic:{$topicDeleted}")
                runOnUiThread {
                    handleNoTopic()
                    val message = java.lang.String.format(
                        getString(R.string.no_longer_have_access),
                        topic?.title
                    )
                    (activity as CallActivity)?.showMessage(message)
                }
            }
        }

        override fun onTopicArchived(topic: SpacesTopic) {
//            super.onTopicArchived(topic)
            handleTopicArchived(topic)
        }

    }

    @AnyThread
    private fun handleTopicArchived(topic: SpacesTopic) {
        val currentTopic: SpacesTopic? = viewModel.getCurrentTopic()
        if (topic.isEqualTo(currentTopic)) {
            runOnUiThread {
                handleNoTopic()
                (activity as CallActivity)?.showMessage(getString(R.string.space_archived))
            }
        }
    }

    private fun handleNoTopic() {
        UcLog.d(ID, "handleNoTopic:")
        if (viewModel.isGuestUser()) {
            viewModel.onSignoutConfirmed()
            (activity as CallActivity)?.moveToLoginScreen()
        } else {
            unsubscribeTopic()
        }
    }

    private fun unsubscribeTopic() {
        UcLog.d(ID, "unsubscribeTopic:")
//        if (loginSession != null) {
//            loginSession.unsubscribeGroupSubscriptionTopic()
//        }

        if (viewModel.hasConference) {
            UcLog.d(ID, "Terminating conference due to unsubscribing topic")
            (activity as CallActivity)?.shutdownConference()

        }
    }

    private val topicReadyListener: SpacesAPITopicReadyListener = object :
        SpacesAPITopicReadyListener {
        @AnyThread
        override fun onTopicReady(topic: SpacesTopic) {
            postRefreshDelayed()
        }

        @AnyThread
        override fun onTopicUnsubscribed() {
            postRefreshDelayed()
        }
    }

    private val participantListener: MeetingAttendeeListener = object : MeetingAttendeeListener {
        override fun onMeetingAttendeeAdded(member: Member) {
            postRefreshDelayed()
        }

        override fun onMeetingAttendeeUpdate(member: Member) {
            postRefreshDelayed()
        }

        override fun onMeetingAttendeeLeft(member: Member) {
            postRefreshDelayed()
        }
    }

    override fun initializeView() {

        adapter = MembersListAdapter(MembersDiffCallback(), this)

        binding.memberList.layoutManager = LinearLayoutManager(this.context)
        binding.memberList.adapter = adapter
        binding.memberList.addItemDecoration(
            BottomOffsetDecoration(
                resources.getDimensionPixelOffset(R.dimen.recycler_fab_offset)
            )
        )
        viewModel.addUserOnlineListener(this)
        viewModel.addTopicListener(topicListener)
        viewModel.addTopicReadyListener(topicReadyListener)
        viewModel.addMeetingAttendeeListener(participantListener)

        postRefreshDelayed()

    }

    override fun initializeViewModel() {

        viewModel = activity?.run {
            ViewModelProvider(this).get(CallViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
    }

    private fun handleTopicFetchedEvent(spacesTopic: SpacesTopic?) {
        if (spacesTopic != null) {
            (activity as CallActivity)?.showLogs(
                logTag,
                " handleTopicFetchedEvent topic" + spacesTopic
            )
            postRefreshDelayed()
        }
    }

    @AnyThread
    private fun onTopicUpdate() {
        postRefreshDelayed()
    }


    @AnyThread
    private fun postRefreshDelayed() {
        throttle.postTask()
    }

    suspend fun refresh() {
        val currentTopic = viewModel.getCurrentTopic()

        (activity as CallActivity)?.showLogs(
            logTag,
            "currentTopic" + currentTopic?.connectedMembers
        )
        val models = if (currentTopic == null) {
            emptyList()
        } else {
            topic = currentTopic
            buildTopicMemberModels(currentTopic)
        }

        withContext(Dispatchers.Main) {
            adapter.submitList(
                models,
                Runnable { binding.memberList.invalidateItemDecorations() })
        }
    }

    override fun onMemberSelected(member: Member) {

    }

    override fun onMemberActions(member: Member, view: View) {
        showMemberPopupMenu(member, view)
    }

    override fun onMemberAudioToggled(member: Member) {
        if (member.isMe) {
            val conference = viewModel.conference
            if (conference != null) {
                UcLog.d(logTag, "User tapped own audio control in roster")
                val result = conference.attemptToggleAudioMute()
                if (result === SelfMuteToggleResult.MUST_RAISE_HAND) {
                    //showInfoToast(R.string.member_raise_hand_required)
                } else if (result === SelfMuteToggleResult.WAIT_FOR_PERMISSION) {
                    //showInfoToast(R.string.participant_wait_unmute)
                }
            }
        } else {
            val topic = topic!!
            //merge change
            if ((topic.isAdmin || topic.isMember) && member.mediaSessionPhone) {
                viewModel.requestAttendeeToggleAudioUnmute(topic.identity, member)
            } else if ((topic.isAdmin || topic.isMember && !member.isAdmin) && !member.handRaised) {
                viewModel.requestAttendeeAudioMute(topic.identity, member)
            }
        }
    }

    override fun onMemberVideoToggled(member: Member) {
        if (member.isMe) {
            val conference = viewModel.conference
            if (conference != null) {
                UcLog.d(logTag, "User tapped own video control in roster")
                conference.toggleVideoBlock()
            }
        }

    }

    override fun onUserOnlineForTopic(user: SpacesUserInfo?, online: Boolean, topic: SpacesTopic?) {
        postRefreshDelayed()
    }

    private fun showMemberPopupMenu(
        member: Member,
        view: View
    ) {
        val topic: SpacesTopic = topic ?: return
        val conference: Conference? = viewModel.conference
        val context: Context = ContextThemeWrapper(this.activity, R.style.AppTheme)
        memberContextPopupMenu =
            TopicMemberPopupMenu(context, view, member, object : TopicMemberMenuListener {
                override fun onMemberAllowMute(member: Member) {

                }

                override fun onMemberRaiseHand(member: Member) {

                }

                override fun onMemberLowerHand(member: Member) {

                }

                override fun onMemberMakeAdmin(member: Member) {

                }

                override fun onMemberMakeMember(member: Member) {

                }

                override fun onMemberChat(member: Member) {
                }

                override fun onMemberRemove(member: Member) {
                    removeMemberFromTopic(member, topic)
                }

                override fun onMemberLeave(member: Member) {
                    leaveTopic(topic)
                }
            })
        val memberContextMenu = memberContextPopupMenu!!.getMenu()
        memberContextPopupMenu!!.setOnDismissListener { menu -> memberContextPopupMenu = null }
        updateMemberPopupMenu(member, topic, conference)
        if (memberContextMenu.size() > 0) {
            memberContextPopupMenu!!.show()
        }
    }

    private fun leaveTopic(topic: SpacesTopic) {
        if (topic == null) {
            return
        }

        val dialogFragment: LeaveSpaceDialogFragment =
            LeaveSpaceDialogFragment.newInstance(topic.identity, topic.title)
        fragmentManager?.let { dialogFragment.show(it, LeaveSpaceDialogFragment.TAG) }
    }

    @UiThread
    private fun removeMemberFromTopic(
        member: Member?,
        topic: SpacesTopic?
    ) {
        if (topic == null || member == null) {
            return
        }
        val memberId = member.userInfo.identity
        val memberName = member.userInfo.displayName
        val dialogFragment: RemoveParticipantDialogFragment =
            RemoveParticipantDialogFragment.newInstance(
                memberId,
                memberName,
                topic.identity,
                topic.title
            )
        this.fragmentManager?.let { dialogFragment.show(it, RemoveParticipantDialogFragment.TAG) }
    }


    fun updateMemberPopupMenu(
        member: Member,
        topic: SpacesTopic,
        conference: Conference?
    ) {
        val itsMe = member.userInfo.isEqualTo(viewModel.getUserInfo())
        val personal: Boolean = topic.isPersonal
        val myPersonal: Boolean = topic.isPersonalFor(viewModel.getUserIdentity())
        val isGuestUser: Boolean = viewModel.isGuestUser()
        val canChat =
            !itsMe && !viewModel.hasConference && !member.userInfo.isPhone && !isGuestUser
        val canEmail =
            !itsMe && !isGuestUser && !member.userInfo.isPhone && member.userInfo.username.isValidEmailAddress
        // TODO : Remove check for Spaces Calling feature toggle if calling user phone number allowed for all users.

        //set canPhone,canMakeAdmin, canMakeMember to false as its not inlcuded in 1st phase of reference client app
        val canPhone = false
        val canMakeAdmin = false
        val canMakeMember = false

        val canRemove = !itsMe && viewModel.currentTopicCanRemoveMember(member)
        val canLeave = itsMe && !myPersonal && viewModel.currentTopicCanQuit()
        val hasConference = conference != null
        val canLowerHand = hasConference && topic.canLowerHand(itsMe, member)
        val canRaiseHand =
            hasConference && topic.needsSelfUnmutePermission(itsMe, member)
        val canAllowUnmute =
            false && hasConference && topic.canAllowUnmuteOther(itsMe, member)
        UcLog.i(
            ID,
            "updateMemberPopupMenu: " + member.identity + " " + canChat + " " + canMakeAdmin + " " + canMakeMember + " " + canRemove + " " + canLeave + " " + canLowerHand + " " + canRaiseHand + " " + canAllowUnmute
        )
        memberContextPopupMenu!!.updateMenuOptions(
            itsMe,
            canChat,
            canMakeAdmin,
            canMakeMember,
            canRemove,
            canLeave,
            canLowerHand,
            canRaiseHand,
            canAllowUnmute
        )
        memberContextPopupMenu!!.menu.removeGroup(Menu.FIRST)
//        if (canEmail) {
//            memberContextPopupMenu.menu
//                .add(Menu.FIRST, 0, 0, member.userInfo.username)
//                .setOnMenuItemClickListener { menuItem ->
//                    sendEmailIntent(member.userInfo.username)
//                    true
//                }
//        }
//        if (canPhone) {
//            // List each available phone number as a menu item, listing Type: Number.
//            // E.g. +15555551234 (Work)
//            val phoneNumbers =
//                member.userInfo.phoneNumbers
//            for (phoneNumber in phoneNumbers) {
//                val displayedNumber: String =
//                    getPhoneNumberWithType(resources, phoneNumber)
//                memberContextPopupMenu.menu.add(Menu.FIRST, 0, 0, displayedNumber)
//                    .setOnMenuItemClickListener { menuItem ->
//                        placeCallWithExternalApp(this, phoneNumber)
//                        true
//                    }
//            }
//        }
    }

    override fun onDestroy() {

        viewModel.removeUserOnlineListener(this)
        viewModel.removeTopicListener(topicListener)
        viewModel.removeTopicReadyListener(topicReadyListener)
        viewModel.removeMeetingAttendeeListener(participantListener)

        super.onDestroy()
    }


}