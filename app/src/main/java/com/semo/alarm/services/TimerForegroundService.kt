package com.semo.alarm.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.semo.alarm.R
import com.semo.alarm.data.repositories.TimerRepository
import com.semo.alarm.ui.activities.MainActivity
import com.semo.alarm.utils.NotificationAlarmManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 타이머 백그라운드 실행을 위한 Foreground Service
 * 앱이 종료되어도 타이머가 계속 실행되도록 함
 */
@AndroidEntryPoint
class TimerForegroundService : Service() {
    
    @Inject
    lateinit var timerRepository: TimerRepository
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "TimerForegroundService"
        private const val NOTIFICATION_ID = 2000
        private const val CHANNEL_ID = "timer_foreground_channel"
        
        // Service actions
        const val ACTION_START_TIMER = "START_TIMER"
        const val ACTION_PAUSE_TIMER = "PAUSE_TIMER"
        const val ACTION_STOP_TIMER = "STOP_TIMER"
        const val ACTION_TIMER_COMPLETE = "TIMER_COMPLETE"
        const val ACTION_ADD_TIME = "ADD_TIME"

        // Intent extras
        const val EXTRA_TIMER_NAME = "timer_name"
        const val EXTRA_TIMER_DURATION = "timer_duration"
        const val EXTRA_TIMER_ID = "timer_id"
        const val EXTRA_ADD_SECONDS = "add_seconds"
        
        // Broadcast actions for UI updates
        const val BROADCAST_TIMER_UPDATE = "com.semo.alarm.TIMER_UPDATE"
        const val BROADCAST_TIMER_COMPLETE = "com.semo.alarm.TIMER_COMPLETE"
        const val BROADCAST_TIMER_STOPPED = "com.semo.alarm.TIMER_STOPPED"
        
