package com.example.acadex.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.R
import com.example.acadex.data.model.Difficulty
import com.example.acadex.data.model.QuizSet
import com.example.acadex.databinding.ItemQuizSetBinding

class QuizSetAdapter(
    private val onStartQuiz: (QuizSet) -> Unit
) : RecyclerView.Adapter<QuizSetAdapter.ViewHolder>() {

    private val items = mutableListOf<QuizSet>()

    fun submitList(quizzes: List<QuizSet>) {
        items.clear()
        items.addAll(quizzes)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuizSetBinding.inflate(
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

    inner class ViewHolder(private val binding: ItemQuizSetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(quiz: QuizSet) {
            val context = binding.root.context
            binding.quizTitle.text = quiz.title
            binding.subjectBadge.text = quiz.subject
            binding.questionCount.text =
                context.getString(R.string.questions_count, quiz.questions.size)
            binding.difficultyBadge.text = quiz.difficulty.label()

            val bgRes = when (quiz.difficulty) {
                Difficulty.EASY -> R.drawable.bg_difficulty_easy
                Difficulty.HARD -> R.drawable.bg_difficulty_hard
                else -> R.drawable.bg_difficulty_medium
            }
            binding.difficultyBadge.setBackgroundResource(bgRes)

            val textColor = when (quiz.difficulty) {
                Difficulty.EASY -> R.color.difficulty_easy_fg
                Difficulty.HARD -> R.color.difficulty_hard_fg
                else -> R.color.difficulty_medium_fg
            }
            binding.difficultyBadge.setTextColor(
                ContextCompat.getColor(context, textColor)
            )

            binding.btnStartQuiz.text = context.getString(R.string.start)
            binding.btnStartQuiz.setOnClickListener { onStartQuiz(quiz) }
        }
    }
}
