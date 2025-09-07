package com.semo.alarm.character

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat

/**
 * 세모알 알람 캐릭터를 표시하는 커스텀 ImageView
 * 
 * 🐱 Merry the Siamese Cat을 위한 전용 뷰 컴포넌트
 * - 프레임 기반 애니메이션 재생
 * - 세모알 브랜드 색상 효과 (네온 블루 하이라이트)
 * - 캐릭터 상태에 따른 자동 애니메이션 전환
 * - 터치 상호작용 지원
 */
class AlarmCharacterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "AlarmCharacterView"
    }

    // ═══════════════════════════════════════════════════
    // 🎭 애니메이션 관리 변수
    // ═══════════════════════════════════════════════════
    
    /** 현재 캐릭터 상태 */
    private var currentState: CharacterState = CharacterState.IDLE
    
    /** 현재 재생 중인 애니메이션 타입 */
    private var currentAnimationType: AnimationType = AnimationType.IDLE
    
    /** 애니메이션 핸들러 */
    private val animationHandler = Handler(Looper.getMainLooper())
    
    /** 현재 프레임 인덱스 */
    private var currentFrameIndex = 0
    
    /** 애니메이션 실행 중 여부 */
    private var isAnimating = false
    
    /** 현재 애니메이션 러너블 */
    private var currentAnimationRunnable: Runnable? = null
    
    /** 현재 실행 중인 회전 애니메이터 */
    private var currentRotationAnimator: ObjectAnimator? = null
    
    // ═══════════════════════════════════════════════════
    // 🎨 세모알 브랜드 색상 효과
    // ═══════════════════════════════════════════════════
    
    /** 네온 블루 하이라이트 컬러 필터 */
    private val neonBlueFilter = PorterDuffColorFilter(
        Color.parseColor(CharacterConfig.NEON_BLUE), 
        PorterDuff.Mode.SRC_ATOP
    )
    
    /** 딥 그레이 페이드 컬러 필터 */
    private val deepGrayFilter = PorterDuffColorFilter(
        Color.parseColor(CharacterConfig.DEEP_GRAY), 
        PorterDuff.Mode.SRC_ATOP
    )
    
    /** 브랜드 하이라이트 활성화 여부 */
    private var isHighlightEnabled = false
    
    /** 페이드 효과 활성화 여부 */
    private var isFadeEnabled = false

    // ═══════════════════════════════════════════════════
    // 🎯 초기화
    // ═══════════════════════════════════════════════════
    
    init {
        Log.d(TAG, "🐱 AlarmCharacterView initialized")
        
        // 기본 IDLE 애니메이션 시작
        post {
            startAnimation(AnimationType.IDLE)
        }
        
        // 터치 리스너 설정 (캐릭터 상호작용)
        setOnClickListener {
            onCharacterClicked()
        }
    }

    // ═══════════════════════════════════════════════════
    // 🎬 애니메이션 제어 메서드
    // ═══════════════════════════════════════════════════
    
    /**
     * 지정된 애니메이션을 시작합니다
     * @param animationType 재생할 애니메이션 타입
     */
    fun startAnimation(animationType: AnimationType) {
        Log.d(TAG, "🎬 Starting animation: ${animationType.displayName}")
        
        // 기존 애니메이션 중지
        stopAnimation()
        
        currentAnimationType = animationType
        currentFrameIndex = 0
        isAnimating = true
        
        // SPINNING 애니메이션은 실제 회전 사용, 나머지는 프레임 시퀀스
        if (animationType == AnimationType.SPINNING) {
            startRotationBasedSpinning()
        } else {
            playFrameSequence()
        }
    }
    
    /**
     * 현재 애니메이션을 중지합니다
     */
    fun stopAnimation() {
        if (isAnimating) {
            Log.d(TAG, "⏹️ Stopping animation: ${currentAnimationType.displayName}")
            
            isAnimating = false
            
            // 프레임 기반 애니메이션 중지
            currentAnimationRunnable?.let {
                animationHandler.removeCallbacks(it)
            }
            currentAnimationRunnable = null
            
            // 회전 애니메이션 중지
            currentRotationAnimator?.cancel()
            currentRotationAnimator = null
        }
    }
    
    /**
     * 캐릭터 상태를 변경하고 해당 애니메이션을 시작합니다
     * @param state 새로운 캐릭터 상태
     */
    fun setState(state: CharacterState) {
        if (currentState != state) {
            Log.d(TAG, "🔄 Character state changed: ${currentState.name} → ${state.name}")
            
            currentState = state
            val animationType = CharacterConfig.getAnimationForState(state)
            
            // 상태에 따른 색상 효과 적용
            when (state) {
                CharacterState.URGENT -> {
                    applyBrandHighlight(true)
                    applyFadeEffect(false)
                }
                CharacterState.SLEEPING -> {
                    applyBrandHighlight(false)
                    applyFadeEffect(true)
                }
                else -> {
                    applyBrandHighlight(false)
                    applyFadeEffect(false)
                }
            }
            
            startAnimation(animationType)
        }
    }
    
    /**
     * 프레임 시퀀스를 재생합니다
     * SPINNING 애니메이션의 경우 실제 이미지 회전 사용
     */
    private fun playFrameSequence() {
        if (!isAnimating) return
        
        val frames = CharacterConfig.getFramesForAnimation(currentAnimationType)
        if (frames.isEmpty()) return
        
        // 현재 프레임 표시
        val currentFrame = frames[currentFrameIndex]
        setImageResource(currentFrame)
        
        // 다음 프레임 인덱스 계산
        currentFrameIndex++
        if (currentFrameIndex >= frames.size) {
            if (currentAnimationType.isLooping) {
                currentFrameIndex = 0  // 루프 애니메이션은 처음으로
            } else {
                // 비루프 애니메이션은 완료 후 IDLE로 전환
                isAnimating = false
                Log.d(TAG, "✅ Non-looping animation completed: ${currentAnimationType.displayName}")
                
                // 등장 애니메이션 완료 후 IDLE로 전환
                if (currentAnimationType == AnimationType.APPEARING) {
                    post {
                        startAnimation(AnimationType.IDLE)
                    }
                }
                return
            }
        }
        
        // 다음 프레임 스케줄링
        val frameDuration = currentAnimationType.getFrameDuration()
        currentAnimationRunnable = Runnable {
            playFrameSequence()
        }
        
        animationHandler.postDelayed(currentAnimationRunnable!!, frameDuration)
    }
    
    /**
     * 실제 이미지 회전을 사용한 스피닝 애니메이션
     * 별도 이미지 파일 없이 기존 idle 이미지를 회전시킴
     */
    private fun startRotationBasedSpinning() {
        Log.d(TAG, "🌪️ Starting rotation-based spinning animation")
        
        // 기존 idle 이미지를 사용 (character_merry_idle_01)
        val idleFrames = CharacterConfig.getFramesForAnimation(AnimationType.IDLE)
        if (idleFrames.isNotEmpty()) {
            setImageResource(idleFrames[0]) // 첫 번째 idle 이미지 사용
        }
        
        // RotationAnimationHelper를 사용한 부드러운 회전
        currentRotationAnimator = RotationAnimationHelper.startSpinningAnimation(
            view = this,
            duration = currentAnimationType.defaultDuration,
            clockwise = true,
            looping = currentAnimationType.isLooping
        )
        
        Log.d(TAG, "✅ Rotation-based spinning started with duration: ${currentAnimationType.defaultDuration}ms")
    }

    // ═══════════════════════════════════════════════════
    // 🎨 브랜드 색상 효과 메서드
    // ═══════════════════════════════════════════════════
    
    /**
     * 세모알 브랜드 네온 블루 하이라이트 효과를 적용합니다
     * @param enabled true면 네온 블루 효과 적용, false면 제거
     */
    fun applyBrandHighlight(enabled: Boolean) {
        if (isHighlightEnabled != enabled) {
            isHighlightEnabled = enabled
            updateColorFilter()
            
            Log.d(TAG, "💙 Brand highlight ${if (enabled) "enabled" else "disabled"}")
        }
    }
    
    /**
     * 딥 그레이 페이드 효과를 적용합니다 (스누즈/비활성화 상태)
     * @param enabled true면 그레이 페이드 적용, false면 제거
     */
    fun applyFadeEffect(enabled: Boolean) {
        if (isFadeEnabled != enabled) {
            isFadeEnabled = enabled
            updateColorFilter()
            
            Log.d(TAG, "🌫️ Fade effect ${if (enabled) "enabled" else "disabled"}")
        }
    }
    
    /**
     * 현재 설정에 따라 컬러 필터를 업데이트합니다
     */
    private fun updateColorFilter() {
        colorFilter = when {
            isHighlightEnabled -> neonBlueFilter  // 네온 블루 하이라이트 우선
            isFadeEnabled -> deepGrayFilter        // 딥 그레이 페이드
            else -> null                           // 기본 색상
        }
    }

    // ═══════════════════════════════════════════════════
    // 🎯 상호작용 메서드
    // ═══════════════════════════════════════════════════
    
    /**
     * 캐릭터 클릭 시 호출되는 메서드
     */
    private fun onCharacterClicked() {
        Log.d(TAG, "👆 Character clicked! Current state: ${currentState.name}")
        
        // 클릭 시 잠시 특별 애니메이션 재생 (개발자를 위한 이스터에그)
        when (currentState) {
            CharacterState.IDLE -> {
                startAnimation(AnimationType.ATTENTION)
                
                // 3초 후 원래 상태로 복귀
                animationHandler.postDelayed({
                    startAnimation(AnimationType.IDLE)
                }, 3000)
            }
            CharacterState.SPINNING -> {
                // 회전 중이면 더 빠르게 회전
                stopAnimation()
                currentAnimationType = AnimationType.SPINNING
                
                // 빠른 회전을 위해 duration 절반으로 축소
                val originalDuration = AnimationType.SPINNING.defaultDuration
                val fastSpinningRunnable = object : Runnable {
                    override fun run() {
                        playFrameSequence()
                    }
                }
                
                // 5초 후 원래 속도로 복귀
                animationHandler.postDelayed({
                    startAnimation(AnimationType.SPINNING)
                }, 5000)
            }
            else -> {
                // 다른 상태에서는 회전 한 바퀴 후 원래 상태로
                val originalState = currentState
                startAnimation(AnimationType.SPINNING)
                
                animationHandler.postDelayed({
                    setState(originalState)
                }, AnimationType.SPINNING.defaultDuration)
            }
        }
    }
    
    // ═══════════════════════════════════════════════════
    // 🔧 라이프사이클 관리
    // ═══════════════════════════════════════════════════
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
        Log.d(TAG, "🗑️ AlarmCharacterView detached, animations stopped")
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        
        // 다시 연결되면 현재 상태에 맞는 애니메이션 재시작
        if (!isAnimating) {
            val animationType = CharacterConfig.getAnimationForState(currentState)
            startAnimation(animationType)
        }
        
        Log.d(TAG, "🔗 AlarmCharacterView attached, animations resumed")
    }
    
    // ═══════════════════════════════════════════════════
    // 🎯 공개 유틸리티 메서드
    // ═══════════════════════════════════════════════════
    
    /**
     * 현재 캐릭터 상태 반환
     */
    fun getCurrentState(): CharacterState = currentState
    
    /**
     * 현재 애니메이션 타입 반환
     */
    fun getCurrentAnimationType(): AnimationType = currentAnimationType
    
    /**
     * 애니메이션 실행 중 여부 반환
     */
    fun isAnimating(): Boolean = isAnimating
    
    /**
     * 브랜드 하이라이트 활성화 여부 반환
     */
    fun isHighlightEnabled(): Boolean = isHighlightEnabled
}