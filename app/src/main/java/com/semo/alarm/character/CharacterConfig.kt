package com.semo.alarm.character

import com.semo.alarm.R

/**
 * 세모알 캐릭터 시스템의 설정 및 상수들을 관리하는 클래스
 * 
 * 🎨 세모알 브랜드 아이덴티티와 CHARACTER_IMAGE_GUIDE.md 사양을 따릅니다.
 */
object CharacterConfig {
    
    // ═══════════════════════════════════════════════════
    // 🎨 세모알 브랜드 색상 (SemoR Brand Colors)
    // ═══════════════════════════════════════════════════
    
    /** 네온 블루 - 활성화 상태, 알람 하이라이트 */
    const val NEON_BLUE = "#00D4FF"
    
    /** 딥 그레이 - 비활성화 상태, 스누즈 모드 */
    const val DEEP_GRAY = "#6B7280"
    
    /** 순수 화이트 - 텍스트, 하이라이트 */
    const val PURE_WHITE = "#FFFFFF"
    
    /** 딥 블랙 - 텍스트, 강조 */
    const val DEEP_BLACK = "#000000"
    
    // ═══════════════════════════════════════════════════
    // ⏱️ 애니메이션 타이밍 설정
    // ═══════════════════════════════════════════════════
    
    /** 등장 애니메이션 총 소요 시간 (ms) */
    const val APPEARING_DURATION = 2500L
    
    /** 대기 애니메이션 한 사이클 시간 (ms) */
    const val IDLE_DURATION = 3000L
    
    /** 회전 애니메이션 한 바퀴 시간 (ms) - 핵심 기능 */
    const val SPINNING_DURATION = 2000L
    
    /** 관심 끌기 애니메이션 시간 (ms) */
    const val ATTENTION_DURATION = 1500L
    
    /** 독촉 모드 애니메이션 시간 (ms) - 빠른 움직임 */
    const val URGENT_DURATION = 1000L
    
    /** 특별 애니메이션 시간 (ms) */
    const val SPECIAL_DURATION = 2500L
    
    // ═══════════════════════════════════════════════════
    // 🎭 알람 상태 전환 타이밍
    // ═══════════════════════════════════════════════════
    
    /** 등장 → 관심끌기 전환 시간 (ms) */
    const val TRANSITION_TO_ATTENTION = 3000L      // 3초
    
    /** 관심끌기 → 회전 전환 시간 (ms) */
    const val TRANSITION_TO_SPINNING = 30000L      // 30초
    
    /** 회전 → 독촉 전환 시간 (ms) */
    const val TRANSITION_TO_URGENT = 60000L        // 1분
    
    /** 독촉 모드에서 네온 블루 하이라이트 활성화 시간 (ms) */
    const val URGENT_HIGHLIGHT_DELAY = 180000L     // 3분
    
    // ═══════════════════════════════════════════════════
    // 📋 이미지 리소스 배열 (임시 - 실제 이미지 추가 시 업데이트)
    // ═══════════════════════════════════════════════════
    
    /**
     * 등장 애니메이션 프레임 (반복 + 최종)
     * 1~4프레임 반복 → 5프레임으로 IDLE 전환
     * 
     * ✅ 실제 Merry 등장 애니메이션! 왼쪽에서 걸어와 중앙에서 앉는 시퀀스
     */
    val APPEARING_FRAMES = intArrayOf(
        R.drawable.character_merry_appear_01,  // ✅ 왼쪽 20% 위치, 걷기 자세
        R.drawable.character_merry_appear_02,  // ✅ 40% 위치, 걷기 동작  
        R.drawable.character_merry_appear_03,  // ✅ 중앙 도착, 앉기 시작
        R.drawable.character_merry_appear_04,  // ✅ 앉기 진행 중
        R.drawable.character_merry_appear_01,  // 🔄 반복: 다시 걷기 자세
        R.drawable.character_merry_appear_02,  // 🔄 반복: 걷기 동작
        R.drawable.character_merry_appear_03,  // 🔄 반복: 앉기 시작
        R.drawable.character_merry_appear_04,  // 🔄 반복: 앉기 진행
        R.drawable.character_merry_appear_05   // ✅ 최종: 완전히 앉은 자세 (IDLE과 연결)
    )
    
    /**
     * 기본 대기 애니메이션 프레임 (4개)
     * idle_01.png → idle_04.png
     * 
     * ✅ character_merry_idle_01.png 실제 이미지 사용!
     */
    val IDLE_FRAMES = intArrayOf(
        R.drawable.character_merry_idle_01,  // ✅ 실제 Merry 도트 아트!
        R.drawable.character_merry_idle_01,  // 임시로 같은 이미지 사용 (향후 idle_02 추가 시 교체)
        R.drawable.character_merry_idle_01,  // 임시로 같은 이미지 사용 (향후 idle_03 추가 시 교체)
        R.drawable.character_merry_idle_01   // 임시로 같은 이미지 사용 (향후 idle_04 추가 시 교체)
    )
    
