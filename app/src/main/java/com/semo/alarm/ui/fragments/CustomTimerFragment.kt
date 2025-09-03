package com.semo.alarm.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.semo.alarm.databinding.FragmentCustomTimerBinding
import com.semo.alarm.ui.adapters.TimerCategoryPagerAdapter
import com.semo.alarm.ui.viewmodels.CustomTimerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomTimerFragment : Fragment() {
    
    private var _binding: FragmentCustomTimerBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CustomTimerViewModel by viewModels()
    private lateinit var pagerAdapter: TimerCategoryPagerAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomTimerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewPager()
        setupTabLayout()
        setupFab()
        observeViewModel()
        
        // Load categories on start
        viewModel.loadAllCategories()
    }
    
    private fun setupViewPager() {
        pagerAdapter = TimerCategoryPagerAdapter(requireActivity())
        binding.viewPager.adapter = pagerAdapter
    }
    
    private fun setupTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = pagerAdapter.getCategoryTitle(position)
        }.attach()
    }
    
    private fun setupFab() {
        binding.fabAddCustomTimer.setOnClickListener {
            if (pagerAdapter.isEmpty()) {
                Toast.makeText(
                    context,
                    "먼저 카테고리를 불러와야 합니다",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            
            val currentPosition = binding.viewPager.currentItem
            val currentCategory = pagerAdapter.getCategory(currentPosition)
            
            if (currentCategory != null) {
                Toast.makeText(
                    context,
                    "${currentCategory.getDisplayName()} 타이머 추가 기능은 개발 중입니다",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Future implementation:
                // showCreateCustomTimerDialog(currentCategory)
            } else {
                Toast.makeText(
                    context,
                    "카테고리 정보를 불러올 수 없습니다",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun observeViewModel() {
        // Observe error messages
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        // Observe categories
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            if (categories.isNotEmpty()) {
                pagerAdapter.updateCategories(categories)
                setupTabLayout() // Re-setup tabs with new categories
            } else {
                // Show empty state or error
                Toast.makeText(context, "카테고리를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: Show/hide loading indicator if needed
            // For now, we can just log or ignore
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}