package com.semo.alarm.data.repositories

import androidx.lifecycle.LiveData
import com.semo.alarm.data.dao.AlarmDao
import com.semo.alarm.data.entities.Alarm
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao
) {
    
    fun getAllAlarms(): LiveData<List<Alarm>> {
        return alarmDao.getAllAlarms()
    }
    
    fun getActiveAlarms(): LiveData<List<Alarm>> {
        return alarmDao.getActiveAlarms()
    }
    
    suspend fun getAlarmById(id: Int): Alarm? {
        return alarmDao.getAlarmById(id)
    }
    
    suspend fun insertAlarm(alarm: Alarm): Long {
        return alarmDao.insertAlarm(alarm)
    }
    
    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
    }
    
    suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm)
    }
    
    suspend fun deleteAlarmById(id: Int) {
        alarmDao.deleteAlarmById(id)
    }
    
    suspend fun updateAlarmStatus(id: Int, isActive: Boolean) {
        alarmDao.updateAlarmStatus(id, isActive)
    }
    
    suspend fun getAlarmCount(): Int {
        return alarmDao.getAlarmCount()
    }
}