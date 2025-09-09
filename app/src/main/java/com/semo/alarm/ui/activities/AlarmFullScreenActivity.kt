package com.semo.alarm.ui.activities

import android.content.Intent
import android.os.*
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.semo.alarm.R
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.services.AlarmService
import com.semo.alarm.receivers.AlarmNotificationReceiver
import com.semo.alarm.character.AlarmCharacterView
import com.semo.alarm.character.CharacterAnimationManager
import com.semo.alarm.character.CharacterState
import com.semo.alarm.character.AnimationType
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
    private lateinit var characterView: AlarmCharacterView
    private lateinit var dismissButton: Button
    private lateinit var snoozeButton: Button
    private lateinit var stopButton: Button
    
    // 🐱 메리 캐릭터 애니메이션 시스템
    private lateinit var animationManager: CharacterAnimationManager
    private var phaseTimer: Handler = Handler(Looper.getMainLooper())
    
    // 알람 데이터
    private var currentAlarm: Alarm? = null
    
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
        startAlarmScenario()
        
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
        characterView = findViewById(R.id.alarm_character_view)
        dismissButton = findViewById(R.id.btn_dismiss)
        snoozeButton = findViewById(R.id.btn_snooze)
        stopButton = findViewById(R.id.btn_stop)
        
        // 🐱 메리 캐릭터 애니메이션 매니저 초기화
        animationManager = CharacterAnimationManager(characterView)
        
        // 현재 시간 표시
        updateTimeDisplay()
        
        android.util.Log.d(TAG, "✅ Views initialized with AlarmCharacterView")
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
    
    /**
     * 🚀 알람 시나리오를 시작합니다
     * 등장 → 대기 → 관심 끌기 → 회전 → 독촉 단계로 진행
     */
    private fun startAlarmScenario() {
        android.util.Log.d(TAG, "🚀 Starting alarm scenario with CharacterAnimationManager")
        
        // 등장 애니메이션으로 시작
        characterView.setState(CharacterState.APPEARING)
        characterView.applyBrandHighlight(true) // 네온 블루 효과
        
        // 3초 후 대기 상태로 전환
        phaseTimer.postDelayed({
            characterView.setState(CharacterState.IDLE)
        }, 3000L)
        
        // 15초 후 관심 끌기 모드
        phaseTimer.postDelayed({
            characterView.setState(CharacterState.ATTENTION)
            android.util.Log.d(TAG, "👀 Switching to ATTENTION mode")
        }, 15000L)
        
        // 25초 후 회전 모드 (핵심 기능!)
        phaseTimer.postDelayed({
            characterView.setState(CharacterState.SPINNING)
            android.util.Log.d(TAG, "🌪️ Switching to SPINNING mode - Core feature!")
        }, 25000L)
        
        // 40초 후 독촉 모드
        phaseTimer.postDelayed({
            characterView.setState(CharacterState.URGENT)
            android.util.Log.d(TAG, "🚨 Switching to URGENT mode")
        }, 40000L)
    }
    
    private fun updateTimeDisplay() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        timeDisplay.text = currentTime
    }
    
    private fun handleDismissAlarm() {
        stopAlarmScenario()
        
        // AlarmNotificationReceiver에 해제 신호 전송
        currentAlarm?.let { alarm ->
            val dismissIntent = Intent(this, AlarmNotificationReceiver::class.java).apply {
                action = "action_dismiss_notification"
                putExtra("alarm_id", alarm.id)
            }
            sendBroadcast(dismissIntent)
        }
        
        android.util.Log.d(TAG, "✅ Alarm dismissed")
        finish()
    }
    
    private fun handleSnoozeAlarm() {
        currentAlarm?.let { alarm ->
            if (alarm.snoozeEnabled) {
                stopAlarmScenario()
                
                // 스누즈 효과: 페이드 아웃
                characterView.applyFadeEffect(true)
                
                // AlarmNotificationReceiver에 스누즈 신호 전송
                val snoozeIntent = Intent(this, AlarmNotificationReceiver::class.java).apply {
                    action = "action_snooze_notification"
                    putExtra("alarm_id", alarm.id)
                    putExtra("alarm", alarm)
                }
                sendBroadcast(snoozeIntent)
                
                android.util.Log.d(TAG, "😴 Alarm snoozed")
                finish()
            }
        }
    }
    
    private fun handleStopAlarm() {
        stopAlarmScenario()
        
        // AlarmService 완전 정지
        val stopIntent = Intent(this, AlarmService::class.java)
        stopService(stopIntent)
        
        android.util.Log.d(TAG, "⏹️ Alarm stopped")
        finish()
    }
    
    /**
     * 🛑 알람 시나리오를 중지합니다
     */
    private fun stopAlarmScenario() {
        android.util.Log.d(TAG, "🛑 Stopping alarm scenario")
        
        // 모든 예약된 타이머 취소
        phaseTimer.removeCallbacksAndMessages(null)
        
        // 캐릭터 애니메이션 중지
        characterView.stopAnimation()
        animationManager.cleanup()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarmScenario()
        android.util.Log.d(TAG, "🗑️ AlarmFullScreenActivity destroyed")
    }
    
    override fun onBackPressed() {
        // 뒷 버튼 무시 (알람 해제 방지)
        // 의도적으로 super.onBackPressed() 호출하지 않음
    }
}