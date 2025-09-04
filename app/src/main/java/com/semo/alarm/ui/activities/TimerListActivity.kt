package com.semo.alarm.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.semo.alarm.databinding.ActivityTimerListBinding
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.ui.adapters.TimerTemplateAdapter
import com.semo.alarm.ui.viewmodels.CustomTimerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimerListActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTimerListBinding
    private val viewModel: CustomTimerViewModel by viewModels()
    private lateinit var adapter: TimerTemplateAdapter
    
    private var category: TimerCategory? = null
    
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
            onDeleteClick = { template -> onDeleteTemplate(template) }
        )
        
        binding.recyclerViewTemplates.apply {
            layoutManager = LinearLayoutManager(this@TimerListActivity)
            adapter = this@TimerListActivity.adapter
        }
    }
    
    private fun setupFab() {
        binding.fabAddTimer.setOnClickListener {
            category?.let { cat ->
                Toast.makeText(
                    this,
                    "${cat.name} 카테고리에 새 타이머 추가 기능은 개발 중입니다",
                    Toast.LENGTH_SHORT
                ).show()
                
                // TODO: 타이머 생성 화면으로 이동
                // val intent = Intent(this, AddEditTimerActivity::class.java)
                // intent.putExtra("categoryId", cat.id)
                // startActivity(intent)
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
        // 템플릿 사용 횟수 증가
        viewModel.incrementTemplateUsage(template.id)
        
        Toast.makeText(this, "${template.name} 타이머 시작!", Toast.LENGTH_SHORT).show()
        
        // TODO: 타이머 실행 화면으로 이동
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
}