package com.semo.alarm.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.semo.alarm.R
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.data.repositories.AlarmRepository
import com.semo.alarm.ui.activities.MainActivity
import com.semo.alarm.ui.activities.AlarmFullScreenActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService : Service() {
    
    companion object {
        private const val TAG = "AlarmService"
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_DISMISS = "action_dismiss"
        const val ACTION_SNOOZE = "action_snooze"
    }
    
    @Inject
    lateinit var alarmRepository: AlarmRepository
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var currentAlarm: Alarm? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlarmService created")
        
        createNotificationChannel()
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AlarmService onStartCommand: action=${intent?.action}")
        
        when (intent?.action) {
            ACTION_DISMISS -> {
                Log.d(TAG, "Dismiss alarm action received")
                dismissAlarm()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                Log.d(TAG, "Snooze alarm action received")
                snoozeAlarm()
                return START_NOT_STICKY
            }
            else -> {
                val alarmId = intent?.getIntExtra("alarm_id", -1) ?: -1
                val alarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra("alarm", Alarm::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra("alarm")
                }
                
                Log.d(TAG, "Starting alarm: ID=$alarmId, Alarm=${alarm?.time}")
                
                if (alarm != null) {
                    currentAlarm = alarm
                    startAlarm(alarm)
                } else {
                    Log.e(TAG, "Received null alarm!")
                }
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startAlarm(alarm: Alarm) {
        Log.d(TAG, "Starting alarm: ${alarm.label} at ${alarm.time}")
        
        try {
            // Foreground 알림 시작
            Log.d(TAG, "Starting foreground notification")
            startForeground(NOTIFICATION_ID, createAlarmNotification(alarm))
            
            // 알람음 재생
            Log.d(TAG, "Starting alarm sound with volume: ${alarm.volume}")
            startAlarmSound(alarm)
            
            // 진동
            if (alarm.vibrationEnabled) {
                Log.d(TAG, "Starting vibration")
                startVibration()
            } else {
                Log.d(TAG, "Vibration disabled for this alarm")
            }
            
            // 30초 후 자동 종료 (무한 재생 방지)
            Handler(Looper.getMainLooper()).postDelayed({
                if (mediaPlayer?.isPlaying == true) {
                    Log.d(TAG, "Auto-dismissing alarm after 30 seconds")
                    dismissAlarm()
                }
            }, 30000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting alarm", e)
        }
    }
    
    private fun startAlarmSound(alarm: Alarm) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                val soundUri = if (alarm.soundUri.isNotEmpty()) {
                    android.net.Uri.parse(alarm.soundUri)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                
                setDataSource(this@AlarmService, soundUri)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(android.media.AudioManager.STREAM_ALARM)
                }
                
                setVolume(alarm.volume, alarm.volume)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 기본 알람음으로 재시도
            try {
                mediaPlayer = MediaPlayer().apply {
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    setDataSource(this@AlarmService, defaultUri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                    }
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun startVibration() {
        val pattern = longArrayOf(0, 1000, 1000, 1000, 1000)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, 0),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }
    
    private fun dismissAlarm() {
        stopAlarmSound()
        stopVibration()
        
        // 🔧 "한 번만" 알람인 경우 자동으로 비활성화
        currentAlarm?.let { alarm ->
            if (alarm.days == "once") {
                Log.d(TAG, "Disabling 'once only' alarm after execution: ${alarm.id}")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        alarmRepository.updateAlarmStatus(alarm.id, false)
                        Log.d(TAG, "Successfully disabled alarm ${alarm.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to disable alarm ${alarm.id}", e)
                    }
                }
            }
            
            // 🔧 "N회 반복" 알람인 경우도 처리 (미래 확장)
            if (alarm.isCountRepeat()) {
                Log.d(TAG, "Count repeat alarm detected: ${alarm.days}")
                // TODO: 반복 횟수 추적 로직 추가 (현재는 단순히 비활성화)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        alarmRepository.updateAlarmStatus(alarm.id, false)
                        Log.d(TAG, "Successfully disabled count repeat alarm ${alarm.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to disable count repeat alarm ${alarm.id}", e)
                    }
                }
            }
        }
        
        stopForeground(true)
        stopSelf()
    }
    
    private fun snoozeAlarm() {
        currentAlarm?.let { alarm ->
            if (alarm.snoozeEnabled) {
                // 스누즈 구현 (나중에 AlarmScheduler를 통해 재설정)
                stopAlarmSound()
                stopVibration()
                stopForeground(true)
                
                // 스누즈 간격 후 다시 알람 설정
                Handler(Looper.getMainLooper()).postDelayed({
                    startAlarm(alarm)
                }, (alarm.snoozeInterval * 60 * 1000).toLong())
                
                // 스누즈 알림 표시
                showSnoozeNotification(alarm)
            } else {
                dismissAlarm()
            }
        }
    }
    
    private fun stopAlarmSound() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }
    
    private fun stopVibration() {
        vibrator?.cancel()
    }
    
    private fun createAlarmNotification(alarm: Alarm): android.app.Notification {
        // 🐱 메리 캐릭터 풀스크린 알람 Intent
        val fullScreenIntent = Intent(this, AlarmFullScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(AlarmFullScreenActivity.EXTRA_ALARM, alarm)
            putExtra(AlarmFullScreenActivity.EXTRA_ALARM_ID, alarm.id)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // 백업용 MainActivity Intent
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 1, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        // 🚫 해제/스누즈 PendingIntent 제거 - 풀스크린에서만 상호작용 가능
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(alarm.label.ifEmpty { "🐱 메리가 깨우고 있어요!" })
            .setContentText("${alarm.time} - 메리를 터치해서 만나보세요!")
            .setSubText("💤 화면을 터치하면 메리가 나타나요")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            // 🚫 액션 버튼들 모두 제거 - 풀스크린 상호작용만 가능
            .build()
    }
    
    private fun showSnoozeNotification(alarm: Alarm) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sleep)
            .setContentTitle("스누즈")
            .setContentText("${alarm.snoozeInterval}분 후 다시 알림")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
        
        // 스누즈 알림은 3초 후 자동 제거
        Handler(Looper.getMainLooper()).postDelayed({
            notificationManager.cancel(NOTIFICATION_ID + 1)
        }, 3000)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "알람",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "알람 알림"
                setShowBadge(true)
                setBypassDnd(true)
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
        stopVibration()
    }
}