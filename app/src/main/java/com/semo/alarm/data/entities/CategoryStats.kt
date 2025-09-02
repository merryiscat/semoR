package com.semo.alarm.data.entities

import androidx.room.ColumnInfo

data class CategoryStats(
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "count") val count: Int,
    @ColumnInfo(name = "totalUsage") val totalUsage: Int,
    @ColumnInfo(name = "avgUsage") val avgUsage: Double
)