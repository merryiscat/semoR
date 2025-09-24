package com.semo.alarm.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.semo.alarm.R
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.data.entities.TimerTemplate
import com.semo.alarm.databinding.ItemCategoryBinding
import com.semo.alarm.databinding.ItemTimerTemplateBinding

class MixedTimerAdapter(
    private val onCategoryClicked: (TimerCategory) -> Unit,
    private val onCategoryDeleteClicked: (TimerCategory) -> Unit,
    private val onTimerClicked: (TimerTemplate) -> Unit,
    private val onTimerLongClicked: (TimerTemplate) -> Unit,
    private val onTimerDeleteClicked: (TimerTemplate) -> Unit
) : ListAdapter<MixedTimerItem, RecyclerView.ViewHolder>(MixedTimerDiffCallback()) {

    companion object {
        private const val TYPE_CATEGORY = 0
        private const val TYPE_INDEPENDENT_TIMER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MixedTimerItem.CategoryItem -> TYPE_CATEGORY
            is MixedTimerItem.IndependentTimerItem -> TYPE_INDEPENDENT_TIMER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CATEGORY -> {
                val binding = ItemCategoryBinding.inflate(inflater, parent, false)
                CategoryViewHolder(binding)
            }
            TYPE_INDEPENDENT_TIMER -> {
                val binding = ItemTimerTemplateBinding.inflate(inflater, parent, false)
                IndependentTimerViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MixedTimerItem.CategoryItem -> {
                (holder as CategoryViewHolder).bind(item.category, item.templateCount)
            }
            is MixedTimerItem.IndependentTimerItem -> {
                (holder as IndependentTimerViewHolder).bind(item.template)
            }
        }
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: TimerCategory, templateCount: Int) {
            binding.apply {
                tvCategoryName.text = category.name
                tvTemplateCount.text = "${templateCount}개"

                // 기본 카테고리는 삭제 버튼 숨기기
                btnDeleteCategory.visibility = if (category.isDefault) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }

                // 클릭 리스너 설정
                root.setOnClickListener {
                    onCategoryClicked(category)
                }

                btnDeleteCategory.setOnClickListener {
                    onCategoryDeleteClicked(category)
                }
            }
        }
    }

    inner class IndependentTimerViewHolder(
        private val binding: ItemTimerTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(template: TimerTemplate) {
            binding.apply {
                tvTemplateName.text = template.name
                tvDuration.text = template.getFormattedDuration()

                // 독립 타이머임을 나타내는 시각적 구분
                root.alpha = 0.9f

                // 독립 타이머는 삭제 버튼 숨기기 (롱클릭으로만 삭제)
                btnDeleteTemplate.visibility = android.view.View.GONE

                // 클릭 리스너 설정
                root.setOnClickListener {
                    onTimerClicked(template)
                }

                root.setOnLongClickListener {
                    onTimerLongClicked(template)
                    true
                }

                btnDeleteTemplate.setOnClickListener {
                    onTimerDeleteClicked(template)
                }
            }
        }
    }
}

class MixedTimerDiffCallback : DiffUtil.ItemCallback<MixedTimerItem>() {
    override fun areItemsTheSame(oldItem: MixedTimerItem, newItem: MixedTimerItem): Boolean {
        return when {
            oldItem is MixedTimerItem.CategoryItem && newItem is MixedTimerItem.CategoryItem ->
                oldItem.category.id == newItem.category.id
            oldItem is MixedTimerItem.IndependentTimerItem && newItem is MixedTimerItem.IndependentTimerItem ->
                oldItem.template.id == newItem.template.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: MixedTimerItem, newItem: MixedTimerItem): Boolean {
        return when {
            oldItem is MixedTimerItem.CategoryItem && newItem is MixedTimerItem.CategoryItem ->
                oldItem.category == newItem.category && oldItem.templateCount == newItem.templateCount
            oldItem is MixedTimerItem.IndependentTimerItem && newItem is MixedTimerItem.IndependentTimerItem ->
                oldItem.template == newItem.template
            else -> false
        }
    }
}