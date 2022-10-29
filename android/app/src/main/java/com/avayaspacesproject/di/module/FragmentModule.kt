package com.avayaspacesproject.di.module

import com.avayaspacesproject.ui.conference.chat.ChatTabFragment
import com.avayaspacesproject.ui.conference.ideas.IdeasTabFragment
import com.avayaspacesproject.ui.conference.members.MembersTabFragment
import com.avayaspacesproject.ui.conference.tasks.TasksTabFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FragmentModule {

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun provideMemberTabFragment(): MembersTabFragment?

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun provideChatTabFragment(): ChatTabFragment?

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun provideTasksTabFragment(): TasksTabFragment?

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun provideIdeasTabFragment(): IdeasTabFragment?
}