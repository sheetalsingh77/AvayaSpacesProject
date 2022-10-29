package com.avayaspacesproject.di.component


import com.avayaspacesproject.ReferenceApp
import com.avayaspacesproject.di.module.ActivityModule
import com.avayaspacesproject.di.module.LibraryModule
import com.avayaspacesproject.di.module.RefApplicationModule
import com.avayaspacesproject.di.module.ViewModelFactoryModule
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import javax.inject.Singleton

@Singleton
@Component(modules = [AndroidInjectionModule::class, RefApplicationModule::class, LibraryModule::class, ViewModelFactoryModule::class])
interface ReferenceAppComponent : AndroidInjector<ReferenceApp> {
    override fun inject(application: ReferenceApp)

    fun buildActivityComponent(activityModule: ActivityModule): ActivityComponent


}