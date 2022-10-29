package com.avayaspacesproject.ui.conference.tasks

import androidx.recyclerview.widget.DiffUtil

class TopicTasksDiffCallback: DiffUtil.ItemCallback<TopicTaskModel>() {

    override fun areItemsTheSame(oldItem: TopicTaskModel, newItem: TopicTaskModel): Boolean {
        return oldItem.id.equals(newItem.id)
    }

    override fun areContentsTheSame(oldItem: TopicTaskModel, newItem: TopicTaskModel): Boolean {
        return oldItem.equals(newItem)
    }
}