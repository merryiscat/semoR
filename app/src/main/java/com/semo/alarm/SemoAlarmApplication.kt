package com.semo.alarm

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SemoAlarmApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}