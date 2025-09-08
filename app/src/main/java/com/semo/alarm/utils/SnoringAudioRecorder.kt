package com.semo.alarm.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 코골이 실제 오디오 녹음을 담당하는 클래스
 * 
 * 🎙️ 고품질 오디오 녹음 및 파일 관리 기능 제공
 * - AAC 포맷으로 고품질 녹음
 * - 자동 파일명 생성 및 저장소 관리
 * - 용량 제한 및 자동 정리 기능
 */
class SnoringAudioRecorder(
    private val context: Context,
    private val onRecordingSaved: (filePath: String, duration: Long) -> Unit,
    private val onError: (String) -> Unit
) {
    
    companion object {
        private const val TAG = "SnoringAudioRecorder"
        
        // 오디오 품질 설정
        private const val AUDIO_FORMAT = MediaRecorder.OutputFormat.MPEG_4
        private const val AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC
        private const val SAMPLE_RATE = 44100
        private const val BIT_RATE = 128000
        
        // 코골이 클립 설정
        private const val MAX_CLIP_DURATION = 30000L  // 30초
        private const val MIN_CLIP_DURATION = 2000L   // 2초
        
        // 저장소 설정
        private const val MAX_STORAGE_SIZE = 100 * 1024 * 1024L  // 100MB
        private const val AUTO_DELETE_DAYS = 7  // 7일 후 자동 삭제
        
        // 디렉토리 이름
        private const val SNORING_DIR = "snoring_records"
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime: Long = 0
    private var isRecording = false
    
    /**
     * 오디오 녹음 시작
     * @return 녹음 파일 경로 (null이면 실패)
     */
    fun startRecording(): String? {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return null
        }
        
        try {
            // 저장소 용량 확인 및 정리
            cleanupOldFiles()
            
            // 녹음 파일 생성
            val audioFile = createAudioFile()
            if (audioFile == null) {
                onError("오디오 파일 생성에 실패했습니다")
                return null
            }
            
            // MediaRecorder 설정
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(AUDIO_FORMAT)
                setAudioEncoder(AUDIO_ENCODER)
                setAudioSamplingRate(SAMPLE_RATE)
                setAudioEncodingBitRate(BIT_RATE)
                setOutputFile(audioFile.absolutePath)
                setMaxDuration(MAX_CLIP_DURATION.toInt())
                
                // 녹음 완료 리스너
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Log.d(TAG, "Max duration reached, stopping recording")
                        stopRecording()
                    }
                }
                
                prepare()
                start()
            }
            
            currentRecordingFile = audioFile
            recordingStartTime = System.currentTimeMillis()
            isRecording = true
            
            Log.d(TAG, "Started recording: ${audioFile.absolutePath}")
            return audioFile.absolutePath
            
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            onError("녹음 시작에 실패했습니다: ${e.message}")
            cleanup()
            return null
        }
    }
    
    /**
     * 오디오 녹음 중지
     * @return 녹음된 파일 정보 (null이면 실패)
     */
    fun stopRecording(): RecordingResult? {
        if (!isRecording) {
            Log.w(TAG, "Not currently recording")
            return null
        }
        
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            
            val recordingDuration = System.currentTimeMillis() - recordingStartTime
            val file = currentRecordingFile
            
            // 최소 지속시간 확인
            if (recordingDuration < MIN_CLIP_DURATION) {
                Log.d(TAG, "Recording too short (${recordingDuration}ms), deleting file")
                file?.delete()
                cleanup()
                return null
            }
            
            if (file != null && file.exists()) {
                Log.d(TAG, "Recording completed: ${file.absolutePath}, duration: ${recordingDuration}ms")
                onRecordingSaved(file.absolutePath, recordingDuration)
                
                RecordingResult(
                    filePath = file.absolutePath,
                    duration = recordingDuration,
                    fileSize = file.length()
                )
            } else {
                onError("녹음 파일이 생성되지 않았습니다")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            onError("녹음 중지에 실패했습니다: ${e.message}")
            currentRecordingFile?.delete()
            null
        } finally {
            cleanup()
        }
    }
    
    /**
     * 현재 녹음 중인지 확인
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * 녹음 파일 생성
     */
    private fun createAudioFile(): File? {
        try {
            // 코골이 녹음 디렉토리 생성
            val snoringDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), SNORING_DIR)
            if (!snoringDir.exists()) {
                if (!snoringDir.mkdirs()) {
                    Log.e(TAG, "Failed to create snoring directory")
                    return null
                }
            }
            
            // 파일명 생성: snoring_YYYYMMDD_HHmmss.m4a
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "snoring_${timestamp}.m4a"
            
            return File(snoringDir, fileName)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create audio file", e)
            return null
        }
    }
    
    /**
     * 저장소 정리 - 오래된 파일 및 용량 초과 파일 삭제
     */
    private fun cleanupOldFiles() {
        try {
            val snoringDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), SNORING_DIR)
            if (!snoringDir.exists()) return
            
            val files = snoringDir.listFiles() ?: return
            val currentTime = System.currentTimeMillis()
            val cutoffTime = currentTime - (AUTO_DELETE_DAYS * 24 * 60 * 60 * 1000L)
            
            var deletedFiles = 0
            var totalSize = 0L
            
            // 1. 오래된 파일 삭제
            files.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedFiles++
                        Log.d(TAG, "Deleted old file: ${file.name}")
                    }
                } else {
                    totalSize += file.length()
                }
            }
            
            // 2. 용량 초과 시 가장 오래된 파일부터 삭제
            if (totalSize > MAX_STORAGE_SIZE) {
                val remainingFiles = snoringDir.listFiles()?.sortedBy { it.lastModified() } ?: return
                
                for (file in remainingFiles) {
                    if (totalSize <= MAX_STORAGE_SIZE) break
                    
                    totalSize -= file.length()
                    if (file.delete()) {
                        deletedFiles++
                        Log.d(TAG, "Deleted for storage limit: ${file.name}")
                    }
                }
            }
            
            if (deletedFiles > 0) {
                Log.d(TAG, "Cleanup completed: deleted $deletedFiles files")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    /**
     * 저장된 모든 코골이 녹음 파일 목록 반환
     */
    fun getAllRecordingFiles(): List<File> {
        val snoringDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), SNORING_DIR)
        if (!snoringDir.exists()) return emptyList()
        
        return snoringDir.listFiles()?.filter { it.isFile && it.extension == "m4a" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * 특정 녹음 파일 삭제
     */
    fun deleteRecordingFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists() && file.delete()) {
                Log.d(TAG, "Deleted recording file: $filePath")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file: $filePath", e)
            false
        }
    }
    
    /**
     * 전체 저장소 사용량 반환 (바이트)
     */
    fun getTotalStorageUsage(): Long {
        return getAllRecordingFiles().sumOf { it.length() }
    }
    
    /**
     * 리소스 정리
     */
    private fun cleanup() {
        mediaRecorder?.release()
        mediaRecorder = null
        currentRecordingFile = null
        recordingStartTime = 0
        isRecording = false
    }
    
    /**
     * 전체 리소스 해제
     */
    fun release() {
        if (isRecording) {
            stopRecording()
        }
        cleanup()
    }
}

/**
 * 녹음 결과 데이터 클래스
 */
data class RecordingResult(
    val filePath: String,
    val duration: Long,
    val fileSize: Long
) {
    fun getFormattedDuration(): String {
        val seconds = (duration / 1000).toInt()
        return "${seconds}초"
    }
    
    fun getFormattedFileSize(): String {
        return when {
            fileSize < 1024 -> "${fileSize}B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024}KB"
            else -> String.format("%.1fMB", fileSize / (1024.0 * 1024.0))
        }
    }
}