package com.avayaspacesproject.ui.home


import androidx.lifecycle.ViewModelProvider
import com.avayaspacesproject.ReferenceApp
import com.avayaspacesproject.databinding.ActivityHomeBinding
import com.avayaspacesproject.di.component.ActivityComponent
import com.avayaspacesproject.di.module.ActivityModule
import com.avayaspacesproject.ui.base.BaseActivity


class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    private lateinit var activityComponent: ActivityComponent
    private lateinit var homeViewModel: HomeViewModel

    override fun getViewBinding(): ActivityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)

    override fun initializeView() {


    }

    override fun initializeViewModel() {
        homeViewModel = ViewModelProvider(this, viewModelFactory).get(HomeViewModel::class.java)
    }

    override fun injectDependency() {
        activityComponent = ReferenceApp.appComponent.buildActivityComponent(ActivityModule(this))
        activityComponent.inject(this)
    }


}