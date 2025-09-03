package com.semo.alarm.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.databinding.ItemCategoryBinding

class CategoryListAdapter(
    private val onCategoryClicked: (TimerCategory) -> Unit
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