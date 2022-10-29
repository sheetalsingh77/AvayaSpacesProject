package com.avayaspacesproject.ui.conference.chat

import android.os.Handler
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.avaya.spacescsdk.services.spaces.model.SpacesTopic
import com.avayaspacesproject.R
import com.avayaspacesproject.databinding.ListItemMessageContainerBinding
import com.avayaspacesproject.ui.conference.chat.utils.MessageUtil
import com.bumptech.glide.Glide
import io.zang.spaces.api.LoganMessage

class MessageListAdapter(
    diffCallback: MessageDiffCallback,
    private val listener: MessageFragmentListener/*,private val markAsReadProvider : MarkAsReadProvider*/
) :
    ListAdapter<MessageModel, MessageListAdapter.AbstractViewHolder>(diffCallback) {
    private var topic: SpacesTopic? = null
    private val handler = Handler()
    private val markAsReadSentForTime: Long = 0
    // private val markAsReadProvider: MarkAsReadProvider? = null

    // private val markAsReadRunnable = Runnable { LoganAPI.shared().setMarkAsRead(topic) }

    abstract class AbstractViewHolder protected constructor(view: View?) :
        RecyclerView.ViewHolder(view!!)


    override fun onBindViewHolder(holder: AbstractViewHolder, position: Int) {
        when (getItemViewType(position)) {
//            MessageModel.VIEW_TYPE_SEPARATOR -> (holder as SeparatorViewHolder).bindTo(getItem(position) as MessageSeparatorModel?)
//            MessageModel.VIEW_TYPE_UNREAD_LINE -> (holder as UnreadViewHolder).bindTo(getItem(position) as MessageUnreadLineModel?)
//            MessageModel.VIEW_TYPE_POST -> (holder as MessagePostViewHolder).bindTo(getItem(position) as MessagePostModel?)
//            MessageModel.VIEW_TYPE_POST_CHAT -> (holder as MessagePostChatViewHolder).bindTo(getItem(position) as MessagePostChatModel?)
//            MessageModel.VIEW_TYPE_TASK -> (holder as MessageTaskViewHolder).bindTo(getItem(position) as MessageTaskModel?)
//            MessageModel.VIEW_TYPE_MEETING -> (holder as MessageMeetingViewHolder).bindTo(getItem(position) as MessageMeetingModel?)
//            MessageModel.VIEW_TYPE_TASK_CHAT -> (holder as MessageTaskChatViewHolder).bindTo(getItem(position) as MessageTaskChatModel?)
            MessageModel.VIEW_TYPE_MESSAGE -> (holder as MessageViewHolder).bindTo(getItem(position) as MessageChatModel)

        }
    }

    override fun getItemViewType(position: Int): Int {
        val model = getItem(position)
        return model!!.type
    }

    fun submitList(topic: SpacesTopic, list: List<MessageModel?>, commitCallback: Runnable?) {
//        if (this.topic != null && !this.topic!!.iden.equals(topic.iden)) {
//            MessageListAdapter.markAsReadSentForTime = 0
//        }
        this.topic = topic
        submitList(list, commitCallback)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
//            VIEW_TYPE_SEPARATOR -> {
//                val view: View =
//                    inflater.inflate(R.layout.list_item_message_separator, parent, false)
//                SeparatorViewHolder(view)
//            }
//            VIEW_TYPE_UNREAD_LINE -> {
//                val view: View =
//                    inflater.inflate(R.layout.list_item_message_unread, parent, false)
//                io.zang.spaces.widgets.MessageListAdapter.UnreadViewHolder(view)
//            }
//            VIEW_TYPE_POST -> {
//                val view: View =
//                    inflater.inflate(R.layout.list_item_chat_task, parent, false)
//                io.zang.spaces.widgets.MessageListAdapter.MessagePostViewHolder(view)
//            }
//            VIEW_TYPE_POST_CHAT -> {
//                val view: View =
//                    inflater.inflate(R.layout.list_item_chat_task, parent, false)
//                io.zang.spaces.widgets.MessageListAdapter.MessagePostChatViewHolder(view)
//            }
//            VIEW_TYPE_TASK -> {
//                val view: View =
//                    inflater.inflate(R.layout.list_item_chat_task, parent, false)
//                io.zang.spaces.widgets.MessageListAdapter.MessageTaskViewHolder(view)
//            }
//            VIEW_TYPE_MEETING -> {
//                val view: View =
//                    inflater.inflate(R.layout.list_item_chat_meeting, parent, false)
//                io.zang.spaces.widgets.MessageListAdapter.MessageMeetingViewHolder(view)
//            }
//            VIEW_TYPE_TASK_CHAT -> {
//                val view: View =
//                    inflater.inflate(R.layout.list_item_chat_task, parent, false)
//                io.zang.spaces.widgets.MessageListAdapter.MessageTaskChatViewHolder(view)
//            }
            else -> {
                MessageViewHolder(parent.createMessageView(parent), listener)
            }
        }
    }

    private fun ViewGroup.createMessageView(parent: ViewGroup): ListItemMessageContainerBinding {
        return ListItemMessageContainerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }

    class MessageViewHolder internal constructor(
        private val itemBinding: ListItemMessageContainerBinding,
        private val listener: MessageFragmentListener
    ) :
        AbstractViewHolder(itemBinding.root) {

        fun bindTo(model: MessageChatModel) {
            val message: LoganMessage = model.message
            val context = itemBinding.linearLayoutView.context
            //sendMarkAsReadForMessage(message)

            itemBinding.messageContainer.setOnLongClickListener { v: View? ->
                listener.onMessageActions(message, v)
                true
            }
            itemBinding.linearLayoutView.gravity =
                if (model.isOurs()) Gravity.END else Gravity.START
            itemBinding.innerContainer.gravity = if (model.isOurs()) Gravity.END else Gravity.START
            @DrawableRes val backgroundRes =
                if (message.isFromMe) R.drawable.message_background_ours else R.drawable.message_background_theirs
            itemBinding.messageContainer.setBackgroundResource(backgroundRes)
            val contentBodyText: String = model.body
            if (TextUtils.isEmpty(contentBodyText)) {
                itemBinding.messageText.visibility = View.GONE
                itemBinding.messageText.setOnLongClickListener(null)
            } else {
                val html = MessageUtil.getMessageForDisplay(contentBodyText)
                itemBinding.messageText.text = html
                itemBinding.messageText.visibility = View.VISIBLE
                itemBinding.messageText.setOnLongClickListener { v: View? ->
                    listener.onMessageActions(message, v)
                    true
                }
            }
            itemBinding.attachmentContainer.removeAllViews()
            if (message.attachments == null || message.attachments.isEmpty()) {
                itemBinding.attachmentContainer.visibility = View.GONE
            } else {
//                var messageInner: LoganMessage? = message
//                while (messageInner != null) {
//                    val innerCell: MessageCellV2Inner = MessageCellV2Inner.inflate(context)
//                    innerCell.setup(messageInner, listener)
//                    val llp = LinearLayout.LayoutParams(
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT
//                    )
//                    itemBinding.attachmentContainer.addView(innerCell, llp)
//                    messageInner = messageInner.zLinked
//                }
//                itemBinding.attachmentContainer.visibility = View.VISIBLE
            }
            itemBinding.username.visibility =
                if (model.canShowSenderName()) View.VISIBLE else View.GONE
            itemBinding.username.setText(model.senderName.toString())
            val userpic =
                if (model.isOurs()) itemBinding.userpicRight else itemBinding.userpicLeft
            itemBinding.userpicRight.visibility =
                if (model.canShowPicture() && model.isOurs()) View.VISIBLE else View.GONE
            itemBinding.userpicLeft.visibility =
                if (model.canShowPicture() && !model.isOurs()) View.VISIBLE else View.INVISIBLE
            if (model.canShowPicture()) {
                Glide.with(context).load(model.pictureUrl)
                    .error(R.drawable.ic_baseline_person_24).circleCrop().into(userpic)
            }
            if (TextUtils.isEmpty(model.modifiedDate)) {
                itemBinding.timeStamp.visibility = View.GONE
            } else {
                itemBinding.timeStamp.setText(model.modifiedDate)
                itemBinding.timeStamp.gravity = if (model.isOurs()) Gravity.END else Gravity.START
                itemBinding.timeStamp.visibility = View.VISIBLE
            }
        }

    }

}
