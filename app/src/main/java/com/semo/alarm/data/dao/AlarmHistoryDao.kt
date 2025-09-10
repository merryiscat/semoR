package com.semo.alarm.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.semo.alarm.data.entities.AlarmHistory

@Dao
interface AlarmHistoryDao {
    
    @Query("SELECT * FROM alarm_history ORDER BY triggered_time DESC")
    fun getAllAlarmHistory(): LiveData<List<AlarmHistory>>
    
    @Query("SELECT * FROM alarm_history WHERE date = :date ORDER BY triggered_time DESC")
    suspend fun getAlarmHistoryByDate(date: String): List<AlarmHistory>
    
    @Query("SELECT * FROM alarm_history WHERE date = :date ORDER BY triggered_time DESC")
    fun getAlarmHistoryByDateLive(date: String): LiveData<List<AlarmHistory>>
    
    @Query("SELECT * FROM alarm_history WHERE id = :id")
    suspend fun getAlarmHistoryById(id: Long): AlarmHistory?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarmHistory(alarmHistory: AlarmHistory): Long
    
    @Update
    suspend fun updateAlarmHistory(alarmHistory: AlarmHistory)
    
    @Delete
    suspend fun deleteAlarmHistory(alarmHistory: AlarmHistory)
    
    @Query("DELETE FROM alarm_history WHERE id = :id")
    suspend fun deleteAlarmHistoryById(id: Long)
    
    @Query("DELETE FROM alarm_history WHERE date < :cutoffDate")
    suspend fun deleteOldAlarmHistory(cutoffDate: String)
    
    @Query("SELECT COUNT(*) FROM alarm_history")
    suspend fun getAlarmHistoryCount(): Int
    
    @Query("SELECT COUNT(*) FROM alarm_history WHERE date = :date")
    suspend fun getAlarmHistoryCountByDate(date: String): Int
    
    @Query("SELECT * FROM alarm_history WHERE date BETWEEN :startDate AND :endDate ORDER BY triggered_time DESC")
    suspend fun getAlarmHistoryByDateRange(startDate: String, endDate: String): List<AlarmHistory>
}