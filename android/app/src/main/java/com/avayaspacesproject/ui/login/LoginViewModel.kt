package com.avayaspacesproject.ui.login

import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.avaya.spacescsdk.exceptions.CSDKResponseException
import com.avaya.spacescsdk.services.auth.model.Credential
import com.avaya.spacescsdk.services.auth.model.GuestCredential
import com.avaya.spacescsdk.services.auth.model.OAuthCredential
import com.avaya.spacescsdk.services.auth.model.UsernamePasswordCredential
import com.avaya.spacescsdk.services.call.model.*
import com.avaya.spacescsdk.services.spaces.Spaces
import com.avaya.spacescsdk.services.spaces.enums.ResponseErrorCode
import com.avaya.spacescsdk.services.spaces.listeners.FollowInviteTopicListener
import com.avaya.spacescsdk.services.spaces.listeners.InvitationJoin
import com.avaya.spacescsdk.services.spaces.listeners.SpacesAPIGetTopicListener
import com.avaya.spacescsdk.services.spaces.model.SpacesTopic
import com.avaya.spacescsdk.services.user.SpacesUser
import com.avaya.spacescsdk.services.user.listeners.SpacesLoginListener
import com.avaya.spacescsdk.services.user.model.SpacesUserInfo
import com.avayaspacesproject.R
import com.avayaspacesproject.ui.events.GoToDashboardEvent
import com.avayaspacesproject.ui.events.PasswordRequiredEvent
import com.avayaspacesproject.ui.events.SingleLiveEvent
import com.avayaspacesproject.ui.events.TopicFetchedEvent
import javax.inject.Inject

