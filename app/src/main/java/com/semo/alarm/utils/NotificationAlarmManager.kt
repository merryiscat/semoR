package com.semo.alarm.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.semo.alarm.R
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.receivers.AlarmNotificationReceiver
import com.semo.alarm.ui.activities.MainActivity
import java.util.*

/**
 * Android 최신 버전의 백그라운드 제약을 우회하는 알람 매니저
 * 
 * BroadcastReceiver 대신 AlarmManager의 콜백을 직접 사용하여
 * 확실한 알람 발생을 보장합니다.
 */
class NotificationAlarmManager(private val context: Context) {
    
    companion object {
        private const val TAG = "NotificationAlarmManager"
        const val ALARM_CHANNEL_ID = "alarm_notifications"
        const val NOTIFICATION_ID_BASE = 10000
    }
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    /**
     * 알람 스케줄링 - 직접 알림 방식
     */
    fun scheduleAlarm(alarm: Alarm) {
        Log.d(TAG, "Scheduling notification-based alarm: ID=${alarm.id}, Time=${alarm.time}")
        
        // 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Cannot schedule exact alarms - permission not granted!")
                return
            }
        }
        
        val targetTime = getNextAlarmTime(alarm)
        Log.d(TAG, "Target time: ${Date(targetTime)}")
        
        // 알림을 직접 표시하는 PendingIntent 생성
        val notificationIntent = createAlarmNotificationIntent(alarm)
        
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetTime,
                        notificationIntent
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        targetTime,
                        notificationIntent
                    )
                }
                else -> {
                    @Suppress("DEPRECATION")
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        targetTime,
                        notificationIntent
                    )
                }
            }
            Log.d(TAG, "Notification alarm scheduled successfully for ${alarm.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule notification alarm", e)
        }
    }
    
    /**
     * 알람 취소
     */
    fun cancelAlarm(alarmId: Int) {
        Log.d(TAG, "Canceling notification alarm: ID=$alarmId")
        
        val intent = Intent(context, AlarmNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        alarmManager.cancel(pendingIntent)
        
        // 기존 알림도 취소
        notificationManager.cancel(NOTIFICATION_ID_BASE + alarmId)
    }
    
    /**
     * 스누즈 알람 설정
     */
    fun scheduleSnoozeAlarm(alarm: Alarm) {
        Log.d(TAG, "Scheduling snooze alarm for ${alarm.snoozeInterval} minutes")
        
        try {
            val currentTime = System.currentTimeMillis()
            val snoozeTime = currentTime + (alarm.snoozeInterval * 60 * 1000L) // 분을 밀리초로 변환
            
            val snoozeIntent = createAlarmNotificationIntent(alarm.copy(id = alarm.id + 60000)) // 스누즈용 임시 ID
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        snoozeIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        snoozeIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    snoozeIntent
                )
            }
            
            Log.d(TAG, "Snooze alarm scheduled for ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(snoozeTime)}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule snooze alarm", e)
        }
    }
    
    
    /**
     * 알람 알림을 직접 표시하는 PendingIntent 생성
     */
    private fun createAlarmNotificationIntent(alarm: Alarm): PendingIntent {
        val intent = Intent(context, AlarmNotificationReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm", alarm)
            action = "SHOW_ALARM_NOTIFICATION"
        }
        
        return PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
    }
    
    /**
     * 다음 알람 시간 계산
     */
    private fun getNextAlarmTime(alarm: Alarm): Long {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            val (hour, minute) = alarm.getTimeAsHourMinute()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // 현재 시간보다 이전이면 다음 날로 설정
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return calendar.timeInMillis
    }
    
    /**
     * 알림 채널 생성
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "알람 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "알람이 울릴 때 표시되는 알림"
                setShowBadge(true)
                setBypassDnd(true)
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                
                // 알람음 설정
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                setSound(alarmSound, audioAttributes)
            }
            
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
}