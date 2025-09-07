package com.semo.alarm.ui.activities

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
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
import android.widget.TextView
import com.semo.alarm.character.CharacterTestActivity
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
        
        // Android 14+ 풀스크린 인텐트 권한 확인
        checkFullScreenIntentPermission()
        
        
        setupBottomNavigation()
        
        // 🐱 캐릭터 시스템 테스트용 히든 기능 (개발자 전용)
        setupCharacterTestAccess()
        
        // 기본 프래그먼트 설정 (알람 프래그먼트)
        if (savedInstanceState == null) {
            replaceFragment(AlarmFragment())
        }
    }
    
    /**
     * 🐱 캐릭터 시스템 테스트 접근 설정 (개발자용 히든 기능)
     * 앱 제목을 롱클릭하면 캐릭터 테스트 화면으로 이동
     */
    private fun setupCharacterTestAccess() {
        // 제목 TextView를 찾아서 롱클릭 리스너 설정
        val appTitle = findViewById<TextView>(R.id.appTitle)
        appTitle?.setOnLongClickListener {
            // 캐릭터 테스트 액티비티 시작
            val intent = Intent(this, CharacterTestActivity::class.java)
            startActivity(intent)
            true
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
        // 현재 권한 상태 로그 출력 (디버깅용)
        logCurrentPermissionStatus()
        
        // 앱이 처음 실행되거나 권한이 없는 경우 권한 요청
        permissionManager.checkAndRequestAllPermissions()
    }
    
    /**
     * 현재 권한 상태를 로그로 출력 (디버깅용)
     */
    private fun logCurrentPermissionStatus() {
        android.util.Log.d("MainActivity", "🔍 Current Permission Status:")
        android.util.Log.d("MainActivity", "  📱 Notification Permission: ${permissionManager.hasNotificationPermission()}")
        android.util.Log.d("MainActivity", "  ⏰ Exact Alarm Permission: ${permissionManager.hasExactAlarmPermission()}")
        android.util.Log.d("MainActivity", "  🔋 Battery Optimization Ignored: ${permissionManager.isBatteryOptimizationIgnored()}")
        
        if (!permissionManager.isBatteryOptimizationIgnored()) {
            android.util.Log.w("MainActivity", "  ⚠️ Battery optimization is ENABLED - this may prevent alarms from working!")
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // onResume에서 권한 상태 재확인 (배터리 최적화는 사용자가 언제든 변경할 수 있음)
        logCurrentPermissionStatus()
        
        // 배터리 최적화가 활성화된 경우에만 조용히 알림
        if (!permissionManager.isBatteryOptimizationIgnored()) {
            android.util.Log.w("MainActivity", "⚠️ Battery optimization detected on resume - alarm reliability may be affected")
        }
    }
    
    /**
     * Android 14+ 풀스크린 인텐트 권한 확인 및 요청
     * 한 번만 설정하면 모든 알람에서 자동으로 풀스크린이 작동함
     */
    private fun checkFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (!notificationManager.canUseFullScreenIntent()) {
                showFullScreenIntentPermissionDialog()
            }
        }
    }
    
    private fun showFullScreenIntentPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("🐱 메리 캐릭터 풀스크린 알람 설정")
            .setMessage(
                "메리가 알람 시간에 자동으로 나타나려면 권한이 필요해요!\n\n" +
                "다음 화면에서 '풀스크린 인텐트' 권한을 허용해주세요.\n" +
                "한 번만 설정하면 모든 알람에서 작동합니다. 🎉"
            )
            .setPositiveButton("설정하러 가기") { _, _ ->
                openFullScreenIntentSettings()
            }
            .setNegativeButton("나중에") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun openFullScreenIntentSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            // Fallback: 앱 정보 화면으로 이동
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
    
}