package com.semo.alarm.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.semo.alarm.databinding.ItemColorSelectionBinding

class ColorSelectionAdapter(
    private val colors: List<String>,
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorSelectionAdapter.ColorViewHolder>() {
    
    private var selectedColor: String? = null
    
    fun selectColor(color: String) {
        val previousSelectedIndex = colors.indexOf(selectedColor)
        selectedColor = color
        val currentSelectedIndex = colors.indexOf(color)
        
        if (previousSelectedIndex != -1) notifyItemChanged(previousSelectedIndex)
        if (currentSelectedIndex != -1) notifyItemChanged(currentSelectedIndex)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ItemColorSelectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ColorViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position])
    }
    
    override fun getItemCount(): Int = colors.size
    
    inner class ColorViewHolder(private val binding: ItemColorSelectionBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(color: String) {
            binding.viewColor.setBackgroundColor(Color.parseColor(color))
            
            // 선택된 색상 하이라이트
            binding.cardColor.isSelected = (color == selectedColor)
            
            binding.cardColor.setOnClickListener {
                selectColor(color)
                onColorSelected(color)
            }
        }
    }
}