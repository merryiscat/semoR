package com.semo.alarm.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * 코골이 감지를 위한 유틸리티 클래스
 * MediaRecorder를 사용하여 주변 소음을 측정하고 코골이 패턴을 감지
 */
class SnoringDetector(
    private val context: Context,
    private val onSnoringDetected: (decibelLevel: Double, duration: Long) -> Unit,
    private val onError: (String) -> Unit
) {
    
    companion object {
        private const val TAG = "SnoringDetector"
        
        // 코골이 감지 임계값 (dB)
        private const val SNORING_THRESHOLD_DB = 45.0
        
        // 최소 지속 시간 (밀리초)
        private const val MIN_SNORING_DURATION = 2000L
        
        // 녹음 간격 (밀리초)
        private const val RECORDING_INTERVAL = 10000L // 10초
        
        // 실제 녹음 시간 (밀리초)
        private const val RECORDING_DURATION = 3000L // 3초
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var isDetecting = false
    private var snoringStartTime: Long = 0
    private var lastSnoringTime: Long = 0
    
    private val handler = Handler(Looper.getMainLooper())
    private var detectionRunnable: Runnable? = null
    
    /**
     * 코골이 감지 시작
     */
    fun startDetection() {
        if (!hasAudioPermission()) {
            onError("오디오 녹음 권한이 필요합니다")
            return
        }
        
        if (isDetecting) {
            Log.w(TAG, "Already detecting snoring")
            return
        }
        
        isDetecting = true
        scheduleNextDetection()
        Log.d(TAG, "Snoring detection started")
    }
    
    /**
     * 코골이 감지 중지
     */
    fun stopDetection() {
        if (!isDetecting) {
            Log.w(TAG, "Not detecting snoring")
            return
        }
        
        isDetecting = false
        stopRecording()
        cancelScheduledDetection()
        Log.d(TAG, "Snoring detection stopped")
    }
    
    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun scheduleNextDetection() {
        if (!isDetecting) return
        
        detectionRunnable = Runnable {
            if (isDetecting) {
                performDetection()
            }
        }
        
        handler.postDelayed(detectionRunnable!!, RECORDING_INTERVAL)
    }
    
    private fun cancelScheduledDetection() {
        detectionRunnable?.let { handler.removeCallbacks(it) }
        detectionRunnable = null
    }
    
    private fun performDetection() {
        try {
            startRecording()
            
            // 녹음 지속 시간 후 분석
            handler.postDelayed({
                analyzeRecording()
                stopRecording()
                
                // 다음 감지 스케줄링
                if (isDetecting) {
                    scheduleNextDetection()
                }
            }, RECORDING_DURATION)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error performing detection", e)
            onError("코골이 감지 중 오류 발생: ${e.message}")
            stopRecording()
            
            // 오류 발생해도 다음 감지 계속
            if (isDetecting) {
                scheduleNextDetection()
            }
        }
    }
    
    private fun startRecording() {
        stopRecording() // 기존 녹음 정리
        
        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile("/dev/null") // 실제 파일로 저장하지 않음
                
                prepare()
                start()
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Error starting recording", e)
            throw e
        }
    }
    
    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping recording", e)
        } finally {
            mediaRecorder = null
        }
    }
    
    private fun analyzeRecording() {
        try {
            val amplitude = mediaRecorder?.maxAmplitude ?: 0
            if (amplitude == 0) return
            
            // 진폭을 dB로 변환
            val decibelLevel = 20 * log10(amplitude.toDouble() / 32767.0) + 90
            
            Log.d(TAG, "Detected sound level: ${decibelLevel.toInt()} dB")
            
            val currentTime = System.currentTimeMillis()
            
            if (decibelLevel >= SNORING_THRESHOLD_DB) {
                // 코골이 가능성 있는 소음 감지
                if (snoringStartTime == 0L) {
                    // 새로운 코골이 시작
                    snoringStartTime = currentTime
                }
                lastSnoringTime = currentTime
                
            } else {
                // 조용한 상태
                if (snoringStartTime > 0L) {
                    // 코골이가 끝났는지 확인
                    val snoringDuration = lastSnoringTime - snoringStartTime
                    
                    if (snoringDuration >= MIN_SNORING_DURATION) {
                        // 유효한 코골이로 판정
                        Log.d(TAG, "Snoring detected: duration=${snoringDuration}ms, level=${decibelLevel.toInt()}dB")
                        onSnoringDetected(decibelLevel, snoringDuration)
                    }
                    
                    snoringStartTime = 0L
                    lastSnoringTime = 0L
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing recording", e)
        }
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        stopDetection()
    }
}

/**
 * 코골이 감지 결과를 나타내는 데이터 클래스
 */
data class SnoringEvent(
    val timestamp: Long,
    val decibelLevel: Double,
    val duration: Long
) {
    fun toJson(): String {
        return """{"timestamp":$timestamp,"decibelLevel":$decibelLevel,"duration":$duration}"""
    }
}