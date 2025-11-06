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
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import android.webkit.WebView
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
        binding.textAppVersion.text = "ver.0.90"

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


    private fun showAppInfoDialog() {
        val appInfo = """
세상의 모든 알람, 세모알

안녕하세요. 세모알은 세상의 모든 알람 관련 기능들을 통합하여 하나의 앱으로 사용할 수 있도록 만드는 것을 목표로 하고 있습니다.

【현재 버전: ver.0.90 (베타)】
현재 알람/타이머/수면시간 체크/알람 리포트 기능을 제공하고 있습니다.

■ 알람 탭
• 기본적인 알람 설정 기능
• 요일별 알람 설정
• 1회~3회 반복 후 종료되는 알람 기능

■ 타이머 탭
• 기본적인 타이머 설정 기능
• 타이머 별 카테고리 설정 기능
  (카테고리는 출근버스, 요리 등의 카테고리를 만들어서 자주쓰는 타이머들을 한곳에서 모아 사용하는 걸 기획했습니다.)

■ 수면 탭
• 수면 시간 체크 기능
  (주간/야간을 선택하여 핸드폰을 마지막으로 사용한 시간~기상알람 시간을 체크하는 로직으로 기획되어 있습니다.)

■ 리포트 탭
• 일일 리포트: 단순 알림/타이머 사용 리스트 제공
• 주간 리포트: 각 주의 평균 수면시간, 알림앱 지연 시간 등 제공

■ 설정 탭
• 앱 정보, 알람 설정 등 제공

【ver.1.00 목표】
정식 서비스 ver.1.00에 들어가기 앞서 구현된 위 서비스들이 버그 없이 원활하게 동작하는 것을 목표로 삼고 있습니다.

【정식 서비스 이후 확장 목표】
첫번째 기능 확장:
• 인터벌 타이머: 운동할 때 사용하기 용이한 2분-1분-2분-1분 등 연속되는 타이머 기능
• 캐릭터 추가: 현재 알람 캐릭터 메리 외 장군이를 추가할 예정

【피드백】
사용에 있어 불편한 점, 버그리포트, 추가되었으면 하는 아이디어들은
merryiscat20@gmail.com 으로 메일 보내주세요.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("세모알 정보")
            .setMessage(appInfo)
            .setPositiveButton("확인", null)
            .setNeutralButton("메일 보내기") { _, _ ->
                sendFeedbackEmail()
            }
            .show()
    }

    private fun sendFeedbackEmail() {
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("merryiscat20@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "[세모알] 피드백")
            putExtra(Intent.EXTRA_TEXT, "앱 버전: ${binding.textAppVersion.text}\n\n피드백 내용:\n")
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "이메일 앱 선택"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "이메일 앱을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
        }
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
            .setNeutralButton("메일 보내기") { _, _ ->
                sendFeedbackEmail()
            }
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