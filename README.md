# 세모알 (SemoR) - Android 알람 통합 앱

**세모알**은 다양한 종류의 알람을 통합적으로 관리할 수 있는 안드로이드 네이티브 애플리케이션입니다.

## 📱 주요 기능

### 알람 기능
- ✅ 다양한 알람 유형 (기본, 리마인더, 복약, 운동, 업무, 사용자 정의)
- ✅ 반복 설정 (한 번, 매일, 매주, 평일, 주말)
- ✅ 안드로이드 네이티브 알림
- ✅ 백그라운드 알람 (앱이 닫혀도 작동)
- ✅ 정확한 시간 알람 (`SCHEDULE_EXACT_ALARM`)
- ✅ 부팅 후 자동 복원

### 알람 유형
- **기본 알람**: 일반적인 알람
- **리마인더**: 중요한 일정 알림
- **복약 알림**: 약 복용 시간 알림
- **운동 알림**: 운동 시간 알림
- **업무 알림**: 업무 관련 알림
- **사용자 정의**: 개인 맞춤 알람

## 🛠️ 기술 스택

- **Frontend**: React 18 + TypeScript
- **Mobile Framework**: Capacitor
- **상태 관리**: Zustand (로컬스토리지 연동)
- **스타일링**: CSS3 (그라디언트, 글래스모피즘)
- **빌드 도구**: Vite
- **아이콘**: Lucide React
- **날짜 처리**: date-fns

## 🚀 개발 환경 설정

### 필수 요구사항
- **Node.js** 18+
- **Android Studio**
- **Android SDK** (API 33+)
- **Java** 17+

### 설치 및 빌드

1. **의존성 설치**
```bash
npm install
```

2. **Android Studio 열기**
```bash
npm run android:build
```

3. **개발용 실행** (디바이스/에뮬레이터 필요)
```bash
npm run android:run
```

4. **프로젝트 빌드 및 동기화**
```bash
npm run sync
```

## 📦 APK 생성

### Release APK 빌드
1. **Android Studio에서 프로젝트 열기**
```bash
npm run android:open
```

2. **APK 생성**
   - `Build` → `Generate Signed Bundle / APK`
   - `APK` 선택 → `Next`
   - 키스토어 생성 또는 기존 키 사용
   - `Release` 빌드 선택
   - APK 생성 완료

### Debug APK (개발용)
```bash
# Android Studio에서 직접 빌드하거나
./gradlew assembleDebug
```

## 🏗️ 프로젝트 구조

```
src/
├── components/          # React 컴포넌트
│   ├── Header.tsx      # 앱 헤더
│   ├── AddAlarmForm.tsx # 알람 추가 폼
│   ├── AlarmList.tsx   # 알람 목록
│   └── AlarmItem.tsx   # 개별 알람 아이템
├── services/           # 네이티브 서비스
│   └── alarmService.ts # 안드로이드 알람 스케줄링
├── stores/             # 상태 관리
│   └── alarmStore.ts   # Zustand 알람 스토어
├── types/              # TypeScript 타입 정의
│   └── index.ts        # 공통 타입
├── utils/              # 유틸리티 함수
│   ├── dateUtils.ts    # 날짜 관련 유틸리티
│   └── useAlarmManager.ts # 알람 매니저 훅
└── App.tsx             # 메인 앱 컴포넌트

android/                # 안드로이드 네이티브 프로젝트
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml # 앱 권한 및 설정
│       └── java/              # Java/Kotlin 코드
└── gradle/            # Gradle 빌드 설정
```

## 🔐 앱 권한

### 필요한 권한들
- `POST_NOTIFICATIONS`: 알림 표시
- `SCHEDULE_EXACT_ALARM`: 정확한 시간 알람
- `USE_EXACT_ALARM`: 시스템 알람 사용
- `VIBRATE`: 진동 알람
- `WAKE_LOCK`: 화면 깨우기
- `RECEIVE_BOOT_COMPLETED`: 부팅 후 자동 시작

## 🎨 디자인

- **테마**: 모던한 그라디언트 디자인
- **색상**: 보라-파랑 그라디언트 (#667eea → #764ba2)
- **효과**: 글래스모피즘, 블러 효과
- **반응형**: 다양한 안드로이드 화면 크기 지원

## 🔧 개발 참고사항

### 알람 시스템
- **Capacitor LocalNotifications**: 네이티브 알람 API
- **정확한 스케줄링**: Android의 정확한 알람 시스템 활용
- **백그라운드 처리**: 앱 종료 후에도 알람 작동
- **시스템 통합**: Android 알림 시스템과 완전 통합

### 데이터 저장
- **Zustand**: 간단하고 효율적인 상태 관리
- **LocalStorage**: 디바이스 로컬 저장소
- **TypeScript**: 완전한 타입 안전성

## 📱 지원 Android 버전
- **최소 버전**: Android 7.0 (API 24)
- **타겟 버전**: Android 13+ (API 33+)
- **권장 버전**: Android 10+ (API 29+)

## 🤝 기여하기

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

MIT License - 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

---

## 🚨 중요 노트

이 앱은 **안드로이드 전용**으로 개발되었습니다. Capacitor를 통해 React 웹 기술을 사용하지만, 네이티브 안드로이드 기능에 완전히 의존합니다.