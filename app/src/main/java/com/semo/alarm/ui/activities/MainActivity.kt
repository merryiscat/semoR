package com.semo.alarm.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.semo.alarm.R
import com.semo.alarm.databinding.ActivityMainBinding
import com.semo.alarm.ui.fragments.AlarmFragment
import com.semo.alarm.ui.fragments.CustomTimerFragment
import com.semo.alarm.ui.fragments.ReportFragment
import com.semo.alarm.ui.fragments.SettingsFragment
import com.semo.alarm.ui.fragments.SleepFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBottomNavigation()
        
        // 기본 프래그먼트 설정 (알람 프래그먼트)
        if (savedInstanceState == null) {
            replaceFragment(AlarmFragment())
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_alarm -> {
                    replaceFragment(AlarmFragment())
                    true
                }
                R.id.nav_custom_timer -> {
                    replaceFragment(CustomTimerFragment())
                    true
                }
                R.id.nav_sleep -> {
                    replaceFragment(SleepFragment())
                    true
                }
                R.id.nav_report -> {
                    replaceFragment(ReportFragment())
                    true
                }
                R.id.nav_settings -> {
                    replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}