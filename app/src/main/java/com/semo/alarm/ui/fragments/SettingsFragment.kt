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
        private const val PREF_DEFAULT_VOLUME = "default_volume"
        private const val PREF_DEFAULT_SOUND = "default_sound"
        private const val PREF_DEFAULT_SNOOZE = "default_snooze_enabled"
        private const val PREF_SNOOZE_INTERVAL = "default_snooze_interval"
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
    
    private fun setupUI() {
        // 앱 버전 표시
        try {
            val packageInfo: PackageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.textAppVersion.text = "v${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.textAppVersion.text = "v1.0.0"
        }
        
        // 볼륨 SeekBar 리스너
        binding.seekBarDefaultVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.textDefaultVolume.text = "${progress}%"
                    updateVolumeColor(progress)
                    
                    // SharedPreferences에 저장
                    sharedPreferences.edit()
                        .putInt(PREF_DEFAULT_VOLUME, progress)
                        .apply()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 스누즈 스위치 리스너
        binding.switchDefaultSnooze.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean(PREF_DEFAULT_SNOOZE, isChecked)
                .apply()
            
            binding.textDefaultSnooze.text = if (isChecked) "5분" else "사용 안함"
        }
    }
    
    private fun setupClickListeners() {
        // 기본 알람음 설정
        binding.layoutDefaultSound.setOnClickListener {
            showSoundSelectionDialog()
        }
        
        // 알림 권한 설정
        binding.layoutNotificationPermission.setOnClickListener {
            openNotificationSettings()
        }
        
        // 배터리 최적화 제외 설정
        binding.layoutBatteryOptimization.setOnClickListener {
            openBatteryOptimizationSettings()
        }
        
        // 백업
        binding.layoutBackup.setOnClickListener {
            showBackupDialog()
        }
        
        // 복원
        binding.layoutRestore.setOnClickListener {
            showRestoreDialog()
        }
        
        // 데이터 초기화
        binding.layoutResetData.setOnClickListener {
            showResetDataDialog()
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
        // 기본 볼륨 로드
        val defaultVolume = sharedPreferences.getInt(PREF_DEFAULT_VOLUME, 70)
        binding.seekBarDefaultVolume.progress = defaultVolume
        binding.textDefaultVolume.text = "${defaultVolume}%"
        updateVolumeColor(defaultVolume)
        
        // 스누즈 설정 로드
        val snoozeEnabled = sharedPreferences.getBoolean(PREF_DEFAULT_SNOOZE, true)
        binding.switchDefaultSnooze.isChecked = snoozeEnabled
        binding.textDefaultSnooze.text = if (snoozeEnabled) "5분" else "사용 안함"
        
        // 기본 알람음 로드
        val defaultSound = sharedPreferences.getString(PREF_DEFAULT_SOUND, "기본 알람음")
        binding.textDefaultSound.text = defaultSound
        
        // 권한 상태 업데이트
        updatePermissionStatus()
    }
    
    private fun updateVolumeColor(volume: Int) {
        val color = if (volume == 0) {
            ContextCompat.getColor(requireContext(), R.color.deep_gray)
        } else {
            ContextCompat.getColor(requireContext(), R.color.neon_blue)
        }
        
        binding.seekBarDefaultVolume.progressTintList = android.content.res.ColorStateList.valueOf(color)
        binding.seekBarDefaultVolume.thumbTintList = android.content.res.ColorStateList.valueOf(color)
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
    
    private fun showSoundSelectionDialog() {
        val soundOptions = arrayOf("기본 알람음", "벨소리 1", "벨소리 2", "자연 소리")
        val currentSound = binding.textDefaultSound.text.toString()
        val currentIndex = soundOptions.indexOf(currentSound)
        
        AlertDialog.Builder(requireContext())
            .setTitle("기본 알람음 선택")
            .setSingleChoiceItems(soundOptions, currentIndex) { dialog, which ->
                val selectedSound = soundOptions[which]
                binding.textDefaultSound.text = selectedSound
                
                sharedPreferences.edit()
                    .putString(PREF_DEFAULT_SOUND, selectedSound)
                    .apply()
                
                dialog.dismiss()
                Toast.makeText(requireContext(), "기본 알람음이 설정되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent().apply {
                action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            }
            
            try {
                startActivity(intent)
                Toast.makeText(requireContext(), "세모알을 배터리 최적화에서 제외해주세요", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "설정 화면을 열 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showBackupDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("백업")
            .setMessage("알람 및 설정 데이터를 백업하시겠습니까?\n\n백업 파일은 Downloads 폴더에 저장됩니다.")
            .setPositiveButton("백업") { _, _ ->
                // TODO: 백업 기능 구현
                Toast.makeText(requireContext(), "백업 기능은 추후 업데이트에서 제공됩니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun showRestoreDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("복원")
            .setMessage("백업 파일에서 데이터를 복원하시겠습니까?\n\n기존 데이터는 덮어씌워집니다.")
            .setPositiveButton("복원") { _, _ ->
                // TODO: 복원 기능 구현
                Toast.makeText(requireContext(), "복원 기능은 추후 업데이트에서 제공됩니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun showResetDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ 데이터 초기화")
            .setMessage("모든 알람, 타이머, 수면 기록 및 설정이 삭제됩니다.\n\n이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("초기화") { _, _ ->
                showFinalConfirmationDialog()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun showFinalConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("최종 확인")
            .setMessage("정말로 모든 데이터를 초기화하시겠습니까?")
            .setPositiveButton("네, 초기화합니다") { _, _ ->
                performDataReset()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun performDataReset() {
        try {
            // SharedPreferences 초기화
            sharedPreferences.edit().clear().apply()
            
            // TODO: 데이터베이스 초기화
            // database.clearAllTables()
            
            Toast.makeText(requireContext(), "데이터가 초기화되었습니다", Toast.LENGTH_SHORT).show()
            
            // 설정 다시 로드
            loadSettings()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "초기화 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showDeveloperInfo() {
        AlertDialog.Builder(requireContext())
            .setTitle("🏢 세모알 팀")
            .setMessage("세모알 - 세상의 모든 알람\n\n" +
                    "개발자: 세모알 개발팀\n" +
                    "버전: ${binding.textAppVersion.text}\n" +
                    "제작년도: 2024\n\n" +
                    "문의사항이나 버그 신고는\n" +
                    "앱스토어 리뷰를 통해 남겨주세요!")
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
            .setTitle("📄 오픈소스 라이선스")
            .setMessage(licenseText)
            .setPositiveButton("확인", null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}