package com.semo.alarm.ui.adapters

import com.semo.alarm.data.entities.TimerCategory
import com.semo.alarm.data.entities.TimerTemplate

sealed class MixedTimerItem {
    data class CategoryItem(val category: TimerCategory, val templateCount: Int) : MixedTimerItem()
    data class IndependentTimerItem(val template: TimerTemplate) : MixedTimerItem()
}