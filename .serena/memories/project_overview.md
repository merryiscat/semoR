# 세모알(semoR) 프로젝트 개요

## 프로젝트 정보
- **이름**: 세모알 (semoR) - 차세대 스마트 알람 앱
- **타입**: Android 네이티브 애플리케이션
- **언어**: Kotlin
- **완성도**: 90% (출시 가능한 수준)

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
- 🔔 기본 알람: NotificationAlarmManager로 Android 14+ 백그라운드 제약 완전 우회
- ⏱️ 커스텀 타이머: 사용자 정의 카테고리, 실시간 카운트다운, 백그라운드 실행
- 🎨 브랜드 UI: Material3 + 네온블루(#00D4FF)/딥그레이(#6B7280) 조합
- 🏗️ 아키텍처: Room Database, MVVM + Hilt, 6개 핵심 엔티티

### 🚧 진행 중
- 🌙 수면 체크 기능 (75%): SleepRecord 엔티티, 기본 UI 완성
- 📊 리포트 기능 (40%): 탭 구조 완성, 데이터 수집 로직 구현 필요

## 차별화 요소
1. 🎨 독창적 브랜드: 네온 블루 기반 미래지향적 디자인
2. ⚡ 확실한 동작: Android 최신 제약 우회하는 혁신 기술
3. 🔧 완전한 개인화: 무제한 카테고리 & 타이머 시스템
4. 🔊 스마트 오디오: 볼륨-진동 모드 자동 전환
5. 📱 직관적 UX: 클릭 한 번으로 모든 기능 접근