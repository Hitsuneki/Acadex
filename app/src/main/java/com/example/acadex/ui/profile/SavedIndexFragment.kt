package com.example.acadex.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.acadex.R
import com.example.acadex.adapter.FileCardAdapter
import com.example.acadex.ui.saved.SavedIndexSharedViewModel
import com.example.acadex.adapter.FileCardAction
import com.example.acadex.databinding.FragmentMaterialListBinding
import com.example.acadex.databinding.IncludeErrorStateBinding
import com.example.acadex.ui.common.UiState
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SavedIndexFragment : Fragment() {

    private var _binding: FragmentMaterialListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SavedIndexViewModel by viewModels()
    private val savedShared: SavedIndexSharedViewModel by navGraphViewModels(R.id.nav_graph)
    private lateinit var adapter: FileCardAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMaterialListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.title = getString(R.string.row_saved_index)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.emptyText.text = getString(R.string.empty_saved_index)

        adapter = FileCardAdapter(
            onItemClick = { file ->
                val gutendexId = file.remoteId?.toIntOrNull()
                if (gutendexId != null) {
                    findNavController().navigate(
                        SavedIndexFragmentDirections.actionSavedIndexToGutendexDetail(gutendexId)
                    )
                } else {
                    findNavController().navigate(
                        SavedIndexFragmentDirections.actionSavedIndexToFileDetail(file.id)
                    )
                }
            },
            action = FileCardAction.BOOKMARK_FILLED,
            onActionClick = { file ->
                savedShared.setSaved(file.id, false)
                viewModel.unsave(file)
                Snackbar.make(binding.root, R.string.removed_from_saved, Snackbar.LENGTH_SHORT).show()
            }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        val errorBinding = IncludeErrorStateBinding.bind(binding.errorInclude.root)
        errorBinding.btnRetry.setOnClickListener { viewModel.load() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.loadingBar.isVisible = state is UiState.Loading
                        errorBinding.root.isVisible = state is UiState.Error
                        if (state is UiState.Error) errorBinding.errorMessage.text = state.message
                        if (state is UiState.Success) {
                            submitSavedList(state.data)
                        }
                    }
                }
                launch {
                    savedShared.savedOverrides.collect {
                        val state = viewModel.uiState.value
                        if (state is UiState.Success) submitSavedList(state.data)
                    }
                }
            }
        }
    }

    private fun submitSavedList(files: List<com.example.acadex.data.model.ResourceFile>) {
        val overrides = savedShared.savedOverrides.value
        val list = files.map { file ->
            val saved = overrides[file.id] ?: file.isSaved
            file.copy(isSaved = saved)
        }.filter { it.isSaved }
        adapter.submitList(list)
        binding.emptyText.isVisible = list.isEmpty()
        binding.recycler.isVisible = list.isNotEmpty()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
