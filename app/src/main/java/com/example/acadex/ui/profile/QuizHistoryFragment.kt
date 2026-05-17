package com.example.acadex.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.acadex.R
import com.example.acadex.adapters.QuizHistoryAdapter
import com.example.acadex.databinding.FragmentMaterialListBinding
import com.example.acadex.databinding.IncludeErrorStateBinding
import com.example.acadex.ui.common.UiState
import kotlinx.coroutines.launch

class QuizHistoryFragment : Fragment() {

    private var _binding: FragmentMaterialListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizHistoryViewModel by viewModels()
    private val adapter = QuizHistoryAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMaterialListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.title = getString(R.string.row_quiz_history)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.emptyText.text = getString(R.string.empty_quiz_history)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        val errorBinding = IncludeErrorStateBinding.bind(binding.errorInclude.root)
        errorBinding.btnRetry.setOnClickListener { viewModel.load() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.loadingBar.isVisible = state is UiState.Loading
                    errorBinding.root.isVisible = state is UiState.Error
                    if (state is UiState.Error) errorBinding.errorMessage.text = state.message
                    if (state is UiState.Success) {
                        adapter.submitList(state.data)
                        binding.emptyText.isVisible = state.data.isEmpty()
                        binding.recycler.isVisible = state.data.isNotEmpty()
                    }
                }
            }
        }
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
