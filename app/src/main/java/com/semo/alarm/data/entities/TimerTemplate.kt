package com.semo.alarm.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.semo.alarm.data.enums.TimerType
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "timer_templates",
    foreignKeys = [
        ForeignKey(
            entity = TimerCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["categoryId"])]
)
data class TimerTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val categoryId: Int,            // TimerCategory ID 참조
    val name: String,              // '타바타', '반숙란', '뽀모도로'
    val description: String,       // 상세 설명
    val totalDuration: Int,        // 전체 소요시간 (초)
    val timerType: String = TimerType.SIMPLE.name, // 타이머 타입
    val isDefault: Boolean = false, // 기본 제공 템플릿
    val rating: Float = 0.0f,      // 평점 (1-5)
    val usageCount: Int = 0,       // 사용 횟수
    val createdBy: String = "user", // "system" 또는 "user"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {
    
    // 이제 카테고리 정보는 TimerCategory 엔티티에서 가져옴
    
    fun getFormattedDuration(): String {
        val minutes = totalDuration / 60
        val seconds = totalDuration % 60
        return if (minutes > 0) {
            if (seconds > 0) "${minutes}분 ${seconds}초"
            else "${minutes}분"
        } else {
            "${seconds}초"
        }
    }
    
    fun incrementUsage(): TimerTemplate {
        return copy(usageCount = usageCount + 1)
    }
    
    fun getTimerTypeEnum(): TimerType {
        return TimerType.fromString(timerType)
    }
    
    fun isSimpleTimer(): Boolean {
        return getTimerTypeEnum() == TimerType.SIMPLE
    }
    
    fun isIntervalTimer(): Boolean {
        return getTimerTypeEnum() == TimerType.INTERVAL
    }
}