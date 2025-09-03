package com.semo.alarm.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.ui.fragments.TimerCategoryFragment

class TimerCategoryPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {
    
    private var categories = mutableListOf<TimerCategory>()
    
    override fun getItemCount(): Int = categories.size
    
    override fun createFragment(position: Int): Fragment {
        return TimerCategoryFragment.newInstance(categories[position].id)
    }
    
    fun getCategoryTitle(position: Int): String {
        return if (position < categories.size) {
            categories[position].getDisplayName()
        } else {
            "기타"
        }
    }
    
    fun getCategoryId(position: Int): Int {
        return if (position < categories.size) {
            categories[position].id
        } else {
            0
        }
    }
    
    fun getCategory(position: Int): TimerCategory? {
        return if (position < categories.size) {
            categories[position]
        } else {
            null
        }
    }
    
    fun updateCategories(newCategories: List<TimerCategory>) {
        val oldSize = categories.size
        categories.clear()
        categories.addAll(newCategories)
        
        // Notify adapter of data changes
        when {
            oldSize == 0 && newCategories.isNotEmpty() -> {
                // First time loading
                notifyDataSetChanged()
            }
            oldSize != newCategories.size -> {
                // Size changed
                notifyDataSetChanged()
            }
            else -> {
                // Size same, check if content changed
                notifyDataSetChanged()
            }
        }
    }
    
    fun isEmpty(): Boolean = categories.isEmpty()
    
    fun getCategoriesCount(): Int = categories.size
}