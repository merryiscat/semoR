package com.semo.alarm.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.semo.alarm.data.entities.TimerRound
import com.semo.alarm.databinding.ItemTimerRoundBinding

class TimerRoundAdapter(
    private val onDeleteRound: (Int) -> Unit,
    private val onEditRound: (Int) -> Unit
) : ListAdapter<TimerRound, TimerRoundAdapter.TimerRoundViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerRoundViewHolder {
        val binding = ItemTimerRoundBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TimerRoundViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: TimerRoundViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }
    
    inner class TimerRoundViewHolder(
        private val binding: ItemTimerRoundBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(round: TimerRound, position: Int) {
            binding.textViewRoundName.text = round.name
            binding.textViewRoundDuration.text = round.getFormattedDuration()
            binding.textViewRoundIndex.text = "${position + 1}"
            
            // 라운드 색상 적용
            try {
                val color = Color.parseColor(round.color)
                binding.cardViewRound.setCardBackgroundColor(color)
                
                // 색상에 따라 텍스트 색상 조정 (어두운 색이면 흰색 텍스트, 밝은 색이면 검은색 텍스트)
                val textColor = if (isColorDark(color)) {
                    Color.WHITE
                } else {
                    Color.BLACK
                }
                binding.textViewRoundName.setTextColor(textColor)
                binding.textViewRoundDuration.setTextColor(textColor)
                binding.textViewRoundIndex.setTextColor(textColor)
            } catch (e: Exception) {
                // 색상 파싱 실패 시 기본 색상 사용
                binding.cardViewRound.setCardBackgroundColor(Color.parseColor("#3B82F6"))
            }
            
            // 편집 버튼
            binding.btnEditRound.setOnClickListener {
                onEditRound(position)
            }
            
            // 삭제 버튼 (라운드가 1개 이상일 때만 표시)
            if (itemCount > 1) {
                binding.btnDeleteRound.visibility = android.view.View.VISIBLE
                binding.btnDeleteRound.setOnClickListener {
                    onDeleteRound(position)
                }
            } else {
                binding.btnDeleteRound.visibility = android.view.View.GONE
            }
        }
        
        private fun isColorDark(color: Int): Boolean {
            val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
            return darkness >= 0.5
        }
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<TimerRound>() {
        override fun areItemsTheSame(oldItem: TimerRound, newItem: TimerRound): Boolean {
            return oldItem.roundIndex == newItem.roundIndex
        }
        
        override fun areContentsTheSame(oldItem: TimerRound, newItem: TimerRound): Boolean {
            return oldItem == newItem
        }
    }
}