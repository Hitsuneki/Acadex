package com.example.acadex.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.R
import com.example.acadex.adapter.FileCardAdapter
import com.example.acadex.data.model.ResourceFile
import com.example.acadex.data.repository.ProfileRepository
import com.example.acadex.databinding.FragmentHomeBinding
import com.example.acadex.databinding.ItemHistoryCardBinding
import com.example.acadex.util.FileTypeUtils
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    
    private lateinit var mainAdapter: FileCardAdapter
    private lateinit var searchResultsAdapter: FileCardAdapter
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainAdapter = FileCardAdapter(onItemClick = { file ->
            findNavController().navigate(
                HomeFragmentDirections.actionHomeToFileDetail(materialId = file.id)
            )
        })
        binding.filesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.filesRecycler.adapter = mainAdapter

        searchResultsAdapter = FileCardAdapter(onItemClick = { file ->
            binding.searchView.hide()
            // remoteId is an integer for Gutendex books but a UUID for Supabase materials.
            // Use toIntOrNull() to safely distinguish the two cases.
            val gutendexId = file.remoteId?.toIntOrNull()
            if (gutendexId != null) {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeToGutendexDetail(bookId = gutendexId)
                )
            } else {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeToFileDetail(materialId = file.id)
                )
            }
        })
        binding.searchResultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.searchResultsRecycler.adapter = searchResultsAdapter

        historyAdapter = HistoryAdapter { file ->
            val gutendexId = file.remoteId?.toIntOrNull()
            if (gutendexId != null) {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeToGutendexDetail(bookId = gutendexId)
                )
            } else {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeToFileDetail(materialId = file.id)
                )
            }
        }
        binding.historyRecycler.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.historyRecycler.adapter = historyAdapter

        binding.searchView.editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.performSearch(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        viewModel.files.observe(viewLifecycleOwner) { files ->
            binding.swipeRefresh.isRefreshing = false
            mainAdapter.submitList(files.take(5))
            historyAdapter.submitList(files.take(5))
            binding.historyHeader.visibility = if (files.isEmpty()) View.GONE else View.VISIBLE
            binding.historyRecycler.visibility = if (files.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            searchResultsAdapter.submitList(results)
        }

        viewModel.quizCount.observe(viewLifecycleOwner) { count ->
            binding.statQuizzes.text = count.toString()
            binding.quizSetsSubtitle.text = getString(R.string.quiz_sets_available, count)
        }

        viewModel.savedCount.observe(viewLifecycleOwner) { count ->
            binding.statSaved.text = count.toString()
        }

        binding.btnSeeAll.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeToBrowse(false))
        }
        binding.btnSeeAllQuizzes.setOnClickListener {
            findNavController().navigate(R.id.quizFragment)
        }
        binding.quizPromoCard.setOnClickListener {
            findNavController().navigate(R.id.quizFragment)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
            refreshHeader()
        }

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class HistoryAdapter(
        private val onItemClick: (ResourceFile) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.VH>() {

        private var items = emptyList<ResourceFile>()

        fun submitList(list: List<ResourceFile>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemHistoryCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return VH(binding)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.binding.title.text = item.title
            holder.binding.subjectBadge.text = item.subject
            holder.binding.typeIcon.setImageResource(FileTypeUtils.iconRes(item.fileType))
            
            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount(): Int = items.size

        class VH(val binding: ItemHistoryCardBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
