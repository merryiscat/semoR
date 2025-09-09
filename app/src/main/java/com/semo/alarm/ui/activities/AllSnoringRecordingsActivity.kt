package com.semo.alarm.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.semo.alarm.databinding.ActivityAllSnoringRecordingsBinding
import com.semo.alarm.ui.adapters.SnoringRecordingAdapter
import com.semo.alarm.utils.AudioFileInfo
import com.semo.alarm.utils.PlaybackState
import com.semo.alarm.utils.SnoringAudioPlayer
import com.semo.alarm.utils.SnoringAudioRecorder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 모든 코골이 녹음 파일을 표시하는 Activity
 * 
 * 🎵 전체 녹음 목록, 재생/일시정지, 삭제 기능 제공
 */
@AndroidEntryPoint
class AllSnoringRecordingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAllSnoringRecordingsBinding
    
    // 🎙️ 코골이 녹음 관련 컴포넌트
    private lateinit var snoringRecordingAdapter: SnoringRecordingAdapter
    private var audioPlayer: SnoringAudioPlayer? = null
    private var audioRecorder: SnoringAudioRecorder? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllSnoringRecordingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupSnoringRecording()
        loadAllRecordings()
    }
    
    private fun setupUI() {
        // 액션바 설정
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "코골이 녹음 기록"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        
        // 툴바 뒤로가기 버튼
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        
        // 새로고침 버튼
        binding.buttonRefresh.setOnClickListener {
            loadAllRecordings()
        }
        
        // 저장소 정리 버튼
        binding.buttonClearStorage.setOnClickListener {
            clearOldRecordings()
        }
    }
    
    private fun setupSnoringRecording() {
        // 오디오 플레이어 초기화
        audioPlayer = SnoringAudioPlayer(
            context = this,
            onPlaybackStateChanged = { state ->
                snoringRecordingAdapter.updatePlaybackState(audioPlayer?.getCurrentFilePath(), state)
            },
            onError = { errorMessage ->
                showSnackbar("재생 오류: $errorMessage")
            }
        )
        
        // 오디오 레코더 초기화 
        audioRecorder = SnoringAudioRecorder(
            context = this,
            onRecordingSaved = { _, _ -> 
                updateStorageUsage()
                loadAllRecordings()
            },
            onError = { errorMessage ->
                showSnackbar("오류: $errorMessage")
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
        
        binding.recyclerViewAllRecordings.apply {
            adapter = snoringRecordingAdapter
            layoutManager = LinearLayoutManager(this@AllSnoringRecordingsActivity)
        }
        
        // 초기 저장소 사용량 로드
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
    
    private fun loadAllRecordings() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.recyclerViewAllRecordings.visibility = android.view.View.GONE
                binding.textEmptyState.visibility = android.view.View.GONE
                
                val recorder = audioRecorder ?: return@launch
                val allFiles = recorder.getAllRecordingFiles() // 모든 녹음 파일
                
                val audioFileInfos = allFiles.map { file ->
                    AudioFileInfo(
                        filePath = file.absolutePath,
                        fileName = file.name,
                        duration = 0L, // TODO: 실제 파일 duration 읽기
                        fileSize = file.length(),
                        timestamp = file.lastModified()
                    )
                }.sortedByDescending { it.timestamp } // 최신순 정렬
                
                // UI 업데이트
                binding.progressBar.visibility = android.view.View.GONE
                
                if (audioFileInfos.isNotEmpty()) {
                    binding.recyclerViewAllRecordings.visibility = android.view.View.VISIBLE
                    binding.textEmptyState.visibility = android.view.View.GONE
                    binding.textRecordingCount.text = "총 ${audioFileInfos.size}개의 녹음"
                    snoringRecordingAdapter.submitList(audioFileInfos)
                } else {
                    binding.recyclerViewAllRecordings.visibility = android.view.View.GONE
                    binding.textEmptyState.visibility = android.view.View.VISIBLE
                    binding.textRecordingCount.text = "녹음된 파일이 없습니다"
                }
                
            } catch (e: Exception) {
                binding.progressBar.visibility = android.view.View.GONE
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
                
                binding.textStorageUsage.text = "${usageMB}MB / ${maxMB}MB 사용 중"
                
                // 사용량이 80% 이상이면 경고 색상
                if (totalUsage > maxStorage * 0.8) {
                    binding.textStorageUsage.setTextColor(
                        android.graphics.Color.parseColor("#F44336") // 빨간색
                    )
                } else {
                    binding.textStorageUsage.setTextColor(
                        androidx.core.content.ContextCompat.getColor(
                            this@AllSnoringRecordingsActivity,
                            com.semo.alarm.R.color.md_theme_onSurfaceVariant
                        )
                    )
                }
                
            } catch (e: Exception) {
                binding.textStorageUsage.text = "사용량 확인 실패"
            }
        }
    }
    
    private fun clearOldRecordings() {
        lifecycleScope.launch {
            try {
                val recorder = audioRecorder ?: return@launch
                val allFiles = recorder.getAllRecordingFiles()
                
                // 7일 이상 된 파일들 삭제
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                var deletedCount = 0
                
                allFiles.forEach { file ->
                    if (file.lastModified() < sevenDaysAgo) {
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }
                
                if (deletedCount > 0) {
                    showSnackbar("${deletedCount}개의 오래된 녹음을 삭제했습니다")
                    loadAllRecordings()
                    updateStorageUsage()
                } else {
                    showSnackbar("삭제할 오래된 녹음이 없습니다")
                }
                
            } catch (e: Exception) {
                showSnackbar("정리 실패: ${e.message}")
            }
        }
    }
    
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        audioPlayer?.release()
        audioRecorder?.release()
        audioPlayer = null
        audioRecorder = null
    }
    
    companion object {
        /**
         * Activity 시작을 위한 Intent 생성
         */
        fun createIntent(context: android.content.Context): Intent {
            return Intent(context, AllSnoringRecordingsActivity::class.java)
        }
    }
}