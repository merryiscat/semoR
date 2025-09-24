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
            when (intent?.action) {
                "TIMER_STATE_UPDATE" -> {
                    val timerId = intent.getIntExtra("timerId", -1)
                    val isRunning = intent.getBooleanExtra("isRunning", false)
                    val remainingSeconds = intent.getIntExtra("remainingSeconds", 0)

                    if (timerId != -1) {
                        updateTimerInAdapter(timerId, isRunning, remainingSeconds)
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
        
        // Load categories on start
        viewModel.loadAllCategories()
    }
    
    override fun onResume() {
        super.onResume()
        // 카테고리 추가 후 돌아왔을 때 목록 새로고침
        viewModel.loadAllCategories()

        // Register broadcast receiver for timer updates
        val filter = IntentFilter("TIMER_STATE_UPDATE")
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(timerUpdateReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(timerUpdateReceiver)
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
        if (timer.isRunning) {
            // Pause timer
            viewModel.pauseTimer(timer.id)
            Toast.makeText(context, "${timer.name} 타이머 일시정지", Toast.LENGTH_SHORT).show()
        } else {
            // Start timer (resume from where it was paused or start fresh)
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
        // Update both categories and independent templates lists
        viewModel.categories.value?.let { categories ->
            updateMixedList(categories)
        }

        // Also refresh independent templates
        viewModel.loadIndependentTemplates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}