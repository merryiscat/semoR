package com.semo.alarm.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.semo.alarm.databinding.FragmentSleepBinding
import com.semo.alarm.ui.viewmodels.SleepViewModel
import com.semo.alarm.ui.viewmodels.SleepTrackingState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class SleepFragment : Fragment() {
    
    private var _binding: FragmentSleepBinding? = null
    private val binding get() = _binding!!
    
    private val sleepViewModel: SleepViewModel by viewModels()
    
    private val handler = Handler(Looper.getMainLooper())
    private var elapsedTimeRunnable: Runnable? = null
    private var sleepStartTime: Long = 0L
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSleepBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        binding.buttonStartSleep.setOnClickListener {
            sleepViewModel.startSleepTracking()
        }
        
        binding.buttonStopSleep.setOnClickListener {
            sleepViewModel.stopSleepTracking()
        }
        
        binding.switchSnoringDetection.setOnCheckedChangeListener { _, isChecked ->
            // TODO: 코골이 감지 설정 저장
            showSnackbar(if (isChecked) "코골이 감지가 활성화되었습니다" else "코골이 감지가 비활성화되었습니다")
        }
    }
    
    private fun observeViewModel() {
        // 수면 추적 상태 관찰
        sleepViewModel.sleepTrackingState.observe(viewLifecycleOwner) { state ->
            updateUIForTrackingState(state)
        }
        
        // 활성화된 수면 기록 관찰
        sleepViewModel.activeSleepRecord.observe(viewLifecycleOwner) { sleepRecord ->
            if (sleepRecord != null && sleepRecord.isActive) {
                sleepStartTime = sleepRecord.bedtime
                startElapsedTimeUpdates()
            } else {
                stopElapsedTimeUpdates()
            }
        }
        
        // 오늘 수면 요약 관찰
        sleepViewModel.todaysSleepSummary.observe(viewLifecycleOwner) { summary ->
            if (summary != null) {
                binding.cardTodaysSummary.visibility = View.VISIBLE
                
                val bedtimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val bedtime = bedtimeFormat.format(Date(summary.bedtime))
                val wakeupTime = if (summary.wakeupTime != null) {
                    bedtimeFormat.format(Date(summary.wakeupTime))
                } else {
                    "진행 중"
                }
                
                binding.textTodaysSleepTime.text = "$bedtime → $wakeupTime (${summary.sleepDuration})"
                binding.textTodaysSnoring.text = "😴 코골이: ${summary.snoringPercentage}%"
            } else {
                binding.cardTodaysSummary.visibility = View.GONE
            }
        }
        
        // 평균 통계 관찰
        sleepViewModel.averageStats.observe(viewLifecycleOwner) { stats ->
            if (stats != null) {
                binding.textAverageSleep.text = "수면: ${stats.averageSleepDuration}"
                binding.textAverageSnoring.text = "코골이: ${stats.averageSnoringPercentage}% (평균)"
                binding.textRecordCount.text = "기록 개수: ${stats.recordCount}일"
            }
        }
        
        // 에러 메시지 관찰
        sleepViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                showSnackbar(errorMessage)
                sleepViewModel.clearErrorMessage()
            }
        }
    }
    
    private fun updateUIForTrackingState(state: SleepTrackingState) {
        when (state) {
            SleepTrackingState.IDLE -> {
                binding.textSleepStatus.text = "수면 상태: 대기 중"
                binding.textElapsedTime.visibility = View.GONE
                binding.buttonStartSleep.isEnabled = true
                binding.buttonStartSleep.text = "취침 시작"
                binding.buttonStartSleep.backgroundTintList = 
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00D4FF"))
                binding.buttonStopSleep.isEnabled = false
                binding.buttonStopSleep.backgroundTintList = 
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6B7280"))
                stopElapsedTimeUpdates()
            }
            
            SleepTrackingState.STARTING -> {
                binding.textSleepStatus.text = "수면 상태: 시작 중..."
                binding.buttonStartSleep.isEnabled = false
                binding.buttonStopSleep.isEnabled = false
            }
            
            SleepTrackingState.TRACKING -> {
                binding.textSleepStatus.text = "수면 상태: 수면 중"
                binding.textElapsedTime.visibility = View.VISIBLE
                binding.buttonStartSleep.isEnabled = false
                binding.buttonStartSleep.backgroundTintList = 
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6B7280"))
                binding.buttonStopSleep.isEnabled = true
                binding.buttonStopSleep.text = "기상"
                binding.buttonStopSleep.backgroundTintList = 
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00D4FF"))
            }
            
            SleepTrackingState.STOPPING -> {
                binding.textSleepStatus.text = "수면 상태: 종료 중..."
                binding.buttonStartSleep.isEnabled = false
                binding.buttonStopSleep.isEnabled = false
            }
        }
    }
    
    private fun startElapsedTimeUpdates() {
        stopElapsedTimeUpdates() // 기존 것이 있다면 정리
        
        elapsedTimeRunnable = object : Runnable {
            override fun run() {
                if (sleepStartTime > 0) {
                    val elapsedMillis = System.currentTimeMillis() - sleepStartTime
                    val elapsedHours = (elapsedMillis / (1000 * 60 * 60)).toInt()
                    val elapsedMinutes = ((elapsedMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                    
                    binding.textElapsedTime.text = "경과 시간: ${elapsedHours}시간 ${elapsedMinutes}분"
                }
                
                handler.postDelayed(this, 60000) // 1분마다 업데이트
            }
        }
        
        elapsedTimeRunnable?.let { handler.post(it) }
    }
    
    private fun stopElapsedTimeUpdates() {
        elapsedTimeRunnable?.let { handler.removeCallbacks(it) }
        elapsedTimeRunnable = null
    }
    
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        stopElapsedTimeUpdates()
        _binding = null
    }
}