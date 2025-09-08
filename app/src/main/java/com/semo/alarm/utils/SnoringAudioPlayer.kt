package com.semo.alarm.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

/**
 * 코골이 오디오 파일 재생을 담당하는 클래스
 * 
 * 🎵 사용자가 자신의 코골이 녹음을 들을 수 있도록 재생 기능 제공
 * - MediaPlayer 기반 고품질 재생
 * - 재생 상태 관리 및 콜백 제공
 * - 볼륨 및 재생 속도 조절 지원
 */
class SnoringAudioPlayer(
    private val context: Context,
    private val onPlaybackStateChanged: (PlaybackState) -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    
    companion object {
        private const val TAG = "SnoringAudioPlayer"
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var currentFilePath: String? = null
    private var currentState = PlaybackState.IDLE
    
    /**
     * 오디오 파일 재생 시작
     * @param filePath 재생할 오디오 파일 경로
     * @return 재생 시작 성공 여부
     */
    fun play(filePath: String): Boolean {
        if (currentState == PlaybackState.PLAYING && currentFilePath == filePath) {
            Log.w(TAG, "Already playing this file: $filePath")
            return true
        }
        
        // 기존 재생 중지
        stop()
        
        val file = File(filePath)
        if (!file.exists()) {
            val errorMsg = "오디오 파일을 찾을 수 없습니다: $filePath"
            Log.e(TAG, errorMsg)
            onError(errorMsg)
            return false
        }
        
        return try {
            mediaPlayer = MediaPlayer().apply {
                // 오디오 속성 설정
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                // 데이터 소스 설정
                setDataSource(context, Uri.fromFile(file))
                
                // 리스너 설정
                setOnPreparedListener { mediaPlayer ->
                    Log.d(TAG, "MediaPlayer prepared, starting playback")
                    mediaPlayer.start()
                    updateState(PlaybackState.PLAYING)
                }
                
                setOnCompletionListener {
                    Log.d(TAG, "Playback completed")
                    updateState(PlaybackState.COMPLETED)
                    cleanup()
                }
                
                setOnErrorListener { _, what, extra ->
                    val errorMsg = "재생 오류 발생: what=$what, extra=$extra"
                    Log.e(TAG, errorMsg)
                    onError(errorMsg)
                    updateState(PlaybackState.ERROR)
                    cleanup()
                    true // 에러 처리됨을 알림
                }
                
                // 비동기 준비 시작
                prepareAsync()
            }
            
            currentFilePath = filePath
            updateState(PlaybackState.PREPARING)
            
            Log.d(TAG, "Started preparing audio file: $filePath")
            true
            
        } catch (e: Exception) {
            val errorMsg = "오디오 재생 준비 실패: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onError(errorMsg)
            cleanup()
            false
        }
    }
    
    /**
     * 재생 일시정지
     */
    fun pause() {
        if (currentState != PlaybackState.PLAYING) {
            Log.w(TAG, "Cannot pause: not currently playing")
            return
        }
        
        try {
            mediaPlayer?.pause()
            updateState(PlaybackState.PAUSED)
            Log.d(TAG, "Playback paused")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause playback", e)
            onError("재생 일시정지 실패: ${e.message}")
        }
    }
    
    /**
     * 일시정지된 재생 재개
     */
    fun resume() {
        if (currentState != PlaybackState.PAUSED) {
            Log.w(TAG, "Cannot resume: not currently paused")
            return
        }
        
        try {
            mediaPlayer?.start()
            updateState(PlaybackState.PLAYING)
            Log.d(TAG, "Playback resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume playback", e)
            onError("재생 재개 실패: ${e.message}")
        }
    }
    
    /**
     * 재생 중지
     */
    fun stop() {
        if (currentState == PlaybackState.IDLE) {
            return
        }
        
        try {
            mediaPlayer?.stop()
            Log.d(TAG, "Playback stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback", e)
        } finally {
            updateState(PlaybackState.IDLE)
            cleanup()
        }
    }
    
    /**
     * 재생 위치 이동 (초 단위)
     * @param seconds 이동할 위치 (초)
     */
    fun seekTo(seconds: Int) {
        if (currentState == PlaybackState.IDLE || currentState == PlaybackState.PREPARING) {
            Log.w(TAG, "Cannot seek: player not ready")
            return
        }
        
        try {
            val milliseconds = seconds * 1000
            val duration = mediaPlayer?.duration ?: 0
            val seekPosition = milliseconds.coerceAtMost(duration)
            
            mediaPlayer?.seekTo(seekPosition)
            Log.d(TAG, "Seeked to position: ${seekPosition}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seek", e)
            onError("재생 위치 이동 실패: ${e.message}")
        }
    }
    
    /**
     * 현재 재생 위치 반환 (초 단위)
     */
    fun getCurrentPosition(): Int {
        return try {
            (mediaPlayer?.currentPosition ?: 0) / 1000
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current position", e)
            0
        }
    }
    
    /**
     * 전체 재생 시간 반환 (초 단위)
     */
    fun getDuration(): Int {
        return try {
            (mediaPlayer?.duration ?: 0) / 1000
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get duration", e)
            0
        }
    }
    
    /**
     * 볼륨 설정 (0.0 ~ 1.0)
     */
    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(clampedVolume, clampedVolume)
            Log.d(TAG, "Volume set to: $clampedVolume")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set volume", e)
        }
    }
    
    /**
     * 현재 재생 상태 반환
     */
    fun getCurrentState(): PlaybackState = currentState
    
    /**
     * 현재 재생 중인 파일 경로 반환
     */
    fun getCurrentFilePath(): String? = currentFilePath
    
    /**
     * 재생 중인지 확인
     */
    fun isPlaying(): Boolean = currentState == PlaybackState.PLAYING
    
    /**
     * 상태 업데이트
     */
    private fun updateState(newState: PlaybackState) {
        if (currentState != newState) {
            val oldState = currentState
            currentState = newState
            onPlaybackStateChanged(newState)
            Log.d(TAG, "State changed: $oldState -> $newState")
        }
    }
    
    /**
     * 리소스 정리
     */
    private fun cleanup() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        } finally {
            mediaPlayer = null
            currentFilePath = null
        }
    }
    
    /**
     * 전체 리소스 해제
     */
    fun release() {
        stop()
        Log.d(TAG, "SnoringAudioPlayer released")
    }
}

/**
 * 오디오 재생 상태
 */
enum class PlaybackState {
    /** 대기 상태 */
    IDLE,
    
    /** 재생 준비 중 */
    PREPARING,
    
    /** 재생 중 */
    PLAYING,
    
    /** 일시정지됨 */
    PAUSED,
    
    /** 재생 완료 */
    COMPLETED,
    
    /** 오류 발생 */
    ERROR
}

/**
 * 오디오 파일 정보를 나타내는 데이터 클래스
 */
data class AudioFileInfo(
    val filePath: String,
    val fileName: String,
    val duration: Long,
    val fileSize: Long,
    val timestamp: Long
) {
    fun getFormattedDuration(): String {
        val seconds = (duration / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        
        return if (minutes > 0) {
            "${minutes}분 ${remainingSeconds}초"
        } else {
            "${remainingSeconds}초"
        }
    }
    
    fun getFormattedFileSize(): String {
        return when {
            fileSize < 1024 -> "${fileSize}B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024}KB"
            else -> String.format("%.1fMB", fileSize / (1024.0 * 1024.0))
        }
    }
    
    fun getFormattedTimestamp(): String {
        return android.text.format.DateFormat.format("MM/dd HH:mm", timestamp).toString()
    }
}