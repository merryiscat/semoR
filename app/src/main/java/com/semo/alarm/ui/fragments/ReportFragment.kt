package com.semo.alarm.ui.fragments

import android.app.AlertDialog
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
    private var currentReportType = ReportType.DAILY // 기본값: 일반 리포트
    
    enum class ReportType(val displayName: String) {
        DAILY("일반 리포트"),
        WEEKLY("주간 리포트")
    }
    
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
        updateReportTitle()
        setupReportTypeSelector()
        setupViewPager()
        observeViewModel()
    }
    
    private fun setupReportTypeSelector() {
        // 제목을 클릭하면 리포트 타입 선택 다이얼로그 표시
        binding.textReportTitle.setOnClickListener {
            showReportTypeSelectionDialog()
        }
    }
    
    private fun showReportTypeSelectionDialog() {
        val reportTypes = arrayOf(
            ReportType.DAILY.displayName,
            ReportType.WEEKLY.displayName
        )
        
        AlertDialog.Builder(requireContext())
            .setTitle("리포트 타입 선택")
            .setItems(reportTypes) { _, which ->
                when (which) {
                    0 -> switchToReportType(ReportType.DAILY)
                    1 -> switchToReportType(ReportType.WEEKLY)
                }
            }
            .show()
    }
    
    private fun switchToReportType(reportType: ReportType) {
        if (currentReportType != reportType) {
            currentReportType = reportType
            updateReportTitle()
            setupViewPager() // ViewPager 재설정
        }
    }
    
    private fun updateReportTitle() {
        binding.textReportTitle.text = currentReportType.displayName
    }
    
    private fun setupViewPager() {
        val adapter = ReportPagerAdapter(this, currentReportType)
        binding.viewPagerReport.adapter = adapter
        
        if (currentReportType == ReportType.DAILY) {
            // 일반 리포트: 단일 화면
            binding.tabLayoutReport.visibility = View.GONE
            binding.viewPagerReport.isUserInputEnabled = false
        } else {
            // 주간 리포트: 탭 구조 유지  
            binding.tabLayoutReport.visibility = View.VISIBLE
            binding.viewPagerReport.isUserInputEnabled = true
            
            TabLayoutMediator(binding.tabLayoutReport, binding.viewPagerReport) { tab, position ->
                tab.text = when (position) {
                    0 -> "오늘"
                    1 -> "주간"
                    2 -> "4주간"
                    3 -> "종합"
                    else -> "탭 $position"
                }
            }.attach()
        }
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

class ReportPagerAdapter(
    fragment: Fragment, 
    private val reportType: ReportFragment.ReportType
) : FragmentStateAdapter(fragment) {
    
    override fun getItemCount(): Int {
        return if (reportType == ReportFragment.ReportType.DAILY) {
            1 // 일반 리포트는 단일 화면
        } else {
            4 // 주간 리포트는 4개 탭
        }
    }
    
    override fun createFragment(position: Int): Fragment {
        return if (reportType == ReportFragment.ReportType.DAILY) {
            DailyReportFragment() // 🆕 새로운 일반 리포트 프래그먼트
        } else {
            when (position) {
                0 -> ReportTodayFragment()
                1 -> ReportWeeklyFragment() 
                2 -> ReportFourWeekFragment()
                3 -> ReportOverallFragment()
                else -> ReportTodayFragment()
            }
        }
    }
}