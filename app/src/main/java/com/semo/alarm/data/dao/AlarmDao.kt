package com.semo.alarm.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.semo.alarm.data.entities.Alarm

@Dao
interface AlarmDao {
    
    @Query("SELECT * FROM alarms ORDER BY time ASC")
    fun getAllAlarms(): LiveData<List<Alarm>>
    
    @Query("SELECT * FROM alarms WHERE is_active = 1 ORDER BY time ASC")
    fun getActiveAlarms(): LiveData<List<Alarm>>
    
    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Int): Alarm?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long
    
    @Update
    suspend fun updateAlarm(alarm: Alarm)
    
    @Delete
    suspend fun deleteAlarm(alarm: Alarm)
    
    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Int)
    
    @Query("UPDATE alarms SET is_active = :isActive WHERE id = :id")
    suspend fun updateAlarmStatus(id: Int, isActive: Boolean)
    
    @Query("SELECT COUNT(*) FROM alarms")
    suspend fun getAlarmCount(): Int
}