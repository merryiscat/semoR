package com.semo.alarm.ui.activities

import android.os.Bundle
import android.util.Log
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
    private var templateId: Int = -1
    private var isEditMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Intent에서 데이터 받기
        category = intent.getParcelableExtra("category")
        templateId = intent.getIntExtra("templateId", -1)
        isEditMode = templateId != -1
        
        Log.d(TAG, "AddEditTimerActivity - Category: ${category?.name}, TemplateId: $templateId, EditMode: $isEditMode")
        
        setupUI()
        setupTimePickers()
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
            "${category?.name} 타이머 추가"
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
            value = 5 // 기본값 5분
            setOnValueChangedListener { _, _, _ -> updateTotalDuration() }
        }
        
        // 초 NumberPicker (0-59)
        binding.secondPicker.apply {
            minValue = 0
            maxValue = 59
            value = 0
            setOnValueChangedListener { _, _, _ -> updateTotalDuration() }
        }
        
        // 초기 총 시간 업데이트
        updateTotalDuration()
    }
    
    private fun setupButtons() {
        // 저장 버튼
        binding.btnSaveTimer.setOnClickListener {
            saveTimer()
        }
    }
    
    private fun setupNewTimer() {
        // 자동 타이머 이름 생성
        lifecycleScope.launch {
            val categoryId = category?.id ?: return@launch
            val existingTemplates = viewModel.getTemplatesByCategory(categoryId)
            val timerCount = existingTemplates.size + 1
            val autoName = "타이머$timerCount"
            
            binding.editTextTimerName.setText(autoName)
            
            Log.d(TAG, "Auto-generated timer name: $autoName")
        }
    }
    
    private fun loadExistingTemplate() {
        // TODO: 편집 모드 구현
        Toast.makeText(this, "편집 모드는 아직 구현되지 않았습니다", Toast.LENGTH_SHORT).show()
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
        
        val categoryId = category?.id ?: return
        
        // 단순 타이머 템플릿 생성
        val template = TimerTemplate(
            categoryId = categoryId,
            name = name,
            description = "", // 설명 제거
            totalDuration = totalDuration,
            timerType = TimerType.SIMPLE.name, // 단순 타이머만 지원
            isDefault = false,
            createdBy = "user"
        )
        
        // 하나의 라운드만 생성 (단순 타이머)
        val timerRound = TimerRound(
            templateId = 0,
            roundIndex = 0,
            name = name,
            duration = totalDuration,
            color = "#3B82F6"
        )
        
        Log.d(TAG, "Saving timer: $name, Duration: $totalDuration seconds")
        
        // ViewModel을 통해 데이터베이스에 저장
        viewModel.insertTimerTemplate(template, listOf(timerRound))
        
        // 저장 결과 관찰 (일회성)
        viewModel.loading.observe(this) { isLoading ->
            if (!isLoading) {
                // 로딩이 끝나면 성공/실패 확인
                viewModel.error.observe(this) { errorMessage ->
                    if (errorMessage == null) {
                        Toast.makeText(this, "타이머가 저장되었습니다: $name", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}