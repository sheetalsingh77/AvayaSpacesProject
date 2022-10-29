package com.avayaspacesproject.ui.conference.ideas

import android.os.Handler
import android.os.SystemClock
import android.text.format.DateUtils
import android.view.View
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
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
import com.avayaspacesproject.databinding.FragmentIdeasTabBinding
import com.avayaspacesproject.ui.base.BaseFragment
import com.avayaspacesproject.ui.conference.chat.customview.BottomOffsetDecoration
import com.avayaspacesproject.ui.conference.chat.utils.MessageUtil
import io.zang.spaces.api.CategoryCode
import io.zang.spaces.api.LoganMessage
import java.util.*
import javax.inject.Inject


class IdeasTabFragment : BaseFragment<FragmentIdeasTabBinding>(FragmentIdeasTabBinding::inflate),
    PostInteractionListener,
    PagingListener,
    MessageListener,
    MessageModifiedListener {

    private val TAG = "IdeasTabFragment"

    private val handler = Handler()

    // Paging -->
    private var paging = false
    private var didPage = false
    private var pagingRecoveryTime: Long = 0
    private val pagingIndicator: View? = null // TODO: this field appears to be unused


    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: PostViewModel

    private var adapter: TopicIdeasTabAdapter? = null


    override fun getViewBinding(): FragmentIdeasTabBinding =
        FragmentIdeasTabBinding.inflate(layoutInflater)


    override fun initializeView() {
        updatePagingRecoveryTime()

        binding.postList.layoutManager = LinearLayoutManager(requireContext())
        adapter = TopicIdeasTabAdapter(TopicIdeasDiffCallback(), this)
        binding.postList.setAdapter(adapter)
        binding.postList.addItemDecoration(
            BottomOffsetDecoration(
                resources.getDimensionPixelOffset(R.dimen.recycler_fab_offset)
            )
        )

        //binding.swipeRefresh.setOnRefreshListener(OnRefreshListener { this.requestPage() })

        viewModel.addMessageListener(this)
        viewModel.addMessageModifiedListener(this)
        viewModel.addPagingListener(this)
        viewModel.addTopicReadyListener(topicReadyListener)

        refreshInt()
    }

    override fun initializeViewModel() {
        viewModel =
            ViewModelProvider(this, viewModelFactory).get(PostViewModel::class.java)

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

    private fun updatePagingRecoveryTime() {
        pagingRecoveryTime = SystemClock.elapsedRealtime() + DateUtils.SECOND_IN_MILLIS
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

    @UiThread
    private fun refreshWithScroll(scroll: Boolean) {
        val currentTopic: SpacesTopic? = viewModel.getCurrentTopic()
        if (currentTopic == null) {
            adapter!!.submitList(emptyList())
            return
        }
        setPaging(false)
        val models: List<TopicIdeasModel> = buildModels(currentTopic)
        adapter!!.submitList(models) { binding.postList.invalidateItemDecorations() }
    }

    private fun buildModels(currentTopic: SpacesTopic): List<TopicIdeasModel> {
        val ideas: Array<LoganMessage> = currentTopic.copyIdeaMessages()
            ?: return emptyList<TopicIdeasModel>()
        val models: MutableList<TopicIdeasModel> =
            ArrayList<TopicIdeasModel>(ideas.size)
        for (message in ideas) {
            models.add(TopicIdeasModel(message, requireContext()))
        }
        return models
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
        val realm: MessageRealm = MessageRealm.Ideas
        return if (realm !== realmUpdated) {
            false
        } else t.isEqualTo(viewModel.getCurrentTopic())
    }

    override fun onPostSelected(message: LoganMessage, v: View) {
        //TODO("Not yet implemented")
    }

    override fun onPostActions(message: LoganMessage, view: View) {
        MessageUtil.onMessageContextMenu(viewModel.getCurrentTopic()!!, message, view, viewModel)
    }

    override fun onPaging(spacesTopic: SpacesTopic, realmUpdated: MessageRealm) {
        if (!canHandlePaging(spacesTopic, realmUpdated)) {
            return
        }

        updatePagingRecoveryTime()
        this.runOnUiThread(Runnable {
            setPaging(false)
            refresh()
        })
    }

    override fun onPagingNoMoreData(spacesTopic: SpacesTopic, realmUpdated: MessageRealm) {
        if (!canHandlePaging(spacesTopic, realmUpdated)) {
            return
        }

        // Will break future topics! // swipeRefresh.setEnabled(false);

        runOnUiThread(Runnable { setPaging(false) })
    }

    override fun onMessage(msg: LoganMessage) {
        if (msg.topicId == null) {
            return
        }
        if (msg.topicId != viewModel.getTopicId()) {
            return
        }

        if (msg.getCategoryCode() == CategoryCode.LM_CATEGORYCODE_IDEA) {
            refreshInt()
        }
    }

    override fun onMessageUpdated(message: LoganMessage, operation: MessageOperationType) {
        if (message.categoryCode == CategoryCode.LM_CATEGORYCODE_IDEA) {
            this.runOnUiThread(Runnable { refreshWithScroll(true) })
        }
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