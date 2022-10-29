package com.avayaspacesproject.ui.conference.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.AnyThread
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.avaya.spaces.util.*
import com.avaya.spacescsdk.services.messaging.enums.MessageRealm
import com.avaya.spacescsdk.services.messaging.listeners.MessageListener
import com.avaya.spacescsdk.services.messaging.listeners.MessageModifiedListener
import com.avaya.spacescsdk.services.messaging.listeners.MessageOperationType
import com.avaya.spacescsdk.services.messaging.listeners.PagingListener
import com.avaya.spacescsdk.services.messaging.model.AttachmentToUpload
import com.avaya.spacescsdk.services.messaging.model.ThumbnailToUpload
import com.avaya.spacescsdk.services.spaces.listeners.SpacesAPITopicReadyListener
import com.avaya.spacescsdk.services.spaces.listeners.TopicListener
import com.avaya.spacescsdk.services.spaces.model.SpacesTopic
import com.avaya.spacescsdk.utils.EXTRA_TOPIC_ID
import com.avaya.spacescsdk.utils.UcLog
import com.avaya.spacescsdk.utils.getFilePickerIntent
import com.avaya.spacescsdk.utils.objectIdentity
import com.avayaspacesproject.ui.conference.chat.MessageChatModel
import com.avayaspacesproject.R
import com.avayaspacesproject.databinding.FragmentChatTabBinding
import com.avayaspacesproject.ui.base.BaseFragment
import com.avayaspacesproject.ui.conference.SpaceTabType
import com.avayaspacesproject.ui.conference.chat.customview.BottomOffsetDecoration
import com.avayaspacesproject.ui.conference.chat.utils.MessageUtil
import com.avayaspacesproject.ui.conference.chat.utils.PictureCapturer
import com.avayaspacesproject.ui.home.HomeViewModel
import com.esna.extra.BoolRef
import io.zang.spaces.api.CategoryCode
import io.zang.spaces.api.LoganAPI
import io.zang.spaces.api.LoganMessage
import java.io.File
import java.util.ArrayList
import javax.inject.Inject


