package com.semo.alarm.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.databinding.ItemAlarmBinding

class AlarmAdapter(
    private val onAlarmClick: (Alarm) -> Unit,
    private val onAlarmToggle: (Alarm, Boolean) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = ItemAlarmBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlarmViewHolder(
        private val binding: ItemAlarmBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAlarmClick(getItem(position))
                }
            }
            
            binding.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAlarmToggle(getItem(position), isChecked)
                }
            }
        }

        fun bind(alarm: Alarm) {
            binding.textViewTime.text = alarm.time
            binding.textViewLabel.text = alarm.label.ifEmpty { "알람" }
            binding.textViewDays.text = formatDays(alarm.getDaysAsList())
            binding.switchAlarm.isChecked = alarm.isActive
            
            // 활성화 상태에 따른 시각적 피드백
            val context = binding.root.context
            if (alarm.isActive) {
                binding.accentBar.setBackgroundColor(context.getColor(com.semo.alarm.R.color.md_theme_primary))
                binding.textViewTime.alpha = 1.0f
                binding.textViewLabel.alpha = 1.0f
                binding.textViewDays.alpha = 1.0f
            } else {
                binding.accentBar.setBackgroundColor(context.getColor(com.semo.alarm.R.color.inactive_alarm))
                binding.textViewTime.alpha = 0.6f
                binding.textViewLabel.alpha = 0.6f
                binding.textViewDays.alpha = 0.6f
            }
        }
        
        private fun formatDays(days: List<String>): String {
            if (days.isEmpty()) return "한 번만"
            
            val dayNames = mapOf(
                "mon" to "월",
                "tue" to "화", 
                "wed" to "수",
                "thu" to "목",
                "fri" to "금",
                "sat" to "토",
                "sun" to "일"
            )
            
            return days.mapNotNull { dayNames[it] }.joinToString(", ")
        }
    }

    class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem == newItem
        }
    }
}