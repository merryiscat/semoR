package com.semo.alarm

import android.app.Application
import com.semo.alarm.data.DatabaseInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SemoAlarmApplication : Application() {
    
    @Inject
    lateinit var databaseInitializer: DatabaseInitializer
    
    override fun onCreate() {
        super.onCreate()
        
        // 기본 카테고리 및 타이머 템플릿들 초기화
        databaseInitializer.initializeDefaultDataIfNeeded()
    }
}