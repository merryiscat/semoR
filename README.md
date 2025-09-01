# 세모알 (SemoR) - MVP 버전

세상의 모든 알람을 위한 스마트 알람 앱의 MVP (최소 기능 제품) 버전입니다.

## 🎯 MVP 포함 기능

### ✅ 구현 완료된 기능
- 알람 추가/편집/삭제
- 시간 설정 (TimePicker)
- 요일별 반복 설정
- 알람명 설정
- 스누즈 기능 (간격 설정)
- 알람 활성화/비활성화
- 알람 리스트 표시
- 알람 스케줄링 (AlarmManager)
- 알람 알림 (Notification)
- 부팅 후 알람 복원

## 🏗️ 기술 스택

- **Language**: Kotlin
- **Architecture**: MVVM + Repository Pattern
- **Database**: Room (SQLite)
- **DI**: Hilt
- **UI**: Material Design 3
- **Background**: AlarmManager + BroadcastReceiver + Service

## 📁 프로젝트 구조

```
app/src/main/java/com/semo/alarm/
├── ui/                    # UI 레이어
│   ├── activities/        # MainActivity, AddEditAlarmActivity
│   ├── fragments/         # AlarmFragment (기타는 스텁)
│   ├── adapters/          # AlarmAdapter
│   └── viewmodels/        # AlarmViewModel
├── data/                  # 데이터 레이어
│   ├── entities/          # Alarm 엔티티
│   ├── dao/              # AlarmDao
│   ├── database/         # AlarmDatabase
│   └── repositories/     # AlarmRepository
├── services/             # AlarmService
├── receivers/            # AlarmReceiver, BootReceiver
├── utils/                # AlarmScheduler
└── di/                   # Hilt 모듈
```

## 🚀 빌드 방법

1. Android Studio에서 프로젝트 열기
2. Gradle 동기화
3. 에뮬레이터 또는 실제 기기에서 실행

## 📋 권한 요구사항

- `WAKE_LOCK`: 화면이 꺼진 상태에서도 알람 동작
- `RECEIVE_BOOT_COMPLETED`: 부팅 후 알람 재설정
- `VIBRATE`: 진동 알림
- `USE_EXACT_ALARM`: 정확한 시간에 알람 실행
- `SCHEDULE_EXACT_ALARM`: 정확한 알람 스케줄링
- `POST_NOTIFICATIONS`: 알림 표시

## 🔮 향후 계획

MVP에서 제외된 기능들은 다음 버전에서 구현 예정:
- 커스텀 타이머 (운동, 식사 등)
- 수면 추적 및 분석
- 사용 통계 리포트
- 고급 설정 (알람음, 볼륨 등)
- 위젯 지원

## 🐛 알려진 이슈

- 일부 Android 버전에서 배터리 최적화로 인해 알람이 동작하지 않을 수 있음
- 정확한 알람 권한이 필요한 Android 12+ 기기에서 권한 요청 필요