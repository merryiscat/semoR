package com.semo.alarm.character

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.semo.alarm.R

/**
 * 세모알 캐릭터 시스템 테스트용 액티비티
 * 
 * 🐱 Merry 캐릭터의 모든 애니메이션과 브랜드 효과를 테스트할 수 있습니다.
 * - 개발자용 히든 기능 (MainActivity에서 제목 롱클릭으로 접근)
 * - 모든 애니메이션 타입 테스트 가능
 * - 브랜드 색상 효과 테스트 가능
 * - 알람 시나리오 전체 흐름 테스트 가능
 */
class CharacterTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "CharacterTestActivity"
    }
    
    private lateinit var characterView: AlarmCharacterView
    private lateinit var animationManager: CharacterAnimationManager
    private lateinit var stateDisplay: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 바인딩 설정 - 임시로 character_test_layout를 사용
        setContentView(R.layout.character_test_layout)
        
        Log.d(TAG, "🐱 CharacterTestActivity created")
        
        initializeViews()
        setupAnimationButtons()
        setupEffectButtons()
        setupScenarioButtons()
    }
    
    private fun initializeViews() {
        characterView = findViewById(R.id.character_merry)
        stateDisplay = findViewById(R.id.tv_character_state)
        animationManager = CharacterAnimationManager(characterView)
        
        // 초기 상태 표시
        updateStateDisplay()
        
        Log.d(TAG, "✅ Views initialized")
    }
    
    private fun setupAnimationButtons() {
        // 등장 애니메이션
        findViewById<Button>(R.id.btn_appearing).setOnClickListener {
            Log.d(TAG, "🎭 Testing APPEARING animation")
            characterView.startAnimation(AnimationType.APPEARING)
            updateStateDisplay()
        }
        
        // 대기 애니메이션
        findViewById<Button>(R.id.btn_idle).setOnClickListener {
            Log.d(TAG, "😴 Testing IDLE animation")
            characterView.startAnimation(AnimationType.IDLE)
            updateStateDisplay()
        }
        
        // 관심 끌기 애니메이션
        findViewById<Button>(R.id.btn_attention).setOnClickListener {
            Log.d(TAG, "👀 Testing ATTENTION animation")
            characterView.startAnimation(AnimationType.ATTENTION)
            updateStateDisplay()
        }
        
        // 회전 애니메이션 (핵심 기능)
        findViewById<Button>(R.id.btn_spinning).setOnClickListener {
            Log.d(TAG, "🌪️ Testing SPINNING animation - Core feature!")
            characterView.startAnimation(AnimationType.SPINNING)
            updateStateDisplay()
        }
        
        // 독촉 애니메이션
        findViewById<Button>(R.id.btn_urgent).setOnClickListener {
            Log.d(TAG, "🚨 Testing URGENT animation")
            characterView.startAnimation(AnimationType.URGENT)
            updateStateDisplay()
        }
        
        // 특별 애니메이션
        findViewById<Button>(R.id.btn_special).setOnClickListener {
            Log.d(TAG, "✨ Testing SPECIAL animation")
            characterView.startAnimation(AnimationType.SPECIAL)
            updateStateDisplay()
        }
    }
    
    private fun setupEffectButtons() {
        // 네온 블루 하이라이트
        findViewById<Button>(R.id.btn_highlight).setOnClickListener {
            Log.d(TAG, "💙 Testing neon blue highlight")
            characterView.applyBrandHighlight(true)
            characterView.applyFadeEffect(false)
            updateStateDisplay()
        }
        
        // 그레이 페이드
        findViewById<Button>(R.id.btn_fade).setOnClickListener {
            Log.d(TAG, "🌫️ Testing gray fade effect")
            characterView.applyFadeEffect(true)
            characterView.applyBrandHighlight(false)
            updateStateDisplay()
        }
        
        // 기본 색상
        findViewById<Button>(R.id.btn_normal).setOnClickListener {
            Log.d(TAG, "🎨 Resetting to normal colors")
            characterView.applyBrandHighlight(false)
            characterView.applyFadeEffect(false)
            updateStateDisplay()
        }
    }
    
    private fun setupScenarioButtons() {
        // 알람 시나리오 시작
        findViewById<Button>(R.id.btn_start_scenario).setOnClickListener {
            Log.d(TAG, "🚀 Starting full alarm scenario")
            animationManager.startAlarmScenario()
            updateStateDisplay()
        }
        
        // 알람 시나리오 중지
        findViewById<Button>(R.id.btn_stop_scenario).setOnClickListener {
            Log.d(TAG, "⏹️ Stopping alarm scenario")
            animationManager.stopAlarmScenario()
            updateStateDisplay()
        }
    }
    
    /**
     * 현재 캐릭터 상태를 화면에 업데이트합니다
     */
    private fun updateStateDisplay() {
        val currentState = characterView.getCurrentState()
        val currentAnimation = characterView.getCurrentAnimationType()
        val isHighlighted = characterView.isHighlightEnabled()
        val isAnimating = characterView.isAnimating()
        
        val statusText = buildString {
            append("${currentState.name}")
            append(" | ${currentAnimation.displayName}")
            if (isHighlighted) append(" | 💙하이라이트")
            if (isAnimating) append(" | ▶️재생중")
        }
        
        stateDisplay.text = statusText
        
        Log.d(TAG, "📊 State updated: $statusText")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 애니메이션 매니저 정리
        animationManager.cleanup()
        
        Log.d(TAG, "🧹 CharacterTestActivity destroyed")
    }
    
    override fun onPause() {
        super.onPause()
        
        // 액티비티가 백그라운드로 가면 애니메이션 일시 정지
        characterView.stopAnimation()
    }
    
    override fun onResume() {
        super.onResume()
        
        // 액티비티가 포그라운드로 돌아오면 애니메이션 재개
        val currentState = characterView.getCurrentState()
        val animationType = CharacterConfig.getAnimationForState(currentState)
        characterView.startAnimation(animationType)
        
        updateStateDisplay()
    }
}