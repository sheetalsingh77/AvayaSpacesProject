package com.avayaspacesproject.di.component

import androidx.viewbinding.ViewBinding

import com.avayaspacesproject.di.module.ActivityModule
import com.avayaspacesproject.di.module.ActivityScoped
import com.avayaspacesproject.di.module.FragmentModule
import com.avayaspacesproject.di.module.ViewModelModule
import com.avayaspacesproject.ui.SendLogActivity
import com.avayaspacesproject.ui.base.BaseActivity
import com.avayaspacesproject.ui.conference.CallActivity
import com.avayaspacesproject.ui.home.HomeActivity
import com.avayaspacesproject.ui.join.JoinMeetingActivity
import com.avayaspacesproject.ui.login.LoginActivity
import dagger.Subcomponent
import dagger.android.ContributesAndroidInjector

@Subcomponent(modules = [ActivityModule::class, ViewModelModule::class,FragmentModule::class])
interface ActivityComponent {

    fun inject(baseActivity: BaseActivity<ViewBinding>)

    fun inject(loginActivity: LoginActivity)

    fun inject(joinMeetingActivity: JoinMeetingActivity)

    @ActivityScoped
    fun inject(callActivity: CallActivity)

    fun inject(homeActivity: HomeActivity)

    fun inject(sendLogActivity: SendLogActivity)
}