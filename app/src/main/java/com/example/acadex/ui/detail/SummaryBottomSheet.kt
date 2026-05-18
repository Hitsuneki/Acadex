package com.example.acadex.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.acadex.databinding.BottomSheetSummaryBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SummaryBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SummaryBottomSheet"
    }

    private var _binding: BottomSheetSummaryBinding? = null
    private val binding get() = _binding!!

    /** Shared with the hosting FileDetailFragment — retrieved lazily after attach */
    private val parentVm: FileDetailViewModel by lazy {
        (requireParentFragment() as FileDetailFragment).provideViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill material subtitle
        parentVm.material.value?.let { binding.summaryMaterialName.text = it.title }

        binding.btnRetrySummary.setOnClickListener {
            parentVm.summarize()
        }

        binding.btnCopySummary.setOnClickListener {
            val summary = (parentVm.summaryState.value as? SummaryUiState.Success)?.summary
                ?: return@setOnClickListener
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Acadex AI Summary", summary))
            Snackbar.make(binding.root, "Summary copied to clipboard.", Snackbar.LENGTH_SHORT).show()
        }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    parentVm.material.collect { material ->
                        material?.let { binding.summaryMaterialName.text = it.title }
                    }
                }

                launch {
                    parentVm.summaryState.collect { state -> renderState(state) }
                }

                launch {
                    parentVm.summaryElapsed.collect { seconds ->
                        val loading = parentVm.summaryState.value is SummaryUiState.Loading
                        if (loading) {
                            binding.summaryTimerText.text = "Analyzing\u2026 ${seconds}s"
                            binding.summaryTimerBadge.text = "${seconds}s"
                            binding.summaryTimerBadge.isVisible = true
                        } else {
                            binding.summaryTimerBadge.isVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun renderState(state: SummaryUiState) {
        binding.summaryLoadingGroup.isVisible    = false
        binding.summarySuccessGroup.isVisible    = false
        binding.summaryErrorGroup.isVisible      = false
        binding.summaryUnsupportedGroup.isVisible = false
        binding.summaryTimerBadge.isVisible      = false

        when (state) {
            is SummaryUiState.Idle,
            is SummaryUiState.Loading -> {
                binding.summaryLoadingGroup.isVisible = true
                if (state is SummaryUiState.Loading) {
                    binding.summaryTimerBadge.isVisible = true
                }
            }
            is SummaryUiState.Success -> {
                binding.summarySuccessGroup.isVisible = true
                binding.summaryText.text = formatMarkdown(state.summary)
            }
            is SummaryUiState.Error -> {
                binding.summaryErrorGroup.isVisible = true
                binding.summaryErrorText.text = state.message
            }
            is SummaryUiState.Unsupported -> {
                binding.summaryUnsupportedGroup.isVisible = true
            }
        }
    }

    /**
     * Lightweight markdown → readable text: strips ## headings and bullet markers.
     * Upgrade to Markwon library later for full rendering if desired.
     */
    private fun formatMarkdown(raw: String): String = raw.lines().joinToString("\n") { line ->
        when {
            line.startsWith("### ") -> "\n" + line.removePrefix("### ").uppercase()
            line.startsWith("## ")  -> "\n" + line.removePrefix("## ").uppercase()
            line.startsWith("# ")   -> "\n" + line.removePrefix("# ").uppercase()
            line.startsWith("**") && line.trimEnd().endsWith("**") ->
                line.trim().removeSurrounding("**")
            line.startsWith("- ") || line.startsWith("* ") ->
                "  \u2022 " + line.substring(2)
            else -> line
        }
    }.trimStart()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
