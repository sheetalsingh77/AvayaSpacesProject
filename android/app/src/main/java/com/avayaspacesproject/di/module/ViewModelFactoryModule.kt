package com.avayaspacesproject.di.module

import androidx.lifecycle.ViewModelProvider
import com.avayaspacesproject.ui.ViewModelFactory
import dagger.Binds
import dagger.Module

@Module
abstract class ViewModelFactoryModule {

    @Binds
    internal abstract fun bindViewModelFactory(factoryModule: ViewModelFactory): ViewModelProvider.Factory
}