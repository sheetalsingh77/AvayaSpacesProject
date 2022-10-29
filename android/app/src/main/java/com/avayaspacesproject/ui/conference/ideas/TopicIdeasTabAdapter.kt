package com.avayaspacesproject.ui.conference.ideas

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.avayaspacesproject.R
import com.avayaspacesproject.ui.conference.ideas.TopicIdeasTabAdapter.IdeaViewHolder

class TopicIdeasTabAdapter(
    diffCallback: TopicIdeasDiffCallback,
    private val listener: PostInteractionListener
) : ListAdapter<TopicIdeasModel, IdeaViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IdeaViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_idea, parent, false)
        return IdeaViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: IdeaViewHolder, position: Int) {
        holder.bindTo(getItem(position)!!)
    }

    class IdeaViewHolder internal constructor(
        view: View,
        private val listener: PostInteractionListener
    ) : RecyclerView.ViewHolder(view) {
        private val title: TextView
        private val subtitle: TextView
        private val creationDate: TextView
        fun bindTo(model: TopicIdeasModel) {
            val message = model.message
//            itemView.setOnClickListener { v: View? ->
//                listener.onPostSelected(
//                    message,
//                    v!!
//                )
//            }
            itemView.setOnLongClickListener { v: View? ->
                listener.onPostActions(message, v!!)
                true
            }
            title.text = model.title
            title.text = model.title
            val subtitleLabel = model.subtitle
            if (TextUtils.isEmpty(subtitleLabel)) {
                subtitle.visibility = View.GONE
            } else {
                subtitle.visibility = View.VISIBLE
                subtitle.text = subtitleLabel
            }
            val createdDateLabel = model.createdDate
            if (TextUtils.isEmpty(createdDateLabel)) {
                creationDate.visibility = View.GONE
            } else {
                creationDate.text = createdDateLabel
                creationDate.visibility = View.VISIBLE
            }
        }

        init {
            title = view.findViewById(R.id.title)
            subtitle = view.findViewById(R.id.subtitle)
            creationDate = view.findViewById(R.id.created_date)
        }
    }

}