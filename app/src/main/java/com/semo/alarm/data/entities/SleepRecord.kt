package com.semo.alarm.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bedtime: Long,                    // 취침 시간 (timestamp)
    val wakeupTime: Long? = null,         // 기상 시간 (nullable - 진행 중일 때)
    val totalDuration: Long = 0,          // 총 수면 시간 (밀리초)
    val qualityScore: Float = 0f,         // 수면 품질 점수 (1-5)
    val snoringDetected: Boolean = false, // 코골이 감지 여부
    val snoringData: String = "",         // JSON: 코골이 시간대 데이터
    val movementData: String = "",        // JSON: 움직임 데이터
    val notes: String = "",               // 사용자 메모
    val isActive: Boolean = false,        // 현재 수면 중인지 여부
    val createdAt: Long = System.currentTimeMillis()
) {
    
    /**
     * 수면 시간을 시:분 형태로 포맷
     */
    fun getFormattedDuration(): String {
        val hours = (totalDuration / (1000 * 60 * 60)).toInt()
        val minutes = ((totalDuration % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        
        return when {
            hours > 0 -> "${hours}시간 ${minutes}분"
            else -> "${minutes}분"
        }
    }
    
    /**
     * 수면 품질을 텍스트로 변환
     */
    fun getQualityText(): String {
        return when {
            qualityScore >= 4.5f -> "매우 좋음"
            qualityScore >= 3.5f -> "좋음"
            qualityScore >= 2.5f -> "보통"
            qualityScore >= 1.5f -> "나쁨"
            else -> "매우 나쁨"
        }
    }
    
    /**
     * 현재 수면 중인지 확인
     */
    fun isSleeping(): Boolean = isActive && wakeupTime == null
    
    /**
     * 수면이 완료되었는지 확인
     */
    fun isCompleted(): Boolean = !isActive && wakeupTime != null
}