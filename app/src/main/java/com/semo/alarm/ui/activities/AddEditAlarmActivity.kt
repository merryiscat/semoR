package com.semo.alarm.ui.activities

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
        
        binding.switchSnooze.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutSnoozeInterval.alpha = if (isChecked) 1.0f else 0.5f
            binding.seekBarSnooze.isEnabled = isChecked
        }
        
        binding.seekBarSnooze.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val snoozeInterval = if (progress == 0) 1 else progress
                binding.textViewSnoozeValue.text = "${snoozeInterval}분"
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
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
        binding.switchSnooze.isChecked = alarm.snoozeEnabled
        binding.seekBarSnooze.progress = alarm.snoozeInterval
        binding.textViewSnoozeValue.text = "${alarm.snoozeInterval}분"
        
        val selectedDays = alarm.getDaysAsList()
        dayChips.forEach { (day, chip) ->
            chip.isChecked = selectedDays.contains(day)
        }
    }
    
    private fun saveAlarm() {
        // 12시간 형식을 24시간 형식으로 변환
        val hour12 = binding.hourPicker.value
        val minute = binding.minutePicker.value
        val isAM = binding.amPmPicker.value == 0
        
        val hour24 = convertTo24HourFormat(hour12, isAM)
        
        val time = String.format("%02d:%02d", hour24, minute)
        val label = binding.editTextAlarmLabel.text.toString().trim()
        
        val selectedDays = dayChips.filter { it.value.isChecked }.keys.toList()
        val daysString = if (selectedDays.isEmpty()) "once" else selectedDays.joinToString(",")
        val snoozeInterval = if (binding.seekBarSnooze.progress == 0) 1 else binding.seekBarSnooze.progress
        
        val alarm = if (isEditMode && currentAlarm != null) {
            currentAlarm!!.copy(
                time = time,
                label = label,
                days = daysString,
                snoozeEnabled = binding.switchSnooze.isChecked,
                snoozeInterval = snoozeInterval,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            Alarm(
                time = time,
                label = label,
                days = daysString,
                snoozeEnabled = binding.switchSnooze.isChecked,
                snoozeInterval = snoozeInterval
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