package com.semo.alarm.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.data.repositories.AlarmRepository
import com.semo.alarm.utils.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    application: Application,
    private val repository: AlarmRepository
) : AndroidViewModel(application) {
    
    private val alarmScheduler = AlarmScheduler(application)
    
    val allAlarms: LiveData<List<Alarm>> = repository.getAllAlarms()
    val activeAlarms: LiveData<List<Alarm>> = repository.getActiveAlarms()
    
    fun insertAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val alarmId = repository.insertAlarm(alarm)
            val savedAlarm = alarm.copy(id = alarmId.toInt())
            
            if (savedAlarm.isActive) {
                alarmScheduler.scheduleAlarm(savedAlarm)
            }
        }
    }
    
    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
            
            // 기존 알람 취소 후 재설정
            alarmScheduler.cancelAlarm(alarm.id)
            if (alarm.isActive) {
                alarmScheduler.scheduleAlarm(alarm)
            }
        }
    }
    
    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
            alarmScheduler.cancelAlarm(alarm.id)
        }
    }
    
    fun deleteAlarmById(id: Int) {
        viewModelScope.launch {
            repository.deleteAlarmById(id)
            alarmScheduler.cancelAlarm(id)
        }
    }
    
    fun toggleAlarmStatus(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateAlarmStatus(id, isActive)
            
            val alarm = repository.getAlarmById(id)
            if (alarm != null) {
                if (isActive) {
                    alarmScheduler.scheduleAlarm(alarm.copy(isActive = true))
                } else {
                    alarmScheduler.cancelAlarm(id)
                }
            }
        }
    }
    
    suspend fun getAlarmById(id: Int): Alarm? {
        return repository.getAlarmById(id)
    }
}