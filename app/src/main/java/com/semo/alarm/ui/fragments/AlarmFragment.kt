package com.semo.alarm.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.semo.alarm.data.entities.Alarm
import com.semo.alarm.databinding.FragmentAlarmBinding
import com.semo.alarm.ui.activities.AddEditAlarmActivity
import com.semo.alarm.ui.adapters.AlarmAdapter
import com.semo.alarm.ui.viewmodels.AlarmViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmFragment : Fragment() {
    
    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AlarmViewModel by viewModels()
    private lateinit var alarmAdapter: AlarmAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupFab()
        observeAlarms()
    }
    
    private fun setupRecyclerView() {
        alarmAdapter = AlarmAdapter(
            onAlarmClick = { alarm ->
                val intent = Intent(requireContext(), AddEditAlarmActivity::class.java).apply {
                    putExtra("alarm_id", alarm.id)
                    putExtra("alarm", alarm)
                }
                startActivity(intent)
            },
            onAlarmToggle = { alarm, isActive ->
                viewModel.toggleAlarmStatus(alarm.id, isActive)
            }
        )
        
        binding.recyclerViewAlarms.apply {
            adapter = alarmAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupFab() {
        binding.fabAddAlarm.setOnClickListener {
            val intent = Intent(requireContext(), AddEditAlarmActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun observeAlarms() {
        viewModel.allAlarms.observe(viewLifecycleOwner) { alarms ->
            alarmAdapter.submitList(alarms)
            updateAlarmCount(alarms.size)
            
            if (alarms.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.recyclerViewAlarms.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.recyclerViewAlarms.visibility = View.VISIBLE
            }
        }
    }
    
    private fun updateAlarmCount(count: Int) {
        val activeCount = count // 나중에 isActive로 필터링 가능
        binding.alarmCountText.text = "활성 알람 ${activeCount}개"
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}