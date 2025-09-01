package com.semo.alarm.data.database

import androidx.room.TypeConverter

class Converters {
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return if (value.isNullOrEmpty()) {
            null
        } else {
            value.split(",").filter { it.isNotEmpty() }
        }
    }
}