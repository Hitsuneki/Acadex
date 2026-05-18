package com.example.acadex.ui.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.acadex.R
import com.example.acadex.adapter.ViewMode
import com.example.acadex.databinding.FragmentBrowseBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class BrowseFragment : Fragment() {

    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BrowseViewModel by viewModels()

    private var btnViewMode: ImageButton? = null

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

        val adapter = BrowsePagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Archive"
                1 -> "Books"
                else -> ""
            }
        }.attach()

        btnViewMode = requireActivity().findViewById(R.id.btn_view_mode)
        btnViewMode?.visibility = View.VISIBLE
        btnViewMode?.setOnClickListener {
            viewModel.toggleViewMode()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.viewMode.collect { mode ->
                val iconRes = when (mode) {
                    ViewMode.ROW -> R.drawable.ic_view_row
                    ViewMode.TILE -> R.drawable.ic_view_grid
                    ViewMode.COMPACT -> R.drawable.ic_view_compact
                }
                btnViewMode?.setImageResource(iconRes)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        btnViewMode?.visibility = View.GONE
        btnViewMode = null
        _binding = null
    }

    private class BrowsePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ArchiveFragment()
                1 -> GutendexFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}