    /**
     * 회전 애니메이션 프레임 (8개) ⭐ 핵심 기능
     * 🔄 실제 이미지 회전 사용으로 별도 프레임 불필요!
     * 
     * AlarmCharacterView에서 character_merry_idle_01.png를 
     * RotationAnimationHelper로 부드럽게 회전시킵니다.
     */
    val SPINNING_FRAMES = intArrayOf(
        R.drawable.character_merry_idle_01  // ✅ 실제 이미지를 회전시켜 사용
    )
    
    /**
     * 관심 끌기 애니메이션 프레임 (6개)
     * attention_01.png → attention_06.png
     * 
     * ✅ 실제 Merry attention 애니메이션! 주목끌기 동작 시퀀스
     */
    val ATTENTION_FRAMES = intArrayOf(
        R.drawable.character_merry_attention_01,  // ✅ 실제 Merry attention 프레임 1
        R.drawable.character_merry_attention_02,  // ✅ 실제 Merry attention 프레임 2
        R.drawable.character_merry_attention_03,  // ✅ 실제 Merry attention 프레임 3
        R.drawable.character_merry_attention_04,  // ✅ 실제 Merry attention 프레임 4
        R.drawable.character_merry_attention_05,  // ✅ 실제 Merry attention 프레임 5
        R.drawable.character_merry_attention_06   // ✅ 실제 Merry attention 프레임 6
    )
    
    /**
     * 독촉 모드 애니메이션 프레임 (4개)
     * urgent_01.png → urgent_04.png
     */
    val URGENT_FRAMES = intArrayOf(
        R.drawable.ic_alarm_off, // 임시 - 실제 character_merry_urgent_01로 교체 예정
        R.drawable.ic_alarm_off, // 임시 - 실제 character_merry_urgent_02로 교체 예정
        R.drawable.ic_alarm_off, // 임시 - 실제 character_merry_urgent_03로 교체 예정
        R.drawable.ic_alarm_off  // 임시 - 실제 character_merry_urgent_04로 교체 예정
    )
    
    /**
     * 특별 애니메이션 프레임 (7개)
     * 그루밍, 스트레칭, 놀이 자세
     */
    val SPECIAL_FRAMES = intArrayOf(
        R.drawable.ic_report, // 임시 - 실제 character_merry_grooming_01로 교체 예정
        R.drawable.ic_report, // 임시 - 실제 character_merry_grooming_02로 교체 예정
        R.drawable.ic_report, // 임시 - 실제 character_merry_grooming_03로 교체 예정
        R.drawable.ic_report, // 임시 - 실제 character_merry_stretch_01로 교체 예정
        R.drawable.ic_report, // 임시 - 실제 character_merry_stretch_02로 교체 예정
        R.drawable.ic_report, // 임시 - 실제 character_merry_play_01로 교체 예정
        R.drawable.ic_report  // 임시 - 실제 character_merry_play_02로 교체 예정
    )
    
    // ═══════════════════════════════════════════════════
    // 🎨 UI 설정
    // ═══════════════════════════════════════════════════
    
    /** 캐릭터 뷰 기본 크기 (dp) */
    const val CHARACTER_SIZE_DP = 128
    
    /** 캐릭터 뷰 최소 크기 (dp) */
    const val CHARACTER_MIN_SIZE_DP = 64
    
    /** 캐릭터 뷰 최대 크기 (dp) */
    const val CHARACTER_MAX_SIZE_DP = 256
    
    /** 네온 블루 하이라이트 투명도 (0.0f ~ 1.0f) */
    const val NEON_HIGHLIGHT_ALPHA = 0.8f
    
    /** 딥 그레이 페이드 투명도 (0.0f ~ 1.0f) */
    const val GRAY_FADE_ALPHA = 0.5f
    
    /**
     * 애니메이션 타입에 따른 프레임 배열 반환
     * @param animationType 애니메이션 타입
     * @return 해당 애니메이션의 프레임 리소스 배열
     */
    fun getFramesForAnimation(animationType: AnimationType): IntArray {
        return when (animationType) {
            AnimationType.APPEARING -> APPEARING_FRAMES
            AnimationType.IDLE -> IDLE_FRAMES
            AnimationType.SPINNING -> SPINNING_FRAMES
            AnimationType.ATTENTION -> ATTENTION_FRAMES
            AnimationType.URGENT -> URGENT_FRAMES
            AnimationType.SPECIAL -> SPECIAL_FRAMES
        }
    }
    
    /**
     * 캐릭터 상태에 따른 기본 애니메이션 타입 반환
     * @param state 캐릭터 상태
     * @return 해당 상태의 기본 애니메이션 타입
     */
    fun getAnimationForState(state: CharacterState): AnimationType {
        return when (state) {
            CharacterState.APPEARING -> AnimationType.APPEARING
            CharacterState.IDLE -> AnimationType.IDLE
            CharacterState.ATTENTION -> AnimationType.ATTENTION
            CharacterState.SPINNING -> AnimationType.SPINNING
            CharacterState.URGENT -> AnimationType.URGENT
            CharacterState.SLEEPING -> AnimationType.IDLE // 슬리핑은 IDLE과 동일하나 색상만 다름
        }
    }
}