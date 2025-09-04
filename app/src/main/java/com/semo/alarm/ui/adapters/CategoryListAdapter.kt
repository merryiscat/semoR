package com.semo.alarm.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.databinding.ItemCategoryBinding

class CategoryListAdapter(
    private val onCategoryClicked: (TimerCategory) -> Unit,
    private val onDeleteClicked: (TimerCategory) -> Unit
) : ListAdapter<TimerCategory, CategoryListAdapter.CategoryViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: TimerCategory) {
            binding.tvCategoryName.text = category.name
            binding.tvTemplateCount.text = "타이머 관리"
            
            // "기본" 카테고리만 삭제 버튼 숨김 (운동/요리/학습/음료는 삭제 가능)
            if (category.isDefault && category.name == "기본") {
                binding.btnDeleteCategory.visibility = android.view.View.GONE
            } else {
                binding.btnDeleteCategory.visibility = android.view.View.VISIBLE
                binding.btnDeleteCategory.setOnClickListener {
                    onDeleteClicked(category)
                }
            }
            
            binding.root.setOnClickListener {
                onCategoryClicked(category)
            }
        }
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<TimerCategory>() {
        override fun areItemsTheSame(oldItem: TimerCategory, newItem: TimerCategory): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: TimerCategory, newItem: TimerCategory): Boolean {
            return oldItem == newItem
        }
    }
}