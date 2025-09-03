package com.semo.alarm.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.semo.alarm.databinding.ItemIconSelectionBinding

class IconSelectionAdapter(
    private val icons: List<String>,
    private val onIconSelected: (String) -> Unit
) : RecyclerView.Adapter<IconSelectionAdapter.IconViewHolder>() {
    
    private var selectedIcon: String? = null
    
    fun selectIcon(icon: String) {
        val previousSelectedIndex = icons.indexOf(selectedIcon)
        selectedIcon = icon
        val currentSelectedIndex = icons.indexOf(icon)
        
        if (previousSelectedIndex != -1) notifyItemChanged(previousSelectedIndex)
        if (currentSelectedIndex != -1) notifyItemChanged(currentSelectedIndex)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val binding = ItemIconSelectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return IconViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(icons[position])
    }
    
    override fun getItemCount(): Int = icons.size
    
    inner class IconViewHolder(private val binding: ItemIconSelectionBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(icon: String) {
            binding.tvIcon.text = icon
            
            // 선택된 아이콘 하이라이트
            binding.cardIcon.isSelected = (icon == selectedIcon)
            
            binding.cardIcon.setOnClickListener {
                selectIcon(icon)
                onIconSelected(icon)
            }
        }
    }
}