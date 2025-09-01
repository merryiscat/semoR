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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.timePickerAlarm.hour = hour
            binding.timePickerAlarm.minute = minute
        } else {
            @Suppress("DEPRECATION")
            binding.timePickerAlarm.currentHour = hour
            @Suppress("DEPRECATION")
            binding.timePickerAlarm.currentMinute = minute
        }
        
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
        val hour = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.timePickerAlarm.hour
        } else {
            @Suppress("DEPRECATION")
            binding.timePickerAlarm.currentHour
        }
        
        val minute = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.timePickerAlarm.minute
        } else {
            @Suppress("DEPRECATION")
            binding.timePickerAlarm.currentMinute
        }
        
        val time = String.format("%02d:%02d", hour, minute)
        val label = binding.editTextAlarmLabel.text.toString().trim()
        
        val selectedDays = dayChips.filter { it.value.isChecked }.keys.toList()
        val daysString = if (selectedDays.isEmpty()) "once" else selectedDays.toString()
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
}