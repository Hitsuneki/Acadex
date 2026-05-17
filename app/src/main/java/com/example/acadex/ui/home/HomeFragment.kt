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
import com.example.acadex.data.repository.ProfileRepository
import com.example.acadex.databinding.FragmentHomeBinding
import com.example.acadex.util.SubjectChipHelper
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
        adapter = FileCardAdapter(onItemClick = { file ->
            findNavController().navigate(HomeFragmentDirections.actionHomeToFileDetail(materialId = file.id))
        })
        binding.filesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.filesRecycler.adapter = adapter

        viewModel.files.observe(viewLifecycleOwner) { adapter.submitList(it) }
        viewModel.subjects.observe(viewLifecycleOwner) { chips.updateSubjects(it) }
        viewModel.quizCount.observe(viewLifecycleOwner) { count ->
            binding.statQuizzes.text = count.toString()
            binding.quizSetsSubtitle.text = getString(R.string.quiz_sets_available, count)
        }

        binding.btnSearch.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeToBrowse(true))
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
        val name = ProfileRepository.cachedProfile.value?.displayName?.ifBlank { null }
            ?: getString(R.string.default_user_name)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greet = when {
            hour < 12 -> getString(R.string.greeting_morning, name)
            hour < 17 -> getString(R.string.greeting_afternoon, name)
            else -> getString(R.string.greeting_evening, name)
        }
        binding.greetingText.text = greet
        binding.statMaterials.text = viewModel.materialCount().toString()
        binding.statSubjects.text = viewModel.subjectCount().toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
