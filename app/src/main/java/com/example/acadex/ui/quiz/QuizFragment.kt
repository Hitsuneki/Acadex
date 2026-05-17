package com.example.acadex.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.acadex.R
import com.example.acadex.adapters.QuizSetAdapter
import com.example.acadex.data.MockDataSource
import com.example.acadex.databinding.FragmentQuizBinding
import com.example.acadex.util.SubjectChipHelper

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    private lateinit var quizAdapter: QuizSetAdapter
    private lateinit var chipHelper: SubjectChipHelper
    private var selectedSubject = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = getString(R.string.practice_index)
        binding.toolbar.navigationIcon = null

        chipHelper = SubjectChipHelper(binding.subjectChipsRecycler) { subject ->
            selectedSubject = subject
            refreshList()
        }
        chipHelper.setup()

        quizAdapter = QuizSetAdapter { quiz ->
            findNavController().navigate(
                QuizFragmentDirections.actionQuizToQuizTaking(quizId = quiz.id)
            )
        }
        binding.quizRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.quizRecycler.adapter = quizAdapter

        refreshList()
    }

    private fun refreshList() {
        val quizzes = if (selectedSubject == "All") {
            MockDataSource.quizSets
        } else {
            MockDataSource.quizSets.filter { it.subject == selectedSubject }
        }
        quizAdapter.submitList(quizzes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
