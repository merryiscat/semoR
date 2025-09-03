package com.semo.alarm.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.semo.alarm.databinding.FragmentCustomTimerBinding
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
        categoryAdapter = CategoryListAdapter { category ->
            // 카테고리 클릭 시 해당 카테고리의 타이머 목록 화면으로 이동
            val intent = Intent(requireContext(), TimerListActivity::class.java)
            intent.putExtra("category", category)
            startActivity(intent)
        }
        
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}