class LoginViewModel @Inject constructor(private val user: SpacesUser, private val spaces: Spaces) :
    ViewModel() {

    private val defaultGuestUser = "guestUser"
    private val logTag: String = "LoginViewModel"

    private lateinit var spacesUserInfo: MutableLiveData<SpacesUserInfo?>
    private lateinit var spacesTopic: MutableLiveData<SpacesTopic>

    private var action: ExternalIntentAction = UnknownAction

    private val _eventLiveData = MutableLiveData<SingleLiveEvent>()
    val eventLiveData: LiveData<SingleLiveEvent> = _eventLiveData

    private val _responseErrorCode = MutableLiveData<ResponseErrorCode>()
    val responseErrorCode: LiveData<ResponseErrorCode> = _responseErrorCode

    private val _handleError = MutableLiveData<CSDKResponseException?>()
    val handleError: MutableLiveData<CSDKResponseException?> = _handleError

    private val _loginForm = MutableLiveData<JoinMeetingFormState>()
    val loginFormState: LiveData<JoinMeetingFormState> = _loginForm


    val nextUri: Uri?
        get() = when (action) {
            is UnknownAction -> null
            else -> action.uri
        }

    fun login(
        credential: Credential?
    ): MutableLiveData<SpacesUserInfo?> {
        if (!::spacesUserInfo.isInitialized) {
            spacesUserInfo = MutableLiveData()
        }
        when (credential) {
            null -> loginUsingSavedCredentials()
            is GuestCredential -> performGuestLogin(credential)
            is OAuthCredential -> loginAsRegisteredUser(credential)
            is UsernamePasswordCredential -> loginUsingUsernamePasswordCredentials(credential)
            //loginUsingUsernamePasswordCredentials("jerryxie_test3@avaya.com","Test,5389581")

        }


        return spacesUserInfo
    }


    fun loginDataChanged(username: String, meetingUrl: String) {
        if (!isMeetingUrlValid(meetingUrl)) {
            _loginForm.value = MeetingUrlError(R.string.invalid_meeting_url)

        } else if (!isDisplayNameValid(username)) {
            _loginForm.value = DisplayNameError(R.string.invalid_username)
        } else {
            _loginForm.value = JoinMeetingValidData
        }
    }

    fun usernamePasswordLoginChanged(username: String,password:String){
        if (username.isBlank()) {
            _loginForm.value = DisplayUserNameError(R.string.invalid_username)
        }else if(password.isBlank()){
            _loginForm.value = DisplayPasswordError(R.string.invalid_password)
        }else{
            _loginForm.value=ValidUsernamePassword
        }
    }

    private fun isDisplayNameValid(username: String): Boolean = username.isNotBlank()

    private fun isMeetingUrlValid(meetingUrl: String): Boolean {
        return meetingUrl.isNotBlank() && URLUtil.isValidUrl(meetingUrl) && action !is UnmatchedUriAction
    }

    private fun performGuestLogin(credential: GuestCredential) {

        Log.d(logTag, "in performGuestLogin:{$credential.displayName}")


        user?.loginAsGuest(credential, object : SpacesLoginListener {
            override fun onLoginError(exception: CSDKResponseException?) {
                Log.d(logTag, "in onLoginError of loginAsGuestUser")
                Log.d(logTag, "error:" + exception?.message)
                _handleError.postValue(exception)
            }

            override fun onLoginFinished(userInfo: SpacesUserInfo?) {
                Log.d(logTag, "in onLoginFinished of loginAsGuestUser")
                Log.d(logTag, "loganUserInfo:$userInfo")
                spacesUserInfo.postValue(userInfo)
            }
        })
    }

    fun performActionOnUri(url: String, meetingPassword: String) {

        if (action != null) {
            val meetingUri: Uri = Uri.parse(url)

            this.action = spaces.getActionForUri(meetingUri)
            Log.d(logTag, "performActionOnUri:$action")

            handleExternalIntentAction(meetingPassword)
        }
    }

    private fun handleExternalIntentAction(meetingPassword: String) {
        Log.d(logTag, "handleExternalIntentAction:$action")
        if (action.involvesJoiningSpace) {
            when (val action = action) {
                is JoinPersonalMeetingRoomAction -> joinPersonalRoom(action)
                is JoinSpaceAction -> joinSpace(action, meetingPassword)
                is AcceptInvitationAction -> acceptInvitation(action, meetingPassword)
                //is ShowDashboardAction -> goToHomeScreen()
                else -> {}
            }

        } else if (action is ShowDashboardAction) {
            goToHomeScreen()
        }
//        else if (action is UnmatchedUriAction) {
//            reportInvalidUrl()
//        }
    }

    private fun goToHomeScreen() {
        Log.d(logTag, "goToHomeScreen:$action")
        _eventLiveData.postValue(GoToDashboardEvent(action))
    }

    private fun reportInvalidUrl() {
        Log.d(logTag, "reportInvalidUrl")
        _handleError.postValue(CSDKResponseException("Not a valid meeting url"))
    }

    private fun acceptInvitation(action: AcceptInvitationAction, meetingPassword: String) {
        spaces.followInvite(action, meetingPassword, object : FollowInviteTopicListener {
            override fun onCallback(invite: InvitationJoin) {
                _eventLiveData.postValue(
                    TopicFetchedEvent(
                        action,
                        invite.topicTitle,
                        action.meetingPassword, invite.topicId
                    )
                )
            }

            override fun onError(inviteId: String, code: ResponseErrorCode) {
                Log.d(logTag, "acceptInvitation onError:${code}")
                handleSpaceErrorResponse(code)
            }
        })
    }

    private fun joinSpace(action: JoinSpaceAction, meetingPassword: String) {
        spaces.joinSpace(action, meetingPassword, object : SpacesAPIGetTopicListener {
            override fun onTopicFetched(topic: SpacesTopic) {
                Log.d(logTag, "spacesTopic:${topic}")
                _eventLiveData.postValue(
                    TopicFetchedEvent(
                        action,
                        topic.title,
                        action.meetingPassword, topic.topicId
                    )
                )
            }

            override fun onError(topicId: String, code: ResponseErrorCode) {
                Log.d(logTag, "joinSpace onError:${code}")
                handleSpaceErrorResponse(code)
            }
        })
    }

    private fun joinPersonalRoom(action: JoinPersonalMeetingRoomAction) {

        spaces.joinPersonalRoom(action, null, object : SpacesAPIGetTopicListener {
            override fun onTopicFetched(topic: SpacesTopic) {
                Log.d(logTag, "spacesTopic:${topic}")
                _eventLiveData.postValue(
                    TopicFetchedEvent(
                        action,
                        topic.title,
                        action.meetingPassword, topic.topicId
                    )
                )
            }

            override fun onError(topicId: String, code: ResponseErrorCode) {
                Log.d(logTag, "joinPersonalRoom onError:${code}")
                handleSpaceErrorResponse(code)
            }
        })
    }

    private fun handleSpaceErrorResponse(code: ResponseErrorCode) {
        when (code) {
            ResponseErrorCode.SPACE_PASSWORD_WRONG -> _eventLiveData.postValue(
                PasswordRequiredEvent(
                    false
                )
            )
            ResponseErrorCode.SPACE_PASSWORD_REQUIRED -> _eventLiveData.postValue(
                PasswordRequiredEvent(true)
            )

            else -> {}
        }
    }

    fun exit() {
        //user.signOut()
    }

    private fun loginAsRegisteredUser(oAuthCredential: OAuthCredential) {


        user.loginUsingOAuth(
            oAuthCredential, object : SpacesLoginListener {
                override fun onLoginError(exception: CSDKResponseException?) {
                    Log.d(logTag, "in onLoginError of loginAsRegisteredUser")
                    Log.d(logTag, "error:" + exception?.message)
                    _handleError.postValue(exception)
                }

                override fun onLoginFinished(userInfo: SpacesUserInfo?) {
                    Log.d(logTag, "in onLoginFinished of loginAsRegisteredUser")
                    Log.d(logTag, "loganUserInfo:$userInfo")
                    spacesUserInfo.postValue(userInfo)
                }
            })
    }


    private fun loginUsingUsernamePasswordCredentials(userPassCredential: UsernamePasswordCredential) {


        user.loginUsingCredentials(
            userPassCredential, object : SpacesLoginListener {
                override fun onLoginError(exception: CSDKResponseException?) {
                    Log.d(logTag, "in onLoginError of loginAsEmailRegisteredUser")
                    Log.d(logTag, "error:" + exception?.message)
                    _handleError.postValue(exception)
                }

                override fun onLoginFinished(userInfo: SpacesUserInfo?) {
                    Log.d(logTag, "in onLoginFinished of loginAsEmailRegisteredUser")
                    Log.d(logTag, "loganUserInfo:$userInfo")
                    spacesUserInfo.postValue(userInfo)
                }
            })
    }

    private fun loginUsingSavedCredentials() {
        user.loginAsSavedUsed(object : SpacesLoginListener {
            override fun onLoginError(exception: CSDKResponseException?) {
                Log.d(logTag, "in onLoginError of loginAsRegisteredUser")
                Log.d(logTag, "error:" + exception?.message)
                _handleError.postValue(exception)
            }

            override fun onLoginFinished(userInfo: SpacesUserInfo?) {
                Log.d(logTag, "in onLoginFinished of loginAsRegisteredUser")
                Log.d(logTag, "loganUserInfo:$userInfo")
                spacesUserInfo.postValue(userInfo)
            }
        })
    }

    fun signOut() {
        user.signOut()
    }


}

/**
 * Data validation state of the login form.
 */
sealed class JoinMeetingFormState

data class DisplayNameError(
    @StringRes val stringId: Int
) : JoinMeetingFormState()

data class MeetingUrlError(
    @StringRes val stringId: Int
) : JoinMeetingFormState()

data class DisplayUserNameError(
    @StringRes val stringId: Int
): JoinMeetingFormState()

data class DisplayPasswordError(
    @StringRes val stringId: Int
): JoinMeetingFormState()


object JoinMeetingValidData : JoinMeetingFormState()

object ValidUsernamePassword:JoinMeetingFormState()

enum class LoginType {
    GUEST,
    OAUTH,
    CREDENTIALS

}