# 🐛 세모알(semoR) 버그 리포트

## 🔴 우선순위: 높음

### 1. 요일별 반복 알람 자동 해제 미작동
**상태**: 미해결  
**발견일**: 2025-09-08  
**심각도**: 높음

#### 문제 설명
- 요일카드에서 "한 번만" 옵션으로 설정한 알람이 한 번 울린 후에도 자동으로 OFF되지 않음
- 사용자가 수동으로 알람 스위치를 꺼야 하는 상황

#### 재현 방법
1. 새 알람 생성
2. 요일카드에서 "한 번만" 선택
3. 알람 시간 설정 및 활성화
4. 알람이 울린 후 확인
5. **예상**: 알람 스위치가 자동으로 OFF
6. **실제**: 알람 스위치가 ON 상태 유지

#### 추가 확인 필요 사항
- [ ] "2회 반복" 설정 시 2회 후 자동 해제 여부
- [ ] "3회 반복" 설정 시 3회 후 자동 해제 여부
- [ ] 요일별 반복과의 상호작용 확인

#### 예상 원인
- `AlarmReceiver`에서 반복 횟수 체크 로직 누락
- 알람 실행 후 상태 업데이트 로직 부재
- Database 업데이트 누락

#### 관련 파일
- `app/src/main/java/com/semo/alarm/receivers/AlarmReceiver.kt`
- `app/src/main/java/com/semo/alarm/data/entities/Alarm.kt`
- `app/src/main/java/com/semo/alarm/data/repositories/AlarmRepository.kt`

---

## 🟡 우선순위: 중간

### 2. 코골이 감지 효율성 문제
**상태**: 미해결  
**발견일**: 2025-09-10
**심각도**: 중간

#### 문제 설명
- 수면 추적 시 코골이 감지가 70% 시간대를 놓치는 문제
- 10초마다 3초간만 감지하는 비효율적 알고리즘

#### 재현 방법
1. 수면 추적 시작
2. 밤새 코골이 감지 기능 동작
3. **예상**: 코골이 이벤트가 적절히 감지됨
4. **실제**: 대부분의 코골이를 놓침

#### 예상 원인
- 배터리 최적화를 위한 간헐적 감지 로직
- 코골이의 불규칙적 패턴과 감지 주기 불일치

#### 관련 파일
- `app/src/main/java/com/semo/alarm/utils/SnoringDetector.kt:33-40`
- `app/src/main/java/com/semo/alarm/services/SleepTrackingService.kt`

### 3. 타이머 볼륨바 색상 표시 오류
**상태**: 미해결  
**발견일**: 2025-09-08  
**심각도**: 낮음 (UI 개선)

#### 문제 설명
- 타이머 추가/편집 화면에서 볼륨이 0일 때 볼륨바 전체가 네온블루색으로 표시됨
- 볼륨 0일 때는 그레이색으로 표시되어야 함 (브랜드 가이드라인 준수)

#### 재현 방법
1. 타이머 추가 또는 편집 화면 진입
2. 볼륨을 0으로 설정
3. **예상**: 볼륨바가 그레이색 (#6B7280) 표시
4. **실제**: 볼륨바 전체가 네온블루색 (#00D4FF) 표시

#### 예상 원인
- SeekBar의 progress/track 색상 조건부 적용 로직 누락
- 볼륨 0 상태에 대한 UI 상태 관리 부재

#### 관련 파일
- `app/src/main/java/com/semo/alarm/ui/activities/AddEditTimerActivity.kt`
- 타이머 레이아웃 XML 파일
- `app/src/main/res/values/colors.xml`

### 4. 캐릭터 이미지 관리 구조 확장성 문제
**상태**: 미해결  
**발견일**: 2025-09-09  
**심각도**: 낮음 (장기 과제)

#### 문제 설명
- 현재 캐릭터 이미지들이 메인 drawable 폴더에 플랫하게 저장됨
- 28개 캐릭터 확장 시 drawable 폴더가 300개+ 이미지로 폭발적 증가 예상
- 현재 구조: `character_merry_attention_01.png` 방식으로 하드코딩

#### 장기 개선 방안
1. **assets 폴더 활용** (권장)
   ```
   assets/characters/
   ├── merry/idle/, attention/, spinning/
   ├── luna/idle/, attention/ 
   └── ruby/...
   ```

2. **CharacterConfig 동적 로딩**
   - 하드코딩된 `ATTENTION_FRAMES` 배열 → 동적 리소스 매핑
   - 캐릭터별 리소스 매니저 구현

#### 관련 파일
- `app/src/main/java/com/semo/alarm/character/CharacterConfig.kt`
- `app/src/main/res/drawable/` (전체 구조)

#### 우선순위
- 현재는 메리 캐릭터만 있어 급하지 않음
- 다중 캐릭터 개발 시작 전 리팩토링 필요

---

## 📝 버그 리포트 작성 가이드

새로운 버그 발견 시 다음 형식으로 추가:

```markdown
### N. 버그 제목
**상태**: 미해결/진행중/해결됨  
**발견일**: YYYY-MM-DD  
**심각도**: 높음/중간/낮음

#### 문제 설명
- 구체적인 문제 상황 설명

#### 재현 방법
1. 단계별 재현 방법

#### 예상 원인
- 추정되는 원인

#### 관련 파일
- 관련 소스코드 파일 경로
```

---

## 🚀 해결된 버그

### ✅ 타이머 편집 버그 (해결됨)
**해결일**: 2025-09-10  
**위치**: `AddEditTimerActivity.kt:302`  
**문제**: 편집 시 새 타이머 생성되던 문제

### ✅ 타이머 기본값 문제 (해결됨)  
**해결일**: 2025-09-10  
**위치**: `AddEditTimerActivity.kt:104`  
**문제**: 5분 → 0초로 기본값 변경 완료