        // Broadcast extras
        const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
        const val EXTRA_IS_RUNNING = "is_running"
    }
    
    private var countDownTimer: CountDownTimer? = null
    private var timerName: String = "타이머"
    private var timerId: Int = 0
    private var remainingSeconds: Int = 0
    private var isTimerRunning: Boolean = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TimerForegroundService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action = ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_TIMER -> {
                timerName = intent.getStringExtra(EXTRA_TIMER_NAME) ?: "타이머"
                val durationSeconds = intent.getIntExtra(EXTRA_TIMER_DURATION, 0)
                timerId = intent.getIntExtra(EXTRA_TIMER_ID, 0)

                Log.d(TAG, "Starting timer: $timerName for $durationSeconds seconds")
                startTimer(durationSeconds)
            }
            ACTION_PAUSE_TIMER -> {
                Log.d(TAG, "Pausing timer: $timerName")
                pauseTimer()
            }
            ACTION_STOP_TIMER -> {
                Log.d(TAG, "Stopping timer: $timerName")
                stopTimer()
            }
            ACTION_TIMER_COMPLETE -> {
                Log.d(TAG, "Timer completed: $timerName")
                onTimerComplete()
            }
            ACTION_ADD_TIME -> {
                val addSeconds = intent.getIntExtra(EXTRA_ADD_SECONDS, 0)
                timerId = intent.getIntExtra(EXTRA_TIMER_ID, 0)
                Log.d(TAG, "Adding $addSeconds seconds to timer: $timerName")
                addTime(addSeconds)
            }
        }
        
        return START_STICKY // 시스템이 서비스를 종료해도 재시작
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "타이머 실행 중",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "백그라운드에서 실행 중인 타이머"
                setShowBadge(false)
                setSound(null, null) // 조용한 알림
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startTimer(durationSeconds: Int) {
        remainingSeconds = durationSeconds
        isTimerRunning = true
        
        // DB에 타이머 실행 상태 업데이트
        serviceScope.launch {
            try {
                timerRepository.updateTimerState(timerId, isRunning = true, remainingSeconds = remainingSeconds)
                Log.d(TAG, "Timer started: ID=$timerId, durationSeconds=$durationSeconds")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update timer state on start: ${e.message}")
            }
        }
        
        // Foreground notification 시작
        startForeground(NOTIFICATION_ID, createTimerNotification())
        
        // CountDownTimer 시작
        countDownTimer = object : CountDownTimer(durationSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()
                
                // UI에 상태 브로드캐스트
                sendTimerUpdateBroadcast()
                
                // 알림 업데이트
                updateNotification()
                
                Log.d(TAG, "Timer tick: ${formatTime(remainingSeconds)}")
            }
            
            override fun onFinish() {
                Log.d(TAG, "Timer finished: $timerName")
                onTimerComplete()
            }
        }
        
        countDownTimer?.start()
    }
    
    private fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false

        // DB에 일시정지 상태 및 현재 남은 시간 저장
        serviceScope.launch {
            try {
                timerRepository.updateTimerState(timerId, isRunning = false, remainingSeconds = remainingSeconds)
                Log.d(TAG, "Timer paused: ID=$timerId, remainingSeconds=$remainingSeconds")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update timer state on pause: ${e.message}")
            }
        }

        // UI에 일시정지 브로드캐스트
        sendTimerUpdateBroadcast()

        // 일시정지 상태 알림 업데이트
        updateNotification()
    }

    /**
     * 타이머에 시간을 추가합니다 (실행 중일 때만 작동)
     */
    private fun addTime(secondsToAdd: Int) {
        if (!isTimerRunning) {
            Log.w(TAG, "Cannot add time - timer is not running")
            return
        }

        // 기존 타이머 취소
        countDownTimer?.cancel()

        // 시간 추가
        remainingSeconds += secondsToAdd

        Log.d(TAG, "⏱️ Added $secondsToAdd seconds. New remaining time: $remainingSeconds seconds")

        // DB에 업데이트된 시간 저장
        serviceScope.launch {
            try {
                timerRepository.updateTimerState(timerId, isRunning = true, remainingSeconds = remainingSeconds)
                Log.d(TAG, "Timer state updated with added time: ID=$timerId, remainingSeconds=$remainingSeconds")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update timer state after adding time: ${e.message}")
            }
        }

        // 새로운 시간으로 타이머 재시작
        countDownTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()

                // UI에 상태 브로드캐스트
                sendTimerUpdateBroadcast()

                // 알림 업데이트
                updateNotification()

                Log.d(TAG, "Timer tick: ${formatTime(remainingSeconds)}")
            }

            override fun onFinish() {
                Log.d(TAG, "Timer finished: $timerName")
                onTimerComplete()
            }
        }

        countDownTimer?.start()

        // UI에 업데이트 브로드캐스트
        sendTimerUpdateBroadcast()
        updateNotification()
    }
    
    private fun stopTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        remainingSeconds = 0
        
        // DB에 타이머 중지 상태 업데이트 (리셋)
        serviceScope.launch {
            try {
                timerRepository.updateTimerState(timerId, isRunning = false, remainingSeconds = 0)
                Log.d(TAG, "Timer stopped/reset: ID=$timerId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update timer state on stop: ${e.message}")
            }
        }
        
        // UI에 중지 브로드캐스트
        sendTimerStoppedBroadcast()
        
        // 서비스 종료
        stopForeground(true)
        stopSelf()
    }
    
    private fun onTimerComplete() {
        countDownTimer?.cancel()
        isTimerRunning = false
        remainingSeconds = 0
        
        // DB에 타이머 완료 상태 업데이트
        serviceScope.launch {
            try {
                timerRepository.updateTimerState(timerId, isRunning = false, remainingSeconds = 0)
                Log.d(TAG, "Timer completed: ID=$timerId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update timer state on complete: ${e.message}")
            }
        }
        
        // UI에 완료 브로드캐스트
        sendTimerCompleteBroadcast()
        
        // 타이머 완료 알림 표시
        val notificationAlarmManager = NotificationAlarmManager(this)
        notificationAlarmManager.showTimerCompleteNotification(timerName)
        
        // Foreground 서비스 종료
        stopForeground(true)
        stopSelf()
        
        Log.d(TAG, "⏰ Timer '$timerName' completed successfully")
    }
    
    /**
     * UI에 타이머 상태 업데이트 브로드캐스트
     */
    private fun sendTimerUpdateBroadcast() {
        val intent = Intent(BROADCAST_TIMER_UPDATE).apply {
            putExtra(EXTRA_TIMER_ID, timerId)
            putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
            putExtra(EXTRA_IS_RUNNING, isTimerRunning)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Log.d(TAG, "📡 브로드캐스트 전송: ID=$timerId, ${remainingSeconds}초")
    }
    
    /**
     * UI에 타이머 완료 브로드캐스트
     */
    private fun sendTimerCompleteBroadcast() {
        val intent = Intent(BROADCAST_TIMER_COMPLETE).apply {
            putExtra(EXTRA_TIMER_ID, timerId)
            putExtra(EXTRA_REMAINING_SECONDS, 0)
            putExtra(EXTRA_IS_RUNNING, false)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    
    /**
     * UI에 타이머 중지 브로드캐스트
     */
    private fun sendTimerStoppedBroadcast() {
        val intent = Intent(BROADCAST_TIMER_STOPPED).apply {
            putExtra(EXTRA_TIMER_ID, timerId)
            putExtra(EXTRA_REMAINING_SECONDS, 0)
            putExtra(EXTRA_IS_RUNNING, false)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    
    private fun createTimerNotification(): android.app.Notification {
        // 메인 액티비티로 이동하는 인텐트
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        // 타이머 중지 버튼
        val stopIntent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_STOP_TIMER
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        val status = if (isTimerRunning) "실행 중" else "일시정지"
        val timeDisplay = formatTime(remainingSeconds)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("$timerName $status")
            .setContentText("남은 시간: $timeDisplay")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true) // 사용자가 스와이프로 제거할 수 없음
            .setContentIntent(mainPendingIntent)
            .addAction(
                R.drawable.ic_alarm,
                "중지",
                stopPendingIntent
            )
            .build()
    }
    
    private fun updateNotification() {
        val notification = createTimerNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
            else -> String.format("%02d:%02d", minutes, secs)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        Log.d(TAG, "TimerForegroundService destroyed")
    }
}