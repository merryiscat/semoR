package com.semo.alarm.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.semo.alarm.databinding.ActivityAddEditCategoryBinding
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.ui.adapters.IconSelectionAdapter
import com.semo.alarm.ui.adapters.ColorSelectionAdapter
import com.semo.alarm.ui.viewmodels.CategoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddEditCategoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAddEditCategoryBinding
    private val categoryViewModel: CategoryViewModel by viewModels()
    
    private var editingCategory: TimerCategory? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 편집 모드 확인
        editingCategory = intent.getParcelableExtra("category")
        
        setupUI()
        setupClickListeners()
        
        // 편집 모드면 기존 데이터 로드
        editingCategory?.let { category ->
            binding.etCategoryName.setText(category.name)
        }
    }
    
    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = if (editingCategory != null) "카테고리 편집" else "카테고리 추가"
    }
    
    
    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveCategory()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun saveCategory() {
        val name = binding.etCategoryName.text.toString().trim()
        
        if (name.isEmpty()) {
            Toast.makeText(this, "카테고리 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        val category = if (editingCategory != null) {
            // 편집 모드
            editingCategory!!.copy(
                name = name,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            // 새로 추가 - 기본값 사용
            TimerCategory(
                name = name,
                description = "",
                icon = "📁", // 기본 아이콘
                color = "#6B7280", // 기본 색상 (딥 그레이)
                isDefault = false,
                createdBy = "user"
            )
        }
        
        try {
            if (editingCategory != null) {
                categoryViewModel.updateCategory(category)
            } else {
                categoryViewModel.addCategory(category)
            }
            
            Toast.makeText(this, "카테고리가 저장되었습니다", Toast.LENGTH_SHORT).show()
            
            // 안전하게 종료
            setResult(RESULT_OK)
            finish()
            
        } catch (e: Exception) {
            Toast.makeText(this, "저장 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("AddEditCategoryActivity", "Error saving category", e)
        }
    }
}