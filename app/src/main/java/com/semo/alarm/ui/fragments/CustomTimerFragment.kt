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
            // TODO: Open custom timer creation dialog/activity
            val currentPosition = binding.viewPager.currentItem
            val currentCategoryId = pagerAdapter.getCategoryId(currentPosition)
            val categoryName = pagerAdapter.getCategoryTitle(currentPosition)
            
            Toast.makeText(
                context,
                "${categoryName} 타이머 추가 기능은 개발 중입니다",
                Toast.LENGTH_SHORT
            ).show()
            
            // Future implementation:
            // showCreateCustomTimerDialog(currentCategoryId)
        }
    }
    
    private fun observeViewModel() {
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}