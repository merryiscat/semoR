package com.semo.alarm.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.semo.alarm.R
import com.semo.alarm.databinding.ItemSnoringRecordingBinding
import com.semo.alarm.utils.AudioFileInfo
import com.semo.alarm.utils.PlaybackState
import com.semo.alarm.utils.SnoringEvent
import java.text.SimpleDateFormat
import java.util.*

/**
 * 코골이 녹음 파일 목록을 표시하는 RecyclerView 어댑터
 * 
 * 🎵 각 녹음 파일의 재생/일시정지 기능과 상태 관리 제공
 */
class SnoringRecordingAdapter(
    private val onPlayPauseClick: (AudioFileInfo) -> Unit,
    private val onItemClick: (AudioFileInfo) -> Unit = {}
) : ListAdapter<AudioFileInfo, SnoringRecordingAdapter.SnoringRecordingViewHolder>(DiffCallback()) {
    
    // 현재 재생 중인 파일과 상태 추적
    private var currentPlayingFile: String? = null
    private var currentPlaybackState: PlaybackState = PlaybackState.IDLE
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnoringRecordingViewHolder {
        val binding = ItemSnoringRecordingBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return SnoringRecordingViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: SnoringRecordingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    /**
     * 재생 상태 업데이트
     * @param filePath 재생 중인 파일 경로
     * @param state 현재 재생 상태
     */
    fun updatePlaybackState(filePath: String?, state: PlaybackState) {
        val oldPlayingFile = currentPlayingFile
        currentPlayingFile = if (state == PlaybackState.PLAYING || state == PlaybackState.PAUSED) filePath else null
        currentPlaybackState = state
        
        // 이전에 재생 중이던 아이템과 현재 재생 중인 아이템 업데이트
        currentList.forEachIndexed { index, audioFile ->
            if (audioFile.filePath == oldPlayingFile || audioFile.filePath == currentPlayingFile) {
                notifyItemChanged(index)
            }
        }
    }
    
    inner class SnoringRecordingViewHolder(
        private val binding: ItemSnoringRecordingBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(audioFile: AudioFileInfo) {
            // 녹음 시간 표시 (HH:mm 형식)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.textRecordingTime.text = timeFormat.format(Date(audioFile.timestamp))
            
            // 지속시간 표시
            binding.textDuration.text = audioFile.getFormattedDuration()
            
            // 파일 크기 표시
            binding.textFileSize.text = audioFile.getFormattedFileSize()
            
            // 강도 레벨 표시 (임시로 파일 크기 기반으로 계산)
            val intensityLevel = calculateIntensityLevel(audioFile.fileSize)
            binding.textIntensityLevel.text = intensityLevel.text
            binding.textIntensityLevel.backgroundTintList = 
                android.content.res.ColorStateList.valueOf(intensityLevel.color)
            
            // 재생 버튼 상태 설정
            val isCurrentlyPlaying = currentPlayingFile == audioFile.filePath
            updatePlayButtonState(isCurrentlyPlaying)
            
            // 클릭 리스너 설정
            binding.buttonPlayPause.setOnClickListener {
                onPlayPauseClick(audioFile)
            }
            
            binding.root.setOnClickListener {
                onItemClick(audioFile)
            }
        }
        
        private fun updatePlayButtonState(isCurrentlyPlaying: Boolean) {
            when {
                isCurrentlyPlaying && currentPlaybackState == PlaybackState.PLAYING -> {
                    // 재생 중 - 일시정지 아이콘
                    binding.buttonPlayPause.icon = ContextCompat.getDrawable(
                        binding.root.context, 
                        R.drawable.ic_stop  // pause 아이콘이 없다면 stop 사용
                    )
                    binding.buttonPlayPause.backgroundTintList = 
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#FF6B6B")  // 빨간색
                        )
                }
                isCurrentlyPlaying && currentPlaybackState == PlaybackState.PAUSED -> {
                    // 일시정지됨 - 재생 아이콘 (강조)
                    binding.buttonPlayPause.icon = ContextCompat.getDrawable(
                        binding.root.context, 
                        R.drawable.ic_play
                    )
                    binding.buttonPlayPause.backgroundTintList = 
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#00D4FF")  // 네온 블루
                        )
                }
                else -> {
                    // 기본 상태 - 재생 아이콘
                    binding.buttonPlayPause.icon = ContextCompat.getDrawable(
                        binding.root.context, 
                        R.drawable.ic_play
                    )
                    binding.buttonPlayPause.backgroundTintList = 
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#00D4FF")  // 네온 블루
                        )
                }
            }
        }
        
        /**
         * 파일 크기 기반 강도 레벨 계산 (임시)
         * 실제로는 오디오 분석 결과를 사용해야 함
         */
        private fun calculateIntensityLevel(fileSize: Long): IntensityLevel {
            return when {
                fileSize > 500 * 1024 -> IntensityLevel.SEVERE      // 500KB 이상
                fileSize > 300 * 1024 -> IntensityLevel.HEAVY       // 300KB 이상
                fileSize > 150 * 1024 -> IntensityLevel.MODERATE    // 150KB 이상
                else -> IntensityLevel.LIGHT                        // 150KB 미만
            }
        }
    }
    
    /**
     * DiffUtil.Callback for efficient list updates
     */
    class DiffCallback : DiffUtil.ItemCallback<AudioFileInfo>() {
        override fun areItemsTheSame(oldItem: AudioFileInfo, newItem: AudioFileInfo): Boolean {
            return oldItem.filePath == newItem.filePath
        }
        
        override fun areContentsTheSame(oldItem: AudioFileInfo, newItem: AudioFileInfo): Boolean {
            return oldItem == newItem
        }
    }
    
    /**
     * 코골이 강도 레벨
     */
    private enum class IntensityLevel(val text: String, val color: Int) {
        LIGHT("경미", android.graphics.Color.parseColor("#4CAF50")),      // 초록
        MODERATE("보통", android.graphics.Color.parseColor("#FF9800")),   // 주황
        HEAVY("심함", android.graphics.Color.parseColor("#F44336")),      // 빨강
        SEVERE("매우심함", android.graphics.Color.parseColor("#9C27B0"))  // 보라
    }
}

/**
 * SnoringEvent를 AudioFileInfo로 변환하는 확장 함수
 */
fun SnoringEvent.toAudioFileInfo(): AudioFileInfo? {
    if (!hasAudioFile()) return null
    
    val file = java.io.File(audioFilePath!!)
    if (!file.exists()) return null
    
    return AudioFileInfo(
        filePath = audioFilePath,
        fileName = file.name,
        duration = duration,
        fileSize = file.length(),
        timestamp = timestamp
    )
}