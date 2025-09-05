package com.semo.alarm.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.semo.alarm.databinding.FragmentReportTodayBinding
import com.semo.alarm.ui.viewmodels.ReportViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportTodayFragment : Fragment() {
    
    private var _binding: FragmentReportTodayBinding? = null
    private val binding get() = _binding!!
    
    private val reportViewModel: ReportViewModel by activityViewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportTodayBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }
    
    private fun observeViewModel() {
        // Observe today's report data
        reportViewModel.todaysReport.observe(viewLifecycleOwner) { reportData ->
            if (reportData != null) {
                updateUI(reportData)
            } else {
                // Show placeholder or generate report
                reportViewModel.generateTodaysReportIfNeeded()
            }
        }
        
        // Observe suggestions
        reportViewModel.suggestions.observe(viewLifecycleOwner) { suggestions ->
            if (suggestions.isNullOrEmpty()) {
                binding.recyclerViewSuggestions.visibility = View.GONE
                binding.textNoSuggestions.visibility = View.VISIBLE
            } else {
                binding.recyclerViewSuggestions.visibility = View.VISIBLE
                binding.textNoSuggestions.visibility = View.GONE
                // TODO: Setup suggestions adapter
            }
        }
    }
    
    private fun updateUI(reportData: com.semo.alarm.data.entities.ReportData) {
        with(binding) {
            // 수면 데이터
            textTodaySleep.text = reportData.getFormattedSleepDuration()
            textTodaySleepStatus.text = reportData.getSleepQualityText()
            
            // 알람 데이터
            textTodayAlarm.text = reportData.getFormattedDismissalTime()
            textTodayAlarmStatus.text = if (reportData.alarmDismissalTime <= 120) "빠름" else "보통"
            
            // 타이머 데이터
            textTodayTimer.text = reportData.getFormattedTimerUsage()
            textTodayTimerStatus.text = when {
                reportData.timerCompletionRate >= 0.9f -> "우수"
                reportData.timerCompletionRate >= 0.7f -> "활발"
                reportData.timerCompletionRate >= 0.5f -> "보통"
                else -> "개선 필요"
            }
            
            // 생산성 점수
            val productivityScore = reportData.productivityScore.toInt()
            textProductivityScore.text = productivityScore.toString()
            progressProductivity.progress = productivityScore
            textProductivityGrade.text = "등급: ${reportData.getProductivityGrade()}"
            
            // 라이프스타일 점수
            val lifestyleScore = reportData.lifestyleScore.toInt()
            textLifestyleScore.text = lifestyleScore.toString()
            progressLifestyle.progress = lifestyleScore
            textLifestyleStatus.text = "상태: ${reportData.getLifestyleText()}"
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Placeholder fragments for other tabs (to be implemented later)
class ReportWeeklyFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(android.R.layout.simple_list_item_1, container, false).apply {
            findViewById<android.widget.TextView>(android.R.id.text1).text = "📈 주간 리포트\n\n구현 예정..."
        }
    }
}

class ReportMonthlyFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(android.R.layout.simple_list_item_1, container, false).apply {
            findViewById<android.widget.TextView>(android.R.id.text1).text = "📊 월간 리포트\n\n구현 예정..."
        }
    }
}

class ReportOverallFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(android.R.layout.simple_list_item_1, container, false).apply {
            findViewById<android.widget.TextView>(android.R.id.text1).text = "🏆 종합 리포트\n\n구현 예정..."
        }
    }
}