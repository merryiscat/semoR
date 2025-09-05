package com.semo.alarm.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "report_data")
data class ReportData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,                          // YYYY-MM-DD 형식
    val alarmDismissalTime: Int = 0,          // 평균 알람 해제 시간(초)
    val snoozeCount: Int = 0,                 // 스누즈 횟수
    val timerUsageMinutes: Int = 0,           // 총 타이머 사용 시간(분)
    val timerCompletionRate: Float = 0f,      // 타이머 완주율 (0.0-1.0)
    val sleepDuration: Int = 0,               // 수면 시간(분)
    val sleepQualityScore: Float = 0f,        // 수면 품질 점수 (1.0-5.0)
    val snoringPercentage: Float = 0f,        // 코골이 비율 (0.0-100.0)
    val productivityScore: Float = 0f,        // 생산성 점수 (0.0-100.0)
    val lifestyleScore: Float = 0f,           // 라이프스타일 점수 (0.0-100.0)
    val createdAt: Long = System.currentTimeMillis()
) {
    
    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        fun getTodayDateString(): String {
            return dateFormat.format(Date())
        }
        
        fun getDateString(timestamp: Long): String {
            return dateFormat.format(Date(timestamp))
        }
    }
    
    /**
     * 알람 해제 시간을 분:초 형태로 포맷
     */
    fun getFormattedDismissalTime(): String {
        val minutes = alarmDismissalTime / 60
        val seconds = alarmDismissalTime % 60
        return "${minutes}분 ${seconds}초"
    }
    
    /**
     * 타이머 사용 시간을 시:분 형태로 포맷
     */
    fun getFormattedTimerUsage(): String {
        val hours = timerUsageMinutes / 60
        val minutes = timerUsageMinutes % 60
        
        return when {
            hours > 0 -> "${hours}시간 ${minutes}분"
            else -> "${minutes}분"
        }
    }
    
    /**
     * 수면 시간을 시:분 형태로 포맷
     */
    fun getFormattedSleepDuration(): String {
        val hours = sleepDuration / 60
        val minutes = sleepDuration % 60
        
        return "${hours}시간 ${minutes}분"
    }
    
    /**
     * 수면 품질을 텍스트로 변환
     */
    fun getSleepQualityText(): String {
        return when {
            sleepQualityScore >= 4.5f -> "매우 좋음"
            sleepQualityScore >= 3.5f -> "좋음"
            sleepQualityScore >= 2.5f -> "보통"
            sleepQualityScore >= 1.5f -> "나쁨"
            else -> "매우 나쁨"
        }
    }
    
    /**
     * 생산성 점수를 등급으로 변환
     */
    fun getProductivityGrade(): String {
        return when {
            productivityScore >= 90f -> "S"
            productivityScore >= 80f -> "A"
            productivityScore >= 70f -> "B"
            productivityScore >= 60f -> "C"
            else -> "D"
        }
    }
    
    /**
     * 라이프스타일 점수를 텍스트로 변환
     */
    fun getLifestyleText(): String {
        return when {
            lifestyleScore >= 90f -> "매우 건강함"
            lifestyleScore >= 80f -> "건강함"
            lifestyleScore >= 70f -> "양호함"
            lifestyleScore >= 60f -> "개선 필요"
            else -> "관리 필요"
        }
    }
    
    /**
     * 종합 평가 - 가장 취약한 영역 찾기
     */
    fun getWeakestArea(): String {
        val scores = mapOf(
            "수면" to (sleepQualityScore * 20f), // 5점 만점을 100점 만점으로 변환
            "알람" to (if (alarmDismissalTime <= 120) 100f else maxOf(0f, 100f - (alarmDismissalTime / 10f))), // 2분 이내면 100점
            "타이머" to (timerCompletionRate * 100f),
            "생산성" to productivityScore
        )
        
        return scores.minByOrNull { it.value }?.key ?: "수면"
    }
    
    /**
     * 오늘의 하이라이트 - 가장 좋은 영역
     */
    fun getBestArea(): String {
        val scores = mapOf(
            "수면" to (sleepQualityScore * 20f),
            "알람" to (if (alarmDismissalTime <= 120) 100f else maxOf(0f, 100f - (alarmDismissalTime / 10f))),
            "타이머" to (timerCompletionRate * 100f),
            "생산성" to productivityScore
        )
        
        return scores.maxByOrNull { it.value }?.key ?: "생산성"
    }
}