package com.example.acadex.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.acadex.R
import com.example.acadex.adapter.FileCardAdapter
import com.example.acadex.data.MockDataSource
import com.example.acadex.databinding.FragmentHomeBinding
import com.example.acadex.util.SubjectChipHelper
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: FileCardAdapter
    private lateinit var chips: SubjectChipHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chips = SubjectChipHelper(binding.subjectChipsRecycler) { viewModel.setSubject(it) }
        chips.setup()
        adapter = FileCardAdapter { findNavController().navigate(HomeFragmentDirections.actionHomeToFileDetail(it.id)) }
        binding.filesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.filesRecycler.adapter = adapter
        viewModel.files.observe(viewLifecycleOwner) { adapter.submitList(it) }
        binding.btnSearch.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeToBrowse(true))
        }
        binding.btnBell.setOnClickListener {
            Snackbar.make(binding.root, R.string.feature_coming_soon, Snackbar.LENGTH_SHORT).show()
        }
        binding.btnSeeAll.setOnClickListener { findNavController().navigate(R.id.browseFragment) }
        binding.btnSeeAllQuizzes.setOnClickListener { findNavController().navigate(R.id.quizFragment) }
        binding.quizPromoCard.setOnClickListener { findNavController().navigate(R.id.quizFragment) }
        refreshHeader()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        refreshHeader()
    }

    private fun refreshHeader() {
        val name = MockDataSource.profileName
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greet = when {
            hour < 12 -> getString(R.string.greeting_morning, name)
            hour < 17 -> getString(R.string.greeting_afternoon, name)
            else -> getString(R.string.greeting_evening, name)
        }
        binding.greetingText.text = greet
        binding.statMaterials.text = MockDataSource.files.size.toString()
        binding.statSubjects.text = (MockDataSource.subjects.size - 1).toString()
        binding.statQuizzes.text = MockDataSource.quizSets.size.toString()
        binding.quizSetsSubtitle.text = getString(R.string.quiz_sets_available, MockDataSource.quizSets.size)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
