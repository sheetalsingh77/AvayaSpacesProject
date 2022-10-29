package com.avayaspacesproject.ui.conference

import android.content.res.Resources
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.avayaspacesproject.R
import com.avayaspacesproject.ui.base.BaseTabFragment
import com.avayaspacesproject.ui.conference.chat.ChatTabFragment
import com.avayaspacesproject.ui.conference.ideas.IdeasTabFragment
import com.avayaspacesproject.ui.conference.members.MembersTabFragment
import com.avayaspacesproject.ui.conference.tasks.TasksTabFragment

class SpaceTabPagerAdapter(
    private val resources: Resources,
    fm: FragmentManager?
) : FragmentPagerAdapter(fm!!) {
    private var guestMode = false
    fun isGuestMode(): Boolean {
        return guestMode
    }

    fun setGuestMode(guest: Boolean) {
        if (guestMode != guest) {
            guestMode = guest
            notifyDataSetChanged()
        }
    }

    override fun getItemId(position: Int): Long {
        return indexToType(position, guestMode).index.toLong()
    }

    override fun getItemPosition(`object`: Any): Int {
        val v =
            (if (`object` is BaseTabFragment) `object` else null)
                ?: return 0
        var index = typeToIndex(v.tabType)
        if (index < 0) {
            index = 0
        }
        return index
    }

    fun typeToIndex(tabType: SpaceTabType?): Int {
        when (tabType) {
            SpaceTabType.CHAT_TAB -> return CHAT_INDEX
            SpaceTabType.MEMBERS_TAB -> return if (guestMode) GUEST_MEMBERS_INDEX else MEMBERS_INDEX
            SpaceTabType.IDEAS_TAB -> return if (guestMode) PagerAdapter.POSITION_NONE else IDEAS_INDEX
            SpaceTabType.TASKS_TAB -> return if (guestMode) PagerAdapter.POSITION_NONE else TASKS_INDEX
            else -> {}
        }
        return -1
    }

    override fun getItem(index: Int): Fragment {
        val tabType = indexToType(index, guestMode)
        return when {
            tabType === SpaceTabType.MEMBERS_TAB -> {
                MembersTabFragment()
            }
            tabType === SpaceTabType.TASKS_TAB -> {
                TasksTabFragment()
            }
            tabType === SpaceTabType.IDEAS_TAB -> {
                IdeasTabFragment()
            }
            else -> {
                ChatTabFragment()
            }
        }
    }

    override fun getCount(): Int {
        return if (isGuestMode()) GUEST_TAB_COUNT else TAB_COUNT
    }

    override fun getPageTitle(index: Int): CharSequence? {
        return when (indexToType(index, guestMode)) {
            SpaceTabType.CHAT_TAB -> resources.getString(R.string.chat)
            SpaceTabType.MEMBERS_TAB -> resources.getString(R.string.people)
            SpaceTabType.IDEAS_TAB -> resources.getString(R.string.posts)
            SpaceTabType.TASKS_TAB -> resources.getString(R.string.tasks)
        }
        return ""
    }

    companion object {
        val TAB_COUNT = SpaceTabType.values().size
        const val GUEST_TAB_COUNT = 2
        val CHAT_INDEX = SpaceTabType.CHAT_TAB.index
        val IDEAS_INDEX = SpaceTabType.IDEAS_TAB.index
        val TASKS_INDEX = SpaceTabType.TASKS_TAB.index
        val MEMBERS_INDEX = SpaceTabType.MEMBERS_TAB.index
        const val GUEST_MEMBERS_INDEX = 1
        fun indexToType(index: Int, guestMode: Boolean): SpaceTabType {
            return when (index) {
                0 -> SpaceTabType.CHAT_TAB
                1 -> if (guestMode) SpaceTabType.MEMBERS_TAB else SpaceTabType.IDEAS_TAB
                2 -> SpaceTabType.TASKS_TAB
                3 -> SpaceTabType.MEMBERS_TAB
                else -> SpaceTabType.CHAT_TAB
            }
        }
    }

}