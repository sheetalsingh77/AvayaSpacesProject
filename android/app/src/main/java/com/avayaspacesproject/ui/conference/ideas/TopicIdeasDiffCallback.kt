package com.avayaspacesproject.ui.conference.ideas

import androidx.recyclerview.widget.DiffUtil

class TopicIdeasDiffCallback : DiffUtil.ItemCallback<TopicIdeasModel>() {
    override fun areItemsTheSame(
        oldItem: TopicIdeasModel,
        newItem: TopicIdeasModel
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: TopicIdeasModel,
        newItem: TopicIdeasModel
    ): Boolean {
        return oldItem.equals(newItem)
    }
}