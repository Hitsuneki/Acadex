package com.example.acadex.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.R
import com.example.acadex.data.model.QuizHistoryEntry
import com.example.acadex.databinding.ItemQuizHistoryBinding

class QuizHistoryAdapter : ListAdapter<QuizHistoryEntry, QuizHistoryAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemQuizHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(private val b: ItemQuizHistoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(entry: QuizHistoryEntry) {
            val ctx = b.root.context
            b.quizTitle.text = entry.quizTitle
            b.subjectBadge.text = entry.subject
            b.difficultyBadge.text = entry.difficulty
            b.scoreText.text = ctx.getString(
                R.string.quiz_history_score,
                entry.score,
                entry.total,
                entry.percentage
            )
            b.dateText.text = entry.takenAt
            val bgRes = when (entry.difficulty.uppercase()) {
                "HARD" -> R.color.difficulty_hard_bg
                "MEDIUM" -> R.color.difficulty_medium_bg
                else -> R.color.difficulty_easy_bg
            }
            val fgRes = when (entry.difficulty.uppercase()) {
                "HARD" -> R.color.difficulty_hard_fg
                "MEDIUM" -> R.color.difficulty_medium_fg
                else -> R.color.difficulty_easy_fg
            }
            b.difficultyBadge.setBackgroundColor(ContextCompat.getColor(ctx, bgRes))
            b.difficultyBadge.setTextColor(ContextCompat.getColor(ctx, fgRes))
        }
    }

    private class Diff : DiffUtil.ItemCallback<QuizHistoryEntry>() {
        override fun areItemsTheSame(a: QuizHistoryEntry, b: QuizHistoryEntry) = a.id == b.id
        override fun areContentsTheSame(a: QuizHistoryEntry, b: QuizHistoryEntry) = a == b
    }
}
