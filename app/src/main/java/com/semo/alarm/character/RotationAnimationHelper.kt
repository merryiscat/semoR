package com.semo.alarm.character

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * 기존 캐릭터 이미지를 사용한 부드러운 회전 애니메이션 헬퍼
 * 
 * 🔄 별도 이미지 파일 없이 하나의 이미지로 360도 회전 구현
 * - 메모리 효율적 (8개 이미지 vs 1개 이미지)
 * - 부드러운 회전 (45도씩 끊김 없이 연속 회전)
 * - 세모알 브랜드 효과와 호환
 */
object RotationAnimationHelper {
    
    /**
     * 360도 회전 애니메이션을 시작합니다
     * @param view 회전할 뷰 (AlarmCharacterView)
     * @param duration 회전 총 시간 (ms)
     * @param clockwise 시계방향 여부 (true: 시계방향, false: 반시계방향)
     * @param looping 무한 반복 여부
     * @return 생성된 애니메이터 (제어용)
     */
    fun startSpinningAnimation(
        view: View,
        duration: Long = CharacterConfig.SPINNING_DURATION,
        clockwise: Boolean = true,
        looping: Boolean = true
    ): ObjectAnimator {
        
        // 기존 회전 애니메이션 중지
        view.animate().cancel()
        
        // 시작 각도와 끝 각도 설정
        val startRotation = view.rotation
        val endRotation = if (clockwise) {
            startRotation + 360f
        } else {
            startRotation - 360f
        }
        
        // ObjectAnimator로 부드러운 회전 생성
        val rotationAnimator = ObjectAnimator.ofFloat(
            view, 
            "rotation", 
            startRotation, 
            endRotation
        ).apply {
            this.duration = duration
            interpolator = LinearInterpolator() // 일정한 속도로 회전
            
            if (looping) {
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
            }
        }
        
        rotationAnimator.start()
        return rotationAnimator
    }
    
    /**
     * 특정 각도로 즉시 회전합니다 (프레임 기반 애니메이션 시뮬레이션)
     * @param view 회전할 뷰
     * @param degrees 목표 각도 (0, 45, 90, 135, 180, 225, 270, 315)
     * @param duration 회전 시간 (ms)
     */
    fun rotateToAngle(
        view: View,
        degrees: Float,
        duration: Long = 200L
    ): ObjectAnimator {
        
        val currentRotation = view.rotation
        val targetRotation = degrees
        
        // 최단 경로로 회전하도록 각도 조정
        val adjustedTarget = adjustRotationPath(currentRotation, targetRotation)
        
        val rotationAnimator = ObjectAnimator.ofFloat(
            view,
            "rotation",
            currentRotation,
            adjustedTarget
        ).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
        }
        
        rotationAnimator.start()
        return rotationAnimator
    }
    
    /**
     * 8프레임 회전 애니메이션 (기존 시스템과 호환)
     * @param view 회전할 뷰
     * @param frameCallback 각 프레임마다 호출될 콜백
     * @param frameDuration 각 프레임 지속 시간 (ms)
     * @param looping 무한 반복 여부
     */
    fun startFrameBasedSpinning(
        view: View,
        frameCallback: ((frameIndex: Int, angle: Float) -> Unit)? = null,
        frameDuration: Long = CharacterConfig.SPINNING_DURATION / 8,
        looping: Boolean = true
    ) {
        
        val angles = floatArrayOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
        var currentFrame = 0
        
        fun playNextFrame() {
            val angle = angles[currentFrame]
            
            rotateToAngle(view, angle, frameDuration).apply {
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        frameCallback?.invoke(currentFrame, angle)
                        
                        currentFrame = (currentFrame + 1) % angles.size
                        
                        if (looping || currentFrame != 0) {
                            // 다음 프레임 스케줄링
                            view.postDelayed({
                                playNextFrame()
                            }, 50L) // 짧은 지연 후 다음 프레임
                        }
                    }
                })
            }
        }
        
        playNextFrame()
    }
    
    /**
     * 회전 애니메이션 중지
     * @param view 애니메이션을 중지할 뷰
     * @param resetRotation 원래 각도로 리셋 여부
     */
    fun stopSpinningAnimation(view: View, resetRotation: Boolean = false) {
        view.animate().cancel()
        view.clearAnimation()
        
        if (resetRotation) {
            view.rotation = 0f
        }
    }
    
    /**
     * 최단 경로 회전을 위한 각도 조정
     */
    private fun adjustRotationPath(current: Float, target: Float): Float {
        val normalizedCurrent = current % 360f
        var adjustedTarget = target
        
        // 180도 이상 차이나면 반대 방향으로 회전
        val diff = target - normalizedCurrent
        when {
            diff > 180f -> adjustedTarget = target - 360f
            diff < -180f -> adjustedTarget = target + 360f
        }
        
        return adjustedTarget
    }
    
    /**
     * 현재 회전 각도를 0~360 범위로 정규화
     */
    fun normalizeRotation(rotation: Float): Float {
        return ((rotation % 360f) + 360f) % 360f
    }
}