package com.semo.alarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.semo.alarm.data.database.AlarmDatabase
import com.semo.alarm.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 부팅 완료 후 모든 활성화된 알람을 다시 스케줄링
            val database = AlarmDatabase.getDatabase(context)
            val alarmScheduler = AlarmScheduler(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarms = database.alarmDao().getAllAlarms().value
                    alarms?.forEach { alarm ->
                        if (alarm.isActive) {
                            alarmScheduler.scheduleAlarm(alarm)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}