package com.avayaspacesproject.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.avayaspacesproject.ui.conference.SpaceTabType
import javax.inject.Inject

class HomeViewModel @Inject constructor(): ViewModel() {
    val selectedTab = MutableLiveData(SpaceTabType.CHAT_TAB)

}