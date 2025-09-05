package com.semo.alarm.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.semo.alarm.data.entities.SleepRecord

@Dao
interface SleepRecordDao {
    
    @Query("SELECT * FROM sleep_records ORDER BY createdAt DESC")
    fun getAllSleepRecords(): LiveData<List<SleepRecord>>
    
    @Query("SELECT * FROM sleep_records ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentSleepRecords(limit: Int = 7): LiveData<List<SleepRecord>>
    
    @Query("SELECT * FROM sleep_records WHERE isActive = 1 LIMIT 1")
    fun getActiveSleepRecord(): LiveData<SleepRecord?>
    
    @Query("SELECT * FROM sleep_records WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSleepRecordSync(): SleepRecord?
    
    @Query("SELECT * FROM sleep_records WHERE id = :id")
    suspend fun getSleepRecordById(id: Long): SleepRecord?
    
    @Query("SELECT AVG(totalDuration) FROM sleep_records WHERE isActive = 0 AND totalDuration > 0")
    suspend fun getAverageSleepDuration(): Long?
    
    @Query("SELECT AVG(CASE WHEN snoringData != '' THEN 1.0 ELSE 0.0 END) * 100 FROM sleep_records WHERE isActive = 0")
    suspend fun getAverageSnoringPercentage(): Float?
    
    @Query("SELECT COUNT(*) FROM sleep_records WHERE isActive = 0 AND DATE(createdAt/1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-7 days')")
    suspend fun getSleepRecordsCountLast7Days(): Int
    
    @Query("SELECT * FROM sleep_records WHERE DATE(createdAt/1000, 'unixepoch', 'localtime') = DATE('now', 'localtime') AND isActive = 0 LIMIT 1")
    suspend fun getTodaysSleepRecord(): SleepRecord?
    
    @Insert
    suspend fun insertSleepRecord(sleepRecord: SleepRecord): Long
    
    @Update
    suspend fun updateSleepRecord(sleepRecord: SleepRecord)
    
    @Delete
    suspend fun deleteSleepRecord(sleepRecord: SleepRecord)
    
    @Query("DELETE FROM sleep_records WHERE id = :id")
    suspend fun deleteSleepRecordById(id: Long)
    
    @Query("UPDATE sleep_records SET isActive = 0, wakeupTime = :wakeupTime, totalDuration = :totalDuration WHERE id = :id")
    suspend fun finishSleepRecord(id: Long, wakeupTime: Long, totalDuration: Long)
    
    @Query("UPDATE sleep_records SET snoringData = :snoringData, snoringDetected = :snoringDetected WHERE id = :id")
    suspend fun updateSnoringData(id: Long, snoringData: String, snoringDetected: Boolean)
}