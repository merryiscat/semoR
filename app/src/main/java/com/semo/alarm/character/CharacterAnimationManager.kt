package com.semo.alarm.character

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * 세모알 캐릭터 애니메이션을 관리하는 고급 매니저 클래스
 * 
 * 🎭 알람 시나리오에 따른 자동 상태 전환 및 애니메이션 제어
 * - 알람 시간 경과에 따른 자동 캐릭터 상태 변경
 * - 복합 애니메이션 시퀀스 관리
 * - 사용자 상호작용에 따른 동적 반응
 */
class CharacterAnimationManager(
    private val characterView: AlarmCharacterView
) {
    
    companion object {
        private const val TAG = "CharacterAnimationManager"
    }
    
    // ═══════════════════════════════════════════════════
    // 🎯 알람 시나리오 관리
    // ═══════════════════════════════════════════════════
    
    /** 알람 시작 시간 (타임스탬프) */
    private var alarmStartTime: Long = 0
    
    /** 자동 상태 전환 핸들러 */
    private val stateTransitionHandler = Handler(Looper.getMainLooper())
    
    /** 예약된 상태 전환 작업들 */
    private val scheduledTransitions = mutableListOf<Runnable>()
    
    /** 현재 알람 시나리오 실행 중 여부 */
    private var isAlarmScenarioActive = false
    
    // ═══════════════════════════════════════════════════
    // 🚀 알람 시나리오 시작
    // ═══════════════════════════════════════════════════
    
    /**
     * 알람이 시작될 때 호출하여 전체 캐릭터 시나리오를 시작합니다
     * 
     * 시나리오 순서:
     * 1. 등장 (즉시) → 3초 후 대기
     * 2. 관심 끌기 (30초 후)
     * 3. 회전 독촉 (1분 후) ⭐ 핵심 기능
     * 4. 긴급 독촉 (3분 후) + 네온 블루 효과
     */
    fun startAlarmScenario() {
        if (isAlarmScenarioActive) {
            Log.w(TAG, "⚠️ Alarm scenario already active, stopping previous scenario")
            stopAlarmScenario()
        }
        
        Log.d(TAG, "🚀 Starting alarm scenario")
        alarmStartTime = System.currentTimeMillis()
        isAlarmScenarioActive = true
        
        // 1단계: 등장 애니메이션
        characterView.setState(CharacterState.APPEARING)
        
        // 2단계: 3초 후 대기 상태로 전환
        scheduleTransition(CharacterConfig.TRANSITION_TO_ATTENTION) {
            characterView.setState(CharacterState.ATTENTION)
            Log.d(TAG, "🎭 Stage 2: Attention mode activated")
        }
        
        // 3단계: 30초 후 회전 시작 (핵심 기능)
        scheduleTransition(CharacterConfig.TRANSITION_TO_SPINNING) {
            characterView.setState(CharacterState.SPINNING)
            Log.d(TAG, "🌪️ Stage 3: SPINNING mode activated - Core feature!")
        }
        
        // 4단계: 1분 후 독촉 모드
        scheduleTransition(CharacterConfig.TRANSITION_TO_URGENT) {
            characterView.setState(CharacterState.URGENT)
            Log.d(TAG, "🚨 Stage 4: Urgent mode activated")
        }
        
        // 5단계: 3분 후 네온 블루 하이라이트 추가
        scheduleTransition(CharacterConfig.URGENT_HIGHLIGHT_DELAY) {
            characterView.applyBrandHighlight(true)
            Log.d(TAG, "💙 Stage 5: Neon blue highlight activated!")
        }
    }
    
    /**
     * 알람 시나리오를 중지합니다 (알람 해제 시)
     */
    fun stopAlarmScenario() {
        if (!isAlarmScenarioActive) return
        
        Log.d(TAG, "⏹️ Stopping alarm scenario")
        
        // 모든 예약된 전환 취소
        scheduledTransitions.forEach { runnable ->
            stateTransitionHandler.removeCallbacks(runnable)
        }
        scheduledTransitions.clear()
        
        // 캐릭터를 슬리핑 상태로 전환 (페이드 효과)
        characterView.setState(CharacterState.SLEEPING)
        
        // 하이라이트 효과 제거
        characterView.applyBrandHighlight(false)
        
        isAlarmScenarioActive = false
        alarmStartTime = 0
    }
    
    /**
     * 스누즈 모드 활성화
     * @param snoozeMinutes 스누즈 시간 (분)
     */
    fun activateSnoozeMode(snoozeMinutes: Int = 5) {
        Log.d(TAG, "😴 Snooze mode activated for ${snoozeMinutes} minutes")
        
        // 현재 시나리오 일시 중지
        stopAlarmScenario()
        
        // 딥 그레이 페이드 효과로 슬리핑 상태
        characterView.setState(CharacterState.SLEEPING)
        
        // 스누즈 시간 후 알람 시나리오 재시작
        stateTransitionHandler.postDelayed({
            Log.d(TAG, "⏰ Snooze time over, restarting alarm scenario")
            startAlarmScenario()
        }, snoozeMinutes * 60 * 1000L)
    }
    
    // ═══════════════════════════════════════════════════
    // 🎬 특별 애니메이션 제어
    // ═══════════════════════════════════════════════════
    
    /**
     * 특별 이벤트 애니메이션 재생 (생일, 기념일 등)
     * @param duration 특별 애니메이션 재생 시간 (ms)
     */
    fun playSpecialAnimation(duration: Long = CharacterConfig.SPECIAL_DURATION) {
        Log.d(TAG, "✨ Playing special animation for ${duration}ms")
        
        val previousState = characterView.getCurrentState()
        
        // 특별 애니메이션 재생
        characterView.startAnimation(AnimationType.SPECIAL)
        
        // 지정 시간 후 원래 상태로 복귀
        stateTransitionHandler.postDelayed({
            characterView.setState(previousState)
            Log.d(TAG, "✅ Special animation completed, returned to ${previousState.name}")
        }, duration)
    }
    
    /**
     * 캐릭터 상호작용 반응 (터치, 음성 명령 등)
     * @param interactionType 상호작용 타입
     */
    fun handleInteraction(interactionType: InteractionType) {
        Log.d(TAG, "👆 Handling interaction: $interactionType")
        
        when (interactionType) {
            InteractionType.GENTLE_TOUCH -> {
                // 부드러운 터치 - 잠시 관심 끌기 애니메이션
                val currentState = characterView.getCurrentState()
                characterView.startAnimation(AnimationType.ATTENTION)
                
                stateTransitionHandler.postDelayed({
                    characterView.setState(currentState)
                }, 2000)
            }
            
            InteractionType.DOUBLE_TAP -> {
                // 더블 탭 - 360도 회전 한 바퀴
                characterView.startAnimation(AnimationType.SPINNING)
                
                stateTransitionHandler.postDelayed({
                    // 원래 상태로 복귀하지 않고 IDLE로 전환
                    characterView.setState(CharacterState.IDLE)
                }, AnimationType.SPINNING.defaultDuration)
            }
            
            InteractionType.LONG_PRESS -> {
                // 롱 프레스 - 특별 애니메이션 재생
                playSpecialAnimation()
            }
            
            InteractionType.VOICE_COMMAND -> {
                // 음성 명령 - 향후 확장을 위한 플레이스홀더
                Log.d(TAG, "🎤 Voice command interaction (not implemented yet)")
            }
        }
    }
    
    // ═══════════════════════════════════════════════════
    // 🛠️ 내부 유틸리티 메서드
    // ═══════════════════════════════════════════════════
    
    /**
     * 지정된 시간 후 상태 전환을 예약합니다
     * @param delayMs 지연 시간 (밀리초)
     * @param action 실행할 액션
     */
    private fun scheduleTransition(delayMs: Long, action: () -> Unit) {
        val runnable = Runnable {
            if (isAlarmScenarioActive) {
                action()
            }
        }
        
        scheduledTransitions.add(runnable)
        stateTransitionHandler.postDelayed(runnable, delayMs)
    }
    
    /**
     * 현재 알람 경과 시간 반환
     * @return 알람 시작 후 경과 시간 (밀리초)
     */
    fun getElapsedTime(): Long {
        return if (alarmStartTime > 0) {
            System.currentTimeMillis() - alarmStartTime
        } else 0
    }
    
    /**
     * 현재 알람 시나리오 활성화 여부 반환
     */
    fun isAlarmScenarioActive(): Boolean = isAlarmScenarioActive
    
    // ═══════════════════════════════════════════════════
    // 🧹 리소스 정리
    // ═══════════════════════════════════════════════════
    
    /**
     * 매니저 리소스 정리 (액티비티 종료 시 호출)
     */
    fun cleanup() {
        Log.d(TAG, "🧹 Cleaning up CharacterAnimationManager")
        
        stopAlarmScenario()
        
        // 모든 핸들러 작업 취소
        stateTransitionHandler.removeCallbacksAndMessages(null)
    }
}

/**
 * 사용자 상호작용 타입
 */
enum class InteractionType {
    /** 부드러운 터치 */
    GENTLE_TOUCH,
    
    /** 더블 탭 */
    DOUBLE_TAP,
    
    /** 롱 프레스 */
    LONG_PRESS,
    
    /** 음성 명령 (향후 확장) */
    VOICE_COMMAND
}