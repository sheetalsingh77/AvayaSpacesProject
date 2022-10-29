package com.avayaspacesproject.di.module


import com.avaya.spacescsdk.services.call.AudioDeviceManager
import com.avaya.spacescsdk.services.messaging.MessagingService
import com.avaya.spacescsdk.services.spaces.Spaces
import com.avaya.spacescsdk.services.user.SpacesUser
import com.avayaspacesproject.ReferenceApp
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

//this module is for references of spaces sdk
@Module
class LibraryModule {

    @Singleton
    @Provides
    fun provideSpacesUser(): SpacesUser = ReferenceApp.spacesUser

    @Singleton
    @Provides
    fun provideSpaces(): Spaces = ReferenceApp.spacesUser.getSpaces()

    @Singleton
    @Provides
    fun provideAudioDeviceManager(): AudioDeviceManager =
        ReferenceApp.spacesUser.getCallService().getAudioDeviceManager()

    @Singleton
    @Provides
    fun provideMessagingService(): MessagingService = ReferenceApp.spacesUser.getMessagingService()


}