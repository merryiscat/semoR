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
import androidx.core.app.NotificationCompat
import com.semo.alarm.R
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.ui.activities.MainActivity
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
            showAlarmNotification(context, alarm)
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
     * 알람 알림 표시
     */
    private fun showAlarmNotification(context: Context, alarm: Alarm) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 메인 액티비티로 이동하는 인텐트
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
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
        
        // 알림 생성
        val notification = NotificationCompat.Builder(context, NotificationAlarmManager.ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(alarm.label.ifEmpty { "🔔 알람" })
            .setContentText("${alarm.time} - 터치하여 확인")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(mainPendingIntent, true)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
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
        
        notificationManager.notify(
            NotificationAlarmManager.NOTIFICATION_ID_BASE + alarm.id,
            notification
        )
        
        Log.d(TAG, "Alarm notification displayed: ${alarm.label}")
    }
    
    /**
     * 알람음 재생
     */
    private fun playAlarmSound(context: Context, alarm: Alarm) {
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
        if (!alarm.vibrationEnabled) return
        
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, -1), // 한 번만 진동
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
            
            Log.d(TAG, "Vibration started")
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
}