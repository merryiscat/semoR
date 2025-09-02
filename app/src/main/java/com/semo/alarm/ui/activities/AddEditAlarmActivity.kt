package com.semo.alarm.ui.activities

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.semo.alarm.R
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.databinding.ActivityAddEditAlarmBinding
import com.semo.alarm.ui.viewmodels.AlarmViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditAlarmActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAddEditAlarmBinding
    private val viewModel: AlarmViewModel by viewModels()
    
    private var alarmId: Int = -1
    private var isEditMode: Boolean = false
    private var currentAlarm: Alarm? = null
    
    private val dayChips by lazy {
        mapOf(
            "mon" to binding.chipMon,
            "tue" to binding.chipTue,
            "wed" to binding.chipWed,
            "thu" to binding.chipThu,
            "fri" to binding.chipFri,
            "sat" to binding.chipSat,
            "sun" to binding.chipSun
        )
    }
    
    private val repeatChips by lazy {
        listOf(binding.chipOnce, binding.chipTwice, binding.chipThrice)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityAddEditAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupViews()
        checkEditMode()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupViews() {
        // Custom NumberPicker TimePicker 설정
        setupCustomTimePicker()
        
        binding.buttonSave.setOnClickListener {
            saveAlarm()
        }
        
        binding.buttonCancel.setOnClickListener {
            finish()
        }
        
        // 반복 옵션 설정
        setupRepeatOptions()
        
        // 초기 Chip 색상 설정
        initializeChipColors()
        
        // 전체 선택/해제 버튼
        binding.btnSelectAll.setOnClickListener {
            selectAllDays()
        }
        
        binding.btnDeselectAll.setOnClickListener {
            deselectAllDays()
        }
        
        // 소리 & 진동 설정
        setupSoundAndVibrationSettings()
    }
    
    private fun setupCustomTimePicker() {
        // AM/PM NumberPicker 설정
        binding.amPmPicker.apply {
            minValue = 0
            maxValue = 1
            displayedValues = arrayOf("AM", "PM")
            wrapSelectorWheel = false
        }
        
        // 시간 NumberPicker 설정 (1-12)
        binding.hourPicker.apply {
            minValue = 1
            maxValue = 12
            wrapSelectorWheel = true
            
            // 12→1 또는 1→12 변경 시 AM/PM 자동 전환
            setOnValueChangedListener { _, oldVal, newVal ->
                when {
                    oldVal == 12 && newVal == 1 -> {
                        // 12 → 1: AM/PM 전환
                        binding.amPmPicker.value = if (binding.amPmPicker.value == 0) 1 else 0
                    }
                    oldVal == 1 && newVal == 12 -> {
                        // 1 → 12: AM/PM 전환  
                        binding.amPmPicker.value = if (binding.amPmPicker.value == 0) 1 else 0
                    }
                }
            }
        }
        
        // 분 NumberPicker 설정 (0-59)
        binding.minutePicker.apply {
            minValue = 0
            maxValue = 59
            wrapSelectorWheel = true
            // 2자리 표시를 위한 formatter
            setFormatter { value -> String.format("%02d", value) }
            
            // 59→00 또는 00→59 변경 시 시간 증감
            setOnValueChangedListener { _, oldVal, newVal ->
                when {
                    oldVal == 59 && newVal == 0 -> {
                        // 59분 → 00분: 시간 +1
                        incrementHour()
                    }
                    oldVal == 0 && newVal == 59 -> {
                        // 00분 → 59분: 시간 -1
                        decrementHour()
                    }
                }
            }
        }
    }
    
    private fun incrementHour() {
        val currentHour = binding.hourPicker.value
        if (currentHour == 12) {
            binding.hourPicker.value = 1
            // 12 → 1이므로 AM/PM 전환
            binding.amPmPicker.value = if (binding.amPmPicker.value == 0) 1 else 0
        } else {
            binding.hourPicker.value = currentHour + 1
        }
    }
    
    private fun decrementHour() {
        val currentHour = binding.hourPicker.value
        if (currentHour == 1) {
            binding.hourPicker.value = 12
            // 1 → 12이므로 AM/PM 전환
            binding.amPmPicker.value = if (binding.amPmPicker.value == 0) 1 else 0
        } else {
            binding.hourPicker.value = currentHour - 1
        }
    }
    
    private fun checkEditMode() {
        val alarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("alarm", Alarm::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("alarm")
        }
        
        alarmId = intent.getIntExtra("alarm_id", -1)
        
        if (alarm != null && alarmId != -1) {
            isEditMode = true
            currentAlarm = alarm
            binding.toolbar.title = getString(R.string.btn_edit)
            populateFields(alarm)
        } else {
            binding.toolbar.title = getString(R.string.btn_add)
            // 새 알람일 때 기본 이름 설정
            setDefaultAlarmName()
        }
    }
    
    private fun setupRepeatOptions() {
        // 반복 옵션 Chip들을 그룹처럼 동작하도록 설정
        repeatChips.forEach { chip ->
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // 다른 반복 옵션들은 해제 및 색상 변경
                    repeatChips.filter { it != chip }.forEach { otherChip ->
                        otherChip.isChecked = false
                        updateChipAppearance(otherChip, false)
                    }
                    
                    // 선택된 옵션의 색상 변경
                    updateChipAppearance(chip, true)
                    
                    // 반복 옵션에 따라 요일 선택 처리
                    when (chip.id) {
                        binding.chipOnce.id -> {
                            // 한 번만 선택 시 모든 요일 해제 및 색상 변경
                            dayChips.values.forEach { dayChip ->
                                dayChip.isChecked = false
                                updateChipAppearance(dayChip, false)
                            }
                        }
                        binding.chipTwice.id, binding.chipThrice.id -> {
                            // 2회, 3회 반복 선택 시 요일은 그대로 둠
                        }
                    }
                }
            }
        }
        
        // 요일 선택 시 한 번만 옵션 해제 및 색상 변경
        dayChips.values.forEach { chip ->
            chip.setOnCheckedChangeListener { _, isChecked ->
                updateChipAppearance(chip, isChecked)
                
                if (isChecked) {
                    binding.chipOnce.isChecked = false
                    updateChipAppearance(binding.chipOnce, false)
                }
                // 모든 요일이 해제되면 한 번만 자동 선택
                if (dayChips.values.none { it.isChecked }) {
                    binding.chipOnce.isChecked = true
                    updateChipAppearance(binding.chipOnce, true)
                }
            }
        }
    }
    
    private fun initializeChipColors() {
        // 모든 요일 Chip을 초기 상태(비선택)로 설정
        dayChips.values.forEach { chip ->
            updateChipAppearance(chip, chip.isChecked)
        }
        
        // 반복 옵션 Chip들도 초기화 (한 번만이 기본 선택)
        repeatChips.forEach { chip ->
            updateChipAppearance(chip, chip.isChecked)
        }
    }
    
    private fun selectAllDays() {
        binding.chipOnce.isChecked = false
        updateChipAppearance(binding.chipOnce, false)
        dayChips.values.forEach { chip ->
            chip.isChecked = true
            updateChipAppearance(chip, true)
        }
    }
    
    private fun deselectAllDays() {
        dayChips.values.forEach { chip ->
            chip.isChecked = false
            updateChipAppearance(chip, false)
        }
        binding.chipOnce.isChecked = true
        updateChipAppearance(binding.chipOnce, true)
    }
    
    private fun updateChipAppearance(chip: Chip, isSelected: Boolean) {
        if (isSelected) {
            // 선택된 상태: 네온 블루 배경, 검은색 텍스트
            chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.day_chip_on_bg))
            chip.setTextColor(ContextCompat.getColor(this, R.color.day_chip_on_text))
        } else {
            // 선택되지 않은 상태: 회색 배경, 흰색 텍스트
            chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.day_chip_off_bg))
            chip.setTextColor(ContextCompat.getColor(this, R.color.day_chip_off_text))
        }
    }
    
    private fun setDefaultAlarmName() {
        lifecycleScope.launch {
            try {
                val alarmCount = viewModel.getAlarmCount()
                val defaultName = "알람${alarmCount + 1}"
                binding.editTextAlarmLabel.setText(defaultName)
            } catch (e: Exception) {
                binding.editTextAlarmLabel.setText("알람1")
            }
        }
    }
    
    private fun populateFields(alarm: Alarm) {
        val (hour, minute) = alarm.getTimeAsHourMinute()
        
        // 24시간 형식을 12시간 형식으로 변환
        val (hour12, amPm) = convertTo12HourFormat(hour, minute)
        
        binding.amPmPicker.value = amPm
        binding.hourPicker.value = hour12
        binding.minutePicker.value = minute
        
        binding.editTextAlarmLabel.setText(alarm.label)
        
        // 반복 설정 복원
        when (alarm.days) {
            "once" -> {
                binding.chipOnce.isChecked = true
                updateChipAppearance(binding.chipOnce, true)
                binding.chipTwice.isChecked = false
                updateChipAppearance(binding.chipTwice, false)
                binding.chipThrice.isChecked = false
                updateChipAppearance(binding.chipThrice, false)
                dayChips.values.forEach { chip ->
                    chip.isChecked = false
                    updateChipAppearance(chip, false)
                }
            }
            "twice" -> {
                binding.chipOnce.isChecked = false
                updateChipAppearance(binding.chipOnce, false)
                binding.chipTwice.isChecked = true
                updateChipAppearance(binding.chipTwice, true)
                binding.chipThrice.isChecked = false
                updateChipAppearance(binding.chipThrice, false)
            }
            "thrice" -> {
                binding.chipOnce.isChecked = false
                updateChipAppearance(binding.chipOnce, false)
                binding.chipTwice.isChecked = false
                updateChipAppearance(binding.chipTwice, false)
                binding.chipThrice.isChecked = true
                updateChipAppearance(binding.chipThrice, true)
            }
            else -> {
                // 요일별 반복
                binding.chipOnce.isChecked = false
                updateChipAppearance(binding.chipOnce, false)
                binding.chipTwice.isChecked = false
                updateChipAppearance(binding.chipTwice, false)
                binding.chipThrice.isChecked = false
                updateChipAppearance(binding.chipThrice, false)
                
                val selectedDays = alarm.getDaysAsList()
                dayChips.forEach { (day, chip) ->
                    val isSelected = selectedDays.contains(day)
                    chip.isChecked = isSelected
                    updateChipAppearance(chip, isSelected)
                }
            }
        }
        
        // 소리 & 진동 설정 복원
        val volumeProgress = (alarm.volume * 100).toInt()
        binding.seekBarVolume.progress = volumeProgress
        binding.textViewVolumePercent.text = "${volumeProgress}%"
        
        binding.buttonSelectSound.text = if (alarm.soundUri.isEmpty()) "기본 알람음" else alarm.soundUri
        binding.switchVibrationMode.isChecked = alarm.silentMode
        
        // SeekBar 색상 초기 설정
        updateSeekBarColors(volumeProgress)
        
        // 진동 모드 상태에 따라 소리 컨트롤 활성화/비활성화
        updateSoundControlsEnabled(!alarm.silentMode)
    }
    
    private fun setupSoundAndVibrationSettings() {
        // 초기 SeekBar 색상 설정 (기본값 70%)
        updateSeekBarColors(binding.seekBarVolume.progress)
        
        // 볼륨 SeekBar 설정 - 볼륨 기반 진동 모드 자동 전환
        binding.seekBarVolume.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                binding.textViewVolumePercent.text = "${progress}%"
                
                // SeekBar 색상 업데이트
                updateSeekBarColors(progress)
                
                if (fromUser) {
                    // 볼륨이 0%가 되면 자동으로 진동 모드 활성화
                    if (progress == 0) {
                        binding.switchVibrationMode.isChecked = true
                        updateSoundControlsEnabled(false)
                    }
                    // 볼륨이 0%에서 올라가면 진동 모드 비활성화
                    else if (binding.switchVibrationMode.isChecked) {
                        binding.switchVibrationMode.isChecked = false
                        updateSoundControlsEnabled(true)
                    }
                }
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        // 알람음 선택 버튼
        binding.buttonSelectSound.setOnClickListener {
            showSoundSelectionDialog()
        }
        
        // 진동 모드 스위치 - OFF일 때 볼륨을 1%로 자동 조정
        binding.switchVibrationMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 진동 모드 ON: 볼륨을 0%로, 소리 컨트롤 비활성화
                binding.seekBarVolume.progress = 0
                binding.textViewVolumePercent.text = "0%"
                updateSeekBarColors(0)
                updateSoundControlsEnabled(false)
            } else {
                // 진동 모드 OFF: 볼륨을 1%로, 소리 컨트롤 활성화
                binding.seekBarVolume.progress = 1
                binding.textViewVolumePercent.text = "1%"
                updateSeekBarColors(1)
                updateSoundControlsEnabled(true)
            }
        }
    }
    
    private fun updateSoundControlsEnabled(enabled: Boolean) {
        binding.buttonSelectSound.isEnabled = enabled
        // SeekBar는 항상 활성화 - 진동 모드에서도 볼륨 조절 가능해야 함
        binding.seekBarVolume.isEnabled = true
        binding.textViewVolumePercent.alpha = if (enabled) 1.0f else 0.5f
    }
    
    private fun updateSeekBarColors(progress: Int) {
        val isVibrationMode = progress == 0
        
        if (isVibrationMode) {
            // 진동 모드 (0%): 딥 그레이 색상
            binding.seekBarVolume.thumb = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.seekbar_thumb_gray)
            binding.seekBarVolume.progressDrawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.seekbar_layer_gray)
        } else {
            // 소리 모드 (1% 이상): 네온 블루 색상
            binding.seekBarVolume.thumb = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.seekbar_thumb_neon)
            binding.seekBarVolume.progressDrawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.seekbar_layer_neon)
        }
    }
    
    private fun showSoundSelectionDialog() {
        val soundOptions = arrayOf("기본 알람음", "벨소리 1", "벨소리 2", "자연 소리", "음악 파일에서 선택")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("알람음 선택")
            .setItems(soundOptions) { _, which ->
                binding.buttonSelectSound.text = soundOptions[which]
            }
            .show()
    }

    private fun saveAlarm() {
        // 12시간 형식을 24시간 형식으로 변환
        val hour12 = binding.hourPicker.value
        val minute = binding.minutePicker.value
        val isAM = binding.amPmPicker.value == 0
        
        val hour24 = convertTo24HourFormat(hour12, isAM)
        
        val time = String.format("%02d:%02d", hour24, minute)
        
        // 알람명 처리: 입력된 값을 그대로 사용 (기본값이 이미 설정되어 있음)
        val label = binding.editTextAlarmLabel.text.toString().trim().ifEmpty { "알람" }
        
        // 반복 옵션 및 요일 처리
        val daysString = when {
            binding.chipOnce.isChecked -> "once"
            binding.chipTwice.isChecked -> "twice"
            binding.chipThrice.isChecked -> "thrice"
            else -> {
                val selectedDays = dayChips.filter { it.value.isChecked }.keys.toList()
                if (selectedDays.isEmpty()) "once" else selectedDays.joinToString(",")
            }
        }
        
        // 소리 & 진동 설정값 가져오기
        val volume = binding.seekBarVolume.progress / 100.0f
        val soundUri = if (binding.buttonSelectSound.text == "기본 알람음") "" else binding.buttonSelectSound.text.toString()
        val vibrationEnabled = true // 진동은 항상 활성화 (볼륨 0%일 때 무음+진동, 볼륨 있을 때 소리+진동)
        val silentMode = binding.switchVibrationMode.isChecked
        
        val alarm = if (isEditMode && currentAlarm != null) {
            currentAlarm!!.copy(
                time = time,
                label = label,
                days = daysString,
                volume = volume,
                soundUri = soundUri,
                vibrationEnabled = vibrationEnabled,
                silentMode = silentMode,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            Alarm(
                time = time,
                label = label,
                days = daysString,
                volume = volume,
                soundUri = soundUri,
                vibrationEnabled = vibrationEnabled,
                silentMode = silentMode
            )
        }
        
        if (isEditMode) {
            viewModel.updateAlarm(alarm)
        } else {
            viewModel.insertAlarm(alarm)
        }
        
        Toast.makeText(this, getString(R.string.msg_alarm_saved), Toast.LENGTH_SHORT).show()
        finish()
    }
    
    /**
     * 24시간 형식을 12시간 형식으로 변환
     * @return Pair<시간(1-12), AM/PM(0=AM, 1=PM)>
     */
    private fun convertTo12HourFormat(hour24: Int, minute: Int): Pair<Int, Int> {
        return when {
            hour24 == 0 -> Pair(12, 0) // 00:xx -> 12:xx AM
            hour24 < 12 -> Pair(hour24, 0) // 01:xx~11:xx -> 1:xx~11:xx AM
            hour24 == 12 -> Pair(12, 1) // 12:xx -> 12:xx PM
            else -> Pair(hour24 - 12, 1) // 13:xx~23:xx -> 1:xx~11:xx PM
        }
    }
    
    /**
     * 12시간 형식을 24시간 형식으로 변환
     */
    private fun convertTo24HourFormat(hour12: Int, isAM: Boolean): Int {
        return when {
            isAM && hour12 == 12 -> 0 // 12 AM -> 00
            isAM -> hour12 // 1~11 AM -> 1~11
            !isAM && hour12 == 12 -> 12 // 12 PM -> 12
            else -> hour12 + 12 // 1~11 PM -> 13~23
        }
    }
}