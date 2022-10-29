package com.avayaspacesproject.di.module

import androidx.lifecycle.ViewModel
import com.avayaspacesproject.ui.conference.CallViewModel
import com.avayaspacesproject.ui.conference.chat.ChatViewModel
import com.avayaspacesproject.ui.conference.ideas.PostViewModel
import com.avayaspacesproject.ui.conference.members.TopicViewModel
import com.avayaspacesproject.ui.conference.tasks.TaskViewModel
import com.avayaspacesproject.ui.home.HomeViewModel
import com.avayaspacesproject.ui.join.JoinViewModel
import com.avayaspacesproject.ui.login.LoginViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    internal abstract fun bindLoginViewModel(viewModel: LoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(JoinViewModel::class)
    internal abstract fun bindJoinViewModel(viewModel: JoinViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CallViewModel::class)
    internal abstract fun bindCallViewModel(viewModel: CallViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    internal abstract fun bindHomeViewModel(viewModel: HomeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TopicViewModel::class)
    internal abstract fun bindTopicViewModel(viewModel: TopicViewModel): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(ChatViewModel::class)
    internal abstract fun bindChatViewModel(viewModel: ChatViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TaskViewModel::class)
    internal abstract fun bindTaskViewModel(viewModel: TaskViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PostViewModel::class)
    internal abstract fun bindPostViewModel(viewModel: PostViewModel): ViewModel
}