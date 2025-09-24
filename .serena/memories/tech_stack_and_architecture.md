# 기술 스택 및 아키텍처

## 핵심 기술 스택
- **Language**: Kotlin
- **Architecture**: MVVM + Repository Pattern
- **Dependency Injection**: Hilt (Dagger)
- **Database**: Room SQLite (version 2.6.1)
- **UI Framework**: Material3 + ViewBinding + DataBinding
- **Build System**: Gradle with Android Gradle Plugin 8.1.4

## Android 설정
- **Namespace**: com.semo.alarm
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

## 주요 라이브러리
### Core Android
- androidx.core:core-ktx:1.12.0
- androidx.appcompat:appcompat:1.6.1
- com.google.android.material:material:1.11.0

### Architecture Components
- androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0
- androidx.lifecycle:lifecycle-livedata-ktx:2.7.0
- androidx.navigation:navigation-fragment-ktx:2.7.6

### Database
- androidx.room:room-runtime:2.6.1
- androidx.room:room-ktx:2.6.1
- androidx.room:room-compiler:2.6.1 (KSP)

### Dependency Injection
- com.google.dagger:hilt-android:2.48
- com.google.dagger:hilt-compiler:2.48 (KSP)

### Background Processing
- androidx.work:work-runtime-ktx:2.9.0

## 프로젝트 구조
```
app/src/main/java/com/semo/alarm/
├── ui/                    # UI 레이어
│   ├── activities/        # Activities (7개)
│   ├── fragments/         # Fragments
│   ├── adapters/          # RecyclerView Adapters
│   └── viewmodels/        # ViewModels
├── data/                  # 데이터 레이어
│   ├── entities/          # Room Entities (11개)
│   ├── dao/              # Data Access Objects
│   ├── database/         # Database Configuration
│   ├── repositories/     # Repository Pattern
│   └── enums/            # Enums
├── services/             # Background Services
├── receivers/            # BroadcastReceivers
├── utils/                # Utility Classes
├── di/                   # Hilt Dependency Injection
└── character/            # Character System
```

## 아키텍처 패턴
- **MVVM**: Model-View-ViewModel with LiveData
- **Repository Pattern**: Data abstraction layer
- **Dependency Injection**: Hilt for clean architecture
- **Single Activity**: Navigation Component with Fragments