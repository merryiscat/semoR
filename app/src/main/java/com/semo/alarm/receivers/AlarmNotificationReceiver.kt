package com.semo.alarm.receivers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.os.PowerManager
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.semo.alarm.R
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.ui.activities.MainActivity
import com.semo.alarm.ui.activities.AlarmFullScreenActivity
import com.semo.alarm.utils.NotificationAlarmManager

/**
 * 알람 알림을 직접 표시하고 사운드/진동을 재생하는 Receiver
 * 
 * 기존 AlarmReceiver + AlarmService의 기능을 통합하여
 * 더 확실한 알람 동작을 보장합니다.
 */
class AlarmNotificationReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmNotificationReceiver"
        private const val ACTION_DISMISS = "action_dismiss_notification"
        private const val ACTION_SNOOZE = "action_snooze_notification"
        private const val ACTION_DISMISS_TIMER = "DISMISS_TIMER_ALARM"
        
        // 전역 MediaPlayer 관리 - 알람 해제를 위해 필요
        private var activeMediaPlayer: MediaPlayer? = null
        private val activeMediaPlayers = mutableMapOf<Int, MediaPlayer>()
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 AlarmNotificationReceiver triggered! Action: ${intent.action}")
        
        when (intent.action) {
            "SHOW_ALARM_NOTIFICATION" -> {
                handleAlarmNotification(context, intent)
            }
            ACTION_DISMISS -> {
                handleDismissAlarm(context, intent)
            }
            ACTION_SNOOZE -> {
                handleSnoozeAlarm(context, intent)
            }
            ACTION_DISMISS_TIMER -> {
                handleDismissTimer(context, intent)
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
            }
        }
    }
    
    /**
     * 알람 알림 표시 및 사운드/진동 시작
     */
    private fun handleAlarmNotification(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        val alarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("alarm", Alarm::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("alarm")
        }
        
        Log.d(TAG, "Processing alarm notification: ID=$alarmId, Alarm=${alarm?.time}")
        
        if (alarm != null) {
            // 🔥 최우선: 화면 켜기 및 풀스크린 실행 (알림보다 먼저)
            wakeUpScreenAndStartFullScreen(context, alarm)
            
            // 알림 표시 (백업용)
            showAlarmNotification(context, alarm)
            
            // 사운드와 진동 시작  
            playAlarmSound(context, alarm)
            startVibration(context, alarm)
        } else {
            Log.e(TAG, "Received null alarm!")
        }
    }
    
    /**
     * 알람 해제 처리
     */
    private fun handleDismissAlarm(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        Log.d(TAG, "🔇 Dismissing alarm: $alarmId")
        
        // 1. 알림 취소
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationAlarmManager.NOTIFICATION_ID_BASE + alarmId)
        
        // 2. 해당 알람의 MediaPlayer 중지 및 해제
        activeMediaPlayers[alarmId]?.let { mediaPlayer ->
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                    Log.d(TAG, "🔇 MediaPlayer stopped for alarm $alarmId")
                }
                mediaPlayer.release()
                Log.d(TAG, "🔇 MediaPlayer released for alarm $alarmId")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping MediaPlayer for alarm $alarmId", e)
            }
            activeMediaPlayers.remove(alarmId)
        }
        
        // 3. 전역 MediaPlayer도 확인
        activeMediaPlayer?.let { mediaPlayer ->
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                    Log.d(TAG, "🔇 Global MediaPlayer stopped")
                }
                mediaPlayer.release()
                Log.d(TAG, "🔇 Global MediaPlayer released")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping global MediaPlayer", e)
            }
            activeMediaPlayer = null
        }
        
        // 4. 진동 중지
        stopVibration(context)
        
        Log.d(TAG, "✅ Alarm $alarmId completely dismissed")
    }
    
    /**
     * 스누즈 처리
     */
    private fun handleSnoozeAlarm(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        val alarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("alarm", Alarm::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("alarm")
        }
        
        Log.d(TAG, "😴 Snoozing alarm: $alarmId")
        
        if (alarm != null && alarm.snoozeEnabled) {
            // 1. 현재 알림 취소
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NotificationAlarmManager.NOTIFICATION_ID_BASE + alarmId)
            
            // 2. MediaPlayer 중지 및 해제 (dismiss와 동일한 로직)
            activeMediaPlayers[alarmId]?.let { mediaPlayer ->
                try {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.stop()
                        Log.d(TAG, "🔇 MediaPlayer stopped for snoozed alarm $alarmId")
                    }
                    mediaPlayer.release()
                    Log.d(TAG, "🔇 MediaPlayer released for snoozed alarm $alarmId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping MediaPlayer for snoozed alarm $alarmId", e)
                }
                activeMediaPlayers.remove(alarmId)
            }
            
            // 3. 전역 MediaPlayer도 정리
            activeMediaPlayer?.let { mediaPlayer ->
                try {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.stop()
                    }
                    mediaPlayer.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping global MediaPlayer for snooze", e)
                }
                activeMediaPlayer = null
            }
            
            // 4. 스누즈 간격 후 다시 알람 설정
            val alarmManager = NotificationAlarmManager(context)
            alarmManager.scheduleSnoozeAlarm(alarm)
            
            Log.d(TAG, "😴 Alarm snoozed for ${alarm.snoozeInterval} minutes")
        }
        
        // 5. 진동 중지
        stopVibration(context)
        
        Log.d(TAG, "✅ Alarm $alarmId snoozed successfully")
    }
    
    /**
     * 타이머 알람 해제 처리
     */
    private fun handleDismissTimer(context: Context, intent: Intent) {
        val timerName = intent.getStringExtra("timer_name") ?: "타이머"
        Log.d(TAG, "⏰ Dismissing timer alarm: $timerName")
        
        // NotificationAlarmManager를 통해 타이머 알람 해제
        val alarmManager = NotificationAlarmManager(context)
        alarmManager.dismissTimerAlarm()
        
        Log.d(TAG, "✅ Timer alarm '$timerName' dismissed successfully")
    }
    
    /**
     * 알람 알림 표시
     */
    private fun showAlarmNotification(context: Context, alarm: Alarm) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 🐱 메리 캐릭터 풀스크린 알람 Intent
        val fullScreenIntent = Intent(context, AlarmFullScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(AlarmFullScreenActivity.EXTRA_ALARM, alarm)
            putExtra(AlarmFullScreenActivity.EXTRA_ALARM_ID, alarm.id)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // 백업용 MainActivity Intent (알림 탭)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context, 1, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        // 해제 버튼 인텐트
        val dismissIntent = Intent(context, AlarmNotificationReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra("alarm_id", alarm.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, alarm.id, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        // 스누즈 버튼 인텐트
        val snoozeIntent = Intent(context, AlarmNotificationReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("alarm_id", alarm.id)
            putExtra("alarm", alarm)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, alarm.id + 1000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        // 알림 생성 - 최고 우선순위로 풀스크린 강제 실행
        val notification = NotificationCompat.Builder(context, NotificationAlarmManager.ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.character_merry_idle_01))
            .setContentTitle(alarm.label.ifEmpty { "🐱 메리가 깨우고 있어요!" })
            .setContentText("${alarm.time} - 탭하면 메리를 만날 수 있어요!")
            .setSubText("알람이 울리고 있습니다")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setLights(0xFF00D4FF.toInt(), 1000, 500)
            .setDefaults(0) // 기본값 제거하고 수동 설정
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM))
            .setOnlyAlertOnce(false) // 반복 알림 허용
            .addAction(
                R.drawable.ic_alarm,
                "해제",
                dismissPendingIntent
            )
            .apply {
                if (alarm.snoozeEnabled) {
                    addAction(
                        R.drawable.ic_timer,
                        "스누즈 (${alarm.snoozeInterval}분)",
                        snoozePendingIntent
                    )
                }
            }
            .build()
        
        // 풀스크린 강제 실행을 위한 플래그 추가
        notification.flags = notification.flags or 
                android.app.Notification.FLAG_INSISTENT or // 계속 알림
                android.app.Notification.FLAG_NO_CLEAR or  // 삭제 방지
                android.app.Notification.FLAG_SHOW_LIGHTS // LED 표시
        
        notificationManager.notify(
            NotificationAlarmManager.NOTIFICATION_ID_BASE + alarm.id,
            notification
        )
        
        Log.d(TAG, "🐱 High-priority alarm notification with fullscreen intent displayed: ${alarm.label}")
        
        // Android 14+ 추가 디버그 로그
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val canUseFullScreen = notificationManager.canUseFullScreenIntent()
            Log.d(TAG, "📱 Full Screen Intent Permission Status: $canUseFullScreen")
            if (!canUseFullScreen) {
                Log.w(TAG, "⚠️ Full Screen Intent permission not granted! Please enable it in Settings.")
            }
        }
    }
    
    /**
     * 알람음 재생
     */
    private fun playAlarmSound(context: Context, alarm: Alarm) {
        // 볼륨이 0이면 소리를 재생하지 않음 (진동만 모드)
        if (alarm.volume == 0.0f) {
            Log.d(TAG, "🔇 Volume is 0% - skipping sound playback (vibration only mode)")
            return
        }
        
        try {
            // 기존 MediaPlayer 정리
            activeMediaPlayers[alarm.id]?.let { existingPlayer ->
                try {
                    if (existingPlayer.isPlaying) {
                        existingPlayer.stop()
                    }
                    existingPlayer.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing existing MediaPlayer", e)
                }
            }
            
            val soundUri = if (alarm.soundUri.isNotEmpty()) {
                android.net.Uri.parse(alarm.soundUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            
            Log.d(TAG, "🔊 Playing alarm sound with volume: ${alarm.volume}")
            
            val mediaPlayer = MediaPlayer().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setDataSource(context.createAttributionContext("alarm_sound"), soundUri)
                } else {
                    setDataSource(context, soundUri)
                }
                
                // 먼저 오디오 속성 설정
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
                
                isLooping = true // 계속 반복 재생 (알람이 확실히 들리도록)
                prepare()
                
                // prepare() 후에 볼륨 설정 (더 확실한 적용을 위해)
                setVolume(alarm.volume, alarm.volume)
                Log.d(TAG, "🔊 MediaPlayer volume set to: ${alarm.volume} (${(alarm.volume * 100).toInt()}%)")
                
                start()
                
                // 완료 리스너 - 루핑이므로 일반적으로 호출되지 않음
                setOnCompletionListener { mp ->
                    Log.d(TAG, "MediaPlayer completed for alarm ${alarm.id}")
                    activeMediaPlayers.remove(alarm.id)
                    mp.release()
                }
            }
            
            // MediaPlayer를 저장하여 나중에 중지할 수 있도록 함
            activeMediaPlayers[alarm.id] = mediaPlayer
            activeMediaPlayer = mediaPlayer
            
            Log.d(TAG, "🔊 Alarm sound started and stored for alarm ${alarm.id}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play alarm sound", e)
        }
    }
    
    /**
     * 진동 시작
     */
    private fun startVibration(context: Context, alarm: Alarm) {
        // 진동 조건: vibrationEnabled이거나 볼륨이 0%인 경우
        val shouldVibrate = alarm.vibrationEnabled || alarm.volume == 0.0f
        
        if (!shouldVibrate) {
            Log.d(TAG, "Vibration disabled - vibrationEnabled: ${alarm.vibrationEnabled}, volume: ${alarm.volume}")
            return
        }
        
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            // 더 긴 진동 패턴으로 알람 진동 강화
            val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, 0), // 무한 반복 진동
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0) // 무한 반복
            }
            
            Log.d(TAG, "🔊 Vibration started - vibrationEnabled: ${alarm.vibrationEnabled}, volume: ${alarm.volume}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibration", e)
        }
    }
    
    /**
     * 진동 중지
     */
    private fun stopVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            vibrator.cancel()
            Log.d(TAG, "Vibration stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop vibration", e)
        }
    }
    
    /**
     * 🔥 화면 켜기 및 풀스크린 알람 강제 실행
     */
    private fun wakeUpScreenAndStartFullScreen(context: Context, alarm: Alarm) {
        try {
            Log.d(TAG, "🔥 Attempting to wake up screen and start fullscreen alarm")
            
            // 1. 화면 켜기 - 더 강력한 WakeLock
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or 
                PowerManager.ACQUIRE_CAUSES_WAKEUP or 
                PowerManager.ON_AFTER_RELEASE,
                "SemoAlarm:FullWakeUp"
            )
            
            wakeLock.acquire(10000) // 10초 동안 화면 유지
            Log.d(TAG, "🔥 WakeLock acquired - screen should turn on")
            
            // 2. 짧은 지연 후 풀스크린 액티비티 실행 (화면이 완전히 켜진 후)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val fullScreenIntent = Intent(context, AlarmFullScreenActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or  // CLEAR_TASK로 변경
                                Intent.FLAG_ACTIVITY_NO_ANIMATION or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        putExtra(AlarmFullScreenActivity.EXTRA_ALARM, alarm)
                        putExtra(AlarmFullScreenActivity.EXTRA_ALARM_ID, alarm.id)
                        putExtra("WAKE_UP_SCREEN", true) // 추가 플래그
                    }
                    
                    context.startActivity(fullScreenIntent)
                    Log.d(TAG, "🔥 AlarmFullScreenActivity started with enhanced flags!")
                    
                } catch (activityException: Exception) {
                    Log.e(TAG, "🚫 Failed to start AlarmFullScreenActivity", activityException)
                    
                    // 백업: MainActivity 실행
                    try {
                        val backupIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("SHOW_ALARM_NOTIFICATION", alarm.id)
                        }
                        context.startActivity(backupIntent)
                        Log.d(TAG, "🔄 Backup: MainActivity started instead")
                    } catch (backupException: Exception) {
                        Log.e(TAG, "🚫 Even backup MainActivity failed!", backupException)
                    }
                }
            }, 500) // 500ms 지연
            
            // 10초 후 WakeLock 해제
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (wakeLock.isHeld) {
                    wakeLock.release()
                    Log.d(TAG, "🔥 WakeLock released after 10 seconds")
                }
            }, 10000)
            
        } catch (e: Exception) {
            Log.e(TAG, "🚫 Failed to wake up screen and start fullscreen", e)
            
            // 최후의 수단: 알림만으로라도 사용자 알림
            Log.w(TAG, "⚠️ Fallback: Relying on high-priority notification only")
        }
    }
}