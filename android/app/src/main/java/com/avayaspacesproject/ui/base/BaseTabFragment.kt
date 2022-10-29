package com.avayaspacesproject.ui.base

import androidx.fragment.app.Fragment
import com.avayaspacesproject.ui.conference.SpaceTabType
import com.avayaspacesproject.utils.OnBackPressed
import dagger.android.support.DaggerFragment

abstract class BaseTabFragment : Fragment(),
    OnBackPressed {
    override fun onBackPressed(): Boolean {
        return false
    }

    abstract val tabType: SpaceTabType?
}