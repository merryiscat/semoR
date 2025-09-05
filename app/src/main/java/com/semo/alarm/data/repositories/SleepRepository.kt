package com.semo.alarm.data.repositories

import androidx.lifecycle.LiveData
import com.semo.alarm.data.dao.SleepRecordDao
import com.semo.alarm.data.entities.SleepRecord
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepRepository @Inject constructor(
    private val sleepRecordDao: SleepRecordDao
) {
    
    fun getAllSleepRecords(): LiveData<List<SleepRecord>> {
        return sleepRecordDao.getAllSleepRecords()
    }
    
    fun getRecentSleepRecords(limit: Int = 7): LiveData<List<SleepRecord>> {
        return sleepRecordDao.getRecentSleepRecords(limit)
    }
    
    fun getActiveSleepRecord(): LiveData<SleepRecord?> {
        return sleepRecordDao.getActiveSleepRecord()
    }
    
    suspend fun getActiveSleepRecordSync(): SleepRecord? {
        return sleepRecordDao.getActiveSleepRecordSync()
    }
    
    suspend fun getSleepRecordById(id: Long): SleepRecord? {
        return sleepRecordDao.getSleepRecordById(id)
    }
    
    suspend fun getAverageSleepDuration(): Long {
        return sleepRecordDao.getAverageSleepDuration() ?: 0L
    }
    
    suspend fun getAverageSnoringPercentage(): Float {
        return sleepRecordDao.getAverageSnoringPercentage() ?: 0f
    }
    
    suspend fun getSleepRecordsCountLast7Days(): Int {
        return sleepRecordDao.getSleepRecordsCountLast7Days()
    }
    
    suspend fun getTodaysSleepRecord(): SleepRecord? {
        return sleepRecordDao.getTodaysSleepRecord()
    }
    
    suspend fun insertSleepRecord(sleepRecord: SleepRecord): Long {
        return sleepRecordDao.insertSleepRecord(sleepRecord)
    }
    
    suspend fun updateSleepRecord(sleepRecord: SleepRecord) {
        sleepRecordDao.updateSleepRecord(sleepRecord)
    }
    
    suspend fun deleteSleepRecord(sleepRecord: SleepRecord) {
        sleepRecordDao.deleteSleepRecord(sleepRecord)
    }
    
    suspend fun deleteSleepRecordById(id: Long) {
        sleepRecordDao.deleteSleepRecordById(id)
    }
    
    suspend fun finishSleepRecord(id: Long, wakeupTime: Long, totalDuration: Long) {
        sleepRecordDao.finishSleepRecord(id, wakeupTime, totalDuration)
    }
    
    suspend fun updateSnoringData(id: Long, snoringData: String, snoringDetected: Boolean) {
        sleepRecordDao.updateSnoringData(id, snoringData, snoringDetected)
    }
    
    suspend fun startSleepTracking(): Long {
        val currentTime = System.currentTimeMillis()
        val sleepRecord = SleepRecord(
            bedtime = currentTime,
            wakeupTime = null,
            totalDuration = 0L,
            qualityScore = 0f,
            snoringDetected = false,
            snoringData = "",
            movementData = "",
            notes = "",
            isActive = true,
            createdAt = currentTime
        )
        return insertSleepRecord(sleepRecord)
    }
    
    suspend fun stopSleepTracking(sleepRecordId: Long): SleepRecord? {
        val sleepRecord = getSleepRecordById(sleepRecordId) ?: return null
        val currentTime = System.currentTimeMillis()
        val totalDuration = currentTime - sleepRecord.bedtime
        
        val updatedRecord = sleepRecord.copy(
            wakeupTime = currentTime,
            totalDuration = totalDuration,
            isActive = false
        )
        
        updateSleepRecord(updatedRecord)
        return updatedRecord
    }
}