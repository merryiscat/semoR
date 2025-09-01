# 세모알(SemoR) 빌드 가이드

## 🚨 Java 버전 호환성 문제 해결

현재 시스템에 Java 23이 설치되어 있어서 Gradle 8.5와 호환성 문제가 발생합니다.

### 방법 1: Android Studio 사용 (강력 권장)

1. **Android Studio 다운로드**
   - https://developer.android.com/studio
   - Android Studio는 내장 JDK 17을 사용하므로 호환성 문제 없음

2. **프로젝트 열기**
   ```
   File > Open > C:\Users\minhy\llm\SemoR 폴더 선택
   ```

3. **Gradle 동기화**
   - 자동으로 시작되거나 상단 바에서 "Sync Now" 클릭

4. **빌드 실행**
   ```
   Build > Make Project (Ctrl+F9)
   ```

### 방법 2: 명령줄에서 Java 17 사용

1. **Java 17 설치**
   - Windows: https://adoptium.net/temurin/releases/
   - 또는 `winget install Microsoft.OpenJDK.17`

2. **환경변수 설정**
   ```cmd
   set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot
   set PATH=%JAVA_HOME%\bin;%PATH%
   ```

3. **Java 버전 확인**
   ```cmd
   java -version
   # 17.x.x가 출력되어야 함
   ```

4. **빌드 실행**
   ```cmd
   cd C:\Users\minhy\llm\SemoR
   .\gradlew.bat assembleDebug
   ```

### 방법 3: Gradle 프로퍼티로 강제 Java 버전 지정

1. **local.properties 파일 생성**
   ```
   # 파일: C:\Users\minhy\llm\SemoR\local.properties
   org.gradle.java.home=C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.9.9-hotspot
   ```

## 🎯 완성된 기능들

✅ **모든 MVP 기능 구현 완료:**
- 알람 추가/편집/삭제
- 시간 설정 및 요일별 반복
- 스누즈 기능
- 백그라운드 알람 처리
- 부팅 후 알람 복원
- Material Design 3 UI

**Java 버전만 해결하면 바로 실행 가능합니다!**

## 🔍 문제 해결

### 빌드 성공 후 권한 설정
앱이 정상 작동하려면 다음 권한이 필요합니다:

1. **배터리 최적화 제외**
   - 설정 > 배터리 > 배터리 사용량 최적화 > 세모알 > 최적화 안함

2. **정확한 알람 권한** (Android 12+)
   - 설정 > 앱 > 세모알 > 권한 > 알람 및 미리 알림

3. **알림 권한**
   - 설정 > 앱 > 세모알 > 권한 > 알림

## 📱 테스트 방법

1. 알람 추가 테스트
2. 1-2분 후 알람 설정하여 울리는지 확인
3. 스누즈 기능 테스트
4. 요일별 반복 알람 테스트