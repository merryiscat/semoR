# 코드 스타일 및 컨벤션

## Kotlin 코딩 스타일
### 네이밍 컨벤션
- **클래스**: PascalCase (예: `MainActivity`, `AlarmRepository`)
- **함수/변수**: camelCase (예: `onCreate`, `alarmManager`)
- **상수**: UPPER_SNAKE_CASE (예: `DEFAULT_SNOOZE_INTERVAL`)
- **패키지**: lowercase (예: `com.semo.alarm.ui.activities`)

### 파일 구조
- **Activities**: `ui/activities/` 디렉토리
- **Fragments**: `ui/fragments/` 디렉토리
- **ViewModels**: `ui/viewmodels/` 디렉토리
- **Entities**: `data/entities/` 디렉토리
- **DAOs**: `data/dao/` 디렉토리
- **Repositories**: `data/repositories/` 디렉토리

## Android 특화 컨벤션
### ViewBinding 사용
```kotlin
private lateinit var binding: ActivityMainBinding

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
}
```

### Hilt 의존성 주입
```kotlin
@HiltAndroidApp
class SemoAlarmApplication : Application()

@AndroidEntryPoint
class MainActivity : AppCompatActivity()

@Inject
lateinit var repository: AlarmRepository
```

### Room Database 패턴
```kotlin
@Entity(tableName = "alarms")
data class Alarm(...)

@Dao
interface AlarmDao { ... }

@Repository
class AlarmRepository @Inject constructor(...)
```

## UI/UX 컨벤션
### 브랜드 컬러 사용
- **네온 블루**: `#00D4FF` (활성화 상태, 주요 버튼)
- **딥 그레이**: `#6B7280` (비활성화 상태, 보조 요소)
- **Pure White**: `#FFFFFF` / **Deep Black**: `#000000` (텍스트)

### Material3 컴포넌트 우선 사용
- Material Design 3 가이드라인 준수
- 일관된 UI/UX 패턴 유지

## 주석 및 문서화
### 한국어 주석 허용
```kotlin
// 기본 카테고리 및 타이머 템플릿들 초기화
databaseInitializer.initializeDefaultDataIfNeeded()
```

### 중요 로직에 대한 설명 추가
```kotlin
/**
 * NotificationAlarmManager로 Android 14+ 백그라운드 제약 완전 우회
 */
```

## 에러 처리
- try-catch 블록 적극 활용
- 사용자 친화적 에러 메시지 제공
- 로그 시스템 활용 (특히 수면 이벤트 로그)

## 성능 최적화
- LiveData/ViewModel 패턴으로 메모리 누수 방지
- 백그라운드 처리는 Service 또는 WorkManager 사용
- Room Database 쿼리 최적화