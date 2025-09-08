package com.semo.alarm.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.AudioRecord
import android.media.AudioFormat
import android.media.MediaRecorder.AudioSource
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
 * 📈 v2.0: 실제 오디오 녹음 기능 통합
 */
class SnoringDetector(
    private val context: Context,
    private val onSnoringDetected: (decibelLevel: Double, duration: Long, audioFilePath: String?) -> Unit,
    private val onError: (String) -> Unit
) {
    
    companion object {
        private const val TAG = "SnoringDetector"
        
        // 코골이 감지 임계값 (dB)
        private const val SNORING_THRESHOLD_DB = 30.0  // 테스트용으로 낮춤 (원래: 45.0)
        
        // 최소 지속 시간 (밀리초)
        private const val MIN_SNORING_DURATION = 2000L
        
        // 녹음 간격 (밀리초)
        private const val RECORDING_INTERVAL = 10000L // 10초
        
        // 실제 녹음 시간 (밀리초)
        private const val RECORDING_DURATION = 3000L // 3초
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var audioRecord: AudioRecord? = null
    private var isDetecting = false
    private var snoringStartTime: Long = 0
    private var lastSnoringTime: Long = 0
    
    private val handler = Handler(Looper.getMainLooper())
    private var detectionRunnable: Runnable? = null
    
    // 🎙️ 실제 오디오 녹음 기능 추가
    private var audioRecorder: SnoringAudioRecorder? = null
    private var currentRecordingPath: String? = null
    private var isRecordingAudio = false
    private var currentTempFile: java.io.File? = null
    
    /**
     * 코골이 감지 시작
     * @param enableRecording 실제 오디오 녹음 여부 (기본값: true)
     */
    fun startDetection(enableRecording: Boolean = true) {
        if (!hasAudioPermission()) {
            onError("오디오 녹음 권한이 필요합니다")
            return
        }
        
        if (isDetecting) {
            Log.w(TAG, "Already detecting snoring")
            return
        }
        
        // 오디오 레코더 초기화 (녹음 활성화 시)
        if (enableRecording) {
            initializeAudioRecorder()
        }
        
        isDetecting = true
        scheduleNextDetection()
        Log.d(TAG, "Snoring detection started (recording: $enableRecording)")
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
        stopAudioRecording()
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
            
            // 녹음 시작 후 약간의 시간을 두고 분석 (마이크 초기화 대기)
            handler.postDelayed({
                // 중간에 한 번 더 체크
                handler.postDelayed({
                    analyzeRecording()
                }, 500) // 0.5초 후 분석
                
                // 녹음 종료
                handler.postDelayed({
                    stopRecording()
                    
                    // 다음 감지 스케줄링
                    if (isDetecting) {
                        scheduleNextDetection()
                    }
                }, RECORDING_DURATION)
            }, 100) // 녹음 시작 후 0.1초 대기
            
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
        
        // 권한 확인
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                Log.e(TAG, "❌ RECORD_AUDIO permission not granted!")
                onError("오디오 녹음 권한이 필요합니다")
                return
            } else {
                Log.d(TAG, "✅ RECORD_AUDIO permission confirmed")
            }
        }
        
        try {
            // 임시 파일 생성 (SnoringAudioRecorder에서 실제 녹음 관리)
            val tempFile = java.io.File(context.cacheDir, "temp_detection_${System.currentTimeMillis()}.3gp")
            
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setAudioSamplingRate(8000)  // 샘플링 레이트 명시
                setAudioEncodingBitRate(12200)  // 비트레이트 명시
                setOutputFile(tempFile.absolutePath)
                
                prepare()
                start()
                
                Log.d(TAG, "📱 MediaRecorder started with file: ${tempFile.absolutePath}")
            }
            
            // 감지 완료 후 임시 파일 삭제를 위해 저장
            currentTempFile = tempFile
            
        } catch (e: IOException) {
            Log.e(TAG, "Error starting recording", e)
            throw e
        } catch (e: RuntimeException) {
            Log.e(TAG, "MediaRecorder runtime error", e)
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
            
            // 임시 파일 삭제
            currentTempFile?.let { tempFile ->
                try {
                    if (tempFile.exists()) {
                        val deleted = tempFile.delete()
                        Log.d(TAG, "Temporary detection file deleted: ${tempFile.name}, success: $deleted")
                    } else {
                        Log.d(TAG, "Temporary file does not exist: ${tempFile.name}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete temp file", e)
                }
            }
            currentTempFile = null
        }
    }
    
    private fun analyzeRecording() {
        try {
            // AudioRecord 방법 시도
            val audioLevel = analyzeAudioWithAudioRecord()
            
            if (audioLevel > 0) {
                Log.d(TAG, "🎙️ AudioRecord level: $audioLevel, threshold: ${SNORING_THRESHOLD_DB.toInt()} dB")
                
                val currentTime = System.currentTimeMillis()
                
                if (audioLevel >= SNORING_THRESHOLD_DB) {
                    // 코골이 가능성 있는 소음 감지
                    Log.i(TAG, "🚨 SNORING DETECTED! dB: ${audioLevel.toInt()} (threshold: ${SNORING_THRESHOLD_DB.toInt()})")
                    
                    if (snoringStartTime == 0L) {
                        // 새로운 코골이 시작 - 오디오 녹음 시작
                        Log.i(TAG, "🎙️ Starting audio recording for snoring event")
                        snoringStartTime = currentTime
                        startAudioRecording()
                    }
                    lastSnoringTime = currentTime
                    
                } else {
                    // 조용한 상태
                    if (snoringStartTime > 0L) {
                        // 코골이가 끝났는지 확인
                        val snoringDuration = lastSnoringTime - snoringStartTime
                        
                        if (snoringDuration >= MIN_SNORING_DURATION) {
                            // 유효한 코골이로 판정 - 오디오 녹음 중지
                            val audioFilePath = stopAudioRecording()
                            Log.d(TAG, "Snoring detected: duration=${snoringDuration}ms, level=${audioLevel.toInt()}dB, audio: $audioFilePath")
                            onSnoringDetected(audioLevel, snoringDuration, audioFilePath)
                        } else {
                            // 너무 짧은 소음 - 녹음 취소
                            stopAudioRecording()
                        }
                        
                        snoringStartTime = 0L
                        lastSnoringTime = 0L
                    }
                }
            } else {
                Log.d(TAG, "🔇 No sound detected (AudioRecord level: 0)")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing recording", e)
        }
    }
    
    /**
     * AudioRecord를 사용한 직접 오디오 레벨 분석
     * MediaRecorder.getMaxAmplitude()보다 더 안정적
     */
    private fun analyzeAudioWithAudioRecord(): Double {
        var audioRecord: AudioRecord? = null
        try {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "❌ Invalid buffer size for AudioRecord")
                return 0.0
            }
            
            audioRecord = AudioRecord(
                AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "❌ AudioRecord not initialized")
                return 0.0
            }
            
            audioRecord.startRecording()
            
            val buffer = ShortArray(bufferSize)
            val readSize = audioRecord.read(buffer, 0, bufferSize)
            
            if (readSize > 0) {
                // RMS (Root Mean Square) 계산
                var sum = 0.0
                for (i in 0 until readSize) {
                    sum += (buffer[i] * buffer[i]).toDouble()
                }
                val rms = sqrt(sum / readSize)
                
                // dB로 변환
                val db = if (rms > 0) {
                    20 * log10(rms / 32767.0) + 90
                } else {
                    0.0
                }
                
                Log.d(TAG, "📊 AudioRecord: readSize=$readSize, RMS=$rms, dB=$db")
                return db
                
            } else {
                Log.w(TAG, "⚠️ AudioRecord read failed: $readSize")
                return 0.0
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ AudioRecord error", e)
            return 0.0
        } finally {
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing AudioRecord", e)
            }
        }
    }
    
    // ═══════════════════════════════════════════════════
    // 🎙️ 실제 오디오 녹음 기능
    // ═══════════════════════════════════════════════════
    
    /**
     * 오디오 레코더 초기화
     */
    private fun initializeAudioRecorder() {
        audioRecorder = SnoringAudioRecorder(
            context = context,
            onRecordingSaved = { filePath, duration ->
                Log.d(TAG, "Audio recording saved: $filePath (${duration}ms)")
            },
            onError = { errorMessage ->
                Log.e(TAG, "Audio recording error: $errorMessage")
                onError("오디오 녹음 오류: $errorMessage")
            }
        )
    }
    
    /**
     * 오디오 녹음 시작
     */
    private fun startAudioRecording() {
        if (audioRecorder == null || isRecordingAudio) return
        
        currentRecordingPath = audioRecorder?.startRecording()
        isRecordingAudio = currentRecordingPath != null
        
        if (isRecordingAudio) {
            Log.d(TAG, "Started audio recording: $currentRecordingPath")
        }
    }
    
    /**
     * 오디오 녹음 중지
     * @return 녹음된 파일 경로 (null이면 녹음 실패 또는 미녹음)
     */
    private fun stopAudioRecording(): String? {
        if (!isRecordingAudio || audioRecorder == null) {
            return null
        }
        
        val recordingResult = audioRecorder?.stopRecording()
        val filePath = recordingResult?.filePath
        
        isRecordingAudio = false
        currentRecordingPath = null
        
        if (filePath != null) {
            Log.d(TAG, "Stopped audio recording: $filePath (${recordingResult.getFormattedDuration()})")
        }
        
        return filePath
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        stopDetection()
        audioRecorder?.release()
        audioRecorder = null
    }
}

