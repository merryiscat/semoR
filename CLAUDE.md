# 알람 앱 설계 문서

세모알 - 세상의 모든 알람

## 프로젝트 개요
다양한 커스텀 기능과 수면 관리 기능을 포함한 스마트 알람 애플리케이션

## 주요 기능

### 1. 기본 알람 기능
- 알람 추가 및 편집
- 반복 설정 (요일별, 매일, 주말/평일)
- 알람명 설정

### 2. 커스텀 알람 기능
- **운동 알람**: 인터벌 타이머, 세트 알림 등
- **식사 알람**: 반숙 타이머, 요리 타이머 등
- 사용자 정의 타이머 템플릿

### 3. 수면 체크 기능
- 취침/기상 시간 추적
- 코골이 감지 및 기록
- 수면 패턴 분석

### 4. 알람 리포트
- 알람 해제 소요 시간 기록
- 알람 사용 패턴 분석
- 수면 품질 리포트

### 5. 설정
- 앱 정보 및 버전
- 알림 권한 설정
- 백업/복원

## 추가 제안 기능

### 사용성 개선
- 스누즈 옵션 (간격, 횟수 설정)
- 점진적 볼륨 증가 (gentle wake-up)
- 알람음 커스터마이징 (음악, 자연 소리 등)
- 진동 패턴 설정

### 스마트 기능
- 스마트 기상 (얕은 잠에서 깨우기)
- 날씨 연동 알람
- 위치 기반 알람

### 편의 기능
- 주말/평일 다른 설정
- 알람 그룹/카테고리 관리
- 음성 명령 지원
- 위젯 지원

### 데이터 관리
- 백업/복원 기능
- 계정 동기화
- 수면 패턴 분석 및 조언

## 기술 스택 및 아키텍처 (안드로이드 네이티브)

### 플랫폼 선택
- **Android Native** (Kotlin/Java)
- **Android Studio** (개발 환경)
- **Gradle** (빌드 시스템)

### 아키텍처
- **MVVM** (Model-View-ViewModel)
- **Architecture Components** (LiveData, ViewModel, Room)
- **Data Binding** (양방향 데이터 바인딩)

### 로컬 데이터베이스
- **Room Database** (SQLite ORM)
- **SharedPreferences** (설정값 저장)

### 백그라운드 작업
- **AlarmManager** (정확한 알람 스케줄링)
- **WorkManager** (백그라운드 태스크)
- **Foreground Service** (지속적인 백그라운드 작업)

### UI/UX
- **Material Design 3** (Google 디자인 시스템)
- **Navigation Component** (프래그먼트 네비게이션)
- **ViewBinding** (뷰 바인딩)
- **RecyclerView** (리스트 표시)

### 오디오/멀티미디어
- **MediaPlayer** (알람음 재생)
- **AudioManager** (볼륨 제어)
- **MediaRecorder** (코골이 감지용)

### 권한 및 시스템
- **Notification API** (알림 표시)
- **Permission API** (권한 관리)
- **Sensor API** (움직임 감지)

### 차트/그래프
- **MPAndroidChart** (데이터 시각화)

### 기타
- **Coroutines** (비동기 처리)
- **Hilt/Dagger** (의존성 주입)
- **Retrofit** (네트워크 - 필요시)

## 앱 아키텍처 (안드로이드 네이티브)

```
app/src/main/java/com/semo/alarm/
├── ui/                    # UI 레이어
│   ├── activities/        # 액티비티
│   ├── fragments/         # 프래그먼트
│   ├── adapters/          # RecyclerView 어댑터
│   └── viewmodels/        # ViewModel 클래스
├── data/                  # 데이터 레이어  
│   ├── database/          # Room 데이터베이스
│   ├── entities/          # 데이터베이스 엔티티
│   ├── dao/              # Data Access Object
│   └── repositories/      # 데이터 저장소
├── domain/               # 도메인 레이어
│   ├── models/           # 도메인 모델
│   ├── usecases/         # 비즈니스 로직
│   └── interfaces/       # 인터페이스
├── services/             # 백그라운드 서비스
│   ├── AlarmService      # 알람 서비스
│   ├── NotificationService # 알림 서비스
│   └── SleepTrackingService # 수면 추적 서비스
├── utils/                # 유틸리티
├── di/                   # 의존성 주입 (Hilt)
└── receivers/            # BroadcastReceiver
```

