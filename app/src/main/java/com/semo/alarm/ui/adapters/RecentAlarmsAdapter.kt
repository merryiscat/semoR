package com.semo.alarm.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.semo.alarm.R
import com.semo.alarm.data.entities.AlarmHistory

/**
 * 최근 알람 기록을 표시하는 RecyclerView 어댑터
 * 
 * 표시 컬럼: 시간 | 라벨 | 타입 | 상태 | 삭제
 */
class RecentAlarmsAdapter(
    private val onDeleteClick: (AlarmHistory) -> Unit
) : ListAdapter<AlarmHistory, RecentAlarmsAdapter.AlarmHistoryViewHolder>(AlarmHistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_alarm, parent, false)
        return AlarmHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlarmHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textAlarmTime: TextView = itemView.findViewById(R.id.textAlarmTime)
        private val textAlarmLabel: TextView = itemView.findViewById(R.id.textAlarmLabel)
        private val chipAlarmType: Chip = itemView.findViewById(R.id.chipAlarmType)
        private val chipAlarmStatus: Chip = itemView.findViewById(R.id.chipAlarmStatus)
        private val buttonDeleteAlarm: ImageButton = itemView.findViewById(R.id.buttonDeleteAlarm)

        fun bind(alarmHistory: AlarmHistory) {
            // 시간
            textAlarmTime.text = alarmHistory.getFormattedTime()
            
            // 라벨
            textAlarmLabel.text = alarmHistory.label.ifEmpty { "알람" }
            
            // 타입 Chip 설정
            setupTypeChip(alarmHistory.type)
            
            // 상태 Chip 설정
            setupStatusChip(alarmHistory)
            
            // 삭제 버튼 클릭 리스너
            buttonDeleteAlarm.setOnClickListener {
                onDeleteClick(alarmHistory)
            }
        }
        
        private fun setupTypeChip(type: AlarmHistory.AlarmType) {
            chipAlarmType.text = type.displayName
            
            when (type) {
                AlarmHistory.AlarmType.ALARM -> {
                    chipAlarmType.setTextColor(Color.parseColor("#00D4FF")) // 네온 블루
                    chipAlarmType.setChipBackgroundColor(
                        itemView.context.getColorStateList(R.color.neon_blue_light) 
                            ?: android.content.res.ColorStateList.valueOf(Color.parseColor("#E6F7FF"))
                    )
                    chipAlarmType.setChipStrokeColor(
                        itemView.context.getColorStateList(R.color.neon_blue)
                            ?: android.content.res.ColorStateList.valueOf(Color.parseColor("#00D4FF"))
                    )
                }
                AlarmHistory.AlarmType.TIMER -> {
                    chipAlarmType.setTextColor(Color.parseColor("#8B5CF6")) // 보라색
                    chipAlarmType.setChipBackgroundColor(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#F3E8FF"))
                    )
                    chipAlarmType.setChipStrokeColor(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#8B5CF6"))
                    )
                }
            }
        }
        
        private fun setupStatusChip(alarmHistory: AlarmHistory) {
            chipAlarmStatus.text = alarmHistory.getStatusWithSnooze()
            
            val statusColor = Color.parseColor(alarmHistory.status.colorResId)
            chipAlarmStatus.setTextColor(statusColor)
            chipAlarmStatus.setChipStrokeColor(
                android.content.res.ColorStateList.valueOf(statusColor)
            )
            
            // 배경색 설정 (상태별)
            val backgroundColor = when (alarmHistory.status) {
                AlarmHistory.AlarmStatus.COMPLETED -> Color.parseColor("#ECFDF5") // 연한 녹색
                AlarmHistory.AlarmStatus.SNOOZED -> Color.parseColor("#FFFBEB")   // 연한 노란색
                AlarmHistory.AlarmStatus.IGNORED -> Color.parseColor("#FEF2F2")   // 연한 빨간색
                AlarmHistory.AlarmStatus.RUNNING -> Color.parseColor("#EFF6FF")   // 연한 파란색
            }
            chipAlarmStatus.setChipBackgroundColor(
                android.content.res.ColorStateList.valueOf(backgroundColor)
            )
        }
    }

    class AlarmHistoryDiffCallback : DiffUtil.ItemCallback<AlarmHistory>() {
        override fun areItemsTheSame(oldItem: AlarmHistory, newItem: AlarmHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AlarmHistory, newItem: AlarmHistory): Boolean {
            return oldItem == newItem
        }
    }
}