package com.avayaspacesproject

import android.annotation.SuppressLint
import android.content.Context
import com.avaya.spacescsdk.SpacesCSDK
import com.avaya.spacescsdk.listeners.SDKInitListener
import com.avaya.spacescsdk.services.user.SpacesUser
import com.avayaspacesproject.di.component.DaggerReferenceAppComponent
import com.avayaspacesproject.di.component.ReferenceAppComponent
import com.avayaspacesproject.di.module.RefApplicationModule
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication


class ReferenceApp : DaggerApplication() {

    companion object {
        lateinit var spacesUser: SpacesUser
        lateinit var appComponent: ReferenceAppComponent
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

    }

    override fun onCreate() {
        setupDagger()
        super.onCreate()
        context = applicationContext

        setupSDKInitialization()

    }

    fun setupSDKInitialization() {
        SpacesCSDK.init(
            object : SDKInitListener {
                override fun onInit(spacesUserSdk: SpacesUser) {
                    spacesUser = spacesUserSdk
                }
            })

    }

    override fun applicationInjector(): AndroidInjector<ReferenceApp> = appComponent

    private fun setupDagger() {

        appComponent = DaggerReferenceAppComponent.builder().refApplicationModule(
            RefApplicationModule(this)
        ).build()
        appComponent.inject(this)
    }
}