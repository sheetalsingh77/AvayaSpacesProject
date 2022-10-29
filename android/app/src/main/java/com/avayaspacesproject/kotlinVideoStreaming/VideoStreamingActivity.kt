package com.avayaspacesproject.kotlinVideoStreaming

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.avaya.spacescsdk.services.auth.model.GuestCredential
import com.avayaspacesproject.R

class VideoStreamingActivity: AppCompatActivity {

    constructor(){
//        super();
    }

    private var joinAsGuestChosen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_streaming)
    }


     fun onClick(v: View?) {
        when (v?.id) {
            R.id.join_as_guest_button -> {

//                showLogs(logTAG, "clicked join as guest")
                Log.d("LoginActivity", "clicked join as guest")
                joinAsGuestChosen = true
                val guestUsername = R.id.display_name.toString();
                val guestCredential = GuestCredential(guestUsername)
                Log.d("LoginActivity user name", guestUsername)
//                performLogin(guestCredential)
            }
//            R.id.sign_in_button -> {
//                showLogs(logTAG, "clicked on Sign in / Create account button")
//                joinAsGuestChosen = false
//
//                val oAuthCredential = OAuthCredential(
//                    "58f62bb6a31329531a98",
//                    "1852cd814e261c27b30337ab2698c6df0922fee0",
//                    "spacesmobilesdk://oauth2"
//                )
//
//                performLogin(oAuthCredential)
//            }
//            R.id.user_pass_login_button -> {
//                showLogs(logTAG, "clicked on Sign in button")
//                val username = binding.username.text.toString()
//                val password = binding.password.text.toString()
//                val userPassCredential = UsernamePasswordCredential(
//                    "58f62bb6a31329531a98",
//                    "1852cd814e261c27b30337ab2698c6df0922fee0",
//                    username, password
//
//                )
//                performLogin(userPassCredential)
//            }
//
//            R.id.connect_button -> {
//                meetingUrl = binding.meetingUrl.text.toString()
//                meetingPassword = binding.meetingPassword.text.toString()
//                spacesUserInfo = spacesUser.getUserInfo()!!
//                viewModel.performActionOnUri(meetingUrl, meetingPassword)
//
//            }
//            R.id.sign_out -> {
//                viewModel.signOut();
//                toggleSignInView(true)
//                toggleMeetingView(false)
//            }

        }
    }

}

