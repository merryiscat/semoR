package com.semo.alarm.utils

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: AppCompatActivity) {
    
    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 1001
        private const val REQUEST_SCHEDULE_EXACT_ALARM = 1002
        private const val REQUEST_BATTERY_OPTIMIZATION = 1003
    }
    
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var exactAlarmPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var batteryOptimizationLauncher: ActivityResultLauncher<Intent>
    
    fun initializePermissionLaunchers() {
        // 알림 권한 런처
        notificationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                showNotificationPermissionDialog()
            }
        }
        
        // 정확한 알람 권한 런처
        exactAlarmPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (!hasExactAlarmPermission()) {
                showExactAlarmPermissionDialog()
            }
        }
        
        // 배터리 최적화 제외 런처
        batteryOptimizationLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // 결과에 관계없이 사용자에게 설정 완료 메시지 표시
        }
    }
    
    /**
     * 모든 필수 권한을 확인하고 요청
     */
    fun checkAndRequestAllPermissions() {
        val missingPermissions = getMissingPermissions()
        
        if (missingPermissions.isNotEmpty()) {
            showPermissionRationaleDialog(missingPermissions)
        }
    }
    
    /**
     * 누락된 권한 목록 반환
     */
    private fun getMissingPermissions(): List<PermissionInfo> {
        val missing = mutableListOf<PermissionInfo>()
        
        // 1. POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                missing.add(PermissionInfo.NOTIFICATION)
            }
        }
        
        // 2. SCHEDULE_EXACT_ALARM (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasExactAlarmPermission()) {
                missing.add(PermissionInfo.EXACT_ALARM)
            }
        }
        
        // 3. 배터리 최적화 제외
        if (!isBatteryOptimizationIgnored()) {
            missing.add(PermissionInfo.BATTERY_OPTIMIZATION)
        }
        
        return missing
    }
    
    /**
     * 권한 설명 다이얼로그 표시
     */
    private fun showPermissionRationaleDialog(missingPermissions: List<PermissionInfo>) {
        val permissionMessages = missingPermissions.joinToString("\n\n") { 
            "• ${it.title}: ${it.description}" 
        }
        
        AlertDialog.Builder(activity)
            .setTitle("알람 앱 권한 필요")
            .setMessage("세모알이 정상적으로 작동하려면 다음 권한이 필요합니다:\n\n$permissionMessages\n\n지금 설정하시겠습니까?")
            .setPositiveButton("설정하기") { _, _ ->
                requestMissingPermissions(missingPermissions)
            }
            .setNegativeButton("나중에") { _, _ ->
                // 사용자가 나중에 선택한 경우에도 앱은 계속 작동
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 누락된 권한들을 순차적으로 요청
     */
    private fun requestMissingPermissions(permissions: List<PermissionInfo>) {
        permissions.forEach { permission ->
            when (permission) {
                PermissionInfo.NOTIFICATION -> requestNotificationPermission()
                PermissionInfo.EXACT_ALARM -> requestExactAlarmPermission()
                PermissionInfo.BATTERY_OPTIMIZATION -> requestBatteryOptimizationExclusion()
            }
        }
    }
    
    /**
     * 알림 권한 확인
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 이하에서는 기본적으로 허용
        }
    }
    
    /**
     * 정확한 알람 권한 확인
     */
    fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 12 이하에서는 기본적으로 허용
        }
    }
    
    /**
     * 배터리 최적화 제외 상태 확인
     */
    fun isBatteryOptimizationIgnored(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(activity.packageName)
        } else {
            true // Android 6.0 이하에서는 해당 없음
        }
    }
    
    /**
     * 알림 권한 요청
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    /**
     * 정확한 알람 권한 요청
     */
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            exactAlarmPermissionLauncher.launch(intent)
        }
    }
    
    /**
     * 배터리 최적화 제외 요청
     */
    private fun requestBatteryOptimizationExclusion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            batteryOptimizationLauncher.launch(intent)
        }
    }
    
    /**
     * 알림 권한 거부 시 설명 다이얼로그
     */
    private fun showNotificationPermissionDialog() {
        AlertDialog.Builder(activity)
            .setTitle("알림 권한 필요")
            .setMessage("알람이 울릴 때 알림을 표시하기 위해 알림 권한이 필요합니다. 설정에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("취소") { _, _ -> }
            .show()
    }
    
    /**
     * 정확한 알람 권한 거부 시 설명 다이얼로그
     */
    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(activity)
            .setTitle("정확한 알람 권한 필요")
            .setMessage("정시에 정확히 알람을 울리기 위해 정확한 알람 권한이 필요합니다. 설정에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                requestExactAlarmPermission()
            }
            .setNegativeButton("취소") { _, _ -> }
            .show()
    }
    
    /**
     * 앱 설정 화면으로 이동
     */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivity(intent)
    }
    
    /**
     * 권한 정보 데이터 클래스
     */
    enum class PermissionInfo(val title: String, val description: String) {
        NOTIFICATION("알림 권한", "알람이 울릴 때 화면에 알림을 표시합니다"),
        EXACT_ALARM("정확한 알람 권한", "설정한 시간에 정확히 알람을 울립니다"),
        BATTERY_OPTIMIZATION("배터리 최적화 제외", "백그라운드에서도 알람이 정상 작동합니다")
    }
}