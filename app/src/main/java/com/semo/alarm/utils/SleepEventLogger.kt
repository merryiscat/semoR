package com.semo.alarm.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 수면 이벤트 로깅 시스템
 * 코골이 감지, 수면 상태 변화 등을 상세히 기록하여 디버깅 지원
 */
class SleepEventLogger private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SleepEventLogger"
        private const val LOG_DIR_NAME = "SemoAlarm_SleepLogs"
        private const val MAX_LOG_FILES = 7 // 최대 7일치 로그 보관
        
        @Volatile
        private var INSTANCE: SleepEventLogger? = null
        
        fun getInstance(context: Context): SleepEventLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SleepEventLogger(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    
    private var currentLogFile: File? = null
    private var currentSession: String? = null
    private val logBuffer = StringBuilder()
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    
    private val logDir: File by lazy {
        val externalDir = context.getExternalFilesDir(null)
        val logDir = File(externalDir, LOG_DIR_NAME)
        if (!logDir.exists()) {
            logDir.mkdirs()
            Log.d(TAG, "Created log directory: ${logDir.absolutePath}")
        }
        logDir
    }
    
    /**
     * 수면 세션 시작
     */
    fun startSleepSession() {
        val sessionId = "sleep_${fileTimestampFormat.format(Date())}"
        currentSession = sessionId
        
        val today = dateFormat.format(Date())
        currentLogFile = File(logDir, "${today}_${sessionId}.log")
        
        logEvent(SleepEventType.SESSION_START, "수면 세션 시작", mapOf(
            "sessionId" to sessionId,
            "timestamp" to System.currentTimeMillis(),
            "device" to "${android.os.Build.MODEL} (${android.os.Build.VERSION.RELEASE})"
        ))
        
        // 주기적으로 버퍼를 플러시 (30초마다)
        executor.scheduleWithFixedDelay({
            flushLogs()
        }, 30, 30, TimeUnit.SECONDS)
        
        Log.i(TAG, "Sleep session started: $sessionId")
    }
    
    /**
     * 수면 세션 종료
     */
    fun endSleepSession() {
        if (currentSession == null) return
        
        logEvent(SleepEventType.SESSION_END, "수면 세션 종료", mapOf(
            "sessionId" to currentSession!!,
            "timestamp" to System.currentTimeMillis(),
            "duration" to (System.currentTimeMillis() - getSessionStartTime())
        ))
        
        flushLogs()
        currentSession = null
        currentLogFile = null
        
        // 오래된 로그 파일 정리
        cleanupOldLogs()
        
        Log.i(TAG, "Sleep session ended")
    }
    
    /**
     * 코골이 감지 이벤트 로그
     */
    fun logSnoringEvent(decibelLevel: Double, duration: Long, audioFilePath: String?, additionalInfo: Map<String, Any> = emptyMap()) {
        val eventData = mutableMapOf<String, Any>(
            "decibelLevel" to decibelLevel,
            "duration" to duration,
            "intensity" to getIntensityLevel(decibelLevel),
            "hasAudio" to (audioFilePath != null)
        )
        
        audioFilePath?.let { eventData["audioFilePath"] = it }
        eventData.putAll(additionalInfo)
        
        logEvent(SleepEventType.SNORING_DETECTED, 
            "코골이 감지: ${decibelLevel.toInt()}dB, ${duration}ms", 
            eventData)
    }
    
    /**
     * 감지 과정 상세 로그
     */
    fun logDetectionProcess(
        analysisType: String, 
        audioLevel: Double, 
        threshold: Double, 
        isAboveThreshold: Boolean,
        additionalInfo: Map<String, Any> = emptyMap()
    ) {
        val eventData = mutableMapOf<String, Any>(
            "analysisType" to analysisType,
            "audioLevel" to audioLevel,
            "threshold" to threshold,
            "isAboveThreshold" to isAboveThreshold,
            "difference" to (audioLevel - threshold)
        )
        eventData.putAll(additionalInfo)
        
        val status = if (isAboveThreshold) "🚨 임계값 초과" else "🔇 조용함"
        logEvent(SleepEventType.DETECTION_ANALYSIS, 
            "감지 분석 [$analysisType]: ${audioLevel.toInt()}dB vs ${threshold.toInt()}dB - $status", 
            eventData)
    }
    
    /**
     * 오디오 녹음 이벤트 로그
     */
    fun logAudioRecording(action: String, filePath: String?, duration: Long? = null, error: String? = null) {
        val eventData = mutableMapOf<String, Any>(
            "action" to action
        )
        
        filePath?.let { eventData["filePath"] = it }
        duration?.let { eventData["duration"] = it }
        error?.let { eventData["error"] = it }
        
        val message = when (action) {
            "start" -> "🎙️ 오디오 녹음 시작: $filePath"
            "stop" -> "⏹️ 오디오 녹음 중지: $filePath (${duration}ms)"
            "error" -> "❌ 오디오 녹음 오류: $error"
            else -> "🎙️ 오디오 녹음: $action"
        }
        
        logEvent(SleepEventType.AUDIO_RECORDING, message, eventData)
    }
    
    /**
     * 감지 상태 변화 로그
     */
    fun logDetectionState(state: String, details: Map<String, Any> = emptyMap()) {
        val eventData = mutableMapOf<String, Any>(
            "state" to state
        )
        eventData.putAll(details)
        
        logEvent(SleepEventType.DETECTION_STATE, "감지 상태: $state", eventData)
    }
    
    /**
     * 오류 이벤트 로그
     */
    fun logError(errorType: String, message: String, exception: Exception? = null) {
        val eventData = mutableMapOf<String, Any>(
            "errorType" to errorType,
            "message" to message
        )
        
        exception?.let { 
            eventData["exception"] = it.javaClass.simpleName
            eventData["stackTrace"] = it.stackTraceToString().take(1000) // 최대 1000자
        }
        
        logEvent(SleepEventType.ERROR, "❌ 오류 [$errorType]: $message", eventData)
    }
    
    /**
     * 일반 정보 로그
     */
    fun logInfo(message: String, data: Map<String, Any> = emptyMap()) {
        logEvent(SleepEventType.INFO, message, data)
    }
    
    /**
     * 시스템 상태 로그 (메모리, 배터리 등)
     */
    fun logSystemStatus() {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        val eventData = mapOf(
            "memoryUsed" to usedMemory,
            "memoryFree" to freeMemory,
            "memoryTotal" to totalMemory,
            "memoryMax" to maxMemory,
            "memoryUsagePercent" to ((usedMemory.toDouble() / maxMemory) * 100).toInt()
        )
        
        logEvent(SleepEventType.SYSTEM_STATUS, 
            "시스템 상태: 메모리 사용률 ${eventData["memoryUsagePercent"]}%", 
            eventData)
    }
    
    private fun logEvent(type: SleepEventType, message: String, data: Map<String, Any>) {
        if (currentSession == null) return
        
        val timestamp = System.currentTimeMillis()
        val timeStr = timeFormat.format(Date(timestamp))
        
        val logEntry = StringBuilder()
        logEntry.append("[$timeStr] [${type.name}] $message")
        
        if (data.isNotEmpty()) {
            logEntry.append("\n  Data: {")
            data.forEach { (key, value) -> 
                logEntry.append("\"$key\": \"$value\", ")
            }
            logEntry.setLength(logEntry.length - 2) // 마지막 ", " 제거
            logEntry.append("}")
        }
        logEntry.append("\n")
        
        synchronized(logBuffer) {
            logBuffer.append(logEntry)
        }
        
        // 중요한 이벤트는 즉시 플러시
        if (type in listOf(SleepEventType.SNORING_DETECTED, SleepEventType.ERROR, SleepEventType.SESSION_START, SleepEventType.SESSION_END)) {
            flushLogs()
        }
    }
    
    private fun flushLogs() {
        synchronized(logBuffer) {
            if (logBuffer.isEmpty() || currentLogFile == null) return
            
            try {
                FileWriter(currentLogFile!!, true).use { writer ->
                    writer.write(logBuffer.toString())
                    writer.flush()
                }
                logBuffer.clear()
                Log.d(TAG, "Logs flushed to: ${currentLogFile?.name}")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to flush logs", e)
            }
        }
    }
    
    private fun cleanupOldLogs() {
        try {
            val logFiles = logDir.listFiles { file -> file.name.endsWith(".log") }
                ?.sortedByDescending { it.lastModified() }
            
            if (logFiles != null && logFiles.size > MAX_LOG_FILES) {
                logFiles.drop(MAX_LOG_FILES).forEach { file ->
                    if (file.delete()) {
                        Log.d(TAG, "Deleted old log file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old logs", e)
        }
    }
    
    private fun getSessionStartTime(): Long {
        return currentSession?.let { sessionId ->
            val timestampPart = sessionId.substringAfter("sleep_")
            try {
                fileTimestampFormat.parse(timestampPart)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } ?: System.currentTimeMillis()
    }
    
    private fun getIntensityLevel(decibelLevel: Double): String {
        return when {
            decibelLevel >= 65.0 -> "매우 심함"
            decibelLevel >= 55.0 -> "심함" 
            decibelLevel >= 50.0 -> "보통"
            decibelLevel >= 45.0 -> "경미"
            else -> "미약"
        }
    }
    
    /**
     * 현재 로그 파일 경로 반환
     */
    fun getCurrentLogFile(): File? = currentLogFile
    
    /**
     * 로그 디렉토리 반환
     */
    fun getLogDirectory(): File = logDir
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        endSleepSession()
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}

/**
 * 수면 이벤트 타입
 */
enum class SleepEventType {
    SESSION_START,      // 수면 세션 시작
    SESSION_END,        // 수면 세션 종료
    SNORING_DETECTED,   // 코골이 감지
    DETECTION_ANALYSIS, // 감지 분석 과정
    AUDIO_RECORDING,    // 오디오 녹음 관련
    DETECTION_STATE,    // 감지 상태 변화
    SYSTEM_STATUS,      // 시스템 상태
    ERROR,              // 오류
    INFO                // 일반 정보
}