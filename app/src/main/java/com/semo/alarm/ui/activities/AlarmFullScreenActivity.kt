package com.semo.alarm.ui.activities

import android.animation.ValueAnimator
import android.content.Intent
import android.os.*
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.semo.alarm.R
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.services.AlarmService
import com.semo.alarm.receivers.AlarmNotificationReceiver
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AlarmFullScreenActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "AlarmFullScreenActivity"
        const val EXTRA_ALARM = "extra_alarm"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
    
    // UI 컴포넌트
    private lateinit var timeDisplay: TextView
    private lateinit var alarmLabel: TextView
    private lateinit var merryCharacter: ImageView
    private lateinit var dismissButton: Button
    private lateinit var snoozeButton: Button
    private lateinit var stopButton: Button
    
    // 애니메이션 관련
    private var frameAnimator: ValueAnimator? = null
    private var phaseTimer: Handler = Handler(Looper.getMainLooper())
    private var currentPhase: AnimationPhase = AnimationPhase.APPEARING
    private var currentFrameIndex: Int = 0
    
    // 알람 데이터
    private var currentAlarm: Alarm? = null
    
    // 애니메이션 단계
    enum class AnimationPhase(val frames: List<String>, val duration: Long) {
        APPEARING(listOf("character_merry_idle_01"), 2000L), // 임시로 idle_01 사용
        IDLE(listOf(
            "character_merry_idle_01", 
            "character_merry_idle_02", 
            "character_merry_idle_03", 
            "character_merry_idle_04"
        ), 4000L),
        ATTENTION(listOf(
            "attention_01", 
            "attention_02", 
            "attention_03"
        ), 3000L),
        URGENT(listOf(
            "urgent_01", 
            "urgent_02", 
            "urgent_03", 
            "urgent_04"
        ), 2000L)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_fullscreen)
        
        android.util.Log.d(TAG, "🔥 AlarmFullScreenActivity.onCreate() started!")
        
        setupFullScreenMode()
        
        // 특별 처리: 화면 깨우기 시나리오
        if (intent.getBooleanExtra("WAKE_UP_SCREEN", false)) {
            android.util.Log.d(TAG, "🔥 Wake-up scenario detected - ensuring screen is active")
            ensureScreenIsActive()
        }
        
        initializeViews()
        loadAlarmData()
        setupButtonListeners()
        startMerryAnimation()
        
        android.util.Log.d(TAG, "🔥 AlarmFullScreenActivity.onCreate() completed!")
    }
    
    private fun setupFullScreenMode() {
        // 풀스크린 모드 설정
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        
        // Status Bar, Navigation Bar 숨기기
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // 잠금 화면 위에 표시
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        // 화면 켜짐 유지
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    /**
     * 화면이 확실히 활성화되도록 보장 (알람 시나리오용)
     */
    private fun ensureScreenIsActive() {
        try {
            android.util.Log.d(TAG, "🔥 Ensuring screen is fully active...")
            
            // 추가 window flags 설정
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            
            // PowerManager를 통한 추가 화면 켜기
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                if (!powerManager.isInteractive) {
                    android.util.Log.d(TAG, "🔥 Screen is not interactive - attempting to activate")
                    val wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "SemoAlarm:ActivityWakeUp"
                    )
                    wakeLock.acquire(3000) // 3초 동안
                    
                    // 3초 후 해제
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (wakeLock.isHeld) {
                            wakeLock.release()
                            android.util.Log.d(TAG, "🔥 Activity WakeLock released")
                        }
                    }, 3000)
                }
            }
            
            android.util.Log.d(TAG, "✅ Screen activation measures applied")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "🚫 Failed to ensure screen is active", e)
        }
    }
    
    private fun initializeViews() {
        timeDisplay = findViewById(R.id.tv_alarm_time)
        alarmLabel = findViewById(R.id.tv_alarm_label)
        merryCharacter = findViewById(R.id.iv_merry_character)
        dismissButton = findViewById(R.id.btn_dismiss)
        snoozeButton = findViewById(R.id.btn_snooze)
        stopButton = findViewById(R.id.btn_stop)
        
        // 현재 시간 표시
        updateTimeDisplay()
    }
    
    private fun loadAlarmData() {
        currentAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_ALARM, Alarm::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ALARM)
        }
        
        currentAlarm?.let { alarm ->
            alarmLabel.text = alarm.label.ifEmpty { "알람" }
            
            // 스누즈 버튼 표시 여부
            if (!alarm.snoozeEnabled) {
                snoozeButton.visibility = View.GONE
            }
        }
    }
    
    private fun setupButtonListeners() {
        dismissButton.setOnClickListener {
            handleDismissAlarm()
        }
        
        snoozeButton.setOnClickListener {
            handleSnoozeAlarm()
        }
        
        stopButton.setOnClickListener {
            handleStopAlarm()
        }
    }
    
    private fun startMerryAnimation() {
        // 등장 애니메이션 시작
        currentPhase = AnimationPhase.APPEARING
        playAnimationPhase(currentPhase)
        
        // 15초 후 관심 끌기 모드
        phaseTimer.postDelayed({
            if (currentPhase == AnimationPhase.IDLE) {
                currentPhase = AnimationPhase.ATTENTION
                playAnimationPhase(currentPhase)
            }
        }, 15000L)
        
        // 30초 후 독촉 모드
        phaseTimer.postDelayed({
            if (currentPhase != AnimationPhase.URGENT) {
                currentPhase = AnimationPhase.URGENT
                playAnimationPhase(currentPhase)
            }
        }, 30000L)
    }
    
    private fun playAnimationPhase(phase: AnimationPhase) {
        frameAnimator?.cancel()
        
        val frames = phase.frames
        val frameDuration = phase.duration / frames.size
        
        currentFrameIndex = 0
        
        frameAnimator = ValueAnimator.ofInt(0, frames.size - 1).apply {
            duration = phase.duration
            repeatCount = if (phase == AnimationPhase.APPEARING) 0 else ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            
            addUpdateListener { animator ->
                val frameIndex = animator.animatedValue as Int
                if (frameIndex < frames.size) {
                    val frameResName = frames[frameIndex]
                    updateMerryFrame(frameResName)
                }
            }
            
            start()
        }
        
        // APPEARING 완료 후 IDLE로 전환
        if (phase == AnimationPhase.APPEARING) {
            phaseTimer.postDelayed({
                currentPhase = AnimationPhase.IDLE
                playAnimationPhase(currentPhase)
            }, phase.duration)
        }
    }
    
    private fun updateMerryFrame(frameResName: String) {
        // drawable 리소스에서 프레임 로드
        val resId = try {
            resources.getIdentifier(frameResName, "drawable", packageName)
        } catch (e: Exception) {
            R.drawable.character_merry_idle_01 // 기본 이미지
        }
        
        if (resId != 0) {
            merryCharacter.setImageResource(resId)
        }
    }
    
    private fun updateTimeDisplay() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        timeDisplay.text = currentTime
    }
    
    private fun handleDismissAlarm() {
        stopMerryAnimation()
        
        // AlarmNotificationReceiver에 해제 신호 전송
        currentAlarm?.let { alarm ->
            val dismissIntent = Intent(this, AlarmNotificationReceiver::class.java).apply {
                action = "action_dismiss_notification"
                putExtra("alarm_id", alarm.id)
            }
            sendBroadcast(dismissIntent)
        }
        
        finish()
    }
    
    private fun handleSnoozeAlarm() {
        currentAlarm?.let { alarm ->
            if (alarm.snoozeEnabled) {
                stopMerryAnimation()
                
                // AlarmNotificationReceiver에 스누즈 신호 전송
                val snoozeIntent = Intent(this, AlarmNotificationReceiver::class.java).apply {
                    action = "action_snooze_notification"
                    putExtra("alarm_id", alarm.id)
                    putExtra("alarm", alarm)
                }
                sendBroadcast(snoozeIntent)
                
                finish()
            }
        }
    }
    
    private fun handleStopAlarm() {
        stopMerryAnimation()
        
        // AlarmService 완전 정지
        val stopIntent = Intent(this, AlarmService::class.java)
        stopService(stopIntent)
        
        finish()
    }
    
    private fun stopMerryAnimation() {
        frameAnimator?.cancel()
        phaseTimer.removeCallbacksAndMessages(null)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopMerryAnimation()
    }
    
    override fun onBackPressed() {
        // 뒷 버튼 무시 (알람 해제 방지)
        // 의도적으로 super.onBackPressed() 호출하지 않음
    }
}