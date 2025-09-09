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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.semo.alarm.databinding.FragmentSleepBinding
import com.semo.alarm.ui.adapters.SnoringRecordingAdapter
import com.semo.alarm.ui.viewmodels.SleepViewModel
import com.semo.alarm.ui.viewmodels.SleepTrackingState
import com.semo.alarm.utils.AudioFileInfo
import com.semo.alarm.utils.PlaybackState
import com.semo.alarm.utils.SnoringAudioPlayer
import com.semo.alarm.utils.SnoringAudioRecorder
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
    
    // 🎙️ 코골이 녹음 관련 컴포넌트
    private lateinit var snoringRecordingAdapter: SnoringRecordingAdapter
    private var audioPlayer: SnoringAudioPlayer? = null
    private var audioRecorder: SnoringAudioRecorder? = null
    
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
        setupSnoringRecording()
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
    
    // ═══════════════════════════════════════════════════
    // 🎙️ 코골이 녹음 기능
    // ═══════════════════════════════════════════════════
    
    private fun setupSnoringRecording() {
        // 오디오 플레이어 초기화
        audioPlayer = SnoringAudioPlayer(
            context = requireContext(),
            onPlaybackStateChanged = { state ->
                snoringRecordingAdapter.updatePlaybackState(audioPlayer?.getCurrentFilePath(), state)
            },
            onError = { errorMessage ->
                showSnackbar("재생 오류: $errorMessage")
            }
        )
        
        // 오디오 레코더 초기화 (저장소 사용량 표시용)
        audioRecorder = SnoringAudioRecorder(
            context = requireContext(),
            onRecordingSaved = { _, _ -> 
                updateStorageUsage()
                loadRecentRecordings()
            },
            onError = { errorMessage ->
                showSnackbar("녹음 오류: $errorMessage")
            }
        )
        
        // 리사이클러뷰 설정
        snoringRecordingAdapter = SnoringRecordingAdapter(
            onPlayPauseClick = { audioFile ->
                handlePlayPauseClick(audioFile)
            },
            onItemClick = { audioFile ->
                // 아이템 클릭 시 재생/일시정지 토글
                handlePlayPauseClick(audioFile)
            }
        )
        
        binding.recyclerViewSnoringRecordings.apply {
            adapter = snoringRecordingAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        
        // 모든 녹음 보기 버튼
        binding.buttonViewAllRecordings.setOnClickListener {
            startActivity(com.semo.alarm.ui.activities.AllSnoringRecordingsActivity.createIntent(requireContext()))
        }
        
        // 초기 데이터 로드
        loadRecentRecordings()
        updateStorageUsage()
    }
    
    private fun handlePlayPauseClick(audioFile: AudioFileInfo) {
        val player = audioPlayer ?: return
        
        when {
            player.getCurrentFilePath() == audioFile.filePath && player.isPlaying() -> {
                // 현재 재생 중인 파일 일시정지
                player.pause()
            }
            player.getCurrentFilePath() == audioFile.filePath && player.getCurrentState() == PlaybackState.PAUSED -> {
                // 현재 일시정지된 파일 재개
                player.resume()
            }
            else -> {
                // 새로운 파일 재생
                player.play(audioFile.filePath)
            }
        }
    }
    
    private fun loadRecentRecordings() {
        lifecycleScope.launch {
            try {
                val recorder = audioRecorder ?: return@launch
                val recentFiles = recorder.getAllRecordingFiles().take(3) // 최근 3개만 표시
                
                val audioFileInfos = recentFiles.map { file ->
                    AudioFileInfo(
                        filePath = file.absolutePath,
                        fileName = file.name,
                        duration = 0L, // 실제 구현에서는 파일에서 읽어와야 함
                        fileSize = file.length(),
                        timestamp = file.lastModified()
                    )
                }
                
                // UI 업데이트
                if (audioFileInfos.isNotEmpty()) {
                    binding.cardSnoringRecordings.visibility = View.VISIBLE
                    binding.textRecordingCount.text = "${audioFileInfos.size}개"
                    snoringRecordingAdapter.submitList(audioFileInfos)
                } else {
                    binding.cardSnoringRecordings.visibility = View.GONE
                }
                
            } catch (e: Exception) {
                showSnackbar("녹음 목록 로드 실패: ${e.message}")
            }
        }
    }
    
    private fun updateStorageUsage() {
        lifecycleScope.launch {
            try {
                val recorder = audioRecorder ?: return@launch
                val totalUsage = recorder.getTotalStorageUsage()
                val maxStorage = 100 * 1024 * 1024L // 100MB
                
                val usageMB = String.format("%.1f", totalUsage / (1024.0 * 1024.0))
                val maxMB = String.format("%.0f", maxStorage / (1024.0 * 1024.0))
                
                binding.textStorageUsage.text = "${usageMB}MB / ${maxMB}MB"
                
                // 사용량이 80% 이상이면 경고 색상
                if (totalUsage > maxStorage * 0.8) {
                    binding.textStorageUsage.setTextColor(
                        android.graphics.Color.parseColor("#F44336") // 빨간색
                    )
                } else {
                    binding.textStorageUsage.setTextColor(
                        androidx.core.content.ContextCompat.getColor(
                            requireContext(), 
                            com.semo.alarm.R.color.md_theme_onSurface
                        )
                    )
                }
                
            } catch (e: Exception) {
                binding.textStorageUsage.text = "사용량 확인 실패"
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        stopElapsedTimeUpdates()
        audioPlayer?.release()
        audioRecorder?.release()
        audioPlayer = null
        audioRecorder = null
        _binding = null
    }
}