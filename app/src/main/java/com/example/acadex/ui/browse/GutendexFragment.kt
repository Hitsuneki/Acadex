package com.example.acadex.ui.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.adapter.FileCardAdapter
import com.example.acadex.adapter.ViewMode
import com.example.acadex.databinding.FragmentGutendexBinding
import com.example.acadex.util.SubjectChipHelper
import kotlinx.coroutines.launch

class GutendexFragment : Fragment() {

    private var _binding: FragmentGutendexBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GutendexViewModel by viewModels()
    private val parentViewModel: BrowseViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private lateinit var fileAdapter: FileCardAdapter
    private lateinit var chipHelper: SubjectChipHelper

    private val topics = listOf(
        "All", "Fiction", "Philosophy", "History", "Science", "Poetry",
        "Drama", "Mathematics", "Psychology", "Economics", "Law",
        "Religion", "Adventure", "Mystery"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGutendexBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chipHelper = SubjectChipHelper(binding.topicChipsRecycler) { topic ->
            viewModel.setTopic(topic)
        }
        chipHelper.setup(topics)

        fileAdapter = FileCardAdapter(onItemClick = { file ->
            val idStr = file.remoteId ?: return@FileCardAdapter
            findNavController().navigate(
                BrowseFragmentDirections.actionBrowseToGutendexDetail(bookId = idStr.toInt())
            )
        })
        binding.booksRecycler.adapter = fileAdapter

        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.setSearch(binding.searchEditText.text.toString())
                true
            } else {
                false
            }
        }

        binding.booksRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lm = recyclerView.layoutManager ?: return
                val totalItemCount = lm.itemCount
                val lastVisibleItemPosition = when (lm) {
                    is LinearLayoutManager -> lm.findLastVisibleItemPosition()
                    is GridLayoutManager -> lm.findLastVisibleItemPosition()
                    else -> 0
                }
                if (totalItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 3) {
                    viewModel.loadNextPage()
                }
            }
        })

        // Observe ViewMode from parent
        viewLifecycleOwner.lifecycleScope.launch {
            parentViewModel.viewMode.collect { mode ->
                fileAdapter.viewMode = mode
                binding.booksRecycler.layoutManager = when (mode) {
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
                    is GutendexUiState.Loading -> {
                        binding.shimmerViewContainer.visibility = View.VISIBLE
                        binding.booksRecycler.visibility = View.GONE
                        binding.emptyState.visibility = View.GONE
                        binding.paginationProgress.visibility = View.GONE
                        binding.endOfListText.visibility = View.GONE
                    }
                    is GutendexUiState.Success -> {
                        binding.shimmerViewContainer.visibility = View.GONE
                        binding.booksRecycler.visibility = if (state.books.isEmpty()) View.GONE else View.VISIBLE
                        binding.emptyState.visibility = if (state.books.isEmpty()) View.VISIBLE else View.GONE
                        
                        fileAdapter.submitList(state.books)

                        binding.paginationProgress.visibility = if (state.isPaginating) View.VISIBLE else View.GONE
                        binding.endOfListText.visibility = if (!state.hasMore && state.books.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                    is GutendexUiState.Error -> {
                        binding.shimmerViewContainer.visibility = View.GONE
                        binding.booksRecycler.visibility = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                        binding.emptyState.text = state.message
                        binding.paginationProgress.visibility = View.GONE
                        binding.endOfListText.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