## UI/UX 플로우

### 메인 네비게이션 구조
```
MainActivity + BottomNavigationView + Fragment
├── AlarmFragment (홈) - 메인 알람 리스트
├── CustomTimerFragment - 운동/식사 타이머
├── SleepFragment - 수면 추적 및 분석
├── ReportFragment - 사용 통계 및 분석
└── SettingsFragment - 앱 설정
```

### 주요 화면 플로우

#### 1. AlarmFragment (알람 홈)
- **RecyclerView**: 알람 리스트 표시 (활성/비활성)
- **FloatingActionButton**: 새 알람 추가
- **ItemTouchHelper**: 스와이프로 편집/삭제
- **Switch**: 알람 활성화/비활성화

#### 2. AddEditAlarmActivity (알람 생성/편집)
- **TimePicker**: 시간 설정
- **CheckBox**: 요일별 반복 설정
- **Spinner**: 알람음 선택
- **EditText**: 알람명 입력
- **Switch + SeekBar**: 스누즈 설정

#### 3. CustomTimerFragment (커스텀 타이머)
- **TabLayout**: 카테고리 탭 (운동, 식사, 기타)
- **RecyclerView**: 타이머 템플릿 목록
- **Dialog**: 커스텀 타이머 생성

#### 4. SleepFragment (수면 추적)
- **CardView**: 수면 시작/종료 버튼
- **LineChart**: 수면 패턴 그래프 (MPAndroidChart)
- **RecyclerView**: 코골이 기록 리스트

#### 5. ReportFragment (리포트)
- **ViewPager2 + TabLayout**: 주간/월간 탭
- **BarChart/LineChart**: 알람 사용 통계
- **CardView**: 수면 품질 요약

### 알림 및 상호작용
- **NotificationManager**: 알람 알림 표시
- **AlarmManager**: 정확한 시간에 알람 발생  
- **BroadcastReceiver**: 알람 해제/스누즈 처리
- **Foreground Service**: 백그라운드에서 지속 실행

## 데이터베이스 스키마

### SQLite 테이블 설계