/**
 * 코골이 감지 결과를 나타내는 데이터 클래스
 * 📈 v2.0: 오디오 파일 경로 추가
 */
data class SnoringEvent(
    val timestamp: Long,
    val decibelLevel: Double,
    val duration: Long,
    val audioFilePath: String? = null  // 🎙️ 녹음된 오디오 파일 경로
) {
    fun toJson(): String {
        val audioPath = audioFilePath?.let { "\"$it\"" } ?: "null"
        return """{"timestamp":$timestamp,"decibelLevel":$decibelLevel,"duration":$duration,"audioFilePath":$audioPath}"""
    }
    
    /**
     * 지속시간을 사용자 친화적 형식으로 포맷
     */
    fun getFormattedDuration(): String {
        val seconds = (duration / 1000).toInt()
        return "${seconds}초"
    }
    
    /**
     * 오디오 파일이 있는지 확인
     */
    fun hasAudioFile(): Boolean = !audioFilePath.isNullOrEmpty()
    
    /**
     * 코골이 강도 레벨 반환
     */
    fun getIntensityLevel(): String {
        return when {
            decibelLevel >= 65.0 -> "매우 심함"
            decibelLevel >= 55.0 -> "심함"
            decibelLevel >= 50.0 -> "보통"
            decibelLevel >= 45.0 -> "경미"
            else -> "미약"
        }
    }
}