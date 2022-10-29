package com.avayaspacesproject.di.module

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.avayaspacesproject.ReferenceApp
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RefApplicationModule(private val application: ReferenceApp) {

    @Provides
    @Singleton
    fun provideApplicationContext(): Context = application.applicationContext

    @Provides
    @Singleton
    fun provideApplication(): ReferenceApp {
        return application
    }

    @Provides
    @DefaultSharedPreferences
    fun provideSharedPreferences(): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application)


}