package com.avayaspacesproject;

import android.util.Log
import com.facebook.react.bridge.*;

class CounterKotModule internal constructor(context: ReactApplicationContext?) :
    ReactContextBaseJavaModule(context) {
    override fun getName(): String {
        return "CounterKotModule"
    }

    @ReactMethod
    fun createCounterKotEvent(cb: Callback) {
        Log.d("counter module", "logged from counter kotlin module")
        cb.invoke("data returned from native kotlin project")
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