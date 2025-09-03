package com.semo.alarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.services.AlarmService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AlarmReceiver triggered!")
        
        val alarmId = intent.getIntExtra("alarm_id", -1)
        val alarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("alarm", Alarm::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("alarm")
        }
        
        Log.d(TAG, "Received alarm: ID=$alarmId, Alarm=${alarm?.time ?: "null"}")
        
        if (alarmId != -1 && alarm != null) {
            // 알람 서비스 시작
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("alarm", alarm)
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                    Log.d(TAG, "Started foreground service for alarm $alarmId")
                } else {
                    context.startService(serviceIntent)
                    Log.d(TAG, "Started service for alarm $alarmId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start alarm service", e)
            }
        } else {
            Log.e(TAG, "Invalid alarm data: alarmId=$alarmId, alarm=$alarm")
        }
    }
}