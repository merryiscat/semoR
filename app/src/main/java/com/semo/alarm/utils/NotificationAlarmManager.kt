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
import com.semo.alarm.ui.activities.AlarmFullScreenActivity
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
                "🐱 메리 캐릭터 알람",
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "메리가 알람 시간에 풀스크린으로 나타납니다"
                setShowBadge(true)
                setBypassDnd(true)
                enableLights(true)
                lightColor = 0xFF00D4FF.toInt() // 네온 블루
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                
                // 풀스크린 인텐트 허용을 위한 설정
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
                
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
    
    /**
     * 타이머 완료 알림 표시 - 지속적인 알람
     */
    fun showTimerCompleteNotification(timerName: String) {
        Log.d(TAG, "Showing persistent timer complete notification: $timerName")
        
        // 🐱 메리 캐릭터 풀스크린 타이머 Intent (타이머용 더미 알람 생성)
        val timerAlarm = Alarm(
            id = -1,
            time = "00:00",
            label = "⏰ $timerName 완료!",
            isActive = true,
            days = "once",
            soundUri = "",
            volume = 1.0f,
            vibrationEnabled = true,
            snoozeEnabled = false,
            snoozeInterval = 5
        )
        
        val fullScreenIntent = Intent(context, AlarmFullScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(AlarmFullScreenActivity.EXTRA_ALARM, timerAlarm)
            putExtra(AlarmFullScreenActivity.EXTRA_ALARM_ID, -1)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // 백업용 MainActivity Intent
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            1,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        // Dismiss action
        val dismissIntent = Intent(context, AlarmNotificationReceiver::class.java).apply {
            action = "DISMISS_TIMER_ALARM"
            putExtra("timer_name", timerName)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_BASE + 1001,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("🐱 메리가 타이머 완료를 알려줘요!")
            .setContentText("⏰ $timerName - 메리를 만나보세요")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_close_white, "끄기", dismissPendingIntent)
            .build()
        
        // Make notification persistent and high priority
        notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT or android.app.Notification.FLAG_NO_CLEAR
        
        notificationManager.notify(NOTIFICATION_ID_BASE + 1000, notification)
        Log.d(TAG, "Persistent timer complete notification displayed: $timerName")
    }
    
    /**
     * 타이머 알람 해제
     */
    fun dismissTimerAlarm() {
        notificationManager.cancel(NOTIFICATION_ID_BASE + 1000)
        Log.d(TAG, "Timer alarm dismissed")
    }
}