package com.semo.alarm.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "timer_rounds",
    foreignKeys = [
        ForeignKey(
            entity = TimerTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["templateId"])]
)
data class TimerRound(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val templateId: Int,           // TimerTemplate ID 참조
    val roundIndex: Int,          // 순서 (0부터 시작)
    val name: String,             // "운동", "휴식", "준비", "완료"
    val duration: Int,            // 초 단위
    val soundUri: String = "",    // 라운드별 알람음 (비어있으면 기본)
    val vibrationPattern: String = "", // JSON 형태의 진동 패턴
    val color: String = "#3B82F6", // 라운드별 색상 (HEX)
    val cycles: Int = 1           // 해당 라운드의 반복 횟수
) : Parcelable {
    
    companion object {
        // 미리 정의된 색상들
        const val COLOR_EXERCISE = "#EF4444"      // 빨강 (운동)
        const val COLOR_REST = "#3B82F6"         // 파랑 (휴식)
        const val COLOR_PREPARE = "#F59E0B"      // 주황 (준비)
        const val COLOR_COMPLETE = "#10B981"     // 초록 (완료)
        
        // 미리 정의된 진동 패턴
        const val VIBRATION_SOFT = "[0, 500, 200, 500]"         // 부드러운 진동
        const val VIBRATION_STRONG = "[0, 1000, 300, 1000]"     // 강한 진동
        const val VIBRATION_PULSE = "[0, 200, 100, 200, 100, 200]" // 펄스 진동
    }
    
    fun getFormattedDuration(): String {
        val minutes = duration / 60
        val seconds = duration % 60
        return if (minutes > 0) {
            if (seconds > 0) "${minutes}분 ${seconds}초"
            else "${minutes}분"
        } else {
            "${seconds}초"
        }
    }
    
    fun getVibrationPatternArray(): LongArray {
        return if (vibrationPattern.isNotEmpty()) {
            try {
                // JSON 배열 형태의 문자열을 LongArray로 변환
                vibrationPattern
                    .removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().toLong() }
                    .toLongArray()
            } catch (e: Exception) {
                longArrayOf(0, 500) // 기본 진동 패턴
            }
        } else {
            longArrayOf(0, 500) // 기본 진동 패턴
        }
    }
    
    fun isExerciseRound(): Boolean {
        return color == COLOR_EXERCISE || name.contains("운동", ignoreCase = true)
    }
    
    fun isRestRound(): Boolean {
        return color == COLOR_REST || name.contains("휴식", ignoreCase = true) || name.contains("쉬기", ignoreCase = true)
    }
}