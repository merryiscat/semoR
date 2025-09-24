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
import com.semo.alarm.databinding.FragmentCustomTimerBinding
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.ui.activities.AddEditCategoryActivity
import com.semo.alarm.ui.activities.TimerListActivity
import com.semo.alarm.ui.adapters.CategoryListAdapter
import com.semo.alarm.ui.viewmodels.CustomTimerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomTimerFragment : Fragment() {
    
    private var _binding: FragmentCustomTimerBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CustomTimerViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryListAdapter
    
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
    }
    
    private fun setupRecyclerView() {
        categoryAdapter = CategoryListAdapter(
            onCategoryClicked = { category ->
                // 카테고리 클릭 시 해당 카테고리의 타이머 목록 화면으로 이동
                val intent = Intent(requireContext(), TimerListActivity::class.java)
                intent.putExtra("category", category)
                startActivity(intent)
            },
            onDeleteClicked = { category -> 
                onDeleteCategory(category)
            }
        )
        
        binding.recyclerViewCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
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
            // 기본 카테고리 찾아서 바로 타이머 추가 화면으로 이동
            val categories = categoryAdapter.currentList
            val defaultCategory = categories.find { it.name == "기본" && it.isDefault }

            val intent = Intent(requireContext(), com.semo.alarm.ui.activities.AddEditTimerActivity::class.java)
            if (defaultCategory != null) {
                intent.putExtra("category", defaultCategory)
                intent.putExtra("categoryId", defaultCategory.id)
            } else {
                // 기본 카테고리가 없으면 첫 번째 카테고리 사용, 그것도 없으면 경고
                if (categories.isNotEmpty()) {
                    intent.putExtra("category", categories.first())
                    intent.putExtra("categoryId", categories.first().id)
                } else {
                    Toast.makeText(context, "먼저 카테고리를 추가해주세요", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
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
        
        // Observe categories
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            if (categories.isNotEmpty()) {
                categoryAdapter.submitList(categories)
                binding.emptyStateLayout.visibility = android.view.View.GONE
                binding.recyclerViewCategories.visibility = android.view.View.VISIBLE
                
                // 카테고리 개수 업데이트 (알람과 동일한 방식)
                binding.categoryCountText.text = "카테고리 ${categories.size}개"
            } else {
                // 카테고리가 없을 때 처리 (알람과 동일한 방식)
                categoryAdapter.submitList(emptyList())
                binding.emptyStateLayout.visibility = android.view.View.VISIBLE
                binding.recyclerViewCategories.visibility = android.view.View.GONE
                binding.categoryCountText.text = "카테고리 0개"
            }
        }
        
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}