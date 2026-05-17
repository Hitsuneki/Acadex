package com.example.acadex.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.data.model.Comment
import com.example.acadex.databinding.ItemCommentBinding

class CommentAdapter(
    private val currentUserId: String?,
    private val onDelete: (Comment) -> Unit
) : ListAdapter<Comment, CommentAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), currentUserId, onDelete)
    }

    class VH(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: Comment, currentUserId: String?, onDelete: (Comment) -> Unit) {
            binding.commenterName.text = comment.commenterName
            binding.commentText.text = comment.text
            binding.commentDate.text = comment.relativeTime
            val initials = comment.commenterName.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2).joinToString("").ifEmpty { "?" }
            binding.avatarInitials.text = initials
            val own = currentUserId != null && comment.userId == currentUserId
            binding.btnDeleteComment.isVisible = own
            binding.btnDeleteComment.setOnClickListener { onDelete(comment) }
        }
    }

    private class Diff : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(a: Comment, b: Comment) = a.id == b.id
        override fun areContentsTheSame(a: Comment, b: Comment) = a == b
    }
}
