# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

### Building and Development
- `npm run build` - Build the React app using Vite
- `npm run lint` - Run ESLint on TypeScript/React files
- `npm run type-check` - Run TypeScript type checking without emitting files
- `npm run sync` - Build and sync with Capacitor (updates native apps)

### Android Development
- `npm run android:open` - Open Android Studio with the project
- `npm run android:run` - Sync and run on Android device/emulator
- `npm run android:build` - Sync and open in Android Studio for building
- `./gradlew assembleDebug` - Build debug APK from android/ directory

## Architecture Overview

### Core Architecture Pattern
This is a **React + Capacitor hybrid mobile app** targeting Android only, built with TypeScript. The architecture follows a typical React patterns with Capacitor providing native mobile capabilities.

### State Management
- **Zustand** with persistence: All alarm data is managed in `alarmStore.ts` with automatic localStorage persistence
- **Singleton AlarmService**: Native notification scheduling through `AlarmService` class
- **React hooks integration**: `useAlarmManager.ts` bridges React state with native services

### Key Architectural Components

#### 1. Alarm Management Flow
```
React Components → Zustand Store → AlarmService → Capacitor LocalNotifications → Android System
```

#### 2. Data Flow
- **alarmStore.ts**: CRUD operations for alarm data with Zustand persistence
- **alarmService.ts**: Singleton service that handles native notification scheduling
- **useAlarmManager.ts**: React hook that synchronizes alarms with the native service

#### 3. Native Integration
- **Capacitor plugins**: LocalNotifications for Android alarm scheduling
- **Android permissions**: Exact alarms, notifications, wake locks, boot receiver
- **Background processing**: Uses Android's exact alarm system for reliability

### TypeScript Architecture
- **Strict typing**: Full TypeScript with strict mode enabled
- **Path aliases**: `@/*` maps to `src/*` directory
- **Enum-based types**: AlarmType and RepeatType for type safety

### Component Structure
- **Header.tsx**: App title and branding
- **AddAlarmForm.tsx**: Alarm creation form with validation
- **AlarmList.tsx**: Renders list of all alarms
- **AlarmItem.tsx**: Individual alarm with toggle/delete actions

### Critical Implementation Details

#### Alarm ID Handling
The app generates UUIDs for alarms but converts them to numeric IDs for Android notifications using:
```typescript
parseInt(alarmId.replace(/-/g, '').substring(0, 8), 16)
```

#### Service Lifecycle
- AlarmService is a singleton that manages all notification scheduling
- useAlarmManager hook handles service initialization and cleanup
- Alarms are rescheduled when app becomes visible to ensure accuracy

#### Repeat Types Implementation
- Uses Capacitor's schedule API with specific repeat patterns
- Weekdays/weekends use custom day-of-week configurations
- All scheduling happens through the singleton AlarmService

## Development Notes

### Android-Specific Considerations
- App requires exact alarm permissions (SCHEDULE_EXACT_ALARM)
- Uses Capacitor 7.x with LocalNotifications plugin
- Target Android API 33+ with backwards compatibility to API 24
- Gradle builds are handled through Android Studio integration

### Capacitor Configuration
- **App ID**: com.semor.app
- **App Name**: 세모알 (Korean)
- **Web directory**: dist/ (Vite build output)
- **Icons**: Configured in android/app/src/main/res/

### File Structure Patterns
- Components use PascalCase naming
- Services and utilities use camelCase
- TypeScript interfaces are co-located with implementation
- All paths use @ alias for src/ directory imports