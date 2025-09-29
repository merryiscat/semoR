# 세모알(semoR) 프로젝트 개요

## 프로젝트 정보
- **이름**: 세모알 (semoR) - 차세대 스마트 알람 앱
- **타입**: Android 네이티브 애플리케이션
- **언어**: Kotlin
- **완성도**: 95% (출시 가능한 수준)

## 주요 목적
다양한 커스텀 기능과 수면 관리 기능을 포함한 Android 네이티브 알람 애플리케이션

## 기술 스택
- **언어**: Kotlin
- **아키텍처**: MVVM + Hilt (Dependency Injection)
- **데이터베이스**: Room Database (v10, 10-step migration)
- **UI**: Material3 + ViewBinding + DataBinding
- **백그라운드**: AlarmManager + Foreground Service
- **네비게이션**: Navigation Component

## 현재 기능 상태

### ✅ 완성된 기능 (100%)
- 🔔 **기본 알람**: NotificationAlarmManager로 Android 14+ 백그라운드 제약 완전 우회
- ⏱️ **커스텀 타이머**: 사용자 정의 카테고리, 실시간 카운트다운, 백그라운드 실행
- 🎨 **브랜드 UI**: Material3 + 네온블루(#00D4FF)/딥그레이(#6B7280) 조합
- 🏗️ **아키텍처**: Room Database, MVVM + Hilt, 6개 핵심 엔티티
- ⚙️ **설정**: 앱 설정 관리

### 🚧 진행 중
- 🌙 **수면 체크 기능** (80%): SleepRecord 엔티티, 기본 UI 완성, MPAndroidChart 시각화 구현 필요
- 📊 **리포트 기능** (50%): 탭 구조 완성, 데이터 수집 로직 구현 필요

## 브랜드 아이덴티티

### 브랜드 컬러
- **네온 블루**: `#00D4FF` (활성화 상태, 주요 버튼)
- **딥 그레이**: `#6B7280` (비활성화 상태, 보조 요소)
- **Pure White**: `#FFFFFF` / **Deep Black**: `#000000` (텍스트)

### 디자인 철학
**"Simple yet Futuristic"** - 단순하면서도 미래지향적인 디자인

## 차별화 요소
1. 🎨 **독창적 브랜드**: 네온 블루 기반 미래지향적 디자인
2. ⚡ **확실한 동작**: Android 최신 제약 우회하는 혁신 기술
3. 🔧 **완전한 개인화**: 무제한 카테고리 & 타이머 시스템
4. 🔊 **스마트 오디오**: 볼륨-진동 모드 자동 전환
5. 📱 **직관적 UX**: 클릭 한 번으로 모든 기능 접근

## 핵심 컴포넌트 구조
```
app/src/main/java/com/semo/alarm/
├── ui/                    # Activities & Fragments
├── data/                  # Room Database & Entities
├── services/              # Background Services
├── utils/                 # Utilities
└── receivers/            # BroadcastReceivers
```

## 주요 기능

### 1. 기본 알람
- 시간 설정 (커스텀 TimePicker)
- 요일별 반복 (세모알 브랜드 색상 Chip)
- 스마트 소리 & 진동 (볼륨 0% ↔ 진동 모드 자동 전환)
- 100% 확실한 알람 동작 (NotificationAlarmManager)

### 2. 커스텀 타이머
- **무제한 사용자 카테고리**: "홈트레이닝🏠", "베이킹🧁", "독서📚" 등
- **실시간 카운트다운**: 화면에서 20:00→19:59→00:00 표시
- **백그라운드 실행**: TimerForegroundService
- **롱클릭 편집**: 어디서든 즉시 편집 모드

### 3. 수면 체크 (구현 중)
- 취침/기상 시간 추적
- 수면 패턴 분석 및 시각화 (MPAndroidChart)
- 7일/30일 수면 통계

### 4. 설정 & 리포트
- 앱 설정 관리
- 사용 통계 (구현 중)

## 🚀 다음 개발 목표

### 1️⃣ 수면 체크 완성 (우선순위: 높음)
- MPAndroidChart 라이브러리 통합
- 7일/30일 수면 패턴 시각화
- LineChart/BarChart 활용한 데이터 표시

### 2️⃣ 리포트 시스템 구현 (우선순위: 중간)
- 알람/타이머 사용 통계 자동 생성
- 4주 기반 데이터 보관 시스템
- 개인화된 인사이트 및 사용 패턴 분석

## 프로젝트 상태
**현재 세모알은 출시 가능한 수준의 완성된 앱입니다!** 🎉
- 모든 주요 버그 해결 완료
- 핵심 기능들 100% 동작
- 남은 2개 기능만 완성하면 완전체