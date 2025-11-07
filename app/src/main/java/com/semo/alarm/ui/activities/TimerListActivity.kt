package com.semo.alarm.ui.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.semo.alarm.databinding.ActivityTimerListBinding
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.services.TimerForegroundService
import com.semo.alarm.ui.activities.AddEditTimerActivity
import com.semo.alarm.ui.adapters.TimerTemplateAdapter
import com.semo.alarm.ui.viewmodels.CustomTimerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimerListActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTimerListBinding
    private val viewModel: CustomTimerViewModel by viewModels()
    private lateinit var adapter: TimerTemplateAdapter
    
    private var category: TimerCategory? = null
    
    // Timer update broadcast receiver
    private val timerUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TimerForegroundService.BROADCAST_TIMER_UPDATE -> {
                    val timerId = intent.getIntExtra(TimerForegroundService.EXTRA_TIMER_ID, -1)
                    val remainingSeconds = intent.getIntExtra(TimerForegroundService.EXTRA_REMAINING_SECONDS, 0)
                    val isRunning = intent.getBooleanExtra(TimerForegroundService.EXTRA_IS_RUNNING, false)
                    
                    Log.d("TimerListActivity", "🔄 Timer update: ID=$timerId, ${remainingSeconds}초 남음")
                    updateTimerInAdapter(timerId, remainingSeconds, isRunning)
                }
                TimerForegroundService.BROADCAST_TIMER_COMPLETE -> {
                    val timerId = intent.getIntExtra(TimerForegroundService.EXTRA_TIMER_ID, -1)
                    Log.d("TimerListActivity", "Timer complete: ID=$timerId")
                    updateTimerInAdapter(timerId, 0, false)
                }
                TimerForegroundService.BROADCAST_TIMER_STOPPED -> {
                    val timerId = intent.getIntExtra(TimerForegroundService.EXTRA_TIMER_ID, -1)
                    Log.d("TimerListActivity", "Timer stopped: ID=$timerId")
                    updateTimerInAdapter(timerId, 0, false)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 카테고리 정보 받기
        category = intent.getParcelableExtra("category")
        
        setupUI()
        setupRecyclerView()
        setupFab()
        observeTemplates()
        
        // Register broadcast receiver for timer updates
        registerTimerUpdateReceiver()
        
        // 해당 카테고리의 템플릿들 로드
        category?.let { cat ->
            viewModel.loadTemplatesByCategory(cat.id)
        }
    }
    
    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        category?.let { cat ->
            binding.toolbar.title = "${cat.getDisplayName()} 타이머"
        } ?: run {
            binding.toolbar.title = "타이머 목록"
        }
    }
    
    private fun setupRecyclerView() {
        adapter = TimerTemplateAdapter(
            onItemClick = { template -> onTemplateClicked(template) },
            onDeleteClick = { template -> onDeleteTemplate(template) },
            onResetTimer = { template -> onResetTimer(template) },
            onEditClick = { template -> onEditTemplate(template) }
        )

        binding.recyclerViewTemplates.apply {
            layoutManager = LinearLayoutManager(this@TimerListActivity)
            adapter = this@TimerListActivity.adapter
        }
    }
    
    private fun setupFab() {
        binding.fabAddTimer.setOnClickListener {
            category?.let { cat ->
                val intent = Intent(this, AddEditTimerActivity::class.java)
                intent.putExtra("category", cat)
                startActivity(intent)
            }
        }
    }
    
    private fun observeTemplates() {
        viewModel.templates.observe(this) { templates ->
            adapter.submitList(templates)
            
            // 빈 상태 처리
            if (templates.isEmpty()) {
                showEmptyState()
            } else {
                hideEmptyState()
            }
        }
    }
    
    private fun showEmptyState() {
        binding.emptyStateLayout.visibility = android.view.View.VISIBLE
        binding.recyclerViewTemplates.visibility = android.view.View.GONE
        
        category?.let { cat ->
            binding.emptyStateTitle.text = "${cat.name} 타이머가 없습니다"
            binding.emptyStateMessage.text = "+ 버튼을 눌러 새로운 ${cat.name} 타이머를 만들어보세요"
        }
    }
    
    private fun hideEmptyState() {
        binding.emptyStateLayout.visibility = android.view.View.GONE
        binding.recyclerViewTemplates.visibility = android.view.View.VISIBLE
    }
    
    private fun onTemplateClicked(template: TimerTemplate) {
        if (template.isRunning) {
            // Pause timer
            viewModel.pauseTimer(template.id)
            Toast.makeText(this, "${template.name} 타이머 일시정지", Toast.LENGTH_SHORT).show()
        } else {
            // Start timer (resume from where it was paused or start fresh)
            viewModel.startTimer(template.id)
            Toast.makeText(this, "${template.name} 타이머 시작!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun onResetTimer(template: TimerTemplate) {
        // Reset timer
        viewModel.resetTimer(template.id)
        Toast.makeText(this, "${template.name} 타이머 리셋", Toast.LENGTH_SHORT).show()
    }
    
    private fun onDeleteTemplate(template: TimerTemplate) {
        AlertDialog.Builder(this)
            .setTitle("템플릿 삭제")
            .setMessage("'${template.name}' 템플릿을 삭제하시겠습니까?\n삭제된 템플릿은 복구할 수 없습니다.")
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteTemplate(template.id)
                Toast.makeText(this, "${template.name} 템플릿이 삭제되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun onEditTemplate(template: TimerTemplate) {
        // 편집 화면으로 이동
        val intent = Intent(this, AddEditTimerActivity::class.java)
        intent.putExtra("category", category)
        intent.putExtra("templateId", template.id)
        intent.putExtra("template", template)
        startActivity(intent)

        Toast.makeText(this, "${template.name} 편집", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister broadcast receiver
        unregisterTimerUpdateReceiver()
    }
    
    /**
     * 타이머 업데이트 브로드캐스트 수신기 등록
     */
    private fun registerTimerUpdateReceiver() {
        val filter = IntentFilter().apply {
            addAction(TimerForegroundService.BROADCAST_TIMER_UPDATE)
            addAction(TimerForegroundService.BROADCAST_TIMER_COMPLETE)
            addAction(TimerForegroundService.BROADCAST_TIMER_STOPPED)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(timerUpdateReceiver, filter)
        Log.d("TimerListActivity", "Timer update receiver registered")
    }
    
    /**
     * 타이머 업데이트 브로드캐스트 수신기 해제
     */
    private fun unregisterTimerUpdateReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(timerUpdateReceiver)
        Log.d("TimerListActivity", "Timer update receiver unregistered")
    }
    
    /**
     * 어댑터에서 특정 타이머 상태 업데이트
     */
    private fun updateTimerInAdapter(timerId: Int, remainingSeconds: Int, isRunning: Boolean) {
        val currentList = adapter.currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == timerId }
        
        if (index != -1) {
            val updatedTemplate = currentList[index].copy(
                isRunning = isRunning,
                remainingSeconds = remainingSeconds
            )
            currentList[index] = updatedTemplate
            adapter.submitList(currentList)
            
            Log.d("TimerListActivity", "✅ UI 업데이트: '${updatedTemplate.name}' ${remainingSeconds}초")
        } else {
            Log.w("TimerListActivity", "❌ Timer ID $timerId not found in adapter")
        }
    }
}