package com.semo.alarm.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

/**
 * 알람 실행 기록을 저장하는 엔티티
 * 
 * 일반 리포트에서 "최근 알람" 섹션에 표시할 데이터
 */
@Entity(tableName = "alarm_history")
data class AlarmHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "alarm_id")
    val alarmId: Int,                   // 원본 알람 ID (Alarm 테이블 참조)
    
    @ColumnInfo(name = "triggered_time")
    val triggeredTime: Long,            // 알람이 울린 시간 (timestamp)
    
    @ColumnInfo(name = "dismissed_time")
    val dismissedTime: Long?,           // 알람을 해제한 시간 (nullable)
    
    @ColumnInfo(name = "alarm_time")
    val alarmTime: String,              // 설정된 알람 시간 "HH:MM"
    
    @ColumnInfo(name = "label")
    val label: String,                  // 알람 라벨
    
    @ColumnInfo(name = "type")
    val type: AlarmType,                // 알람 타입 (알람, 타이머)
    
    @ColumnInfo(name = "status")
    val status: AlarmStatus,            // 상태 (완료, 스누즈, 무시)
    
    @ColumnInfo(name = "snooze_count")
    val snoozeCount: Int = 0,           // 스누즈 횟수
    
    @ColumnInfo(name = "date")
    val date: String,                   // 날짜 "YYYY-MM-DD"
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    
    /**
     * 알람 타입
     */
    enum class AlarmType(val displayName: String) {
        ALARM("알람"),
        TIMER("타이머")
    }
    
    /**
     * 알람 상태
     */
    enum class AlarmStatus(val displayName: String, val colorResId: String) {
        COMPLETED("완료", "#22C55E"),      // 녹색
        SNOOZED("스누즈", "#F59E0B"),      // 노란색  
        IGNORED("무시", "#EF4444"),        // 빨간색
        RUNNING("실행중", "#3B82F6")       // 파란색
    }
    
    /**
     * 표시용 시간 포맷
     */
    fun getFormattedTime(): String = alarmTime
    
    /**
     * 소요 시간 계산 (알람 울린 시간 ~ 해제 시간)
     */
    fun getDismissalDuration(): Long? {
        return dismissedTime?.let { it - triggeredTime }
    }
    
    /**
     * 소요 시간을 사용자 친화적 형식으로 표시
     */
    fun getFormattedDismissalDuration(): String {
        val duration = getDismissalDuration() ?: return "진행중"
        val seconds = (duration / 1000).toInt()
        return when {
            seconds < 60 -> "${seconds}초"
            seconds < 3600 -> "${seconds / 60}분 ${seconds % 60}초"
            else -> "${seconds / 3600}시간 ${(seconds % 3600) / 60}분"
        }
    }
    
    /**
     * 날짜를 Date 객체로 변환
     */
    fun getDateAsDate(): Date {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.parse(date) ?: Date()
    }
    
    /**
     * 스누즈 정보 포함 상태 텍스트
     */
    fun getStatusWithSnooze(): String {
        return if (status == AlarmStatus.SNOOZED && snoozeCount > 0) {
            "${status.displayName} (${snoozeCount}회)"
        } else {
            status.displayName
        }
    }
}