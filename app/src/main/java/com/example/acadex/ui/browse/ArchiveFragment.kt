package com.example.acadex.ui.browse

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.acadex.R
import com.example.acadex.adapter.FileCardAdapter
import com.example.acadex.adapter.ViewMode
import com.example.acadex.data.SortOption
import com.example.acadex.data.SubjectCatalog
import com.example.acadex.databinding.FragmentArchiveBinding
import com.example.acadex.util.SubjectChipHelper
import kotlinx.coroutines.launch

class ArchiveFragment : Fragment() {

    private var _binding: FragmentArchiveBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArchiveViewModel by viewModels()
    private val parentViewModel: BrowseViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private lateinit var fileAdapter: FileCardAdapter
    private lateinit var chipHelper: SubjectChipHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArchiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chipHelper = SubjectChipHelper(binding.subjectChipsRecycler) { subject ->
            viewModel.setSubject(subject)
        }
        chipHelper.setup()

        val sortOptions = resources.getStringArray(R.array.sort_options)
        binding.sortDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sortOptions)
        )
        binding.sortDropdown.setText(sortOptions[0], false)
        binding.sortDropdown.setOnItemClickListener { _, _, position, _ ->
            val sort = when (position) {
                1 -> SortOption.MOST_DOWNLOADED
                2 -> SortOption.TOP_RATED
                else -> SortOption.NEWEST
            }
            viewModel.setSort(sort)
        }

        fileAdapter = FileCardAdapter(onItemClick = { file ->
            findNavController().navigate(
                BrowseFragmentDirections.actionBrowseToFileDetail(materialId = file.id)
            )
        })
        binding.filesRecycler.adapter = fileAdapter

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Observe ViewMode from parent
        viewLifecycleOwner.lifecycleScope.launch {
            parentViewModel.viewMode.collect { mode ->
                fileAdapter.viewMode = mode
                binding.filesRecycler.layoutManager = when (mode) {
                    ViewMode.ROW -> LinearLayoutManager(requireContext())
                    ViewMode.TILE -> GridLayoutManager(requireContext(), 2)
                    ViewMode.COMPACT -> LinearLayoutManager(requireContext())
                }
            }
        }

        // Observe UI state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is ArchiveUiState.Loading -> {
                        if (!binding.swipeRefresh.isRefreshing) {
                            binding.shimmerViewContainer.visibility = View.VISIBLE
                        }
                        binding.filesRecycler.visibility = View.GONE
                        binding.emptyState.visibility = View.GONE
                    }
                    is ArchiveUiState.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.shimmerViewContainer.visibility = View.GONE
                        binding.filesRecycler.visibility = if (state.files.isEmpty()) View.GONE else View.VISIBLE
                        binding.emptyState.visibility = if (state.files.isEmpty()) View.VISIBLE else View.GONE
                        
                        chipHelper.updateSubjects(SubjectCatalog.forMaterials(state.files))
                        fileAdapter.submitList(state.files)
                    }
                    is ArchiveUiState.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.shimmerViewContainer.visibility = View.GONE
                        binding.filesRecycler.visibility = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                        binding.emptyState.text = state.message
                    }
                }
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
