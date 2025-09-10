package com.semo.alarm.ui.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.semo.alarm.R
import com.semo.alarm.data.entities.AlarmHistory
import com.semo.alarm.ui.adapters.RecentAlarmsAdapter
import com.semo.alarm.ui.viewmodels.ReportViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class DailyReportFragment : Fragment() {
    
    private val reportViewModel: ReportViewModel by activityViewModels()
    private lateinit var dateSelector: TextView
    private lateinit var recentAlarmsRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var recentAlarmsAdapter: RecentAlarmsAdapter
    
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREAN)
    private val maxDaysBack = 31 // 최대 31일 전까지
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_daily_report, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        setupRecyclerView()
        observeViewModel()
        
        // 초기 데이터 로드 (오늘 날짜)
        loadRecentAlarms(calendar.time)
    }
    
    private fun setupViews(view: View) {
        dateSelector = view.findViewById(R.id.textDateSelector)
        recentAlarmsRecyclerView = view.findViewById(R.id.recyclerViewRecentAlarms)
        emptyStateLayout = view.findViewById(R.id.layoutEmptyState)
        
        // 초기 날짜 설정 (오늘)
        updateDateDisplay()
        
        // 날짜 선택기 클릭 리스너
        dateSelector.setOnClickListener {
            showDatePickerDialog()
        }
    }
    
    private fun setupRecyclerView() {
        recentAlarmsAdapter = RecentAlarmsAdapter(
            onDeleteClick = { alarmHistory ->
                deleteAlarmHistory(alarmHistory)
            }
        )
        
        recentAlarmsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentAlarmsAdapter
        }
    }
    
    private fun observeViewModel() {
        reportViewModel.recentAlarms.observe(viewLifecycleOwner) { alarms ->
            recentAlarmsAdapter.submitList(alarms)
            updateEmptyState(alarms.isEmpty())
        }
        
        reportViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: 로딩 상태 표시
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            recentAlarmsRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            recentAlarmsRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }
    
    private fun showDatePickerDialog() {
        val currentDate = calendar.time
        val minDate = Calendar.getInstance().apply {
            time = currentDate
            add(Calendar.DAY_OF_YEAR, -maxDaysBack)
        }
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                updateDateDisplay()
                loadRecentAlarms(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // 날짜 선택 범위 제한
        datePickerDialog.datePicker.minDate = minDate.timeInMillis
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        
        datePickerDialog.show()
    }
    
    private fun updateDateDisplay() {
        dateSelector.text = dateFormat.format(calendar.time)
    }
    
    private fun loadRecentAlarms(date: Date) {
        reportViewModel.loadRecentAlarmsForDate(date)
    }
    
    private fun deleteAlarmHistory(alarmHistory: AlarmHistory) {
        reportViewModel.deleteAlarmHistory(alarmHistory)
    }
}