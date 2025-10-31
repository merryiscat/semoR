package com.semo.alarm.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.semo.alarm.R
import com.semo.alarm.databinding.ActivityAddEditTimerBinding
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.data.entities.TimerRound
import com.semo.alarm.data.enums.TimerType
import com.semo.alarm.ui.viewmodels.CustomTimerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditTimerActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "AddEditTimerActivity"
    }
    
    private lateinit var binding: ActivityAddEditTimerBinding
    private val viewModel: CustomTimerViewModel by viewModels()
    
    private var category: TimerCategory? = null
    private var categoryId: Int = -1
    private var templateId: Int = -1
    private var isEditMode = false
    private var currentTemplate: TimerTemplate? = null
    
    // 소리 & 진동 설정
    private var selectedSoundUri: String = ""
    private var currentVolume: Float = 0.7f
    private var isVibrationEnabled: Boolean = false
    private var testMediaPlayer: MediaPlayer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Intent에서 데이터 받기
        category = intent.getParcelableExtra("category")
        categoryId = intent.getIntExtra("categoryId", -1)
        templateId = intent.getIntExtra("templateId", -1)
        currentTemplate = intent.getParcelableExtra("template")
        isEditMode = templateId != -1
        
        // category가 있으면 categoryId 설정, 없으면 categoryId를 직접 사용
        if (category != null) {
            categoryId = category!!.id
        }
        
        Log.d(TAG, "AddEditTimerActivity - Category: ${category?.name}, CategoryId: $categoryId, TemplateId: $templateId, EditMode: $isEditMode")
        
        setupUI()
        setupTimePickers()
        setupSoundAndVibrationSettings()
        setupButtons()
        
        if (isEditMode) {
            loadExistingTemplate()
        } else {
            setupNewTimer()
        }
    }
    
    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        binding.toolbar.title = if (isEditMode) {
            "타이머 편집"
        } else {
            if (categoryId == -1) "독립 타이머 추가" else "${category?.name} 타이머 추가"
        }
    }
    
    private fun setupTimePickers() {
        // 시간 NumberPicker (0-23)
        binding.hourPicker.apply {
            minValue = 0
            maxValue = 23
            value = 0
            setOnValueChangedListener { _, _, _ -> updateTotalDuration() }
        }
        
        // 분 NumberPicker (0-59) 
        binding.minutePicker.apply {
            minValue = 0
            maxValue = 59
            value = 0 // 기본값 0분
            setOnValueChangedListener { _, oldVal, newVal -> 
                handleMinuteChange(oldVal, newVal)
                updateTotalDuration() 
            }
        }
        
        // 초 NumberPicker (0-59)
        binding.secondPicker.apply {
            minValue = 0
            maxValue = 59
            value = 0
            setOnValueChangedListener { _, oldVal, newVal -> 
                handleSecondChange(oldVal, newVal)
                updateTotalDuration() 
            }
        }
        
        // 초기 총 시간 업데이트
        updateTotalDuration()
    }
    
    private fun setupButtons() {
        // 저장 버튼
        binding.btnSaveTimer.setOnClickListener {
            saveTimer()
        }

        // 빠른 시간 설정 버튼들
        binding.btnSet1Min.setOnClickListener { setQuickTime(0, 1, 0) } // 1분
        binding.btnSet5Min.setOnClickListener { setQuickTime(0, 5, 0) } // 5분
        binding.btnSet10Min.setOnClickListener { setQuickTime(0, 10, 0) } // 10분
        binding.btnSet15Min.setOnClickListener { setQuickTime(0, 15, 0) } // 15분
        binding.btnSet30Min.setOnClickListener { setQuickTime(0, 30, 0) } // 30분
        binding.btnSet1Hour.setOnClickListener { setQuickTime(1, 0, 0) } // 1시간
    }

    /**
     * 빠른 시간 설정
     */
    private fun setQuickTime(hours: Int, minutes: Int, seconds: Int) {
        binding.hourPicker.value = hours
        binding.minutePicker.value = minutes
        binding.secondPicker.value = seconds
        updateTotalDuration()
    }
    
    private fun setupSoundAndVibrationSettings() {
        // 초기 SeekBar 색상 설정 (기본값 70%)
        updateSeekBarColors(binding.seekBarVolume.progress)
        
        // 볼륨 테스트 버튼 설정
        setupVolumeTestButton()
        
        // 알람음 선택 버튼
        binding.buttonSelectSound.setOnClickListener {
            showSoundSelectionDialog()
        }
        
        // 볼륨 SeekBar 설정 - 스마트 볼륨-진동 모드 시스템
        binding.seekBarVolume.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                binding.textViewVolumePercent.text = "${progress}%"
                currentVolume = progress / 100f
                
                // SeekBar 색상 업데이트
                updateSeekBarColors(progress)
                
                if (fromUser) {
                    // 볼륨이 0%가 되면 자동으로 진동 모드 활성화 (무음에서 알림 받기 위함)
                    if (progress == 0) {
                        binding.switchVibrationMode.isChecked = true
                        isVibrationEnabled = true
                    }
                    // 📍 중요 변경: 볼륨이 올라가도 진동 모드는 자동으로 꺼지지 않음
                    // 사용자가 진동+소리 동시 사용을 원할 수 있음
                }
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        // 진동 모드 스위치 - 진동과 소리 독립적 제어
        binding.switchVibrationMode.setOnCheckedChangeListener { _, isChecked ->
            isVibrationEnabled = isChecked
            
            // 진동을 끄고 소리도 0%인 경우에만 소리를 1%로 설정 (알림 받기 위함)
            if (!isChecked && binding.seekBarVolume.progress == 0) {
                binding.seekBarVolume.progress = 1
                currentVolume = 0.01f
                binding.textViewVolumePercent.text = "1%"
                updateSeekBarColors(1)
            }
            // 📍 진동을 켜는 것은 소리에 전혀 영향을 주지 않음 (완전 독립)
        }
    }
    
    private fun setupVolumeTestButton() {
        binding.buttonTestVolume.setOnClickListener {
            testVolume()
        }
    }
    
    private fun showSoundSelectionDialog() {
        val soundOptions = arrayOf(
            "기본 알람음",
            "기본 벨소리",
            "알림음", 
            "음악 파일 선택"
        )
        
        AlertDialog.Builder(this)
            .setTitle("알람음 선택")
            .setItems(soundOptions) { _, which ->
                when (which) {
                    0 -> {
                        selectedSoundUri = ""
                        binding.buttonSelectSound.text = "기본 알람음"
                    }
                    1 -> {
                        selectedSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString()
                        binding.buttonSelectSound.text = "기본 벨소리"
                    }
                    2 -> {
                        selectedSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()
                        binding.buttonSelectSound.text = "알림음"
                    }
                    3 -> {
                        // 파일 선택 (나중에 구현)
                        Toast.makeText(this, "음악 파일 선택 기능은 추후 지원될 예정입니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }
    
    private fun testVolume() {
        try {
            // 기존 테스트 재생 중지
            testMediaPlayer?.release()
            
            val soundUri = if (selectedSoundUri.isNotEmpty()) {
                Uri.parse(selectedSoundUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            
            testMediaPlayer = MediaPlayer().apply {
                setDataSource(this@AddEditTimerActivity, soundUri)
                
                // 오디오 속성 설정
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(android.media.AudioManager.STREAM_ALARM)
                }
                
                isLooping = false
                prepare()
                setVolume(currentVolume, currentVolume)
                start()
                
                setOnCompletionListener { mp ->
                    mp.release()
                    testMediaPlayer = null
                }
            }
            
            Log.d(TAG, "🔊 Testing volume: ${(currentVolume * 100).toInt()}%")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to test volume", e)
            Toast.makeText(this, "알람음 테스트 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateSeekBarColors(progress: Int) {
        // 진동 모드 (0%) vs 소리 모드 (1%+) 시각적 구분을 위한 색상 업데이트
        val seekBar = binding.seekBarVolume
        
        if (progress == 0) {
            // 진동 모드: 회색 SeekBar
            seekBar.thumb = ContextCompat.getDrawable(this, R.drawable.seekbar_thumb_gray)
            seekBar.progressDrawable = ContextCompat.getDrawable(this, R.drawable.seekbar_progress_gray)
        } else {
            // 소리 모드: 네온 블루 SeekBar  
            seekBar.thumb = ContextCompat.getDrawable(this, R.drawable.seekbar_thumb_neon)
            seekBar.progressDrawable = ContextCompat.getDrawable(this, R.drawable.seekbar_progress_neon)
        }
    }
    
    private fun setupNewTimer() {
        // 자동 타이머 이름 생성
        lifecycleScope.launch {
            val autoName = viewModel.generateAutoTimerName(if (categoryId == -1) null else categoryId)
            binding.editTextTimerName.setText(autoName)

            Log.d(TAG, "Auto-generated timer name: $autoName (categoryId: $categoryId)")
        }
    }
    
    private fun loadExistingTemplate() {
        currentTemplate?.let { template ->
            Log.d(TAG, "Loading existing template: ${template.name}")
            
            // 타이머 이름 설정
            binding.editTextTimerName.setText(template.name)
            
            // TimerTemplate의 totalDuration을 사용하여 시간 설정
            val totalSeconds = template.totalDuration
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            
            // 시간 설정
            binding.hourPicker.value = hours
            binding.minutePicker.value = minutes
            binding.secondPicker.value = seconds
            
            Log.d(TAG, "Loaded time: ${hours}h ${minutes}m ${seconds}s (total: ${totalSeconds}s)")
            
            // 소리 설정
            selectedSoundUri = template.soundUri
            currentVolume = template.volume
            isVibrationEnabled = template.vibrationEnabled
            
            // UI 업데이트
            binding.seekBarVolume.progress = (currentVolume * 100).toInt()
            binding.textViewVolumePercent.text = "${(currentVolume * 100).toInt()}%"
            binding.switchVibrationMode.isChecked = isVibrationEnabled
            
            updateSeekBarColors(binding.seekBarVolume.progress)
            updateTotalDuration()
            
            Log.d(TAG, "Template loaded successfully: name=${template.name}, volume=${currentVolume}, vibration=${isVibrationEnabled}")
        } ?: run {
            Log.w(TAG, "No template data found in intent")
            Toast.makeText(this, "편집할 타이머 데이터를 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateTotalDuration() {
        val hours = binding.hourPicker.value
        val minutes = binding.minutePicker.value  
        val seconds = binding.secondPicker.value
        
        val totalSeconds = hours * 3600 + minutes * 60 + seconds
        
        val durationText = when {
            hours > 0 -> {
                when {
                    minutes > 0 && seconds > 0 -> "${hours}시간 ${minutes}분 ${seconds}초"
                    minutes > 0 -> "${hours}시간 ${minutes}분"
                    seconds > 0 -> "${hours}시간 ${seconds}초"
                    else -> "${hours}시간"
                }
            }
            minutes > 0 -> {
                if (seconds > 0) "${minutes}분 ${seconds}초"
                else "${minutes}분"
            }
            else -> "${seconds}초"
        }
        
        binding.textViewTotalDuration.text = "총 시간: $durationText"
    }
    
    /**
     * 분 변경 핸들러: 59→00 시 시간 +1, 00→59 시 시간 -1
     */
    private fun handleMinuteChange(oldValue: Int, newValue: Int) {
        when {
            // 분: 59 → 00 (증가): 시간 +1
            oldValue == 59 && newValue == 0 -> {
                val currentHour = binding.hourPicker.value
                if (currentHour < 23) {
                    binding.hourPicker.value = currentHour + 1
                    Log.d(TAG, "분 59→00: 시간 ${currentHour} → ${currentHour + 1}")
                }
            }
            // 분: 00 → 59 (감소): 시간 -1  
            oldValue == 0 && newValue == 59 -> {
                val currentHour = binding.hourPicker.value
                if (currentHour > 0) {
                    binding.hourPicker.value = currentHour - 1
                    Log.d(TAG, "분 00→59: 시간 ${currentHour} → ${currentHour - 1}")
                }
            }
        }
    }
    
    /**
     * 초 변경 핸들러: 59→00 시 분 +1, 00→59 시 분 -1
     */
    private fun handleSecondChange(oldValue: Int, newValue: Int) {
        when {
            // 초: 59 → 00 (증가): 분 +1
            oldValue == 59 && newValue == 0 -> {
                val currentMinute = binding.minutePicker.value
                if (currentMinute < 59) {
                    binding.minutePicker.value = currentMinute + 1
                    Log.d(TAG, "초 59→00: 분 ${currentMinute} → ${currentMinute + 1}")
                } else {
                    // 분이 59일 때 시간도 증가 (연쇄 효과)
                    binding.minutePicker.value = 0
                    val currentHour = binding.hourPicker.value
                    if (currentHour < 23) {
                        binding.hourPicker.value = currentHour + 1
                        Log.d(TAG, "초 59→00 + 분 59→00: 시간 ${currentHour} → ${currentHour + 1}")
                    }
                }
            }
            // 초: 00 → 59 (감소): 분 -1
            oldValue == 0 && newValue == 59 -> {
                val currentMinute = binding.minutePicker.value
                if (currentMinute > 0) {
                    binding.minutePicker.value = currentMinute - 1
                    Log.d(TAG, "초 00→59: 분 ${currentMinute} → ${currentMinute - 1}")
                } else {
                    // 분이 0일 때 시간도 감소 (연쇄 효과)
                    binding.minutePicker.value = 59
                    val currentHour = binding.hourPicker.value
                    if (currentHour > 0) {
                        binding.hourPicker.value = currentHour - 1
                        Log.d(TAG, "초 00→59 + 분 00→59: 시간 ${currentHour} → ${currentHour - 1}")
                    }
                }
            }
        }
    }
    
    private fun saveTimer() {
        val name = binding.editTextTimerName.text.toString().trim()
        
        if (name.isEmpty()) {
            Toast.makeText(this, "타이머 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        val hours = binding.hourPicker.value
        val minutes = binding.minutePicker.value
        val seconds = binding.secondPicker.value
        val totalDuration = hours * 3600 + minutes * 60 + seconds
        
        if (totalDuration == 0) {
            Toast.makeText(this, "시간을 설정해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // categoryId == -1 means independent timer (null categoryId)
        
        // 단순 타이머 템플릿 생성 (소리 & 진동 설정 포함)
        val template = TimerTemplate(
            categoryId = if (categoryId == -1) null else categoryId,
            name = name,
            description = "", // 설명 제거
            totalDuration = totalDuration,
            timerType = TimerType.SIMPLE.name, // 단순 타이머만 지원
            isDefault = false,
            createdBy = "user",
            // 소리 & 진동 설정 추가
            soundUri = selectedSoundUri,
            volume = currentVolume,
            vibrationEnabled = isVibrationEnabled
        )
        
        // 하나의 라운드만 생성 (단순 타이머)
        val timerRound = TimerRound(
            templateId = 0,
            roundIndex = 0,
            name = name,
            duration = totalDuration,
            color = "#3B82F6"
        )
        
        Log.d(TAG, "Saving timer: $name, Duration: $totalDuration seconds, EditMode: $isEditMode")
        
        if (isEditMode && currentTemplate != null) {
            // 편집 모드: 기존 타이머 업데이트
            val updatedTemplate = currentTemplate!!.copy(
                name = name,
                totalDuration = totalDuration,
                soundUri = selectedSoundUri,
                volume = currentVolume,
                vibrationEnabled = isVibrationEnabled,
                updatedAt = System.currentTimeMillis()
            )
            
            // 새로운 라운드 생성 (단순 타이머이므로 1개 라운드)
            val updatedRound = TimerRound(
                templateId = updatedTemplate.id,
                roundIndex = 0,
                name = name,
                duration = totalDuration,
                color = "#3B82F6"
            )
            
            // TODO: updateTimerTemplate 메서드가 ViewModel에 있는지 확인 필요
            // 임시로 기존 방식 사용
            viewModel.insertTimerTemplate(updatedTemplate, listOf(updatedRound))
            
            Log.d(TAG, "Updating existing timer: ${updatedTemplate.name}")
            Toast.makeText(this, "타이머가 수정되었습니다: $name", Toast.LENGTH_SHORT).show()
        } else {
            // 새로 만들기 모드: 새 타이머 생성
            viewModel.insertTimerTemplate(template, listOf(timerRound))
            
            Log.d(TAG, "Creating new timer: ${template.name}")
            Toast.makeText(this, "타이머가 생성되었습니다: $name", Toast.LENGTH_SHORT).show()
        }
        
        // 저장 결과 관찰 (일회성)
        viewModel.loading.observe(this) { isLoading ->
            if (!isLoading) {
                // 로딩이 끝나면 성공/실패 확인
                viewModel.error.observe(this) { errorMessage ->
                    if (errorMessage != null) {
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    } else {
                        finish()
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // MediaPlayer 정리
        testMediaPlayer?.release()
        testMediaPlayer = null
    }
}