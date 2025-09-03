package com.semo.alarm.ui.activities

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.semo.alarm.R
import com.semo.alarm.databinding.ActivityMainBinding
import com.semo.alarm.ui.fragments.AlarmFragment
import com.semo.alarm.ui.fragments.CustomTimerFragment
import com.semo.alarm.ui.fragments.ReportFragment
import com.semo.alarm.ui.fragments.SettingsFragment
import com.semo.alarm.ui.fragments.SleepFragment
import com.semo.alarm.utils.PermissionManager
import com.semo.alarm.utils.NotificationAlarmManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionManager: PermissionManager
    private lateinit var notificationAlarmManager: NotificationAlarmManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 권한 매니저 초기화
        initializePermissionManager()
        
        // 알람 매니저 초기화
        notificationAlarmManager = NotificationAlarmManager(this)
        
        // 권한 확인 및 요청
        checkAndRequestPermissions()
        
        
        setupBottomNavigation()
        
        // 기본 프래그먼트 설정 (알람 프래그먼트)
        if (savedInstanceState == null) {
            replaceFragment(AlarmFragment())
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_alarm -> {
                    replaceFragment(AlarmFragment())
                    true
                }
                R.id.nav_custom_timer -> {
                    replaceFragment(CustomTimerFragment())
                    true
                }
                R.id.nav_sleep -> {
                    replaceFragment(SleepFragment())
                    true
                }
                R.id.nav_report -> {
                    replaceFragment(ReportFragment())
                    true
                }
                R.id.nav_settings -> {
                    replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    private fun initializePermissionManager() {
        permissionManager = PermissionManager(this)
        permissionManager.initializePermissionLaunchers()
    }
    
    private fun checkAndRequestPermissions() {
        // 앱이 처음 실행되거나 권한이 없는 경우 권한 요청
        permissionManager.checkAndRequestAllPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        
        // 앱이 다시 포그라운드로 왔을 때 권한 상태 재확인
        // (사용자가 설정에서 권한을 변경했을 수 있음)
        if (!permissionManager.hasNotificationPermission() || 
            !permissionManager.hasExactAlarmPermission() ||
            !permissionManager.isBatteryOptimizationIgnored()) {
            // 권한이 아직 부족한 경우, 필요하면 다시 요청 (덜 침입적으로)
        }
    }
    
}