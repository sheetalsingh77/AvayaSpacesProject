package com.avayaspacesproject.ui.conference

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.avaya.spaces.conference.model.*
import com.avaya.spacescsdk.BuildConfig
import com.avaya.spacescsdk.services.call.*
import com.avaya.spacescsdk.services.call.ui.AVVideoPort
import com.avaya.spacescsdk.services.call.ui.CallVideoSink
import com.avayaspacesproject.R
import com.avayaspacesproject.databinding.ConferenceFullscreenBinding
import com.avayaspacesproject.databinding.ConferenceMainBottomControlsBinding
import com.avayaspacesproject.databinding.ConferenceToolbarBinding
import com.avaya.spacescsdk.services.call.ui.AVVideoPort.FULL_LOCAL
import com.avaya.spacescsdk.services.call.ui.AVVideoPort.MIN_REMOTE
import com.avaya.spaces.util.ConnectivityLiveData
import com.avaya.spacescsdk.services.call.enums.MpaasNetworkStatus
import com.avaya.spacescsdk.services.call.model.VideoRenderingFacade
import com.avaya.spacescsdk.services.call.ui.AVActiveSpeakerView
import com.avaya.spacescsdk.services.spaces.model.UserInfo
import com.avaya.spacescsdk.utils.UcLog
import com.avaya.spacescsdk.utils.hideKeyboard


