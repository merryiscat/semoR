# 세모알(semoR) 설계 문서

## 🎨 브랜드 아이덴티티 설계

### 브랜드 컬러 시스템
```
Primary: 네온 블루 #00D4FF (활성화, 선택, 중요 액션)
Secondary: 딥 그레이 #6B7280 (비활성화, 보조 요소)
Text: Pure White #FFFFFF / Deep Black #000000
```

### 색상 조합 규칙
- **Active State**: 네온 블루 배경 + 블랙 텍스트
- **Inactive State**: 딥 그레이 배경 + 화이트 텍스트
- **직관적 상태 표시**: 색상으로 on/off 즉시 구분

### 디자인 철학
**"Simple yet Futuristic"** - 단순하면서 미래지향적

## 🏗️ 시스템 아키텍처 설계

### 기본 구조
```
MVVM + Hilt + Room
├── UI Layer (Activities/Fragments + ViewModels)
├── Repository Layer (데이터 추상화)
├── Database Layer (Room + DAO)
└── Service Layer (Background Services)
```

### 핵심 컴포넌트
```
app/src/main/java/com/semo/alarm/
├── ui/                    # UI 레이어
│   ├── activities/        # MainActivity, AddEditAlarmActivity 등
│   ├── fragments/         # AlarmFragment, CustomTimerFragment 등  
│   ├── adapters/          # RecyclerView 어댑터들
│   └── viewmodels/        # ViewModel 클래스들
├── data/                  # 데이터 레이어
│   ├── database/          # AlarmDatabase
│   ├── entities/          # Room 엔티티들
│   ├── dao/              # Data Access Objects
│   └── repositories/      # Repository 구현체들
├── services/             # 백그라운드 서비스
│   ├── AlarmService      # (폐기된 구 방식)
│   ├── NotificationAlarmManager # (새로운 알람 시스템)
│   └── TimerForegroundService # 타이머 백그라운드 실행
├── utils/                # 유틸리티 클래스들
└── receivers/            # BroadcastReceiver들
```

## 🗃️ 데이터베이스 설계

### Room Database v10 (현재 최신)
6개 핵심 엔티티 + 마이그레이션 시스템

#### 1. Alarm (기본 알람)
```kotlin
@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val time: String,              // "HH:MM" 형식
    val label: String = "",        // 알람명
    val isActive: Boolean = true,  // 활성화 여부
    val repeatDays: String = "",   // JSON: 요일 배열
    val soundUri: String = "",     // 알람음 URI
    val volume: Float = 1.0f,      // 볼륨 (0.0-1.0)
    val vibrationEnabled: Boolean = true,  // 진동 여부
    val snoozeEnabled: Boolean = true,     // 스누즈 허용
    val snoozeInterval: Int = 5    // 스누즈 간격(분)
)
```

#### 2. TimerCategory (사용자 정의 카테고리)
```kotlin
@Entity(tableName = "timer_categories") 
data class TimerCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,              // "홈트레이닝", "베이킹" 등
    val icon: String = "⏰",       // 이모지 아이콘
    val color: String = "#3B82F6", // 카테고리 색상
    val sortOrder: Int = 0         // 표시 순서
)
```

#### 3. TimerTemplate (타이머 템플릿)
```kotlin
@Entity(tableName = "timer_templates")
data class TimerTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,              // "타이머1", "타이머2" 등
    val categoryId: Int,           // TimerCategory 외래키
    val soundUri: String = "",     // 알람음 URI  
    val volume: Float = 1.0f,      // 볼륨
    val vibrationEnabled: Boolean = true // 진동 여부
)
```

#### 4. TimerRound (타이머 라운드)
```kotlin
@Entity(tableName = "timer_rounds")
data class TimerRound(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val templateId: Int,           // TimerTemplate 외래키
    val roundIndex: Int,           // 라운드 순서
    val name: String,              // 라운드명
    val duration: Int,             // 지속 시간(초)
    val color: String = "#3B82F6"  // 라운드 색상
)
```

#### 5. SleepRecord (수면 기록) - 구현 중
```kotlin  
@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bedtime: Long,             // 취침 시간 (timestamp)
    val wakeupTime: Long?,         // 기상 시간 (nullable)
    val totalDuration: Long = 0,   // 총 수면 시간 (밀리초)
    val qualityScore: Float = 0f,  // 수면 품질 점수 (1-5)
    val isActive: Boolean = false  // 현재 수면 중 여부
)
```

#### 6. ReportData (리포트 데이터) - 구현 중
```kotlin
@Entity(tableName = "report_data") 
data class ReportData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,              // "YYYY-MM-DD" 형식
    val alarmCount: Int = 0,       // 일일 알람 사용 횟수
    val timerCount: Int = 0,       // 일일 타이머 사용 횟수
    val sleepDuration: Long = 0,   // 수면 시간 (밀리초)
    val dataJson: String = ""      // 추가 데이터 (JSON)
)
```

### 마이그레이션 히스토리
- v1→v2: volume, vibration 필드 추가
- v3→v4: TimerCategory 도입, 정규화
- v8→v9: SleepRecord 테이블 추가  
- v9→v10: ReportData 테이블 추가

## 🎯 UI/UX 설계

### 메인 네비게이션
```
MainActivity (BottomNavigationView)
├── 🔔 AlarmFragment - 기본 알람 리스트
├── ⏱️ CustomTimerFragment - 커스텀 타이머 (탭별 카테고리)
├── 🌙 SleepFragment - 수면 추적 및 분석
├── 📊 ReportFragment - 사용 통계 리포트
└── ⚙️ SettingsFragment - 앱 설정
```

