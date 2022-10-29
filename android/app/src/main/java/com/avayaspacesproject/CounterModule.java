package com.avayaspacesproject;

import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import java.util.Map;
import java.util.HashMap;

public class CounterModule extends ReactContextBaseJavaModule {
    CounterModule(ReactApplicationContext context) {
        super(context);
    }

    @NonNull
    @Override
    public String getName() {
        return "CounterModule";
    }

    @ReactMethod
    public void createCounterEvent(Callback cb){
        Log.d("counter module","logged from counter module");
        cb.invoke("data returned from native project");
    }

    @ReactMethod
    public void createCounterPromise(Promise pr){
        try{
            pr.resolve("data returned from promise");
        }
        catch(Exception err){
            pr.reject("error returned from promise", err);
        }
    }
}
