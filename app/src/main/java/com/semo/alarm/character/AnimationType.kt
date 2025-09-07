package com.semo.alarm.character

/**
 * 세모알 캐릭터의 애니메이션 타입을 정의하는 enum
 * CHARACTER_IMAGE_GUIDE.md의 애니메이션 분류를 따릅니다.
 */
enum class AnimationType(
    val displayName: String,
    val frameCount: Int,
    val defaultDuration: Long,
    val isLooping: Boolean
) {
    /**
     * 등장 애니메이션 (5프레임)
     * - 알람 시작 시 알에서 부화
     * - 한 번만 재생 후 IDLE로 전환
     */
    APPEARING(
        displayName = "등장",
        frameCount = 5,
        defaultDuration = 800L,
        isLooping = false
    ),
    
    /**
     * 기본 대기 애니메이션 (4프레임)
     * - 평상시 부드러운 호흡, 꼬리 움직임
     * - 무한 반복
     */
    IDLE(
        displayName = "대기",
        frameCount = 4,
        defaultDuration = 3000L,
        isLooping = true
    ),
    
    /**
     * 회전 애니메이션 (8프레임) ⭐ 핵심 기능
     * - 360도 완전 회전으로 강력한 관심 집중
     * - 0도→45도→90도→135도→180도→225도→270도→315도
     * - 무한 반복
     */
    SPINNING(
        displayName = "회전",
        frameCount = 8,
        defaultDuration = 2000L,
        isLooping = true
    ),
    
    /**
     * 관심 끌기 애니메이션 (3프레임)
     * - 가벼운 움직임으로 사용자 관심 유도
     * - 고개 좌우, 앞발 들기
     * - 무한 반복
     */
    ATTENTION(
        displayName = "관심끌기",
        frameCount = 3,
        defaultDuration = 1500L,
        isLooping = true
    ),
    
    /**
     * 독촉 모드 애니메이션 (4프레임)
     * - 강력한 움직임으로 기상 독촉
     * - 입 벌리고 울기, 온몸 어필
     * - 빠른 속도로 무한 반복
     */
    URGENT(
        displayName = "독촉",
        frameCount = 4,
        defaultDuration = 1000L,
        isLooping = true
    ),
    
    /**
     * 특별 애니메이션 (7프레임)
     * - 그루밍, 스트레칭, 놀이 자세
     * - 향후 확장을 위한 예비 슬롯
     */
    SPECIAL(
        displayName = "특별",
        frameCount = 7,
        defaultDuration = 2500L,
        isLooping = false
    );
    
    /**
     * 애니메이션의 총 소요 시간 계산
     * @return 전체 애니메이션 한 사이클의 소요 시간 (ms)
     */
    fun getTotalDuration(): Long {
        return if (isLooping) defaultDuration else defaultDuration
    }
    
    /**
     * 프레임당 소요 시간 계산
     * @return 각 프레임의 표시 시간 (ms)
     */
    fun getFrameDuration(): Long {
        return defaultDuration / frameCount
    }
}