class ConferenceViewControllerImpl(
    private val connectivityLiveData: ConnectivityLiveData?,
    private val audioDeviceManager: AudioDeviceManager,
    private val viewModel: CallViewModel,
    private val parentView: ViewGroup
) : ConferenceViewController, AudioDeviceManagerListener {

    private val logTAG: String = "ConferenceViewControllerImpl"

    private val context: Context = parentView.context

    private val resources: Resources = parentView.resources

    private val activity = context as CallActivity

    private val conference: Conference = viewModel.conference!!

    private var initialized = false

    lateinit var controlView: ViewGroup

    @VisibleForTesting
    internal lateinit var fullscreenVideoLocal: AVVideoPort

    @VisibleForTesting
    internal lateinit var minimizedView: AVVideoPort

    private lateinit var activeSpeakerView: AVActiveSpeakerView

    private val fullscreen: Boolean
        get() = conference.fullscreen.value

    private val localVideoBlocked: Boolean
        get() = conference.videoBlocked.value

    private val rootEglBase: CallSurfaceBase? = conference.rootEglBase

    private val mediaEstablished: Boolean
        get() = conference.mediaEstablished.value

    private val screenSharingAvailable: Boolean
        get() = conference.screenSharingAvailable.value

    private val screenSharingBeingReceived: Boolean
        get() = conference.screenSharingBeingReceived.value


    /** Is the spinner currently being animated? */
    @VisibleForTesting
    internal var startingAnimation: Boolean = false


    private val mediaEstablishedObserver = Observer(::onMediaEstablishedChanged)

    private val fullscreenObserver = Observer(::onFullscreenChanged)

    private val videoBlockObserver = Observer(::onVideoBlockStatusChanged)

    private val muteStateObserver = Observer(::onAudioMuteStatusChanged)

    private val audioMuteInitiatorObserver = Observer(::onAudioMuteInitiatorChanged)

    private val muteAllAppliedObserver = Observer(::onMuteAllAppliedChanged)

    //merge change---added new
    private val activeCameraObserver = Observer(::onActiveCameraChanged)
    private val mpaasNetworkStatusObserver = Observer(::onMpaasNetworkStatusChanged)
    private val activeSpeakerObserver = Observer(::onActiveSpeakerChanged)
    private val screenSharingAvailableObserver = Observer(::onScreenSharingAvailableChanged)
    private val screenSharingBeingReceivedObserver = Observer(::onScreenSharingBeingReceivedChanged)
    private val screenSharingSourceObserver = Observer(::onScreenSharingSourceChanged)
    private val screenSharingFrameObserver = Observer(::onScreenShareFrame)

    private val videoRenderingFacade: VideoRenderingFacade = conference.videoRenderingFacade

    // Inside unit tests, the EGL base context is null. Use this fact to
    // disable things that spin forever in that environment.
    private val unitTest: Boolean = rootEglBase == null

    private lateinit var fullscreenBinding: ConferenceFullscreenBinding
    private lateinit var avControlBinding: ConferenceMainBottomControlsBinding
    private lateinit var conferenceToolbarBinding: ConferenceToolbarBinding

    private var hideControlsAnimator: Animator? = null
    private lateinit var hideToolbarAnimation: ObjectAnimator


    init {
        //Merge change from isActive() to active
        if (conference.active) {

            initializeControls()
            initializeFullscreenView()
            initializeMinimizedView()
            initialized = true
        }


    }

    private fun initializeMinimizedView() {
        minimizedView = createMinimizedVideoView()
        minimizedView.onClick = View.OnClickListener {
            UcLog.l(logTAG, "User tapped minimized remote video")

            //merge change- removed commented line and added as minimizedView.hideKeyboard()
            //DroidTweaks.hideKeyboard(minimizedView)
            minimizedView.hideKeyboard()

            conference.toggleFullscreenMode()
            activity.showHideToolBar(false)
        }

        minimizedView.visibility = GONE
        parentView.addView(
            minimizedView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun initializeFullscreenView() {

        activity.showHideToolBar(false)
        fullscreenBinding =
            ConferenceFullscreenBinding.inflate(LayoutInflater.from(context), parentView, true)

        fullscreenBinding.fullscreenOpaque.setOnClickListener {

            if (conferenceToolbarBinding.conferenceAppBar.isVisible) {

                hideControlsAnimator?.cancel()
                hideControls(true)
            } else {
                //merge change- removed commented line and added as showControls()
                //wakeUpControls()
                showControls()
            }
        }
        fullscreenBinding.fullscreenOpaque.addView(
            controlView
        )

        if (!unitTest) {
            fullscreenBinding.fullscreenRemoteVideo.init(rootEglBase!!)
        }


        fullscreenVideoLocal = createLocalVideoView()
        fullscreenVideoLocal.setMirror(true)
        fullscreenVideoLocal.visibility = GONE
        fullscreenBinding.localVideoContainer.addView(
            fullscreenVideoLocal,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        setupControlsAnimation()
        updateControls()

        //merge change- removed commented line and added as startHideControlsAnimation()
        //wakeUpControls()
        startHideControlsAnimation()

    }

    private fun initializeControls() {

        avControlBinding = ConferenceMainBottomControlsBinding.inflate(
            LayoutInflater.from(context),
            parentView,
            false
        )

        controlView = avControlBinding.controlsBottomContainer

        conferenceToolbarBinding =
            ConferenceToolbarBinding.inflate(LayoutInflater.from(context), parentView, true)
        conferenceToolbarBinding.conferenceToolbar.title = conference.title

        activeSpeakerView = AVActiveSpeakerView.inflate(context)
        activeSpeakerView.visibility = GONE
        // TODO: Add to layout
        controlView.addView(
            activeSpeakerView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        conferenceToolbarBinding.conferenceToolbar.setNavigationOnClickListener {
            activity.showHideToolBar(true)
            conferenceToolbarBinding.conferenceAppBar.visibility = GONE
            onControlTapped(
                ConferenceControlType.FULLSCREEN
            )
        }

        avControlBinding.avControlHangup.setOnClickListener { onControlTapped(ConferenceControlType.HANGUP) }
        avControlBinding.avControlVideo.setOnClickListener { onControlTapped(ConferenceControlType.VIDEO_MUTE) }
        avControlBinding.avControlSwitchCamera.setOnClickListener {
            onControlTapped(
                ConferenceControlType.SWITCH_CAMERA
            )
        }
        avControlBinding.avControlMic.setOnClickListener { onControlTapped(ConferenceControlType.MICROPHONE_MUTE) }
        avControlBinding.avControlSharescreen.setOnClickListener {
            onControlTapped(
                ConferenceControlType.SHARE_SCREEN
            )
        }

    }


    @UiThread
    private fun updateControls() {
        avControlBinding.avControlMuteAll.visibility = GONE

        setVideoControlState()
        setAudioControlState()
        setScreenShareControlState()
    }

    // On-screen buttons handlers
    private fun onControlTapped(control: ConferenceControlType) {
        if (!fullscreen) {
            onFullScreenControlTapped()
            return
        }

        //merge change-- removed below line
        //wakeUpControls()

        when (control) {
            ConferenceControlType.HANGUP -> onHangupControlTapped()
            ConferenceControlType.VIDEO_MUTE -> onVideoControlTapped()
            ConferenceControlType.MICROPHONE_MUTE -> onMicrophoneControlTapped()
            ConferenceControlType.SWITCH_CAMERA -> onSwitchCameraControlTapped()
            ConferenceControlType.FULLSCREEN -> onFullScreenControlTapped()
            ConferenceControlType.SHARE_SCREEN -> onSharingControlTapped()
            else -> {}
        }
    }

    // TODO: Throttle wakeUpControls
    private fun startHideControlsAnimation() {
        UcLog.l(logTAG, "wakeUpControls: $fullscreen")
        if (!fullscreen) {
            return
        }

        hideControlsAnimator?.cancel()
        showControls()

        hideControls(false)
    }

    private fun hideControls(immediate: Boolean) {
        hideControlsAnimator = AnimatorSet().apply {
            //play(hideToolbarAnimation).with(hideBottomControlsAnimation)
            play(hideToolbarAnimation)
            if (!immediate) {
                startDelay = CONTROLS_STAY_TIME
            }
            start()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    hideControlsAnimator = null
                }
            })
        }
    }

    private fun setupControlsAnimation() {
        hideToolbarAnimation =
            ObjectAnimator.ofFloat(conferenceToolbarBinding.conferenceAppBar, "alpha", 1f, 0f)
        hideToolbarAnimation.duration = CONTROLS_FADE_TIME;
        hideToolbarAnimation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                conferenceToolbarBinding.conferenceAppBar.visibility = GONE
                //activity.showHideToolBar(true)
            }
        })

    }

    private fun showControls() {
        if (!conferenceToolbarBinding.conferenceAppBar.isVisible) {
            conferenceToolbarBinding.conferenceAppBar.visibility = VISIBLE
            conferenceToolbarBinding.conferenceAppBar.alpha = 1f
            //activity.showHideToolBar(false)
        }

        if (!avControlBinding.controlsBottomContainer.isVisible) {
            avControlBinding.controlsBottomContainer.visibility = VISIBLE
            avControlBinding.controlsBottomContainer.alpha = 1f
        }

    }

    @UiThread
    private fun onHangupControlTapped() {

        //TODO: merge change-- removed below line and added if() code---need discussion

//        UcLog.l(logTAG, "User tapped hangup button")
//       // val activity = context as CallActivity
//        activity.showEndMeetingDialog()
        if (conference.closing) {
            UcLog.i(logTAG, "Ignoring tap on hangup button since conference is already terminating")
        } else {
            UcLog.l(logTAG, "User tapped hangup button")
            onHangupConfirmed()
        }
    }

    @UiThread
    private fun onFullScreenControlTapped() {
        UcLog.l(logTAG, "User tapped full screen button")
        conferenceToolbarBinding.conferenceAppBar.visibility = GONE
        conference.toggleFullscreenMode()
    }

    @UiThread
    private fun onVideoControlTapped() {
        val action = if (localVideoBlocked) "unblocking" else "blocking"
        UcLog.l(logTAG, "User tapped video block button: $action video")
        conference.toggleVideoBlock()
    }

    @UiThread
    private fun onMicrophoneControlTapped() {
        UcLog.l(logTAG, "User tapped audio mute button in state ${conference.muteState.value}")
        conference.toggleAudioMute()
    }

    override fun onHangupConfirmed() {
        // Reset the flag so that joining the conference again sees the animation.
        viewModel.startingAnimationShownLiveData.value = false
        conference.hangupConference()
        activity.moveToLoginScreen()
    }

    @UiThread
    private fun onSwitchCameraControlTapped() {
        UcLog.l(logTAG, "User tapped switch camera button")
        conference.switchCamera()
    }

    override fun startUi() {
        //merge change from isActive() to active
        if (!conference.active) {
            return
        }
        audioDeviceManager.addAudioDeviceManagerListener(this)

        startObserving()
        //merge change from if(viewModel.startingAnimationShown) to if(viewModel.startingAnimationShown && mediaEstablished)
        if (viewModel.startingAnimationShown && mediaEstablished) {
            updateViewsForScreenMode()

            //merge change---added below new code
            // Handle the case of being recreated after device rotation
            conference.refreshOrientation()
        } else {
            showConnecting()
        }
    }

    private fun showConnecting() {

        //merge changes is here commented code replced by below code in if condition

//        fullscreenBinding.progress.setVisibleAtFront()
//        // The default string references a video call, so change it for an
//        // audio-only call.
//        if (!conference.videoLicensed) {
//            fullscreenBinding.note.setText(R.string.starting_audio_call)
//        }
//        fullscreenBinding.note.setVisibleAtFront()

        if (conference.conferenceType.spacesCalling && conference.conferenceType is OutgoingSpacesCall) {
            fullscreenBinding.note.setText(R.string.calling_ellipsis)
            //below commented code is not required as per requirement currently
//            val directOtherParty = conference.topic.directOtherParty
//            remotePartyName.text = directOtherParty?.userInfo?.displayName
//            Glide.with(remotePartyImage)
//                .load(directOtherParty?.userInfo?.pictureUrl)
//                .error(R.drawable.ic_common_avatar_192dp)
//                .circleCrop()
//                .into(remotePartyImage)
//            remotePartyImage.visibility = VISIBLE
//            remotePartyName.visibility = VISIBLE
            fullscreenBinding.progress.visibility = GONE
        } else {
            fullscreenBinding.note.setText(startingMessageId)
            fullscreenBinding.progress.visibility = VISIBLE
        }
        fullscreenBinding.note.visibility = VISIBLE


        startingAnimation = true
        viewModel.startingAnimationShownLiveData.value = true
    }

    private val startingMessageId: Int
        @StringRes
        get() = if (conference.videoLicensed) {
            R.string.starting_video_call
        } else {
            R.string.starting_audio_call
        }

    private fun removeConnectingView() {
        fullscreenBinding.note.visibility = GONE
        fullscreenBinding.progress.visibility = GONE
        startingAnimation = false
    }

    @UiThread
    private fun onMediaEstablishedChanged(mediaEstablished: Boolean) {
        if (mediaEstablished) {
            if (startingAnimation) {
                //merge change -removed commented code and added removeConnectingView()
//                fullscreenBinding.fullscreenOpaque.removeView(fullscreenBinding.note)
//                fullscreenBinding.fullscreenOpaque.removeView(fullscreenBinding.progress)
//                startingAnimation = false
                removeConnectingView()

                updateViewsForScreenMode()
            }

        }

//        audioRouteControl.isEnabled = mediaEstablished
//        updateAudioRouteControl(audioDeviceManager.currentAudioDevice)
        //merge change--added below new line
        //updateRecordingControl()
    }

    @UiThread
    @Suppress("UNUSED_PARAMETER")
    private fun onFullscreenChanged(newFullscreen: Boolean) {
        // We will get a notification of false when first observing, which
        // we don't care about.
        if (mediaEstablished) {
            updateViewsForScreenMode()
        }
    }

    private fun startObserving() {
        with(conference) {
            fullscreen.observeForever(fullscreenObserver)
            //merge change--added below new line
            isFrontCameraActive.observeForever(activeCameraObserver)
            activeSpeaker.observeForever(activeSpeakerObserver)
            screenSharingAvailable.observeForever(screenSharingAvailableObserver)
            screenSharingBeingReceived.observeForever(screenSharingBeingReceivedObserver)
            screenSharingSource.observeForever(screenSharingSourceObserver)
            screenSharingFrame.observeForever(screenSharingFrameObserver)

            mediaEstablished.observeForever(mediaEstablishedObserver)
            videoBlocked.observeForever(videoBlockObserver)
            muteState.observeForever(muteStateObserver)
            audioMuteInitiator.observeForever(audioMuteInitiatorObserver)
            muteAllApplied.observeForever(muteAllAppliedObserver)

            //merge change--added below new line
            mpaasNetworkStatus.observeForever(mpaasNetworkStatusObserver)
        }
        //connectivityLiveData.observeForever(connectivityObserver)
    }


    private fun updateViewsForScreenMode() {
        UcLog.d(logTAG, "Updating render views: fullscreen=$fullscreen")
        setFullscreenMode(fullscreen)
        videoRenderingFacade.setRemoteRenderer(if (fullscreen) fullscreenBinding.fullscreenRemoteVideo else minimizedView)
        minimizedView.hide(fullscreen)
        minimizedView.visibility = if (fullscreen) GONE else VISIBLE
        fullscreenBinding.fullscreenOpaque.visibility = if (fullscreen) VISIBLE else GONE
        fullscreenBinding.fullscreenRemoteVideo.visibility =
            if (fullscreen) VISIBLE else GONE

        fullscreenBinding.localVideoContainer.visibility =
            if (fullscreen) VISIBLE else GONE
        switchSelfVideo()

        if (BuildConfig.DEBUG) {
            UcLog.d(logTAG, "Minimized: $minimizedView")
            UcLog.d(logTAG, "Fullscreen local: $fullscreenVideoLocal")
            UcLog.d(logTAG, "Fullscreen remote: ${fullscreenBinding.fullscreenRemoteVideo}")
        }

//        if (fullscreen && conferenceToolbarBinding.conferenceAppBar.isVisible) {
//            //merge change
//            //wakeUpControls()
//            startHideControlsAnimation()
//        }
    }

    @UiThread
    private fun switchSelfVideo() {
        if (BuildConfig.DEBUG) {
            UcLog.d(
                logTAG,
                "switchSelfVideo: localVideoBlocked=$localVideoBlocked fullscreen=$fullscreen"
            )
        }
        val needVideo = !localVideoBlocked && fullscreen
        val localRenderer: CallVideoSink? = if (needVideo) fullscreenVideoLocal else null
        videoRenderingFacade.setLocalRenderer(localRenderer)
        fullscreenVideoLocal.hide(!needVideo)
    }

    private fun setFullscreenMode(fullscreen: Boolean) {
//        val activity = context as CallActivity
        //activity.showHideToolBar(true)
        activity.toggleFullscreen(fullscreen)
    }


    override fun stopUi() {
        hideControlsAnimator?.cancel()
        fullscreenBinding.screenSharingView.setImageBitmap(null)
        stopObserving()
        if (fullscreen) {
            setFullscreenMode(false)
        }
        audioDeviceManager.removeAudioDeviceManagerListener(this)
        videoRenderingFacade.setLocalRenderer(null)
        videoRenderingFacade.setRemoteRenderer(null)
        // SurfaceViewRenderer needs to be released on a hangup or when view is destroyed.
        fullscreenBinding.fullscreenRemoteVideo.release()
        fullscreenBinding.fullscreenOpaque.removeAllViews()
        parentView.removeView(fullscreenBinding.fullscreenOpaque)
        parentView.removeView(minimizedView)
        parentView.removeView(conferenceToolbarBinding.conferenceAppBar)
    }

    private fun stopObserving() {
        with(conference) {
            fullscreen.removeObserver(fullscreenObserver)

            //merge change added below new line
            isFrontCameraActive.removeObserver(activeCameraObserver)
            mpaasNetworkStatus.removeObserver(mpaasNetworkStatusObserver)
            activeSpeaker.removeObserver(activeSpeakerObserver)
            screenSharingAvailable.removeObserver(screenSharingAvailableObserver)
            screenSharingBeingReceived.removeObserver(screenSharingBeingReceivedObserver)
            screenSharingSource.removeObserver(screenSharingSourceObserver)
            screenSharingFrame.removeObserver(screenSharingFrameObserver)

            mediaEstablished.removeObserver(mediaEstablishedObserver)
            videoBlocked.removeObserver(videoBlockObserver)
            muteState.removeObserver(muteStateObserver)
            audioMuteInitiator.removeObserver(audioMuteInitiatorObserver)
            muteAllApplied.removeObserver(muteAllAppliedObserver)

        }
        //connectivityLiveData.removeObserver(connectivityObserver)
    }

    private fun setAudioControlState() {
        avControlBinding.avControlMic.setMuteState(conference.muteState.value)
    }

    private fun setVideoControlState() {
        if (conference.videoLicensed) {
            val videoMuteActive = localVideoBlocked
            avControlBinding.avControlVideo.visibility = VISIBLE

            //merge change added below new line
            avControlBinding.avControlVideo.isEnabled = true

            avControlBinding.avControlVideo.setImageResource(if (videoMuteActive) R.drawable.ic_video_off else R.drawable.ic_video_on)

            //merge change-- change from allow to allow_camera, disallow to block_camera
            avControlBinding.avControlVideo.contentDescription =
                if (videoMuteActive) resources.getString(R.string.allow_camera) else resources.getString(
                    R.string.block_camera
                )
            avControlBinding.avControlVideo.text =
                if (videoMuteActive) resources.getString(R.string.allow) else resources.getString(R.string.block)
            avControlBinding.avControlVideo.tag =
                if (videoMuteActive) "video_muted" else "video_unmuted"
            avControlBinding.avControlSwitchCamera.isEnabled = !videoMuteActive
            //merge change added below new line
            avControlBinding.avControlSwitchCamera.visibility = VISIBLE
        } else {
            avControlBinding.avControlVideo.visibility = View.INVISIBLE
            avControlBinding.avControlSwitchCamera.isEnabled = false

            //merge change added below lines
            avControlBinding.avControlVideo.isEnabled = false
            avControlBinding.avControlVideo.setImageResource(R.drawable.ic_video_off)
            avControlBinding.avControlVideo.contentDescription =
                resources.getString(R.string.allow_camera)
            avControlBinding.avControlVideo.text = resources.getString(R.string.allow)
            avControlBinding.avControlVideo.tag = "video_muted"
        }
    }

    //Merge change-- added below observer method
    @UiThread
    private fun onActiveCameraChanged(isFrontCamera: Boolean) {
        fullscreenVideoLocal.setMirror(isFrontCamera)
    }

    @UiThread
    private fun onActiveSpeakerChanged(activeSpeaker: ActiveSpeaker?) {
        val hidden = shouldHideActiveSpeakerView(activeSpeaker)
        UcLog.i(logTAG, "AV: Speaker view hidden: $hidden")
        activeSpeakerView.visibility = if (hidden) GONE else VISIBLE
        if (activeSpeaker != null) {
            activeSpeakerView.applyActiveSpeaker(activeSpeaker)
        }
    }

    //Merge change from AudioDeviceType to AudioDevice
    override fun onAudioDeviceChanged(activeAudioDevice: AudioDevice) {
    }

    @UiThread
    @Suppress("UNUSED_PARAMETER")
    private fun onAudioMuteStatusChanged(newStatus: ConferenceMuteState) {
        UcLog.d(logTAG, "Audio mute status changed to $newStatus")
        setAudioControlState()
    }

    @UiThread
    @Suppress("UNUSED_PARAMETER")
    private fun onVideoBlockStatusChanged(newStatus: Boolean) {
        setVideoControlState()
        switchSelfVideo()
    }

    @UiThread
    private fun onAudioMuteInitiatorChanged(mutingUser: UserInfo?) {
        if ((mutingUser != null) && !TextUtils.isEmpty(mutingUser.displayName)) {
            val message =
                context.getString(R.string.remote_muted_by_message, mutingUser.displayName)
            activity.showMessage(message)
        }
    }

    @UiThread
    private fun onMuteAllAppliedChanged(muteAllApplied: Boolean) {
        if (muteAllApplied) {
            activity.showMessage(context.getString(R.string.all_muted))
        }
    }

    private fun onMpaasNetworkStatusChanged(status: MpaasNetworkStatus) {
        val drawableRes = when (status) {
            MpaasNetworkStatus.NETWORK_STATUS_NO_VALUE -> R.drawable.ic_signal_0
            MpaasNetworkStatus.NETWORK_STATUS_1 -> R.drawable.ic_signal_1
            MpaasNetworkStatus.NETWORK_STATUS_2 -> R.drawable.ic_signal_2
            MpaasNetworkStatus.NETWORK_STATUS_3 -> R.drawable.ic_signal_3
            MpaasNetworkStatus.NETWORK_STATUS_4 -> R.drawable.ic_signal_4
            MpaasNetworkStatus.NETWORK_STATUS_5 -> R.drawable.ic_signal_5
        }
        fullscreenVideoLocal.showNetworkStatusIcon(drawableRes)
    }

    @UiThread
    @Suppress("UNUSED_PARAMETER")
    private fun onScreenSharingAvailableChanged(available: Boolean) {
        setScreenShareControlState()
    }

    @UiThread
    private fun onScreenSharingBeingReceivedChanged(receiving: Boolean) {
        fullscreenBinding.screenSharingView.visibility = if (receiving) VISIBLE else GONE
        setScreenShareControlState()
    }

    @UiThread
    private fun onScreenSharingSourceChanged(source: String?) {
        if (!TextUtils.isEmpty(source)) {
            showScreenSharingReceiveStartToast(source!!)
        }
    }

    private fun showScreenSharingReceiveStartToast(sharerDisplayName: String) {
        activity.showMessage(
            context.getString(R.string.is_sharing_screen, sharerDisplayName)
        )
    }

    @UiThread
    private fun onScreenShareFrame(frame: Bitmap?) {
        if (frame != null) {
            fullscreenBinding.screenSharingView.setImageBitmap(frame)
        }
    }

    private fun setScreenShareControlState() {
        avControlBinding.avControlSharescreen.setImageResource(if (screenSharingBeingReceived) R.drawable.ic_screen_share_off_selector else R.drawable.ic_screenshare_on_selector)
        avControlBinding.avControlSharescreen.text =
            if (screenSharingBeingReceived) resources.getString(R.string.hide) else resources.getString(
                R.string.show
            )
        avControlBinding.avControlSharescreen.tag =
            if (screenSharingBeingReceived) "screenshare_on" else "screenshare_off"
        avControlBinding.avControlSharescreen.isEnabled =
            screenSharingBeingReceived || screenSharingAvailable
    }

    @UiThread
    private fun onSharingControlTapped() {
        UcLog.l(logTAG, "User tapped screen sharing button")
        conference.toggleScreenShare()
    }

    private fun createMinimizedVideoView(): AVVideoPort =
        AVVideoPort.create(context, rootEglBase!!, MIN_REMOTE, 20f, 20f, 5f, 70f)

    private fun createLocalVideoView(): AVVideoPort =
        AVVideoPort.create(context, rootEglBase!!, FULL_LOCAL, 20f, 20f, 75f, 70f)


    override fun onAvailableAudioDevicesListChanged(
        availableAudioDevices: List<AudioDevice>,
        currentAudioDevice: AudioDevice
    ) {
        TODO("Not yet implemented")
    }

}

// Parameters for the controls fade out (all in milliseconds)
private const val CONTROLS_FADE_TIME: Long = 300
private const val CONTROLS_STAY_TIME: Long = 5000

private fun shouldHideActiveSpeakerView(activeSpeaker: ActiveSpeaker?): Boolean =
    activeSpeaker?.member == null || activeSpeaker.member!!.mediaSessionVideo


enum class ConferenceControlType {
    VIDEO_MUTE, SWITCH_CAMERA, MICROPHONE_MUTE, AUDIO_ROUTE, HANGUP, FULLSCREEN, SHARE_SCREEN, MUTE_ALL, RECORD
}
