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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.acadex.R
import com.example.acadex.adapter.FileCardAdapter
import com.example.acadex.data.MockDataSource
import com.example.acadex.databinding.FragmentBrowseBinding
import com.example.acadex.util.SubjectChipHelper

class BrowseFragment : Fragment() {

    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BrowseViewModel by viewModels()
    private val args: BrowseFragmentArgs by navArgs()
    private lateinit var fileAdapter: FileCardAdapter
    private lateinit var chipHelper: SubjectChipHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowseBinding.inflate(inflater, container, false)
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
                1 -> MockDataSource.SortOption.MOST_DOWNLOADED
                2 -> MockDataSource.SortOption.TOP_RATED
                else -> MockDataSource.SortOption.NEWEST
            }
            viewModel.setSort(sort)
        }

        fileAdapter = FileCardAdapter { file ->
            findNavController().navigate(
                BrowseFragmentDirections.actionBrowseToFileDetail(fileId = file.id)
            )
        }
        binding.filesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.filesRecycler.adapter = fileAdapter

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.files.observe(viewLifecycleOwner) { files ->
            fileAdapter.submitList(files)
            binding.emptyState.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
            binding.filesRecycler.visibility = if (files.isEmpty()) View.GONE else View.VISIBLE
        }

        if (args.focusSearch) {
            binding.searchEditText.requestFocus()
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
