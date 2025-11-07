package com.semo.alarm.ui.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import com.semo.alarm.databinding.FragmentCustomTimerBinding
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.ui.activities.AddEditCategoryActivity
import com.semo.alarm.ui.activities.TimerListActivity
import com.semo.alarm.ui.adapters.CategoryListAdapter
import com.semo.alarm.ui.adapters.MixedTimerAdapter
import com.semo.alarm.ui.adapters.MixedTimerItem
import com.semo.alarm.ui.viewmodels.CustomTimerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomTimerFragment : Fragment() {
    
    private var _binding: FragmentCustomTimerBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CustomTimerViewModel by viewModels()
    private lateinit var mixedAdapter: MixedTimerAdapter

    private val timerUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            android.util.Log.d("CustomTimerFragment", "📡 BroadcastReceiver.onReceive: action=${intent?.action}")

            when (intent?.action) {
                com.semo.alarm.services.TimerForegroundService.BROADCAST_TIMER_UPDATE -> {
                    val timerId = intent.getIntExtra(com.semo.alarm.services.TimerForegroundService.EXTRA_TIMER_ID, -1)
                    val isRunning = intent.getBooleanExtra(com.semo.alarm.services.TimerForegroundService.EXTRA_IS_RUNNING, false)
                    val remainingSeconds = intent.getIntExtra(com.semo.alarm.services.TimerForegroundService.EXTRA_REMAINING_SECONDS, 0)

                    android.util.Log.d("CustomTimerFragment", "📡 BROADCAST_TIMER_UPDATE 수신: timerId=$timerId, isRunning=$isRunning, remainingSeconds=$remainingSeconds")

                    if (timerId != -1) {
                        updateTimerInAdapter(timerId, isRunning, remainingSeconds)
                    } else {
                        android.util.Log.w("CustomTimerFragment", "❌ Invalid timerId: $timerId")
                    }
                }
                com.semo.alarm.services.TimerForegroundService.BROADCAST_TIMER_COMPLETE -> {
                    val timerId = intent.getIntExtra(com.semo.alarm.services.TimerForegroundService.EXTRA_TIMER_ID, -1)
                    android.util.Log.d("CustomTimerFragment", "📡 BROADCAST_TIMER_COMPLETE 수신: timerId=$timerId")
                    if (timerId != -1) {
                        updateTimerInAdapter(timerId, isRunning = false, remainingSeconds = 0)
                    }
                }
                com.semo.alarm.services.TimerForegroundService.BROADCAST_TIMER_STOPPED -> {
                    val timerId = intent.getIntExtra(com.semo.alarm.services.TimerForegroundService.EXTRA_TIMER_ID, -1)
                    android.util.Log.d("CustomTimerFragment", "📡 BROADCAST_TIMER_STOPPED 수신: timerId=$timerId")
                    if (timerId != -1) {
                        updateTimerInAdapter(timerId, isRunning = false, remainingSeconds = 0)
                    }
                }
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomTimerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupAddCategoryButton()
        setupAddTimerButton()
        observeViewModel()

        // Register broadcast receiver for timer updates (Fragment 생성 시 등록)
        val filter = IntentFilter().apply {
            addAction(com.semo.alarm.services.TimerForegroundService.BROADCAST_TIMER_UPDATE)
            addAction(com.semo.alarm.services.TimerForegroundService.BROADCAST_TIMER_COMPLETE)
            addAction(com.semo.alarm.services.TimerForegroundService.BROADCAST_TIMER_STOPPED)
        }
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(timerUpdateReceiver, filter)

        android.util.Log.d("CustomTimerFragment", "✅ BroadcastReceiver 등록 완료 (onViewCreated)")

        // Load categories on start
        viewModel.loadAllCategories()
    }

    override fun onResume() {
        super.onResume()
        // 카테고리 추가 후 돌아왔을 때 목록 새로고침
        viewModel.loadAllCategories()

        android.util.Log.d("CustomTimerFragment", "👁️ Fragment resumed (visible)")
    }

    override fun onPause() {
        super.onPause()

        android.util.Log.d("CustomTimerFragment", "💤 Fragment paused (not visible)")
    }
    
    private fun setupRecyclerView() {
        mixedAdapter = MixedTimerAdapter(
            onCategoryClicked = { category ->
                // 카테고리 클릭 시 해당 카테고리의 타이머 목록 화면으로 이동
                val intent = Intent(requireContext(), TimerListActivity::class.java)
                intent.putExtra("category", category)
                startActivity(intent)
            },
            onCategoryDeleteClicked = { category ->
                onDeleteCategory(category)
            },
            onTimerClicked = { timer ->
                // 카테고리 타이머와 동일한 동작: 실행중=일시정지, 정지=시작/재개
                onTimerClicked(timer)
            },
            onTimerLongClicked = { timer ->
                // 롱클릭으로 편집 화면 이동 (카테고리 타이머와 동일)
                onEditTimer(timer)
            },
            onTimerDeleteClicked = { timer ->
                onDeleteIndependentTimer(timer)
            },
            onTimerResetClicked = { timer ->
                // 실행 중인 타이머 리셋
                onResetTimer(timer)
            }
        )

        binding.recyclerViewCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mixedAdapter
        }
    }
    
    private fun setupAddCategoryButton() {
        binding.btnAddCategory.setOnClickListener {
            val intent = Intent(requireContext(), AddEditCategoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupAddTimerButton() {
        binding.fabAddTimer.setOnClickListener {
            // 독립 타이머로 생성 (categoryId = null)
            val intent = Intent(requireContext(), com.semo.alarm.ui.activities.AddEditTimerActivity::class.java)
            intent.putExtra("categoryId", -1) // -1을 null 표시자로 사용
            startActivity(intent)
        }
    }
    
    private fun observeViewModel() {
        // Observe error messages
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        // Observe categories and combine with independent timers
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            updateMixedList(categories)
        }

        // Observe independent templates
        viewModel.independentTemplates.observe(viewLifecycleOwner) { independentTimers ->
            updateMixedList(viewModel.categories.value)
        }

        // Load independent templates through ViewModel
        viewModel.loadIndependentTemplates()
        
        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: Show/hide loading indicator if needed
        }
    }
    
    private fun onDeleteCategory(category: TimerCategory) {
        // "기본" 카테고리만 삭제 불가 (운동/요리/학습/음료는 삭제 가능)
        if (category.isDefault && category.name == "기본") {
            Toast.makeText(context, "기본 카테고리는 삭제할 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("카테고리 삭제")
            .setMessage("'${category.name}' 카테고리를 삭제하시겠습니까?\n카테고리 내 모든 타이머도 함께 삭제됩니다.")
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteCategory(category)
                Toast.makeText(context, "${category.name} 카테고리가 삭제되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun updateMixedList(categories: List<TimerCategory>?) {
        lifecycleScope.launch {
            val mixedItems = mutableListOf<MixedTimerItem>()

            // Add categories first
            categories?.forEach { category ->
                val templateCount = viewModel.getTemplatesByCategory(category.id).size
                mixedItems.add(MixedTimerItem.CategoryItem(category, templateCount))
            }

            // Add independent timers
            viewModel.independentTemplates.value?.forEach { template ->
                mixedItems.add(MixedTimerItem.IndependentTimerItem(template))
            }

            // Update UI
            if (mixedItems.isNotEmpty()) {
                mixedAdapter.submitList(mixedItems)
                binding.emptyStateLayout.visibility = android.view.View.GONE
                binding.recyclerViewCategories.visibility = android.view.View.VISIBLE

                val categoryCount = categories?.size ?: 0
                val timerCount = viewModel.independentTemplates.value?.size ?: 0
                binding.categoryCountText.text = "카테고리 ${categoryCount}개, 독립 타이머 ${timerCount}개"
            } else {
                mixedAdapter.submitList(emptyList())
                binding.emptyStateLayout.visibility = android.view.View.VISIBLE
                binding.recyclerViewCategories.visibility = android.view.View.GONE
                binding.categoryCountText.text = "카테고리 0개"
            }
        }
    }

    private fun onTimerClicked(timer: TimerTemplate) {
        android.util.Log.d("CustomTimerFragment", "🎯 Timer clicked: ID=${timer.id}, name='${timer.name}', isRunning=${timer.isRunning}, categoryId=${timer.categoryId}")

        if (timer.isRunning) {
            // Pause timer
            android.util.Log.d("CustomTimerFragment", "⏸️ Pausing timer: ID=${timer.id}")
            viewModel.pauseTimer(timer.id)
            Toast.makeText(context, "${timer.name} 타이머 일시정지", Toast.LENGTH_SHORT).show()
        } else {
            // Start timer (resume from where it was paused or start fresh)
            android.util.Log.d("CustomTimerFragment", "▶️ Starting timer: ID=${timer.id}")
            viewModel.startTimer(timer.id)
            Toast.makeText(context, "${timer.name} 타이머 시작!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onEditTimer(timer: TimerTemplate) {
        val intent = Intent(requireContext(), com.semo.alarm.ui.activities.AddEditTimerActivity::class.java)
        intent.putExtra("template", timer)
        startActivity(intent)
    }

    private fun onResetTimer(timer: TimerTemplate) {
        viewModel.resetTimer(timer.id)
        Toast.makeText(context, "${timer.name} 타이머 리셋", Toast.LENGTH_SHORT).show()
    }

    private fun onDeleteIndependentTimer(template: TimerTemplate) {
        AlertDialog.Builder(requireContext())
            .setTitle("독립 타이머 삭제")
            .setMessage("'${template.name}' 타이머를 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteTemplate(template.id)
                Toast.makeText(context, "${template.name} 타이머가 삭제되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateTimerInAdapter(timerId: Int, isRunning: Boolean, remainingSeconds: Int) {
        android.util.Log.d("CustomTimerFragment", "🔔 updateTimerInAdapter 호출: timerId=$timerId, isRunning=$isRunning, remainingSeconds=$remainingSeconds")

        // 현재 어댑터의 리스트에서 해당 타이머를 찾아서 직접 업데이트 (DB 재로드 없이 빠른 UI 업데이트)
        val currentList = mixedAdapter.currentList.toMutableList()

        android.util.Log.d("CustomTimerFragment", "📋 현재 리스트 크기: ${currentList.size}")
        currentList.forEachIndexed { idx, item ->
            when (item) {
                is MixedTimerItem.CategoryItem -> {
                    android.util.Log.d("CustomTimerFragment", "  [$idx] 카테고리: ${item.category.name}")
                }
                is MixedTimerItem.IndependentTimerItem -> {
                    android.util.Log.d("CustomTimerFragment", "  [$idx] 독립 타이머: ID=${item.template.id}, name='${item.template.name}', isRunning=${item.template.isRunning}, remaining=${item.template.remainingSeconds}")
                }
            }
        }

        val index = currentList.indexOfFirst {
            it is MixedTimerItem.IndependentTimerItem && it.template.id == timerId
        }

        if (index != -1) {
            val item = currentList[index]
            if (item is MixedTimerItem.IndependentTimerItem) {
                val updatedTemplate = item.template.copy(
                    isRunning = isRunning,
                    remainingSeconds = remainingSeconds
                )
                currentList[index] = MixedTimerItem.IndependentTimerItem(updatedTemplate)

                // submitList는 새 리스트 객체를 요구하므로 toList()로 복사
                mixedAdapter.submitList(currentList.toList())

                android.util.Log.d("CustomTimerFragment", "✅ 독립 타이머 UI 업데이트 완료: '${updatedTemplate.name}' ${remainingSeconds}초 (index=$index)")
            }
        } else {
            android.util.Log.w("CustomTimerFragment", "❌ Timer ID $timerId not found in adapter list")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Unregister broadcast receiver (Fragment 파괴 시 해제)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(timerUpdateReceiver)
        android.util.Log.d("CustomTimerFragment", "🔴 BroadcastReceiver 등록 해제 (onDestroyView)")

        _binding = null
    }
}