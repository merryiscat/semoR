package com.semo.alarm.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "alarms")
@Parcelize
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "time")
    val time: String, // "HH:MM" 형식
    
    @ColumnInfo(name = "label")
    val label: String = "",
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "days")
    val days: String = "", // JSON: "['mon','tue',...]" 또는 "daily"
    
    @ColumnInfo(name = "sound_uri")
    val soundUri: String = "",
    
    @ColumnInfo(name = "volume")
    val volume: Float = 0.7f, // 0.0-1.0
    
    @ColumnInfo(name = "snooze_enabled")
    val snoozeEnabled: Boolean = true,
    
    @ColumnInfo(name = "snooze_interval")
    val snoozeInterval: Int = 5, // 분
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {
    
    fun getTimeAsHourMinute(): Pair<Int, Int> {
        val parts = time.split(":")
        return Pair(parts[0].toInt(), parts[1].toInt())
    }
    
    fun getDaysAsList(): List<String> {
        return if (days.isNotEmpty() && days != "daily") {
            try {
                days.removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().removeSurrounding("'", "'") }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    fun isRepeating(): Boolean {
        return days.isNotEmpty() && days != "once"
    }
    
    fun isDailyAlarm(): Boolean {
        return days == "daily"
    }
}