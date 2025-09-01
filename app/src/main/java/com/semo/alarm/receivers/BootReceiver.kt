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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED
        ) {
            // 부팅 후 모든 활성 알람 재설정
            rescheduleAlarms(context)
        }
    }
    
    private fun rescheduleAlarms(context: Context) {
        val alarmScheduler = AlarmScheduler(context)
        val database = AlarmDatabase.getDatabase(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 모든 활성 알람 가져오기 (LiveData가 아닌 직접 쿼리)
                val activeAlarms = database.alarmDao().getActiveAlarms().value
                
                activeAlarms?.forEach { alarm ->
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