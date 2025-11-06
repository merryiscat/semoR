package com.semo.alarm.utils

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: AppCompatActivity) {
    
    companion object {
        private const val PREFS_PERMISSION_SETUP_COMPLETED = "permission_setup_completed"
    }

    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var exactAlarmPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var recordAudioPermissionLauncher: ActivityResultLauncher<String>
    
    // 순차적 권한 요청을 위한 큐
    private val permissionQueue = mutableListOf<PermissionInfo>()
    private var isProcessingPermissions = false
    
    fun initializePermissionLaunchers() {
        // 알림 권한 런처
        notificationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                showNotificationPermissionDialog()
            } else {
                // 다음 권한 요청으로 진행
                processNextPermission()
            }
        }
        
        // 정확한 알람 권한 런처
        exactAlarmPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (!hasExactAlarmPermission()) {
                showExactAlarmPermissionDialog()
            } else {
                // 다음 권한 요청으로 진행
                processNextPermission()
            }
        }

        // 오디오 녹음 권한 런처
        recordAudioPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                showRecordAudioPermissionDialog()
            } else {
                // 다음 권한 요청으로 진행
                processNextPermission()
            }
        }
    }
    
    /**
     * 모든 필수 권한을 확인하고 요청
     */
    fun checkAndRequestAllPermissions() {
        val missingPermissions = getMissingPermissions()
        
        android.util.Log.d("PermissionManager", "🔍 Permission check - Missing: ${missingPermissions.map { it.title }}")
        
        if (missingPermissions.isNotEmpty()) {
            // 새로운 권한(오디오 녹음)이 추가되었으므로 항상 권한 요청 표시
            android.util.Log.d("PermissionManager", "📋 Requesting permissions: ${missingPermissions.map { it.title }}")
            showPermissionRationaleDialog(missingPermissions)
        } else {
            // 모든 권한이 허용된 경우
            android.util.Log.d("PermissionManager", "✅ All permissions granted")
            if (!isPermissionSetupCompleted()) {
                markPermissionSetupCompleted()
            }
        }
    }
    
    /**
     * 권한 설정 완료 여부 확인
     */
    private fun isPermissionSetupCompleted(): Boolean {
        val prefs = activity.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREFS_PERMISSION_SETUP_COMPLETED, false)
    }
    
    /**
     * 권한 설정 완료로 표시
     */
    private fun markPermissionSetupCompleted() {
        val prefs = activity.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREFS_PERMISSION_SETUP_COMPLETED, true).apply()
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

        // 3. 오디오 녹음 권한
        if (!hasRecordAudioPermission()) {
            missing.add(PermissionInfo.RECORD_AUDIO)
        }

        return missing
    }
    
    /**
     * 권한 설명 다이얼로그 표시 (최초 실행 시)
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
                android.util.Log.d("PermissionManager", "User chose to skip permission setup")
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 누락된 권한들을 순차적으로 요청
     */
    private fun requestMissingPermissions(permissions: List<PermissionInfo>) {
        if (isProcessingPermissions) {
            return // 이미 권한 요청 처리 중
        }
        
        // 권한 큐에 추가하고 순차 처리 시작
        permissionQueue.clear()
        permissionQueue.addAll(permissions)
        isProcessingPermissions = true
        
        processNextPermission()
    }
    
    /**
     * 큐에서 다음 권한 요청 처리
     */
    private fun processNextPermission() {
        if (permissionQueue.isEmpty()) {
            // 모든 권한 처리 완료
            isProcessingPermissions = false
            markPermissionSetupCompleted()
            showPermissionSetupCompleteMessage()
            return
        }

        val nextPermission = permissionQueue.removeAt(0)
        when (nextPermission) {
            PermissionInfo.NOTIFICATION -> requestNotificationPermission()
            PermissionInfo.EXACT_ALARM -> requestExactAlarmPermission()
            PermissionInfo.RECORD_AUDIO -> requestRecordAudioPermission()
        }
    }
    
    /**
     * 권한 설정 완료 메시지 표시
     */
    private fun showPermissionSetupCompleteMessage() {
        AlertDialog.Builder(activity)
            .setTitle("권한 설정 완료")
            .setMessage("세모알의 모든 권한이 설정되었습니다. 이제 정확한 알람 서비스를 제공할 수 있습니다!")
            .setPositiveButton("확인") { _, _ -> }
            .show()
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
     * 오디오 녹음 권한 확인
     */
    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
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
     * 오디오 녹음 권한 요청
     */
    private fun requestRecordAudioPermission() {
        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
     * 오디오 녹음 권한 거부 시 설명 다이얼로그
     */
    private fun showRecordAudioPermissionDialog() {
        AlertDialog.Builder(activity)
            .setTitle("오디오 녹음 권한 필요")
            .setMessage("수면 중 코골이 감지 및 녹음 기능을 사용하려면 오디오 녹음 권한이 필요합니다. 설정에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("취소") { _, _ ->
                processNextPermission()
            }
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
     * 사용자가 수동으로 권한 설정을 다시 시도할 때 사용
     */
    fun resetAndRequestAllPermissions() {
        // 권한 설정 완료 플래그 초기화
        val prefs = activity.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREFS_PERMISSION_SETUP_COMPLETED, false).apply()
        
        // 권한 재요청
        checkAndRequestAllPermissions()
    }
    
    /**
     * 권한 정보 데이터 클래스
     */
    enum class PermissionInfo(val title: String, val description: String) {
        NOTIFICATION("알림 권한", "알람이 울릴 때 화면에 알림을 표시합니다"),
        EXACT_ALARM("정확한 알람 권한", "설정한 시간에 정확히 알람을 울립니다"),
        RECORD_AUDIO("오디오 녹음 권한", "수면 중 코골이 감지 및 녹음을 수행합니다")
    }
}