package com.avayaspacesproject

import android.util.Log
import com.facebook.react.bridge.*;
import com.avaya.spacescsdk.SpacesCSDK
import com.avaya.spacescsdk.listeners.SDKInitListener
import com.avaya.spacescsdk.services.user.SpacesUser

class ImportandInitSDK internal constructor(context: ReactApplicationContext?) :
    ReactContextBaseJavaModule(context) {
    override fun getName(): String {
        return "ImportandInitSDK"
    }

    @ReactMethod
    fun createCounterKotEvent(cb: Callback) {
        Log.d("counter module", "logged from counter kotlin module")
        cb.invoke("data returned from native kotlin project")
    }
    @ReactMethod
    fun setupSDKInitialization(cb: Callback) {

        SpacesCSDK.init(
            object : SDKInitListener {
                override fun onInit(spacesUserSdk: SpacesUser) {
                    var sdkinit = spacesUserSdk

                    var spaces = sdkinit.getSpaces()
                    Log.d("spaces objectget","")
                    cb.invoke("success");
                }
            })

    }

    @ReactMethod
    fun createCounterKotPromise(pr: Promise) {
        try {
            pr.resolve("data returned from kotlin promise")
        } catch (err: Exception) {
            pr.reject("error returned from kotlin promise",  err)
        }
    }
}