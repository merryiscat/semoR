# 개발 명령어 가이드

## Windows 시스템 기본 명령어
- `dir` - 디렉토리 내용 확인
- `cd [경로]` - 디렉토리 이동
- `copy [원본] [대상]` - 파일 복사
- `del [파일명]` - 파일 삭제
- `find "텍스트" [파일]` - 파일 내 텍스트 검색

## Git 명령어
- `git status` - 현재 상태 확인
- `git add .` - 모든 변경사항 스테이징
- `git commit -m "메시지"` - 커밋
- `git pull` - 원격 저장소에서 가져오기
- `git push` - 원격 저장소로 푸시

## Android 빌드 명령어
### Gradle Wrapper (권장)
- `.\gradlew.bat assembleDebug` - 디버그 APK 빌드
- `.\gradlew.bat assembleRelease` - 릴리즈 APK 빌드
- `.\gradlew.bat clean` - 빌드 캐시 정리
- `.\gradlew.bat build` - 전체 빌드 (lint, test 포함)

### 대체 명령어 (gradlew 실행 불가 시)
- `cmd /c gradlew assembleDebug` - CMD를 통한 실행
- `powershell .\gradlew.bat assembleDebug` - PowerShell을 통한 실행

## 테스트 명령어
- `.\gradlew.bat test` - 단위 테스트 실행
- `.\gradlew.bat connectedAndroidTest` - 계측 테스트 실행

## 린트 및 코드 품질
- `.\gradlew.bat lint` - 린트 검사 실행
- `.\gradlew.bat lintDebug` - 디버그 린트 검사

## APK 위치
- **디버그 APK**: `app\build\outputs\apk\debug\app-debug.apk`
- **릴리즈 APK**: `app\build\outputs\apk\release\app-release.apk`

## 프로젝트 실행 방법
1. Android Studio에서 프로젝트 열기
2. Gradle 동기화 대기
3. 에뮬레이터 또는 실제 기기 연결
4. Run 버튼 클릭 또는 Shift+F10

## 주의사항
- Windows 환경이므로 `gradlew.bat` 사용 (Unix의 `./gradlew` 대신)
- 경로에 공백이 있을 경우 큰따옴표로 감싸기
- 권한 문제 시 관리자 권한으로 실행