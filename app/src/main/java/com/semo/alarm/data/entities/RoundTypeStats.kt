package com.semo.alarm.data.entities

import androidx.room.ColumnInfo

data class RoundTypeStats(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "count") val count: Int,
    @ColumnInfo(name = "totalDuration") val totalDuration: Int,
    @ColumnInfo(name = "avgDuration") val avgDuration: Double
)