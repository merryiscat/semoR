package com.semo.alarm.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.semo.alarm.ui.fragments.TimerCategoryFragment

class TimerCategoryPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {
    
    private val categoryIds = listOf(1, 2, 3, 4) // exercise, cooking, study, drink
    private val categoryTitles = mapOf(
        1 to "운동",
        2 to "요리", 
        3 to "학습",
        4 to "음료"
    )
    
    override fun getItemCount(): Int = categoryIds.size
    
    override fun createFragment(position: Int): Fragment {
        return TimerCategoryFragment.newInstance(categoryIds[position])
    }
    
    fun getCategoryTitle(position: Int): String {
        return categoryTitles[categoryIds[position]] ?: "기타"
    }
    
    fun getCategoryId(position: Int): Int {
        return categoryIds[position]
    }
}