#### 1. alarms (기본 알람)
```sql
CREATE TABLE alarms (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  time TEXT NOT NULL,                -- 'HH:MM' 형식
  label TEXT,                        -- 알람명
  is_active BOOLEAN DEFAULT 1,       -- 활성화 여부
  days TEXT,                         -- JSON: ['mon','tue',...] 또는 'daily'
  sound_uri TEXT,                    -- 알람음 경로
  volume REAL DEFAULT 0.7,           -- 볼륨 (0.0-1.0)
  snooze_enabled BOOLEAN DEFAULT 1,  -- 스누즈 허용
  snooze_interval INTEGER DEFAULT 5, -- 스누즈 간격(분)
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### 2. custom_alarms (커스텀 알람/타이머)
```sql
CREATE TABLE custom_alarms (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,                -- 타이머명
  category TEXT NOT NULL,            -- 'exercise', 'cooking', 'other'
  type TEXT NOT NULL,                -- 'interval', 'countdown', 'sequence'
  config TEXT NOT NULL,              -- JSON 설정 (시간, 반복 등)
  is_template BOOLEAN DEFAULT 0,     -- 템플릿 여부
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### 3. sleep_records (수면 기록)
```sql
CREATE TABLE sleep_records (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sleep_start DATETIME,              -- 취침 시간
  sleep_end DATETIME,                -- 기상 시간
  total_duration INTEGER,            -- 총 수면 시간(분)
  quality_score REAL,                -- 수면 품질 점수 (1-5)
  snoring_detected BOOLEAN DEFAULT 0,-- 코골이 감지 여부
  snoring_data TEXT,                 -- JSON: 코골이 시간대 데이터
  created_at DATE DEFAULT (date('now'))
);
```

#### 4. alarm_logs (알람 사용 기록)
```sql
CREATE TABLE alarm_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  alarm_id INTEGER,                  -- alarms 테이블 참조
  alarm_type TEXT DEFAULT 'basic',   -- 'basic', 'custom'
  triggered_at DATETIME,             -- 알람 발생 시간
  dismissed_at DATETIME,             -- 알람 해제 시간
  snooze_count INTEGER DEFAULT 0,    -- 스누즈 횟수
  response_time INTEGER,             -- 반응 시간(초)
  created_at DATE DEFAULT (date('now')),
  FOREIGN KEY (alarm_id) REFERENCES alarms (id)
);
```

#### 5. settings (앱 설정)
```sql
CREATE TABLE settings (
  key TEXT PRIMARY KEY,              -- 설정 키
  value TEXT,                        -- 설정 값 (JSON 문자열)
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### SharedPreferences 저장 데이터
- `user_preferences`: 사용자 기본 설정
- `app_version`: 앱 버전 정보
- `notification_permissions`: 알림 권한 상태
- `onboarding_completed`: 온보딩 완료 여부
- `default_alarm_sound`: 기본 알람음 URI
- `theme_mode`: 다크/라이트 모드 설정

### 데이터 관계
- `alarms` ← `alarm_logs` (1:N)
- `custom_alarms` → `alarm_logs` (참조 관계)
- `sleep_records` (독립적)
- `settings` (독립적)

## 안드로이드 네이티브 개발 계획

### Phase 1: 프로젝트 기반 구축
1. ✅ 기능 분석 및 기술 스택 선정
2. ✅ 안드로이드 네이티브 아키텍처 설계  
3. ✅ UI/UX 플로우 및 데이터베이스 스키마 설계
4. 🔄 Android Studio 프로젝트 생성 및 초기 설정

### Phase 2: 데이터 레이어 구현
5. Room 데이터베이스 및 엔티티 생성
6. DAO (Data Access Object) 구현
7. Repository 패턴 구현
8. ViewModel 및 LiveData 설정

### Phase 3: UI 레이어 구현
9. MainActivity 및 BottomNavigationView 구현
10. AlarmFragment 및 RecyclerView 어댑터 구현
11. AddEditAlarmActivity 구현 (TimePicker, 설정 UI)
12. Material Design 3 적용

### Phase 4: 핵심 알람 기능 구현
13. AlarmManager를 이용한 알람 스케줄링
14. BroadcastReceiver 알람 처리
15. NotificationManager 알림 표시
16. 알람음 재생 (MediaPlayer)

### Phase 5: 고급 기능 구현
17. 커스텀 타이머 기능
18. 수면 추적 기능
19. 데이터 시각화 (차트)
20. 백그라운드 서비스 최적화

### Phase 6: 테스트 및 최적화
21. 단위 테스트 작성
22. UI 테스트 작성  
23. 성능 최적화
24. 출시 준비 (ProGuard, 서명)

## MVP (최소 기능 제품) 우선순위

### 🎯 MVP Phase 1: 기본 알람 앱
**목표**: 가장 기본적인 알람 기능만 구현해서 동작하는 앱 완성

#### 포함 기능:
- ✅ 알람 추가/편집/삭제
- ✅ 시간 설정 (TimePicker)
- ✅ 요일별 반복 설정
- ✅ 알람명 설정
- ✅ 스누즈 기능
- ✅ 알람 활성화/비활성화
- ✅ 알람 리스트 표시

#### 제외 기능 (나중에 구현):
- ❌ 커스텀 타이머
- ❌ 수면 추적
- ❌ 리포트
- ❌ 고급 설정
- ❌ 위젯

#### 기술 스택 (MVP):
- **Database**: Room (alarms 테이블만)
- **UI**: MainActivity + AlarmFragment + AddEditAlarmActivity
- **Core**: AlarmManager + BroadcastReceiver + NotificationManager
- **Architecture**: MVVM (기본)

### 📋 MVP 개발 단계
1. 🔄 **MVP 기획 및 범위 확정**
2. Android Studio 프로젝트 생성
3. Room Database (alarms 테이블만)
4. Entity, DAO, Repository, ViewModel
5. MainActivity + 기본 네비게이션
6. AlarmFragment (리스트 화면)
7. AddEditAlarmActivity (생성/편집)
8. AlarmManager 알람 스케줄링
9. BroadcastReceiver 알람 처리
10. NotificationManager 알림 표시

## 현재 진행 상황

### 🎉 **Phase 1 완료**
- ✅ **프로젝트 생성**: Android Studio 프로젝트 완성
- ✅ **아키텍처 구현**: MVVM + Hilt + Room + Material3 
- ✅ **기본 구조**: MainActivity + BottomNavigation + 5개 Fragment
- ✅ **데이터베이스**: Room Database + Alarm Entity + DAO + Repository
- ✅ **의존성 주입**: Hilt 완전 설정

### 🚀 **Phase 2: TimePicker 혁신**
- ✅ **커스텀 TimePicker 구현**: NumberPicker 3개 조합 (AM/PM + 시간 + 분)
- ✅ **AM/PM 앞쪽 배치**: 사용자 요청에 따른 레이아웃 최적화
- ✅ **휠 인터페이스 유지**: "훨씬 예쁜" 원래 휠 방식 보존
- ✅ **스마트 자동 전환**: 
  - 12→1 또는 1→12 변경 시 AM/PM 자동 토글
  - 59분→00분 또는 00분→59분 변경 시 시간 자동 증감
- ✅ **Material3 디자인**: 완벽한 테마 통합
- ✅ **중앙 정렬**: '시간' 라벨 제거 후 깔끔한 중앙 배치

### 🎯 **완성된 AddEditAlarmActivity 기능**
- ✅ **시간 설정**: 커스텀 NumberPicker TimePicker
- ✅ **12↔24시간 변환**: 완벽한 시간 포맷 변환 로직
- ✅ **요일별 반복**: Material Chip을 이용한 요일 선택
- ✅ **알람명 입력**: TextInputEditText
- ✅ **스누즈 설정**: Switch + SeekBar (1-15분)
- ✅ **편집 모드**: 기존 알람 수정 완벽 지원

### 🔧 **기술적 해결사항**
- ✅ **Android Resource Linking 오류 해결**: Private 리소스 문제 해결
- ✅ **NumberPicker 스타일링**: Material3 테마 적용
- ✅ **리플렉션 제거**: 복잡한 리플렉션 대신 깔끔한 테마 적용
- ✅ **빌드 최적화**: 모든 빌드 오류 해결

### 📱 **UI/UX 혁신**
```
이전: [시간 라벨] [표준 TimePicker]
현재: [AM/PM] [시간] [:] [분] (중앙 정렬)
```

### 🎨 **Material3 디자인 시스템**
- ✅ **다크 테마**: 검정 + 회색 + 파란색 하이라이트 조합
- ✅ **Material Cards**: 둥근 모서리, 적절한 elevation
- ✅ **Color Scheme**: 완벽한 Material3 색상 팔레트
- ✅ **Typography**: 일관된 폰트 시스템

### ⚙️ **다음 작업 (Phase 3)**
- 🔄 **알람 리스트**: AlarmFragment + RecyclerView 구현  
- 🔄 **알람 스케줄링**: AlarmManager 통합
- 🔄 **알람 알림**: NotificationManager + BroadcastReceiver
- 🔄 **데이터 바인딩**: ViewModel과 UI 연결

### 💡 **주요 성과**
1. **사용자 중심 디자인**: "AM PM을 앞쪽으로", "휠 방식이 훨씬 예쁜데" 요청 완벽 반영
2. **기술적 혁신**: 표준 TimePicker 한계를 뛰어넘는 커스텀 구현
3. **UX 혁신**: 직관적인 시간 변환 (12→1에서 AM/PM 자동 전환)
4. **Material3 완벽 적용**: 네이티브 Android 디자인 가이드라인 준수

**현재 상태**: MVP Phase 1의 핵심인 **AddEditAlarmActivity가 완전히 완성**되었으며, 혁신적인 TimePicker UX가 구현되었습니다! 🎉