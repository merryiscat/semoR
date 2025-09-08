package com.semo.alarm.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.semo.alarm.R
import com.semo.alarm.ui.activities.MainActivity
import com.semo.alarm.utils.SnoringDetector
import com.semo.alarm.utils.SnoringEvent
import java.text.SimpleDateFormat
import java.util.*

/**
 * 수면 추적을 위한 Foreground Service
 * 백그라운드에서 수면 시간을 추적하고, 필요시 코골이 감지 기능 제공
 */
class SleepTrackingService : Service() {
    
    companion object {
        private const val TAG = "SleepTrackingService"
        private const val NOTIFICATION_ID = 3000
        private const val CHANNEL_ID = "sleep_tracking_channel"
        
        // Service actions
        const val ACTION_START_SLEEP_TRACKING = "START_SLEEP_TRACKING"
        const val ACTION_STOP_SLEEP_TRACKING = "STOP_SLEEP_TRACKING"
        const val ACTION_UPDATE_SNORING_DATA = "UPDATE_SNORING_DATA"
        
        // Intent extras
        const val EXTRA_SLEEP_RECORD_ID = "sleep_record_id"
        const val EXTRA_SLEEP_START_TIME = "sleep_start_time"
        const val EXTRA_SNORING_ENABLED = "snoring_enabled"
        const val EXTRA_SNORING_DATA = "snoring_data"
        
        // Broadcast actions for UI updates
        const val BROADCAST_SLEEP_UPDATE = "com.semo.alarm.SLEEP_UPDATE"
        const val BROADCAST_SLEEP_STOPPED = "com.semo.alarm.SLEEP_STOPPED"
        const val BROADCAST_SNORING_DETECTED = "com.semo.alarm.SNORING_DETECTED"
        
        // Broadcast extras
        const val EXTRA_ELAPSED_TIME = "elapsed_time"
        const val EXTRA_FORMATTED_TIME = "formatted_time"
        
        fun startSleepTracking(context: Context, sleepRecordId: Long, startTime: Long, snoringEnabled: Boolean = true) {
            val intent = Intent(context, SleepTrackingService::class.java).apply {
                action = ACTION_START_SLEEP_TRACKING
                putExtra(EXTRA_SLEEP_RECORD_ID, sleepRecordId)
                putExtra(EXTRA_SLEEP_START_TIME, startTime)
                putExtra(EXTRA_SNORING_ENABLED, snoringEnabled)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopSleepTracking(context: Context) {
            val intent = Intent(context, SleepTrackingService::class.java).apply {
                action = ACTION_STOP_SLEEP_TRACKING
            }
            context.startService(intent)
        }
    }
    
    private var sleepRecordId: Long = -1
    private var sleepStartTime: Long = 0
    private var isSnoringEnabled: Boolean = true
    private var isTracking: Boolean = false
    
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    
    private var notificationManager: NotificationManager? = null
    private var snoringDetector: SnoringDetector? = null
    private val snoringEvents = mutableListOf<SnoringEvent>()
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        initializeSnoringDetector()
        Log.d(TAG, "SleepTrackingService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_SLEEP_TRACKING -> {
                sleepRecordId = intent.getLongExtra(EXTRA_SLEEP_RECORD_ID, -1)
                sleepStartTime = intent.getLongExtra(EXTRA_SLEEP_START_TIME, System.currentTimeMillis())
                isSnoringEnabled = intent.getBooleanExtra(EXTRA_SNORING_ENABLED, true)
                
                startSleepTracking()
            }
            
            ACTION_STOP_SLEEP_TRACKING -> {
                stopSleepTracking()
            }
            
            ACTION_UPDATE_SNORING_DATA -> {
                val snoringData = intent.getStringExtra(EXTRA_SNORING_DATA) ?: ""
                updateSnoringData(snoringData)
            }
        }
        
        return START_STICKY // 서비스가 종료되면 시스템이 다시 시작
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startSleepTracking() {
        if (isTracking) {
            Log.w(TAG, "Sleep tracking already started")
            return
        }
        
        isTracking = true
        startForeground(NOTIFICATION_ID, createNotification())
        startElapsedTimeUpdates()
        
        // 코골이 감지 시작 (활성화된 경우)
        if (isSnoringEnabled) {
            snoringDetector?.startDetection()
            Log.d(TAG, "Snoring detection started")
        }
        
        Log.d(TAG, "Sleep tracking started: recordId=$sleepRecordId, startTime=$sleepStartTime")
    }
    
    private fun stopSleepTracking() {
        if (!isTracking) {
            Log.w(TAG, "Sleep tracking not started")
            return
        }
        
        isTracking = false
        stopElapsedTimeUpdates()
        
        // 코골이 감지 중지
        snoringDetector?.stopDetection()
        
        // UI에 수면 추적 종료 알림 (코골이 데이터 포함)
        val intent = Intent(BROADCAST_SLEEP_STOPPED).apply {
            putExtra(EXTRA_SLEEP_RECORD_ID, sleepRecordId)
            putExtra(EXTRA_SNORING_DATA, getSnoringDataJson())
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        
        Log.d(TAG, "Sleep tracking stopped: recordId=$sleepRecordId, snoringEvents=${snoringEvents.size}")
        stopSelf()
    }
    
    private fun startElapsedTimeUpdates() {
        stopElapsedTimeUpdates() // 기존 것이 있다면 정리
        
        updateRunnable = object : Runnable {
            override fun run() {
                if (isTracking && sleepStartTime > 0) {
                    val elapsedTime = System.currentTimeMillis() - sleepStartTime
                    val formattedTime = formatElapsedTime(elapsedTime)
                    
                    // 알림 업데이트
                    updateNotification(formattedTime)
                    
                    // UI 업데이트 브로드캐스트
                    val intent = Intent(BROADCAST_SLEEP_UPDATE).apply {
                        putExtra(EXTRA_ELAPSED_TIME, elapsedTime)
                        putExtra(EXTRA_FORMATTED_TIME, formattedTime)
                        putExtra(EXTRA_SLEEP_RECORD_ID, sleepRecordId)
                    }
                    LocalBroadcastManager.getInstance(this@SleepTrackingService).sendBroadcast(intent)
                }
                
                handler.postDelayed(this, 60000) // 1분마다 업데이트
            }
        }
        
        updateRunnable?.let { handler.post(it) }
    }
    
    private fun stopElapsedTimeUpdates() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
    }
    
    private fun updateSnoringData(snoringData: String) {
        if (isTracking) {
            // 코골이 감지 브로드캐스트
            val intent = Intent(BROADCAST_SNORING_DETECTED).apply {
                putExtra(EXTRA_SLEEP_RECORD_ID, sleepRecordId)
                putExtra(EXTRA_SNORING_DATA, snoringData)
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            
            Log.d(TAG, "Snoring data updated: $snoringData")
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "수면 추적",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "수면 추적 상태를 표시합니다"
                setSound(null, null) // 무음
                vibrationPattern = null
            }
            
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("🌙 수면 추적 중")
        .setContentText("수면 시간을 추적하고 있습니다...")
        .setSmallIcon(R.drawable.ic_sleep)
        .setOngoing(true)
        .setSilent(true)
        .setContentIntent(createPendingIntent())
        .addAction(
            R.drawable.ic_stop,
            "기상",
            createStopPendingIntent()
        )
        .build()
    
    private fun updateNotification(elapsedTime: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("😴 수면 중")
            .setContentText("경과 시간: $elapsedTime")
            .setSmallIcon(R.drawable.ic_sleep)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(createPendingIntent())
            .addAction(
                R.drawable.ic_stop,
                "기상",
                createStopPendingIntent()
            )
            .build()
        
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun createStopPendingIntent(): PendingIntent {
        val intent = Intent(this, SleepTrackingService::class.java).apply {
            action = ACTION_STOP_SLEEP_TRACKING
        }
        
        return PendingIntent.getService(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun initializeSnoringDetector() {
        snoringDetector = SnoringDetector(
            context = this,
            onSnoringDetected = { decibelLevel, duration, audioFilePath ->
                val event = SnoringEvent(
                    timestamp = System.currentTimeMillis(),
                    decibelLevel = decibelLevel,
                    duration = duration,
                    audioFilePath = audioFilePath  // 🎙️ 오디오 파일 경로 추가
                )
                snoringEvents.add(event)
                
                // UI에 코골이 감지 알림
                val intent = Intent(BROADCAST_SNORING_DETECTED).apply {
                    putExtra(EXTRA_SLEEP_RECORD_ID, sleepRecordId)
                    putExtra(EXTRA_SNORING_DATA, event.toJson())
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                
                Log.d(TAG, "Snoring event recorded: ${event.toJson()}")
            },
            onError = { errorMessage ->
                Log.e(TAG, "Snoring detection error: $errorMessage")
            }
        )
    }
    
    private fun getSnoringDataJson(): String {
        if (snoringEvents.isEmpty()) return "[]"
        
        val eventsJson = snoringEvents.joinToString(",") { it.toJson() }
        return "[$eventsJson]"
    }
    
    private fun formatElapsedTime(millis: Long): String {
        val hours = (millis / (1000 * 60 * 60)).toInt()
        val minutes = ((millis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        
        return "${hours}시간 ${minutes}분"
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopElapsedTimeUpdates()
        snoringDetector?.cleanup()
        isTracking = false
        Log.d(TAG, "SleepTrackingService destroyed")
    }
}