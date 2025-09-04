package com.semo.alarm.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.databinding.ItemTimerTemplateBinding

class TimerTemplateAdapter(
    private val onItemClick: (TimerTemplate) -> Unit,
    private val onDeleteClick: (TimerTemplate) -> Unit
) : ListAdapter<TimerTemplate, TimerTemplateAdapter.TimerTemplateViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerTemplateViewHolder {
        val binding = ItemTimerTemplateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimerTemplateViewHolder(binding, onItemClick, onDeleteClick)
    }
    
    override fun onBindViewHolder(holder: TimerTemplateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class TimerTemplateViewHolder(
        private val binding: ItemTimerTemplateBinding,
        private val onItemClick: (TimerTemplate) -> Unit,
        private val onDeleteClick: (TimerTemplate) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(template: TimerTemplate) {
            binding.apply {
                // Set template info
                tvTemplateName.text = template.name
                tvTemplateDescription.text = template.description
                tvDuration.text = formatDuration(template.totalDuration)
                tvUsageCount.text = "${template.usageCount}회 사용"
                
                // Set category icon (기본값 사용, 나중에 카테고리 정보와 함께 업데이트)
                tvCategoryIcon.text = "⏰"
                
                // Set click listeners
                root.setOnClickListener { onItemClick(template) }
                btnDeleteTemplate.setOnClickListener { onDeleteClick(template) }
            }
        }
        
        private fun formatDuration(seconds: Int): String {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            
            return when {
                minutes == 0 -> "${remainingSeconds}초"
                remainingSeconds == 0 -> "${minutes}분"
                else -> "${minutes}분 ${remainingSeconds}초"
            }
        }
        
        // 카테고리 아이콘은 나중에 TimerCategory와 함께 표시
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<TimerTemplate>() {
        override fun areItemsTheSame(oldItem: TimerTemplate, newItem: TimerTemplate): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: TimerTemplate, newItem: TimerTemplate): Boolean {
            return oldItem == newItem
        }
    }
}