package com.avayaspacesproject.ui.conference.tasks

import android.content.Context
import android.os.Handler
import android.os.SystemClock
import android.text.format.DateUtils
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.avaya.spaces.util.runOnUiThread
import com.avaya.spacescsdk.services.messaging.enums.MessageRealm
import com.avaya.spacescsdk.services.messaging.listeners.MessageListener
import com.avaya.spacescsdk.services.messaging.listeners.MessageModifiedListener
import com.avaya.spacescsdk.services.messaging.listeners.MessageOperationType
import com.avaya.spacescsdk.services.messaging.listeners.PagingListener
import com.avaya.spacescsdk.services.spaces.listeners.SpacesAPITopicReadyListener
import com.avaya.spacescsdk.services.spaces.model.SpacesTopic
import com.avayaspacesproject.R
import com.avayaspacesproject.databinding.FragmentTasksTabBinding
import com.avayaspacesproject.ui.base.BaseFragment
import com.avayaspacesproject.ui.conference.chat.customview.BottomOffsetDecoration
import com.avayaspacesproject.ui.conference.chat.utils.MessageUtil
import io.zang.spaces.api.CategoryCode
import io.zang.spaces.api.LoganMessage
import java.util.*
import javax.inject.Inject


class TasksTabFragment: BaseFragment<FragmentTasksTabBinding>(FragmentTasksTabBinding::inflate),
    TaskInteractionListener,
    PagingListener,
    MessageListener,
    MessageModifiedListener {

    private val TAG = "TasksTabFragment"

    private val handler = Handler()

    // Paging -->
    private var paging = false
    private var didPage = false
    private var pagingRecoveryTime: Long = 0
    private val pagingIndicator: View? = null // TODO: this field appears to be unused

    private var filterTaskCompleted = 0 // 0 all, -1 incompleted, 1 completed

    private var filterTaskMy = false


    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: TaskViewModel

    private var adapter: TopicTasksTabAdapter? = null

    override fun getViewBinding(): FragmentTasksTabBinding =
        FragmentTasksTabBinding.inflate(layoutInflater)


    override fun initializeView() {
        updatePagingRecoveryTime()

        binding.taskList.layoutManager = LinearLayoutManager(requireContext())
        adapter = TopicTasksTabAdapter(TopicTasksDiffCallback(), this)
        binding.taskList.setAdapter(adapter)
        binding.taskList.addItemDecoration(
            BottomOffsetDecoration(
                resources.getDimensionPixelOffset(R.dimen.recycler_fab_offset)
            )
        )


        //binding.swipeRefresh.setOnRefreshListener(OnRefreshListener { this.requestPage() })


        updateFilterTaskDescription()

        binding.filter.setOnClickListener { view1: View? -> onTapFilter() }

        viewModel.addMessageListener(this)
        viewModel.addMessageModifiedListener(this)
        viewModel.addPagingListener(this)
        viewModel.addTopicReadyListener(topicReadyListener)

        refreshInt()
    }

    override fun initializeViewModel() {
        viewModel =
            ViewModelProvider(this, viewModelFactory).get(TaskViewModel::class.java)

    }

    private val topicReadyListener: SpacesAPITopicReadyListener =
        object : SpacesAPITopicReadyListener {
            @AnyThread
            override fun onTopicReady(topic: SpacesTopic) {
                refresh()
            }

            @AnyThread
            override fun onTopicUnsubscribed() {
                refresh()
            }
        }

    private fun onTapFilter() {
        val c: Context =
            ContextThemeWrapper(context, R.style.PopupMenu2)
        val p = PopupMenu(c, binding.filter)
        var mId = -1
        var order = 0
        p.menu.add(
            Menu.NONE,
            ++mId,
            ++order,
            requireContext().getString(R.string.all_tasks).toUpperCase()
        )
        p.menu.add(
            Menu.NONE,
            ++mId,
            ++order,
            requireContext().getString(R.string.incomplete_tasks).toUpperCase()
        )
        p.menu.add(
            Menu.NONE,
            ++mId,
            ++order,
            requireContext().getString(R.string.completed_tasks).toUpperCase()
        )
        p.menu.add(Menu.NONE, ++mId, ++order, "____________________________").isEnabled = false
        p.menu.add(
            Menu.NONE,
            ++mId,
            ++order,
            requireContext().getString(R.string.assigned_to_me).toUpperCase()
        )
        p.menu.add(
            Menu.NONE,
            ++mId,
            ++order,
            requireContext().getString(R.string.anyone).toUpperCase()
        )
        p.setOnMenuItemClickListener { item ->
            onTapFilterItem(item.itemId)
            true
        }
        p.show()
    }

    private fun onTapFilterItem(itemId: Int) {
        when (itemId) {
            0 -> filterTaskCompleted = 0
            1 -> filterTaskCompleted = -1
            2 -> filterTaskCompleted = 1
            4 -> filterTaskMy = true
            5 -> filterTaskMy = false
            else -> {
            }
        }
        refreshWithScroll(true)
        updateFilterTaskDescription()
    }
    @AnyThread
    fun refresh() {
        refreshInt()
    }

    @AnyThread
    private fun refreshInt() {
        updatePagingRecoveryTime()
        this.runOnUiThread(Runnable { refreshWithScroll(true) })
    }

    private fun updatePagingRecoveryTime() {
        pagingRecoveryTime = SystemClock.elapsedRealtime() + DateUtils.SECOND_IN_MILLIS
    }

    @UiThread
    private fun refreshWithScroll(scroll: Boolean) {
        val currentTopic: SpacesTopic? = viewModel.getCurrentTopic()
        if (currentTopic == null) {
            adapter!!.submitList(emptyList())
            return
        }
        setPaging(false)
        val tasks: Array<LoganMessage> = currentTopic.copyTaskMessages()
        val filteredTasks: MutableList<LoganMessage> =
            ArrayList()
        if (tasks != null && tasks.size > 0) {
            for (task in tasks) {
                if (filterTaskMy) {
                    if (!task.isTaskAssignee(viewModel.getUserIdentity())) {
                        continue
                    }
                }
                if (filterTaskCompleted != 0) {
                    val needCompleeted: Boolean = filterTaskCompleted > 0
                    val completed =
                        LoganMessage.LM_TASK_STA_COMPLETED == task.contentStatus
                    if (completed != needCompleeted) {
                        continue
                    }
                }
                filteredTasks.add(task)
            }
        }

        Collections.sort(filteredTasks, MessageUtil.TASK_COMPARATOR)
        val models: List<TopicTaskModel> = buildTaskModels(filteredTasks)
        adapter!!.submitList(models) { binding.taskList.invalidateItemDecorations() }
    }

    private fun buildTaskModels(messages: List<LoganMessage>): List<TopicTaskModel> {
        val models: MutableList<TopicTaskModel> =
            ArrayList(messages.size)
        for (message in messages) {
            models.add(TopicTaskModel(message, context))
        }
        return models
    }

    private fun updateFilterTaskDescription() {
        val sb = StringBuilder(64)
        sb.append("    ")
        sb.append(getString(R.string.view))
        sb.append(' ')
        var noFirstPart = false
        if (filterTaskCompleted == 0) {
            if (filterTaskMy) {
                noFirstPart = true
            } else {
                sb.append(getString(R.string.all_tasks))
            }
        } else if (filterTaskCompleted < 0) {
            sb.append(getString(R.string.incomplete_tasks))
        } else {
            sb.append(getString(R.string.completed_tasks))
        }
        if (!noFirstPart && filterTaskMy) {
            sb.append(", ")
        }
        if (filterTaskMy) {
            sb.append(getString(R.string.assigned_to_me))
        }
        binding.filter.setText(sb.toString().toUpperCase())
    }

    override fun onPaging(p0: SpacesTopic, p1: MessageRealm) {
        refresh()
    }

    override fun onPagingNoMoreData(topic: SpacesTopic, realmUpdated: MessageRealm) {
        if (!canHandlePaging(topic, realmUpdated)) {
            return
        }
        this.runOnUiThread { setPaging(false) }
    }

    private fun isPaging(): Boolean {
        return paging
    }

    private fun setPaging(paging: Boolean) {
        this.paging = paging
        if (binding.swipeRefresh != null) {
            this.runOnUiThread(Runnable { binding.swipeRefresh.setRefreshing(paging) })
        }
    }


    private fun canHandlePaging(t: SpacesTopic?, realmUpdated: MessageRealm): Boolean {
        if (!isPaging()) {
            return false
        }
        if (t == null) {
            return false
        }
        val realm: MessageRealm = MessageRealm.Tasks
        return if (realm !== realmUpdated) {
            false
        } else t.isEqualTo(viewModel.getCurrentTopic())
    }

    override fun onMessage(msg: LoganMessage) {
        if (msg.topicId == null) {
            return
        }
        if (msg.topicId != viewModel.getTopicId()) {
            return
        }

        if (msg.getCategoryCode() == CategoryCode.LM_CATEGORYCODE_TASK) {
            refreshInt()
        }
    }

    override fun onMessageUpdated(message: LoganMessage, operation: MessageOperationType) {
        if (message.categoryCode == CategoryCode.LM_CATEGORYCODE_TASK) {
            this.runOnUiThread(Runnable { refreshWithScroll(true) })
        }
    }

    override fun onTaskSelected(message: LoganMessage, v: View) {
        //TODO("Not yet implemented")
    }

    override fun onTaskActions(message: LoganMessage, view: View) {
        MessageUtil.onMessageContextMenu(viewModel.getCurrentTopic()!!, message, view, viewModel)
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        if (binding.swipeRefresh != null) {
            binding.swipeRefresh.setOnRefreshListener(null)
        }

        viewModel.removeMessageListener(this)
        viewModel.removeMessageModifiedListener(this)
        viewModel.removePagingListener(this)
        viewModel.removeTopicReadyListener(topicReadyListener)
        super.onDestroyView()
    }

}