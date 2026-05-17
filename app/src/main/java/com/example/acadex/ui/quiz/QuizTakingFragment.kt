package com.example.acadex.ui.quiz

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.acadex.R
import com.example.acadex.data.MockDataSource
import com.example.acadex.data.model.QuizQuestion
import com.example.acadex.data.model.QuizSet
import com.example.acadex.databinding.FragmentQuizTakingBinding

class QuizTakingFragment : Fragment() {

    private var _binding: FragmentQuizTakingBinding? = null
    private val binding get() = _binding!!

    private val args: QuizTakingFragmentArgs by navArgs()

    private lateinit var quiz: QuizSet
    private var currentIndex = 0
    private val userAnswers = mutableListOf<Int>()
    private var showingReview = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizTakingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val quizSet = MockDataSource.getQuizById(args.quizId)
        if (quizSet == null) {
            findNavController().navigateUp()
            return
        }
        quiz = quizSet

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.resultsToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnNext.setOnClickListener { onNextClicked() }
        binding.btnReviewAnswers.setOnClickListener { showReview() }
        binding.btnTryAgain.setOnClickListener { restartQuiz() }
        binding.btnBackToQuizzes.setOnClickListener {
            findNavController().navigateUp()
        }

        startQuiz()
    }

    private fun startQuiz() {
        currentIndex = 0
        userAnswers.clear()
        showingReview = false
        binding.quizContent.isVisible = true
        binding.resultsContent.isVisible = false
        binding.reviewContainer.isVisible = false
        binding.toolbar.title = quiz.title
        showQuestion()
    }

    private fun restartQuiz() {
        startQuiz()
    }

    private fun showQuestion() {
        val question = quiz.questions[currentIndex]
        val total = quiz.questions.size

        binding.progressLabel.text = getString(
            R.string.question_progress,
            currentIndex + 1,
            total
        )
        binding.questionProgressBar.progress =
            ((currentIndex + 1) * 100) / total

        binding.questionText.text = question.question
        binding.choicesGroup.removeAllViews()

        question.choices.forEachIndexed { index, choice ->
            val radio = RadioButton(requireContext()).apply {
                id = View.generateViewId()
                text = choice
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 24, 0, 24)
            }
            binding.choicesGroup.addView(radio)
        }

        binding.choicesGroup.setOnCheckedChangeListener { _, _ ->
            binding.btnNext.isEnabled = true
        }

        binding.btnNext.isEnabled = false
        binding.btnNext.text = if (currentIndex == total - 1) {
            getString(R.string.finish)
        } else {
            getString(R.string.next)
        }
    }

    private fun onNextClicked() {
        val selectedId = binding.choicesGroup.checkedRadioButtonId
        val selectedIndex = (0 until binding.choicesGroup.childCount).firstOrNull { i ->
            binding.choicesGroup.getChildAt(i).id == selectedId
        } ?: return

        userAnswers.add(selectedIndex)

        if (currentIndex < quiz.questions.size - 1) {
            currentIndex++
            showQuestion()
        } else {
            showResults()
        }
    }

    private fun showResults() {
        binding.quizContent.isVisible = false
        binding.resultsContent.isVisible = true

        var correct = 0
        quiz.questions.forEachIndexed { index, q ->
            if (userAnswers.getOrNull(index) == q.correctIndex) correct++
        }
        val total = quiz.questions.size
        val percent = (correct * 100) / total

        binding.scoreText.text = getString(R.string.score_format, correct, total)
        binding.percentageText.text = "$percent%"
        binding.motivationText.text = when {
            percent >= 80 -> "Excellent work! You're mastering this topic."
            percent >= 60 -> "Good job! Keep practicing to improve."
            else -> "Keep studying — you'll get there!"
        }
    }

    private fun showReview() {
        if (showingReview) return
        showingReview = true
        binding.reviewContainer.isVisible = true
        binding.reviewContainer.removeAllViews()

        val context = requireContext()
        quiz.questions.forEachIndexed { index, question ->
            addReviewItem(context, question, index)
        }
    }

    private fun addReviewItem(context: android.content.Context, question: QuizQuestion, index: Int) {
        val userAnswer = userAnswers.getOrNull(index) ?: -1
        val correct = question.correctIndex

        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(0, 0, 0, 32)
        }

        val title = TextView(context).apply {
            text = "Q${index + 1}: ${question.question}"
            setTypeface(null, Typeface.BOLD)
        }
        container.addView(title)

        question.choices.forEachIndexed { choiceIndex, choice ->
            val tv = TextView(context).apply {
                text = "• $choice"
                setPadding(16, 8, 16, 8)
                when {
                    choiceIndex == correct -> {
                        setBackgroundColor(
                            ContextCompat.getColor(context, R.color.correct_green)
                        )
                    }
                    choiceIndex == userAnswer && userAnswer != correct -> {
                        setBackgroundColor(
                            ContextCompat.getColor(context, R.color.wrong_red)
                        )
                    }
                }
            }
            container.addView(tv)
        }

        binding.reviewContainer.addView(container)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
