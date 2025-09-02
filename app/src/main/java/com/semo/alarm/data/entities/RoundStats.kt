package com.semo.alarm.data.entities

import androidx.room.ColumnInfo

data class RoundStats(
    @ColumnInfo(name = "totalRounds") val totalRounds: Int,
    @ColumnInfo(name = "totalDuration") val totalDuration: Int,
    @ColumnInfo(name = "avgDuration") val avgDuration: Double,
    @ColumnInfo(name = "minDuration") val minDuration: Int,
    @ColumnInfo(name = "maxDuration") val maxDuration: Int
)