class ChatTabFragment: BaseFragment<FragmentChatTabBinding>(FragmentChatTabBinding::inflate),
    MessageFragmentListener,
    PagingListener,
    MessageListener,
    MessageModifiedListener/*,
    MarkAsReadProvider*/ {
    private val TAG = "TopicChatTabFragment"

    private val HAS_SCROLLED_OFFSET = 5

    private val ID = this.objectIdentity


    private var input: ChatInput? = null
    private var chatContainer: ViewGroup? = null
    private val handler = Handler()

    // Paging -->
    private var paging = false
    private var didPage = false
    private var pagingRecoveryTime: Long = 0
    private val pagingIndicator: View? = null // TODO: this field appears to be unused

    // <-- Paging
    private var camera: PictureCapturer? = null

    private var topicId: String? = null
    private var topic: SpacesTopic? = null
    private var adapter: MessageListAdapter? = null
    private var layoutManager: LinearLayoutManager? = null
    lateinit var viewModel: ChatViewModel
    private var homeViewModel: HomeViewModel? = null

    override fun getViewBinding(): FragmentChatTabBinding =
        FragmentChatTabBinding.inflate(layoutInflater)


    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun initializeView() {
        adapter = MessageListAdapter(MessageDiffCallback(), this)

        binding.messageList.adapter = adapter
        binding.messageList.addItemDecoration(
            BottomOffsetDecoration(
                resources.getDimensionPixelOffset(R.dimen.recycler_fab_offset)
            )
        )


        //////////
        updatePagingRecoveryTime()
        binding.messageList.layoutManager = LinearLayoutManager(requireContext())
        (binding.messageList.layoutManager as LinearLayoutManager).setStackFromEnd(true)
        binding.messageList?.setLayoutManager(binding.messageList.layoutManager)
        binding.swipeRefresh?.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener { requestPage() })
        input = ChatInput.inflate(requireContext())
        binding.inputContainer.addView(input)
        input!!.setListener(object : ChatInput.ChatInputListener {
            override fun onSendWithAttachment(text: String, documentFile: DocumentFile?) {
                var attachment: AttachmentToUpload? = null
                if (documentFile != null) {
                    val pfd = requireContext().getParcelFileDescriptor(documentFile.uri)
                    val thumbnail = requireContext().getThumbnailFile(documentFile.uri)
                    var thumbnailToUpload: ThumbnailToUpload? = null
                    if (thumbnail != null) {
                        val thumbnailDf = DocumentFile.fromFile(thumbnail)
                        val thumbnailPfd = requireContext().getParcelFileDescriptor(thumbnailDf.uri)
                        if (thumbnailPfd != null) {
                            thumbnailToUpload = ThumbnailToUpload(thumbnailDf, thumbnailPfd)
                        }
                    }
                    if (pfd != null) {
                        attachment = AttachmentToUpload(documentFile, pfd, thumbnailToUpload)
                    }
                }
                viewModel.sendMessage(topic, text, attachment)

                scrollToBottom()
            }

            override fun onAttachRequest(requestType: Int) {
                when (requestType) {
                    ChatInput.ATTACH_REQ_PHOTO, ChatInput.ATTACH_REQ_VIDEO -> {
                        attachCommentFileTakePicture(requestType == ChatInput.ATTACH_REQ_PHOTO)
                        return
                    }
                    else -> startActivityForResult(getFilePickerIntent(), ChatInput.ATTACH_REQ_FILE)
                }
            }

        })
        binding.messageList?.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val items: Int? = adapter?.itemCount
                    if (items!! > HAS_SCROLLED_OFFSET) {
                        val lastVisibleItemPosition: Int =
                            (binding.messageList.layoutManager as LinearLayoutManager)?.findLastVisibleItemPosition()!!
                        viewModel.hasScrolled
                            .setValue(lastVisibleItemPosition < items - HAS_SCROLLED_OFFSET)
                    }
                }
            })

        // This needs to be subscribed before onResume() because of cases like
        // https://app.asana.com/0/1143253797275249/1188105455946705/f where the
        // event fires before the fragment is resumed and it's needed to set the
        // topic instance.
        viewModel.addTopicReadyListener(topicReadyListener)

    }

    override fun initializeViewModel() {
        viewModel =
            ViewModelProvider(this, viewModelFactory).get(ChatViewModel::class.java)

    }

    private val topicReadyListener: SpacesAPITopicReadyListener =
        object : SpacesAPITopicReadyListener {
            @AnyThread
            override fun onTopicReady(topic: SpacesTopic) {
                UcLog.d(ID, "Setting topic to " + topic.summary())
                this@ChatTabFragment.topic = topic;
                viewModel.hasScrolled.postValue(false)
                runOnUiThread(Runnable {
                    if (input != null) {
                        input!!.prepareForTopic(topic.identity)
                    }
                })
                refresh()
            }

            @AnyThread
            override fun onTopicUnsubscribed() {
                viewModel.hasScrolled.postValue(false)
                runOnUiThread(Runnable {
                    if (input != null) {
                        input!!.prepareForTopic(null)
                    }
                })
                refresh()
            }
        }


    private val topicListener: TopicListener = object : TopicListener {
        @AnyThread
        override fun onTopicUpdated(topic: SpacesTopic) {
            handleTopicUpdated(topic)
        }
    }

    fun getTabType(): SpaceTabType? {
        return SpaceTabType.CHAT_TAB
    }

    fun onBackPressed(): Boolean {
        return false
    }

    override fun onMessage(msg: LoganMessage) {
        if (topic == null || !topic!!.identity.equals(msg.topicId)) {
            return
        }
        refreshInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = arguments
        if (arguments != null) {
            topicId = arguments.getString(EXTRA_TOPIC_ID)
        }

    }


    private fun updatePagingRecoveryTime() {
        pagingRecoveryTime = SystemClock.elapsedRealtime() + DateUtils.SECOND_IN_MILLIS
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        topic =
            if (TextUtils.isEmpty(topicId)) viewModel.getCurrentTopic() else viewModel.getTopicById(
                topicId
            )

        homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
        homeViewModel?.selectedTab?.observe(viewLifecycleOwner,
            Observer<SpaceTabType> { spaceTabCode ->
                if (spaceTabCode === SpaceTabType.CHAT_TAB) {
                    // If we suppress a mark as read because we were on another tab, we need to
                    // poke the list in order to force a mark as read to be sent when we return
                    refreshInt()
                }
            })
    }

    override fun onResume() {
        super.onResume()
//        NotificationCenter.defaultCenter()
//            .addObserver(onTopicContentReloaded, LoganAPI.LoganNotificationTopicContentReloaded)
        viewModel.addTopicListener(topicListener)
        viewModel.addMessageListener(this)
        viewModel.addMessageModifiedListener(this)
        viewModel.addPagingListener(this)
//        val broadcastManager = LocalBroadcastManager.getInstance(requireContext())
//        broadcastManager.registerReceiver(
//            notificationMessageReceiver,
//            IntentFilter(NOTIFICATION_MESSAGE)
//        )
        refreshInt()
    }

    override fun onPause() {
        super.onPause()
        saveInput()
        //  NotificationCenter.defaultCenter().removeObserver(onTopicContentReloaded)
        viewModel.removeTopicListener(topicListener)
        viewModel.removeMessageListener(this)
        viewModel.removeMessageModifiedListener(this)
        viewModel.removePagingListener(this)
        val broadcastManager = LocalBroadcastManager.getInstance(requireContext())
        //    broadcastManager.unregisterReceiver(notificationMessageReceiver)
    }

    override fun onDestroyView() {
        viewModel.removeTopicReadyListener(topicReadyListener)
        if (input != null) {
            input!!.setListener(null)
        }
        handler.removeCallbacksAndMessages(null)
        binding.swipeRefresh?.setOnRefreshListener(null)
        super.onDestroyView()
    }

    @AnyThread
    override fun onMessageUpdated(
        message: LoganMessage,
        operation: MessageOperationType
    ) {
        if (topic == null || !topic!!.identity.equals(message.topicId)) {
            return
        }
        refreshWithScroll(false)
    }

    @AnyThread
    private fun handleTopicUpdated(topicUpdated: SpacesTopic) {
        if (topicUpdated.isEqualTo(topic)) {
            refreshWithScroll(false)
        }
    }

    private fun attachCommentFileTakePicture(photoNotVideo: Boolean) {
        camera = PictureCapturer()
        camera!!.capture(this, photoNotVideo)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (camera!!.onRequestPermissionsResult(
                activity,
                requestCode,
                permissions,
                grantResults
            )
        ) {
            // handled
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode == ChatInput.ATTACH_REQ_FILE) {
            if (data != null) {
                onAttachment(data.data)
            }
        } else {
            val ok = BoolRef()
            if (camera != null) {

                if (camera!!.onActivityResult(requestCode, resultCode, data, ok)) { // handled
                    if (ok.`val`) {
                        val image: File = camera!!.getFile()
                        if (image != null) {
                            onAttachment(image)
                        }
                        camera = null
                    }
                    return
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onAttachment(file: File?) {
        if (file == null || !file.canRead()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.failed_prepare_attachment),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (file.length() > LoganAPI.MAX_ATTACHMENT_SIZE) {
            Toast.makeText(
                requireContext(),
                getString(R.string.file_too_big),
                Toast.LENGTH_LONG
            )
            return
        }
        val documentFile = DocumentFile.fromFile(file)
        input!!.addAttachment(documentFile)
    }

    private fun onAttachment(uri: Uri?) {
        if (uri == null) {
            UcLog.w(ID, "Uri is null")
            Toast.makeText(
                requireContext(),
                getString(R.string.failed_prepare_attachment),
                Toast.LENGTH_LONG
            )
            return
        }
        val documentFile = requireContext().getDocumentFile(uri)
        if (documentFile == null || !documentFile.isFile || documentFile.length() == 0L) {
            UcLog.w(ID, "File is missed or empty or not readable")
            Toast.makeText(
                requireContext(),
                getString(R.string.failed_prepare_attachment),
                Toast.LENGTH_LONG
            )
            return
        }
        if (documentFile.length() > LoganAPI.MAX_ATTACHMENT_SIZE) {
            Toast.makeText(
                requireContext(),
                getString(R.string.file_too_big),
                Toast.LENGTH_LONG
            )
            return
        }
        input!!.addAttachment(documentFile)
    }

    private fun saveInput() {
        if (input != null) {
            input!!.saveUnsentMessage()
        }
    }

    @AnyThread
    fun refresh() {
        refreshInt()
    }

    @AnyThread
    private fun refreshInt() {
        updatePagingRecoveryTime()
        refreshWithScroll(true)
    }

    @AnyThread
    private fun refreshWithScroll(scroll: Boolean) {
        val currentTab: SpaceTabType? = homeViewModel?.selectedTab?.getValue()
        if (currentTab !== SpaceTabType.CHAT_TAB) {
            // A fragment in a ViewPager can be "live" even though it is not visible due to pre-loading
            // Don't bother refreshing if we are not the currently visible tab in the viewpager
            return
        }
        if (!isRunningOnMainThread()) {
            runOnUiThread(Runnable { refreshWithScroll(scroll) })
            return
        }
        if (topic == null) {
            adapter?.submitList(emptyList())
            return
        }
        setPaging(false)
        if (pagingIndicator != null) {
            pagingIndicator.setVisibility(View.GONE)
        }
        val cells = MessageUtil.refreshChat(topic!!)
        val messages = buildMessageList(cells)
        val oldSize = adapter?.itemCount
        adapter?.submitList(topic!!, messages) { onListAdapterCommit(oldSize!!) }
    }

    private fun onListAdapterCommit(oldSize: Int) {
        val newSize = adapter?.itemCount
        if (oldSize == 0 && newSize!! > 0) {
            // First time we get content
            binding.messageList.layoutManager?.scrollToPosition(newSize!! - 1)
        } else if (newSize!! > 0) {
            if (didPage) {
                binding.messageList.layoutManager?.scrollToPosition(newSize - (oldSize - 1))
                didPage = false
                return
            }
            val hasScrolled: Boolean = viewModel.hasScrolled.getValue()!!
            if (!hasScrolled) {
                // If the user has not paged or scrolled and has different content, assume it is new and scroll to bottom
                binding.messageList.layoutManager?.scrollToPosition(newSize - 1)
            }
        }
    }

    private fun buildMessageList(messages: List<LoganMessage>): List<MessageModel> {
        val models: MutableList<MessageModel> = ArrayList(messages.size)
        val context = requireContext()
        for (message in messages) {
            if (message.zShowDateTitle) {
                models.add(MessageSeparatorModel(context, message.createdSafe()))
            }
            val categoryCode = message.categoryCode
            val parentCategoryCode = message.parentCategoryCode
            if (categoryCode == CategoryCode.LM_CATEGORYCODE_CHAT) {
                if (parentCategoryCode == CategoryCode.LM_CATEGORYCODE_IDEA) {
                    models.add(MessagePostChatModel(message, context))
                } else if (parentCategoryCode == CategoryCode.LM_CATEGORYCODE_TASK) {
                    models.add(MessageTaskChatModel(message, context, viewModel))
                } else {
                    models.add(MessageChatModel(message, context))
                }
            } else if (categoryCode == CategoryCode.LM_CATEGORYCODE_IDEA) {
                models.add(MessagePostModel(message, context))
            } else if (categoryCode == CategoryCode.LM_CATEGORYCODE_TASK) {
                models.add(MessageTaskModel(message, context, viewModel))
            } else if (categoryCode == CategoryCode.LM_CATEGORYCODE_MEETING) {
                models.add(MessageMeetingModel(message.meeting,message, context, viewModel))
            }
        }
        UcLog.d(ID, "buildMessageList: " + models.size + ' ' + topic!!.summary())
        return models
    }

    private fun scrollToBottom() {
        binding.messageList.layoutManager?.scrollToPosition(adapter?.itemCount!! - 1)
    }

    private fun setPaging(paging: Boolean) {
        this.paging = paging
        if (binding.swipeRefresh != null) {
            this.runOnUiThread { binding.swipeRefresh?.setRefreshing(paging) }
        }
    }

    private fun requestPage() {
        if (paging) {
            return
        }
        if (SystemClock.elapsedRealtime() < pagingRecoveryTime) {
            return
        }
        setPaging(true)
        if (pagingIndicator != null) {
            pagingIndicator.setVisibility(View.VISIBLE)
        }
        viewModel.pageTopicMessages(MessageRealm.Chat, topic!!)
    }

    private fun canHandlePaging(topic: SpacesTopic, realm: MessageRealm): Boolean {
        return paging &&
                realm == MessageRealm.Chat &&
                topic.isEqualTo(topic)
    }

    @AnyThread
    override fun onPagingNoMoreData(t: SpacesTopic, realm: MessageRealm) {
        if (!canHandlePaging(t, realm)) {
            return
        }
        this.runOnUiThread { setPaging(false) }
    }

    @AnyThread
    override fun onPaging(t: SpacesTopic, realm: MessageRealm) {
        if (!canHandlePaging(t, realm)) {
            return
        }
        updatePagingRecoveryTime()
        this.runOnUiThread {
            paging = false
            if (pagingIndicator != null) {
                pagingIndicator.setVisibility(View.GONE)
            }
            didPage = true
            refresh()
        }
    }

    override fun onMessageSelected(message: LoganMessage?, v: View?) {
//        val shared: ScrMain = ScrMain.getShared()
//        if (message != null && message.categoryCode == CategoryCode.LM_CATEGORYCODE_TASK) {
//            if (shared != null) {
//                shared.startTask(message)
//            }
//        } else if (message != null && message.categoryCode == CategoryCode.LM_CATEGORYCODE_IDEA) {
//            if (shared != null) {
//                shared.startIdea(message)
//            }
//        }
    }

//    override fun onMessageActions(message: LoganMessage?, v: View?) {
//        onMessageContextMenu(topic, message, v, api)
//    }
//
//    fun canMarkAsRead(): Boolean {
//        return homeViewModel != null && homeViewModel.getSelectedTab()
//            .getValue() === SpaceTabType.CHAT_TAB
//    }

}