### 핵심 화면 설계

#### AlarmFragment
- **RecyclerView**: 알람 카드 리스트
- **FloatingActionButton**: 새 알람 추가
- **알람 카드**: 시간 + 알람명 + 활성화 스위치 + 요일 Chip들

#### AddEditAlarmActivity  
- **커스텀 TimePicker**: AM/PM + 시간 + 분 (NumberPicker 조합)
- **요일 선택**: 세모알 브랜드 색상 Chip (선택시 네온블루, 미선택시 딥그레이)
- **소리 & 진동**: 볼륨 0% ↔ 진동 모드 자동 전환 시스템

#### CustomTimerFragment
- **동적 TabLayout**: 사용자 생성 카테고리별 탭
- **타이머 카드**: 실시간 카운트다운 표시 (20:00→19:59→00:00)
- **롱클릭 편집**: 어디서든 즉시 편집 모드 진입

#### AddEditTimerActivity
- **자동 이름 생성**: "타이머1", "타이머2" 순차 생성
- **시간:분:초 NumberPicker**: 직관적 시간 설정
- **소리 & 볼륨**: 알람과 동일한 진동-소리 시스템

## ⚡ 혁신적 알람 시스템 설계

### 문제: Android 14+ 백그라운드 제약
기존: `AlarmManager → BroadcastReceiver → Service` (차단됨)

### 해결: NotificationAlarmManager
새로운: `AlarmManager → NotificationAlarmManager → 직접 알림`

```kotlin
class NotificationAlarmManager(private val context: Context) {
    fun scheduleAlarm(alarm: Alarm) {
        // AlarmManager로 정확한 시간에 스케줄링
        // PendingIntent로 AlarmNotificationReceiver 호출
    }
}

class AlarmNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 즉시 풀스크린 알림 + 사운드 + 진동
        // 시스템 제약 완전 우회
    }
}
```

### 장점
- **100% 확실한 동작**: 시스템 제약 우회
- **즉시 반응**: 풀스크린 알림으로 확실한 알림
- **30초 테스트**: 빠른 검증 가능

## 🎵 오디오 시스템 설계

### 볼륨-진동 연동 시스템
```
볼륨 0% = 자동 진동 모드 ON (딥 그레이 색상)
볼륨 1%+ = 진동 모드 OFF (네온 블루 색상)
```

### 실시간 색상 피드백
- SeekBar 조작 시 즉시 색상 변화
- 진동 모드 스위치 토글로 볼륨 자동 조정

## 🔄 백그라운드 서비스 설계

### TimerForegroundService
```kotlin
class TimerForegroundService : Service() {
    // 타이머 백그라운드 실행
    // 실시간 카운트다운 브로드캐스트
    // 완료시 알람 발생
}
```

### 브로드캐스트 시스템
```kotlin
// 타이머 상태 브로드캐스트
const val ACTION_TIMER_UPDATE = "com.semo.alarm.TIMER_UPDATE"
const val ACTION_TIMER_COMPLETE = "com.semo.alarm.TIMER_COMPLETE"
```

## 🎯 개발 우선순위 설계

### Phase 1: 핵심 안정화 (현재 진행 중)
1. **타이머 편집 버그 수정**: `AddEditTimerActivity.kt:302` loadExistingTemplate() 구현
2. **타이머 기본값 수정**: 5분 → 0초로 변경
3. **알람 자동 꺼짐 문제 해결**: 로그 분석 및 원인 파악

### Phase 2: 수면 기능 구현
1. **SleepFragment UI 완성**: 취침/기상 버튼, 상태 표시
2. **수면 추적 로직**: SleepTrackingService 구현  
3. **데이터 시각화**: MPAndroidChart로 7일/30일 패턴 표시

### Phase 3: 리포트 시스템
1. **데이터 수집**: 알람/타이머 사용 통계 자동 생성
2. **차트 구현**: 사용 패턴 시각화
3. **개인화된 인사이트**: 사용 패턴 기반 조언

## 🚧 알려진 기술적 이슈

### 1. 타이머 편집 시스템
- **문제**: 편집 모드에서 새 타이머 생성됨  
- **원인**: loadExistingTemplate() 메서드 미구현
- **해결**: 기존 타이머 데이터 로딩 로직 구현

### 2. 데이터베이스 마이그레이션
- **현재**: v10까지 완성
- **이슈**: 복잡한 마이그레이션으로 인한 잠재적 데이터 손실 위험
- **대응**: 각 마이그레이션 단계별 테스트 필요

### 3. 메모리 최적화
- **이슈**: 실시간 카운트다운으로 인한 1초마다 UI 업데이트
- **최적화**: 값 변경 시에만 업데이트하는 조건부 로직 추가 고려

## 🔮 미래 확장 계획

### 단기 (1-2개월)
- 수면 추적 완성
- 리포트 시스템 구현
- 스마트 알람 (얕은 잠에서 깨우기)

### 중기 (3-6개월)  
- 위젯 지원
- 백업/복원 시스템
- 음성 명령 지원

### 장기 (6개월+)
- Apple Watch 연동
- AI 기반 개인화된 알람
- 소셜 기능 (가족 알람 공유)

---

이 설계 문서는 세모알 프로젝트의 기술적 청사진을 제공하며, 개발 과정에서 지속적으로 업데이트됩니다.