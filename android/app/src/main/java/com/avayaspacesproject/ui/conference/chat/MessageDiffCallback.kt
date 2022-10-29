package com.avayaspacesproject.ui.conference.chat

import androidx.recyclerview.widget.DiffUtil

class MessageDiffCallback : DiffUtil.ItemCallback<MessageModel>() {
    override fun areItemsTheSame(
        oldItem: MessageModel,
        newItem: MessageModel
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: MessageModel,
        newItem: MessageModel
    ): Boolean {
        return oldItem.type == newItem.type && oldItem == newItem
    }
}