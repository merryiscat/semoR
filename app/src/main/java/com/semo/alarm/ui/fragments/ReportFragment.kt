package com.semo.alarm.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.semo.alarm.databinding.FragmentReportBinding
import com.semo.alarm.ui.viewmodels.ReportViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportFragment : Fragment() {
    
    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!
    
    private val reportViewModel: ReportViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        observeViewModel()
    }
    
    private fun setupViewPager() {
        val adapter = ReportPagerAdapter(this)
        binding.viewPagerReport.adapter = adapter
        
        TabLayoutMediator(binding.tabLayoutReport, binding.viewPagerReport) { tab, position ->
            tab.text = when (position) {
                0 -> "📅 오늘"
                1 -> "📈 주간"
                2 -> "📊 월간"
                3 -> "🏆 종합"
                else -> "탭 $position"
            }
        }.attach()
    }
    
    private fun observeViewModel() {
        // Loading state
        reportViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarReport.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Error messages
        reportViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                showSnackbar(errorMessage)
                reportViewModel.clearErrorMessage()
            }
        }
    }
    
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ReportPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    
    override fun getItemCount(): Int = 4
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ReportTodayFragment()
            1 -> ReportWeeklyFragment() 
            2 -> ReportMonthlyFragment()
            3 -> ReportOverallFragment()
            else -> ReportTodayFragment()
        }
    }
}