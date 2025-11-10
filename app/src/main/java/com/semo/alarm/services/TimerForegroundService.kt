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
 * 여러 타이머를 동시에 관리하는 다중 타이머 시스템
 */
@AndroidEntryPoint
class TimerForegroundService : Service() {
    
    @Inject
    lateinit var timerRepository: TimerRepository
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "TimerForegroundService"
        private const val NOTIFICATION_ID_BASE = 2000 // 기본 ID
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
        
        /**
         * 타이머 ID 기반 고유 Notification ID 생성
         * 각 타이머마다 고유한 알림 ID를 사용하여 중첩 방지
         */
        fun getNotificationId(timerId: Int): Int = NOTIFICATION_ID_BASE + timerId
    }
    
    /**
     * 타이머 정보를 담는 데이터 클래스
     */
    data class TimerInfo(
        val timerId: Int,
        val timerName: String,
        var remainingSeconds: Int,
        var isRunning: Boolean,
        var countDownTimer: CountDownTimer? = null
    )
    
    // 여러 타이머를 동시에 관리하는 Map (Key: timerId, Value: TimerInfo)
    private val activeTimers = mutableMapOf<Int, TimerInfo>()

    // 알림 업데이트 전용 타이머 (모든 타이머 알림을 동시에 업데이트)
    private var notificationUpdateTimer: CountDownTimer? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TimerForegroundService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val timerId = intent?.getIntExtra(EXTRA_TIMER_ID, 0) ?: 0
        
        Log.d(TAG, "onStartCommand: action = $action, timerId = $timerId")
        
        when (action) {
            ACTION_START_TIMER -> {
                val timerName = intent.getStringExtra(EXTRA_TIMER_NAME) ?: "타이머"
                val durationSeconds = intent.getIntExtra(EXTRA_TIMER_DURATION, 0)

                Log.d(TAG, "🚀 Starting timer: ID=$timerId, name=$timerName, duration=${durationSeconds}s")
                startTimer(timerId, timerName, durationSeconds)
            }
            ACTION_PAUSE_TIMER -> {
                Log.d(TAG, "⏸️ Pausing timer: ID=$timerId")
                pauseTimer(timerId)
            }
            ACTION_STOP_TIMER -> {
                Log.d(TAG, "⏹️ Stopping timer: ID=$timerId")
                stopTimer(timerId)
            }
            ACTION_TIMER_COMPLETE -> {
                Log.d(TAG, "✅ Timer completed: ID=$timerId")
                onTimerComplete(timerId)
            }
            ACTION_ADD_TIME -> {
                val addSeconds = intent.getIntExtra(EXTRA_ADD_SECONDS, 0)
                Log.d(TAG, "➕ Adding $addSeconds seconds to timer: ID=$timerId")
                addTime(timerId, addSeconds)
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
    
    private fun startTimer(timerId: Int, timerName: String, durationSeconds: Int) {
        // 기존 타이머가 있으면 취소
        activeTimers[timerId]?.countDownTimer?.cancel()
        
        val timerInfo = TimerInfo(
            timerId = timerId,
            timerName = timerName,
            remainingSeconds = durationSeconds,
            isRunning = true
        )
        
        // DB에 타이머 실행 상태 업데이트
        serviceScope.launch {
            try {
                timerRepository.updateTimerState(timerId, isRunning = true, remainingSeconds = durationSeconds)
                Log.d(TAG, "✅ Timer state saved to DB: ID=$timerId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update timer state on start: ${e.message}")
            }
        }
        
        // Foreground notification 시작 (첫 번째 타이머만)
        if (activeTimers.isEmpty()) {
            // Foreground Service는 빈 알림으로 시작 (실제 타이머 알림은 별도로 표시)
            startForeground(NOTIFICATION_ID_BASE, createSummaryNotification())
        }

        // 개별 타이머 알림 표시 (Foreground와 별개)
        showIndividualNotification(timerInfo)
        
        // CountDownTimer 시작
        timerInfo.countDownTimer = object : CountDownTimer(durationSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timerInfo.remainingSeconds = (millisUntilFinished / 1000).toInt()

                // UI에 상태 브로드캐스트 전송
                sendTimerUpdateBroadcast(timerInfo)

                // 개별 알림 업데이트 (setOnlyAlertOnce로 조용히 갱신)
                updateIndividualNotification(timerInfo)

                Log.d(TAG, "⏱️ Timer $timerId tick: ${formatTime(timerInfo.remainingSeconds)}")
            }

            override fun onFinish() {
                Log.d(TAG, "⏰ Timer $timerId finished")
                onTimerComplete(timerId)
            }
        }
        
        timerInfo.countDownTimer?.start()
        activeTimers[timerId] = timerInfo
        
        Log.d(TAG, "📊 Active timers count: ${activeTimers.size}")
    }
    
    private fun pauseTimer(timerId: Int) {
        val timerInfo = activeTimers[timerId] ?: run {
            Log.w(TAG, "⚠️ Timer $timerId not found for pause")
            return
        }
        
        timerInfo.countDownTimer?.cancel()
        timerInfo.isRunning = false

        // DB에 일시정지 상태 및 현재 남은 시간 저장
        serviceScope.launch {
            try {
                timerRepository.updateTimerState(timerId, isRunning = false, remainingSeconds = timerInfo.remainingSeconds)
                Log.d(TAG, "✅ Timer paused: ID=$timerId, remainingSeconds=${timerInfo.remainingSeconds}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update timer state on pause: ${e.message}")
            }
        }

        // UI에 일시정지 브로드캐스트
        sendTimerUpdateBroadcast(timerInfo)

        // 알림 업데이트
        updateIndividualNotification(timerInfo)
    }

    private fun addTime(timerId: Int, secondsToAdd: Int) {
        val timerInfo = activeTimers[timerId] ?: run {
            Log.w(TAG, "⚠️ Timer $timerId not found for add time")
            return
        }
        
        if (!timerInfo.isRunning) {
            Log.w(TAG, "⚠️ Cannot add time - timer $timerId is not running")
            return
        }

        // 기존 타이머 취소
        timerInfo.countDownTimer?.cancel()

        // 시간 추가
        timerInfo.remainingSeconds += secondsToAdd

        Log.d(TAG, "⏱️ Added $secondsToAdd seconds to timer $timerId. New remaining: ${timerInfo.remainingSeconds}s")

        // DB에 업데이트된 시간 저장
        serviceScope.launch {
            try {
                timerRepository.updateTimerState(timerId, isRunning = true, remainingSeconds = timerInfo.remainingSeconds)
                Log.d(TAG, "✅ Timer state updated with added time: ID=$timerId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update timer state after adding time: ${e.message}")
            }
        }

        // 새로운 시간으로 타이머 재시작
        timerInfo.countDownTimer = object : CountDownTimer(timerInfo.remainingSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timerInfo.remainingSeconds = (millisUntilFinished / 1000).toInt()

                // UI에 상태 브로드캐스트 전송
                sendTimerUpdateBroadcast(timerInfo)

                // 개별 알림 업데이트 (setOnlyAlertOnce로 조용히 갱신)
                updateIndividualNotification(timerInfo)

                Log.d(TAG, "⏱️ Timer $timerId tick: ${formatTime(timerInfo.remainingSeconds)}")
            }

            override fun onFinish() {
                Log.d(TAG, "⏰ Timer $timerId finished")
                onTimerComplete(timerId)
            }
        }

        timerInfo.countDownTimer?.start()

        // UI에 업데이트 브로드캐스트
        sendTimerUpdateBroadcast(timerInfo)

        // 알림 업데이트
        updateIndividualNotification(timerInfo)
    }
    
    private fun stopTimer(timerId: Int) {
        val timerInfo = activeTimers[timerId] ?: run {
            Log.w(TAG, "⚠️ Timer $timerId not found for stop")
            return
        }
        
        timerInfo.countDownTimer?.cancel()
        timerInfo.isRunning = false
        timerInfo.remainingSeconds = 0
        
        // DB에 타이머 중지 상태 업데이트 (리셋)
        serviceScope.launch {
            try {
                timerRepository.updateTimerState(timerId, isRunning = false, remainingSeconds = 0)
                Log.d(TAG, "✅ Timer stopped/reset: ID=$timerId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update timer state on stop: ${e.message}")
            }
        }
        
        // UI에 중지 브로드캐스트
        sendTimerStoppedBroadcast(timerId)
        
        // 개별 알림 제거
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(getNotificationId(timerId))

        // 타이머 제거
        activeTimers.remove(timerId)

        // 모든 타이머가 중지되면 서비스 종료
        if (activeTimers.isEmpty()) {
            Log.d(TAG, "🛑 All timers stopped, stopping service")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            Log.d(TAG, "📊 Remaining active timers: ${activeTimers.size}")
        }
    }
    
    private fun onTimerComplete(timerId: Int) {
        val timerInfo = activeTimers[timerId] ?: run {
            Log.w(TAG, "⚠️ Timer $timerId not found for completion")
            return
        }
        
        timerInfo.countDownTimer?.cancel()
        timerInfo.isRunning = false
        timerInfo.remainingSeconds = 0
        
        // DB에 타이머 완료 상태 업데이트
        serviceScope.launch {
            try {
                timerRepository.updateTimerState(timerId, isRunning = false, remainingSeconds = 0)
                Log.d(TAG, "✅ Timer completed: ID=$timerId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update timer state on complete: ${e.message}")
            }
        }
        
        // UI에 완료 브로드캐스트
        sendTimerCompleteBroadcast(timerId)
        
        // 타이머 완료 알림 표시
        val notificationAlarmManager = NotificationAlarmManager(this)
        notificationAlarmManager.showTimerCompleteNotification(timerInfo.timerName)

        // 타이머 제거
        activeTimers.remove(timerId)

        // 고유 Notification ID 사용하여 진행 중 알림 제거
        val notificationId = getNotificationId(timerId)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        // 모든 타이머가 완료되면 서비스 종료
        if (activeTimers.isEmpty()) {
            Log.d(TAG, "✅ All timers completed, stopping service")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            Log.d(TAG, "📊 Remaining active timers: ${activeTimers.size}")
        }
        
        Log.d(TAG, "⏰ Timer '${timerInfo.timerName}' completed successfully")
    }
    
    /**
     * UI에 타이머 상태 업데이트 브로드캐스트
     */
    private fun sendTimerUpdateBroadcast(timerInfo: TimerInfo) {
        val intent = Intent(BROADCAST_TIMER_UPDATE).apply {
            putExtra(EXTRA_TIMER_ID, timerInfo.timerId)
            putExtra(EXTRA_REMAINING_SECONDS, timerInfo.remainingSeconds)
            putExtra(EXTRA_IS_RUNNING, timerInfo.isRunning)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Log.d(TAG, "📡 브로드캐스트 전송: ID=${timerInfo.timerId}, ${timerInfo.remainingSeconds}초")
    }
    
    /**
     * UI에 타이머 완료 브로드캐스트
     */
    private fun sendTimerCompleteBroadcast(timerId: Int) {
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
    private fun sendTimerStoppedBroadcast(timerId: Int) {
        val intent = Intent(BROADCAST_TIMER_STOPPED).apply {
            putExtra(EXTRA_TIMER_ID, timerId)
            putExtra(EXTRA_REMAINING_SECONDS, 0)
            putExtra(EXTRA_IS_RUNNING, false)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    /**
     * Foreground Service를 위한 요약 알림 생성
     * (첫 번째 타이머 시작 시 Foreground Service에 필요한 알림)
     */
    private fun createSummaryNotification(): android.app.Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("타이머 실행 중")
            .setContentText("백그라운드에서 타이머가 실행되고 있습니다")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .setContentIntent(mainPendingIntent)
            .build()
    }
    /**
     * 개별 타이머의 알림 생성
     */
    private fun createIndividualNotification(timerInfo: TimerInfo): android.app.Notification {
        // 메인 액티비티로 이동하는 인텐트
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, timerInfo.timerId, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // 중지 버튼 인텐트
        val stopIntent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_STOP_TIMER
            putExtra(EXTRA_TIMER_ID, timerInfo.timerId)
        }
        val stopPendingIntent = PendingIntent.getService(
            this, timerInfo.timerId, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val title = "${timerInfo.timerName} 실행 중"
        val text = if (timerInfo.isRunning) {
            "남은 시간: ${formatTime(timerInfo.remainingSeconds)}"
        } else {
            "일시정지"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)  // 🔑 핵심: 업데이트 시 조용히 갱신
            .setShowWhen(false)  // 타임스탬프 표시 안 함
            .setSilent(true)  // 조용한 업데이트
            .setSortKey(String.format("%04d", timerInfo.timerId))  // 🔑 타이머 ID 순서로 정렬
            .setWhen(System.currentTimeMillis() - (timerInfo.timerId * 1000L))  // 🔑 생성 시간 고정
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_alarm, "중지", stopPendingIntent)
            .build()
    }
    
    /**
     * 개별 타이머의 알림을 표시 (Foreground Service가 아닌 일반 알림)
     */
    private fun showIndividualNotification(timerInfo: TimerInfo) {
        val notification = createIndividualNotification(timerInfo)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(getNotificationId(timerInfo.timerId), notification)
        Log.d(TAG, "🔔 Individual notification shown for timer ${timerInfo.timerId}")
    }

    /**
     * 개별 타이머의 알림 업데이트
     */
    private fun updateIndividualNotification(timerInfo: TimerInfo) {
        val notification = createIndividualNotification(timerInfo)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(getNotificationId(timerInfo.timerId), notification)
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
        // 모든 타이머 취소
        activeTimers.values.forEach { it.countDownTimer?.cancel() }
        activeTimers.clear()

        Log.d(TAG, "🛑 TimerForegroundService destroyed")
    }
}