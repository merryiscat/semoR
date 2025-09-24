package com.semo.alarm.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
    private val onTimerDeleteClicked: (TimerTemplate) -> Unit,
    private val onTimerResetClicked: (TimerTemplate) -> Unit
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
                // Set template info
                tvTemplateName.text = template.name

                // Show remaining time if available, otherwise show total duration (same as category timers)
                if (template.remainingSeconds > 0) {
                    // 실행 중이거나 일시정지 상태 - 남은 시간 표시
                    tvDuration.text = formatDuration(template.remainingSeconds)
                    if (template.isRunning) {
                        // 실행 중 - 빨간색
                        tvDuration.setTextColor(ContextCompat.getColor(root.context, com.semo.alarm.R.color.md_theme_error))
                    } else {
                        // 일시정지 - 주황색으로 구분
                        tvDuration.setTextColor(ContextCompat.getColor(root.context, com.semo.alarm.R.color.orange))
                    }
                } else {
                    // 완전 정지 상태 - 초기 설정 시간 표시
                    tvDuration.text = formatDuration(template.totalDuration)
                    tvDuration.setTextColor(ContextCompat.getColor(root.context, com.semo.alarm.R.color.md_theme_onSurfaceVariant))
                }

                // Set category icon for independent timers
                tvCategoryIcon.text = "🔥"  // 독립 타이머 구분용 아이콘

                // Set refresh button state (same logic as category timers)
                if (template.isRunning) {
                    btnRefreshTimer.isEnabled = true
                    btnRefreshTimer.alpha = 1.0f
                } else {
                    btnRefreshTimer.isEnabled = false
                    btnRefreshTimer.alpha = 0.8f
                }

                // Set refresh button click listener
                btnRefreshTimer.setOnClickListener {
                    if (template.isRunning) {
                        onTimerResetClicked(template)
                    }
                }

                // 카테고리 타이머와 동일한 클릭 동작: 실행중=일시정지, 정지=시작
                root.setOnClickListener {
                    onTimerClicked(template)
                }

                // 롱클릭으로 편집 (카테고리 타이머와 동일)
                root.setOnLongClickListener {
                    onTimerLongClicked(template)
                    true
                }

                // 삭제 버튼 표시 (카테고리 타이머와 동일)
                btnDeleteTemplate.setOnClickListener {
                    onTimerDeleteClicked(template)
                }
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