package com.semo.alarm.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "timer_categories")
data class TimerCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,                    // "홈트레이닝", "베이킹", "독서" 등
    val icon: String = "",               // 이모지 아이콘
    val color: String = "#3B82F6",      // 카테고리 색상 (HEX)
    val description: String = "",        // 카테고리 설명
    val isDefault: Boolean = false,      // 기본 제공 카테고리 여부
    val sortOrder: Int = 0,             // 표시 순서
    val createdBy: String = "user",     // "system" 또는 "user"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {
    
    companion object {
        // 기본 제공 카테고리 (하나만)
        fun getDefaultCategories(): List<TimerCategory> {
            return listOf(
                TimerCategory(
                    name = "기본",
                    icon = "",
                    color = "#3B82F6",
                    description = "기본 타이머 카테고리",
                    isDefault = true,
                    sortOrder = 0,
                    createdBy = "system"
                )
            )
        }
        
        // 카테고리 아이콘 후보들
        val AVAILABLE_ICONS = listOf(
            "🏃", "👨‍🍳", "📚", "☕", "🎵", "🧘", "🎮", "💻", 
            "🎨", "🌱", "🏠", "🚗", "⚽", "🏀", "🎯", "🔬",
            "📱", "💼", "🎪", "🌟", "💝", "🎉", "🔥", "⏰"
        )
        
        // 카테고리 색상 후보들
        val AVAILABLE_COLORS = listOf(
            "#EF4444", "#F59E0B", "#10B981", "#3B82F6", "#8B5CF6", 
            "#EC4899", "#14B8A6", "#F97316", "#84CC16", "#6366F1",
            "#D946EF", "#06B6D4", "#EAB308", "#22C55E", "#F43F5E"
        )
    }
    
    fun getDisplayName(): String {
        return if (icon.isNotEmpty()) "$icon $name" else name
    }
    
    fun isCustomCategory(): Boolean {
        return createdBy == "user"
    }
}