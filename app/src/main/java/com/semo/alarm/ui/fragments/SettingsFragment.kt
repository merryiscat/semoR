package com.semo.alarm.ui.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.semo.alarm.R
import com.semo.alarm.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    @Inject
    lateinit var sharedPreferences: SharedPreferences
    
    private lateinit var audioManager: AudioManager
    
    companion object {
        // 필요한 상수들은 여기에 추가
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        setupUI()
        setupClickListeners()
        loadSettings()
    }
    
    override fun onResume() {
        super.onResume()
        // 외부 설정에서 돌아왔을 때 권한 상태 새로고침
        updatePermissionStatus()
    }
    
    private fun setupUI() {
        // 앱 버전 표시
        try {
            val packageInfo: PackageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.textAppVersion.text = "v${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.textAppVersion.text = "v1.0.0"
        }
        
        // UI 설정 완료
    }
    
    private fun setupClickListeners() {
        // 앱 정보
        binding.layoutAppInfo.setOnClickListener {
            showAppInfoDialog()
        }

        // 알림 권한 설정
        binding.layoutNotificationPermission.setOnClickListener {
            openNotificationSettings()
        }

        // 배터리 최적화 제외 설정
        binding.layoutBatteryOptimization.setOnClickListener {
            openBatteryOptimizationSettings()
        }

        // 개발자 정보
        binding.layoutDeveloper.setOnClickListener {
            showDeveloperInfo()
        }

        // 라이선스
        binding.layoutLicense.setOnClickListener {
            showLicenseDialog()
        }
    }
    
    private fun loadSettings() {
        // 권한 상태 업데이트
        updatePermissionStatus()
    }
    
    
    private fun updatePermissionStatus() {
        // 알림 권한 상태
        val notificationEnabled = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        binding.textNotificationStatus.text = if (notificationEnabled) "허용됨" else "거부됨"
        binding.textNotificationStatus.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (notificationEnabled) R.color.green else R.color.red
            )
        )
        
        // 배터리 최적화 상태
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryOptimized = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
        } else {
            false
        }
        
        binding.textBatteryStatus.text = if (batteryOptimized) "설정 필요" else "제외됨"
        binding.textBatteryStatus.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (batteryOptimized) R.color.orange else R.color.green
            )
        )
    }
    
    
    private fun openNotificationSettings() {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                }
                else -> {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    addCategory(Intent.CATEGORY_DEFAULT)
                    data = Uri.parse("package:${requireContext().packageName}")
                }
            }
        }
        
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "설정 화면을 열 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openBatteryOptimizationSettings() {
        // 현재 배터리 최적화 상태 확인
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
        } else {
            true
        }
        
        if (isIgnoringBatteryOptimizations) {
            // 이미 설정되어 있는 경우
            AlertDialog.Builder(requireContext())
                .setTitle("✅ 배터리 최적화 설정 완료")
                .setMessage("세모알이 이미 배터리 최적화에서 제외되어 있습니다.\n알람과 수면 추적이 안정적으로 동작합니다.")
                .setPositiveButton("확인") { _, _ -> }
                .setNeutralButton("다시 설정") { _, _ ->
                    openBatteryOptimizationSettingsForced()
                }
                .show()
        } else {
            // 설정이 필요한 경우
            AlertDialog.Builder(requireContext())
                .setTitle("⚡ 알람 안정성 개선")
                .setMessage("""
                    수면 추적과 알람의 안정적 동작을 위해 배터리 최적화에서 세모알을 제외해주세요.
                    
                    ✅ 알람이 정확한 시간에 울립니다
                    ✅ 수면 추적이 중단되지 않습니다  
                    ✅ 코골이 감지가 정상 작동합니다
                    
                    ⭐ 설정 후 이 화면으로 돌아오면 상태가 자동으로 업데이트됩니다.
                """.trimIndent())
                .setPositiveButton("설정하러 가기") { _, _ ->
                    openBatteryOptimizationSettingsForced()
                }
                .setNegativeButton("나중에") { _, _ -> }
                .show()
        }
    }
    
    private fun openBatteryOptimizationSettingsForced() {
        android.util.Log.d("SettingsFragment", "🔋 Opening battery optimization settings...")

        try {
            // 1차: 직접 배터리 최적화 팝업 요청 (가장 편한 방법)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                startActivity(intent)
                android.util.Log.d("SettingsFragment", "✅ Direct battery optimization popup launched")
                Toast.makeText(requireContext(), "팝업에서 '허용' 버튼을 눌러주세요", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "❌ Direct battery optimization failed: ${e.message}", e)
        }

        try {
            // 2차: 앱 정보 화면 (배터리 설정에 2클릭 필요)
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            startActivity(intent)
            android.util.Log.d("SettingsFragment", "✅ App details settings launched")
            Toast.makeText(requireContext(), "배터리 → 제한 없음으로 설정해주세요", Toast.LENGTH_LONG).show()
            return
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "❌ App details settings failed: ${e.message}", e)
        }

        try {
            // 3차: 배터리 최적화 목록 화면 (앱을 찾아야 함)
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
            android.util.Log.d("SettingsFragment", "✅ Battery optimization list launched")
            Toast.makeText(requireContext(), "목록에서 '세모알'을 찾아 '최적화 안함'으로 설정해주세요", Toast.LENGTH_LONG).show()
            return
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "❌ Battery optimization list failed: ${e.message}", e)
        }

        // 모든 시도 실패 시 수동 안내
        android.util.Log.w("SettingsFragment", "⚠️ All automatic methods failed, showing manual guide")
        showManualBatteryOptimizationGuide()
    }
    
    private fun showManualBatteryOptimizationGuide() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚡ 배터리 최적화 설정 안내")
            .setMessage("""
                알람의 안정적 동작을 위해 다음 단계를 따라주세요:
                
                📱 **일반적인 경우:**
                1️⃣ 설정 → 배터리 (또는 전원 관리)
                2️⃣ 앱 전원 관리 (또는 배터리 최적화)
                3️⃣ 세모알 앱 찾기
                4️⃣ '제한 없음' 또는 '최적화 안함' 선택
                
                📱 **삼성 갤럭시:**
                설정 → 디바이스 케어 → 배터리 → 앱 전원 관리 → 세모알 → 제한 없음
                
                📱 **샤오미:**
                설정 → 앱 → 권한 → 자동실행 → 세모알 활성화
                
                ⚠️ 이 설정을 하지 않으면 수면 추적이 정상 작동하지 않을 수 있습니다.
            """.trimIndent())
            .setPositiveButton("일반 설정으로 이동") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "설정 화면을 열 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("나중에") { _, _ -> }
            .show()
    }
    
    
    
    
    private fun showAppInfoDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("세모알 상세 정보")
            .setMessage("세모알은 최종적으로 세상의 모든 알람 기능들을 통합한 통합 솔루션을 지향하고 있습니다.\n\n" +
                    "현재 beta 1.0 버전에서는 다음을 목표로 하고 있습니다:\n" +
                    "• 모든 버그 수정 및 안정성 확보\n" +
                    "• 성능 최적화\n" +
                    "• 사용자 경험 개선\n\n" +
                    "주요 기능:\n" +
                    "• 혁신적인 알람 시스템\n" +
                    "• 커스텀 타이머\n" +
                    "• 수면 추적\n" +
                    "• 통계 및 리포트\n\n" +
                    "버전: ${binding.textAppVersion.text}\n" +
                    "개발자: MerryisCat")
            .setPositiveButton("확인", null)
            .show()
    }

    private fun showDeveloperInfo() {
        AlertDialog.Builder(requireContext())
            .setTitle("개발자 정보")
            .setMessage("세모알 - 세상의 모든 알람\n\n" +
                    "개발자: MerryisCat\n" +
                    "연락처: merryiscat20@gmail.com\n" +
                    "버전: ${binding.textAppVersion.text}\n" +
                    "제작년도: 2024\n\n" +
                    "문의사항이나 버그 신고는\n" +
                    "이메일로 연락주세요!")
            .setPositiveButton("확인", null)
            .show()
    }
    
    private fun showLicenseDialog() {
        val licenseText = """
            세모알은 다음 오픈소스 라이브러리를 사용합니다:

            • Android Jetpack Components
            • Material Design Components
            • Hilt (의존성 주입)
            • Room Database
            • MPAndroidChart (차트)
            • Kotlin Coroutines

            각 라이브러리는 해당 라이선스를 따릅니다.
            자세한 내용은 Apache License 2.0을 확인하세요.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("오픈소스 라이선스")
            .setMessage(licenseText)
            .setPositiveButton("확인", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}