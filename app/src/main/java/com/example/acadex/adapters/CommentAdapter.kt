package com.example.acadex.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.data.model.Comment
import com.example.acadex.databinding.ItemCommentBinding

class CommentAdapter : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    private val items = mutableListOf<Comment>()

    fun submitList(comments: List<Comment>) {
        items.clear()
        items.addAll(comments)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: Comment) {
            binding.commenterName.text = comment.commenterName
            binding.commentText.text = comment.text
            binding.commentDate.text = comment.date
        }
    }
}
