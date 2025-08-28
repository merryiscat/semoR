# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소의 코드를 작업할 때 참조할 가이드입니다.

## 주요 개발 명령어

### 빌드 및 개발
- `npm run build` - Vite를 사용한 React 앱 빌드
- `npm run lint` - TypeScript/React 파일에 ESLint 실행
- `npm run type-check` - 파일 생성 없이 TypeScript 타입 검사
- `npm run sync` - Capacitor와 빌드 및 동기화 (네이티브 앱 업데이트)

### Android 개발
- `npm run android:open` - Android Studio에서 프로젝트 열기
- `npm run android:run` - 동기화 후 Android 기기/에뮬레이터에서 실행
- `npm run android:build` - 동기화 후 Android Studio에서 빌드용으로 열기
- `./gradlew assembleDebug` - android/ 디렉토리에서 디버그 APK 빌드

# Feature: Alarm App UI Validation

We want to ensure our alarm application UI matches the given design images.

## Design References
- Main screen mockup: `design/alarm_main.png`
- Alarm setting screen mockup: `design/alarm_setting.png`

## Requirements
1. The UI should exactly match the provided design images in layout, colors, spacing, and typography.
2. Automated tests must:
   - Launch the app in an emulator/browser.
   - Capture screenshots of each screen.
   - Compare them against the baseline design images.
   - Report pixel differences above a 1% threshold.

## Playwright Tasks
- Open app and navigate to **Main Alarm Screen**
- Take a screenshot and compare with `alarm_main.png`
- Navigate to **Alarm Setting Screen**
- Take a screenshot and compare with `alarm_setting.png`
- Fail test if diff > 0.01 (1%)

## Code Expectations
Generate Playwright test code with:
- `page.screenshot()`
- `toMatchSnapshot()` or custom image diff (e.g., `pixelmatch`)
- Clear error messages on mismatch

## 아키텍처 개요

### 핵심 아키텍처 패턴
이 앱은 **React + Capacitor 하이브리드 모바일 앱**으로 Android만을 타겟으로 하며, TypeScript로 구축되었습니다. Capacitor가 네이티브 모바일 기능을 제공하는 일반적인 React 패턴을 따릅니다.

### 상태 관리
- **Zustand 지속성**: 모든 알람 데이터는 `alarmStore.ts`에서 localStorage 자동 지속성과 함께 관리됩니다
- **싱글톤 AlarmService**: `AlarmService` 클래스를 통한 네이티브 알림 스케줄링
- **React 훅 통합**: `useAlarmManager.ts`가 React 상태와 네이티브 서비스를 연결합니다

### 주요 아키텍처 구성 요소

#### 1. 알람 관리 플로우
```
React Components → Zustand Store → AlarmService → Capacitor LocalNotifications → Android System
```

#### 2. 데이터 플로우
- **alarmStore.ts**: Zustand 지속성을 사용한 알람 데이터 CRUD 작업
- **alarmService.ts**: 네이티브 알림 스케줄링을 처리하는 싱글톤 서비스
- **useAlarmManager.ts**: 알람을 네이티브 서비스와 동기화하는 React 훅

#### 3. 네이티브 통합
- **Capacitor 플러그인**: Android 알람 스케줄링용 LocalNotifications
- **Android 권한**: 정확한 알람, 알림, 절전 모드 해제, 부팅 리시버
- **백그라운드 처리**: 안정성을 위해 Android의 정확한 알람 시스템 사용

### TypeScript 아키텍처
- **엄격한 타이핑**: strict 모드가 활성화된 완전한 TypeScript
- **경로 별칭**: `@/*`는 `src/*` 디렉토리로 매핑
- **열거형 기반 타입**: 타입 안전성을 위한 AlarmType과 RepeatType

### 컴포넌트 구조
- **Header.tsx**: 앱 제목과 브랜딩
- **AddAlarmForm.tsx**: 유효성 검사를 포함한 알람 생성 폼
- **AlarmList.tsx**: 모든 알람 목록 렌더링
- **AlarmItem.tsx**: 토글/삭제 액션이 있는 개별 알람

### 중요한 구현 세부사항

#### 알람 ID 처리
앱은 알람을 위해 UUID를 생성하지만 Android 알림을 위해 다음과 같이 숫자 ID로 변환합니다:
```typescript
parseInt(alarmId.replace(/-/g, '').substring(0, 8), 16)
```

#### 서비스 수명주기
- AlarmService는 모든 알림 스케줄링을 관리하는 싱글톤입니다
- useAlarmManager 훅이 서비스 초기화와 정리를 처리합니다
- 정확성을 위해 앱이 보이게 될 때 알람이 다시 스케줄링됩니다

#### 반복 유형 구현
- 특정 반복 패턴과 함께 Capacitor의 schedule API를 사용합니다
- 평일/주말은 사용자 정의 요일 구성을 사용합니다
- 모든 스케줄링은 싱글톤 AlarmService를 통해 발생합니다

## 개발 노트

### Android 전용 고려사항
- 앱은 정확한 알람 권한(SCHEDULE_EXACT_ALARM)이 필요합니다
- LocalNotifications 플러그인과 함께 Capacitor 7.x를 사용합니다
- API 24로의 하위 호환성과 함께 Android API 33+를 타겟으로 합니다
- Gradle 빌드는 Android Studio 통합을 통해 처리됩니다

### Capacitor 구성
- **앱 ID**: com.semor.app
- **앱 이름**: 세모알 (한국어)
- **웹 디렉토리**: dist/ (Vite 빌드 출력)
- **아이콘**: android/app/src/main/res/에 구성됨

### 파일 구조 패턴
- 컴포넌트는 PascalCase 명명을 사용합니다
- 서비스와 유틸리티는 camelCase를 사용합니다
- TypeScript 인터페이스는 구현과 함께 배치됩니다
- 모든 경로는 src/ 디렉토리 import용 @ 별칭을 사용합니다