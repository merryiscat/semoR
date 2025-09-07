# 🐱 메리 캐릭터 풀스크린 알람 설계서

## 📱 **개요**
세모알 앱의 메리 캐릭터를 활용한 풀스크린 알람 화면 구현

## 🎯 **핵심 요구사항**
- 기존 Notification 알람을 풀스크린 Activity로 확장
- 메리 캐릭터 애니메이션을 통한 사용자 몰입도 극대화  
- 세모알 브랜드 아이덴티티(네온블루 #00D4FF) 일관성 유지
- 단계별 알람 강도 조절 시스템

## 🏗️ **아키텍처**

### **파일 구조**
```
app/src/main/java/com/semo/alarm/
├── ui/activities/AlarmFullScreenActivity.kt    (새로 생성)
├── ui/components/MerryAnimationView.kt         (새로 생성)  
├── services/AlarmService.kt                    (기존 - 수정)
└── utils/NotificationAlarmManager.kt           (기존 - 수정)

app/src/main/res/
├── layout/activity_alarm_fullscreen.xml       (새로 생성)
├── drawable/characters/merry/                  (기존)
└── values/colors.xml                          (기존 - 추가)
```

## 📐 **화면 레이아웃**

### **전체 구성 (360x640dp 기준)**
```
┌─────────────────────────────────────┐
│ StatusBar (숨김)                    │
├─────────────────────────────────────┤ 
│         🕒 TIME DISPLAY             │ 80dp height
│           07:30 AM                  │ (네온블루, 48sp)
├─────────────────────────────────────┤
│                                     │
│         🐱 MERRY CHARACTER          │ 300dp height
│       [애니메이션 중앙 영역]         │ (240x240dp)  
│                                     │
├─────────────────────────────────────┤
│         📝 ALARM LABEL              │ 60dp height
│        "기상 시간이에요!"           │ (흰색, 18sp)
├─────────────────────────────────────┤
│                                     │
│  [해제]   [스누즈]   [정지]        │ 200dp height
│ DISMISS   SNOOZE    STOP            │ (버튼 높이 48dp)
│                                     │
└─────────────────────────────────────┘
```

### **색상 스키마**
```kotlin
// colors.xml 추가
<color name="fullscreen_background_start">#000000</color>
<color name="fullscreen_background_end">#1a1a1a</color>
<color name="alarm_time_color">@color/neon_blue</color>      // #00D4FF
<color name="alarm_label_color">#FFFFFF</color>
<color name="button_dismiss_color">@color/neon_blue</color>  // #00D4FF  
<color name="button_secondary_color">@color/deep_gray</color> // #6B7280
```

## 🎬 **애니메이션 시스템**

### **1. 빼꼼 등장 애니메이션 (Peek-a-boo) - 2초**
```
Frame 1: appear_01.png (0.2초) - 빈 화면, 벽 가장자리만
Frame 2: appear_02.png (0.3초) - 고양이 귀 끝만 삐죽
Frame 3: appear_03.png (0.5초) - 파란 눈 하나만 보임  
Frame 4: appear_04.png (0.5초) - 얼굴 반쪽 + 호기심 표정
Frame 5: appear_05.png (0.5초) - 완전 등장 → idle_01 전환
```

### **2. 기본 대기 애니메이션 (Idle Loop) - 4초 반복**
```
idle_01.png (1초) → idle_02.png (1초) → idle_03.png (1초) → idle_04.png (1초)
```

### **3. 관심 끌기 애니메이션 (15초 후) - 3초**
```  
attention_01.png (1초) → attention_02.png (1초) → attention_03.png (1초)
```

### **4. 독촉 모드 애니메이션 (30초 후) - 2초 고속 반복**
```
urgent_01.png (0.5초) → urgent_02.png (0.5초) → urgent_03.png (0.5초) → urgent_04.png (0.5초)
+ spinning/ 시리즈 간헐적 삽입 (회전 효과)
```

## 🔧 **기술 구현**

### **AlarmFullScreenActivity 주요 기능**
```kotlin
class AlarmFullScreenActivity : AppCompatActivity() {
    // 핵심 컴포넌트
    private lateinit var merryAnimationView: MerryAnimationView
    private lateinit var timeDisplay: TextView
    private lateinit var alarmLabel: TextView
    private var currentAlarm: Alarm? = null
    private var animationPhase: AnimationPhase = APPEARING
    
    // 애니메이션 단계 관리
    enum class AnimationPhase { APPEARING, IDLE, ATTENTION, URGENT }
    
    // 주요 메서드  
    private fun startMerryAnimation()
    private fun handleDismissAlarm()
    private fun handleSnoozeAlarm()
    private fun handleStopAlarm()
}
```

### **MerryAnimationView 구조**
```kotlin
class MerryAnimationView : AppCompatImageView {
    private var currentPhase: AnimationPhase = APPEARING
    private var frameAnimator: ValueAnimator? = null
    private var currentFrameIndex: Int = 0
    
    fun startAnimation(phase: AnimationPhase)
    fun stopAnimation() 
    private fun loadFrameResources(phase: AnimationPhase): List<Int>
    private fun updateFrame(frameResId: Int)
}
```

## 🚀 **구현 단계**

### **Phase 1: 기본 구조 (MVP)**
1. AlarmFullScreenActivity.kt 생성
2. activity_alarm_fullscreen.xml 레이아웃  
3. AlarmService에서 풀스크린 Intent 연동
4. 정적 메리 이미지 표시

### **Phase 2: 애니메이션 시스템**
1. MerryAnimationView 구현
2. 4단계 애니메이션 로직
3. 프레임 자동 전환 시스템

### **Phase 3: 고도화**
1. 사운드 연동 (AlarmService와 동기화)
2. 배터리 최적화 
3. 접근성 개선
4. 에러 처리 강화

## 📋 **체크리스트**

### **기능 요구사항**
- [ ] 풀스크린 모드 (StatusBar/NavBar 숨김)
- [ ] 메리 캐릭터 애니메이션 표시
- [ ] 알람 시간/라벨 정보 표시  
- [ ] 해제/스누즈/정지 버튼
- [ ] 기존 AlarmService와 연동
- [ ] 세모알 브랜드 디자인 적용

### **사용성 요구사항**  
- [ ] 직관적인 버튼 배치
- [ ] 어두운 환경에서도 가독성 확보
- [ ] 한 손으로 조작 가능한 버튼 크기
- [ ] 실수 방지를 위한 버튼 간격

### **성능 요구사항**
- [ ] 애니메이션 60fps 유지
- [ ] 메모리 사용량 최적화  
- [ ] 배터리 영향 최소화
- [ ] 즉시 응답성 보장

---

**최종 목표**: 사용자가 "일어나기 싫지만... 메리가 너무 귀여워서 어쩔 수 없이 일어나게 되는" 알람 경험 제공 🐱✨