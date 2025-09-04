package com.semo.alarm.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.databinding.FragmentTimerCategoryBinding
import com.semo.alarm.ui.activities.AddEditTimerActivity
import com.semo.alarm.ui.adapters.TimerTemplateAdapter
import com.semo.alarm.ui.viewmodels.CustomTimerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimerCategoryFragment : Fragment() {
    
    private var _binding: FragmentTimerCategoryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CustomTimerViewModel by viewModels()
    private lateinit var adapter: TimerTemplateAdapter
    
    private var categoryId: Int? = null
    
    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        
        fun newInstance(categoryId: Int): TimerCategoryFragment {
            return TimerCategoryFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_CATEGORY_ID, categoryId)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryId = arguments?.getInt(ARG_CATEGORY_ID)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupFab()
        observeTemplates()
        
        // Load templates for this category
        categoryId?.let { id ->
            viewModel.loadTemplatesByCategory(id)
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
            layoutManager = LinearLayoutManager(context)
            adapter = this@TimerCategoryFragment.adapter
        }
    }
    
    private fun setupFab() {
        binding.fabAddTimer.setOnClickListener {
            categoryId?.let { id ->
                Toast.makeText(
                    context,
                    "카테고리 ID $id 에 새 타이머 추가 기능은 개발 중입니다",
                    Toast.LENGTH_SHORT
                ).show()
                
                // TODO: 타이머 생성 화면으로 이동
                // val intent = Intent(requireContext(), AddEditTimerActivity::class.java)
                // intent.putExtra("categoryId", id)
                // startActivity(intent)
            }
        }
    }
    
    private fun observeTemplates() {
        viewModel.templates.observe(viewLifecycleOwner) { templates ->
            if (templates.isEmpty()) {
                showEmptyState()
            } else {
                showTemplates(templates)
            }
        }
    }
    
    private fun showEmptyState() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.recyclerViewTemplates.visibility = View.GONE
        
        // Set category-specific empty state message
        binding.emptyStateTitle.text = "아직 타이머가 없습니다"
        binding.emptyStateMessage.text = "+ 버튼을 눌러 새 타이머를 만들어보세요"
    }
    
    private fun showTemplates(templates: List<TimerTemplate>) {
        binding.emptyStateLayout.visibility = View.GONE
        binding.recyclerViewTemplates.visibility = View.VISIBLE
        adapter.submitList(templates)
    }
    
    private fun onTemplateClicked(template: TimerTemplate) {
        if (template.isRunning) {
            // Pause timer
            viewModel.pauseTimer(template.id)
            Toast.makeText(context, "${template.name} 타이머 일시정지", Toast.LENGTH_SHORT).show()
        } else {
            // Start timer
            viewModel.startTimer(template.id)
            viewModel.incrementTemplateUsage(template.id)
            Toast.makeText(context, "${template.name} 타이머 시작!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun onResetTimer(template: TimerTemplate) {
        // Reset timer
        viewModel.resetTimer(template.id)
        Toast.makeText(context, "${template.name} 타이머 리셋", Toast.LENGTH_SHORT).show()
    }
    
    private fun onDeleteTemplate(template: TimerTemplate) {
        AlertDialog.Builder(requireContext())
            .setTitle("템플릿 삭제")
            .setMessage("'${template.name}' 템플릿을 삭제하시겠습니까?\n삭제된 템플릿은 복구할 수 없습니다.")
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteTemplate(template.id)
                Toast.makeText(context, "${template.name} 템플릿이 삭제되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun onEditTemplate(template: TimerTemplate) {
        // 편집 화면으로 이동
        val intent = Intent(requireContext(), AddEditTimerActivity::class.java)
        intent.putExtra("categoryId", categoryId)
        intent.putExtra("templateId", template.id)
        intent.putExtra("template", template)
        startActivity(intent)
        
        Toast.makeText(context, "${template.name} 편집", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}