package com.semo.alarm.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.semo.alarm.databinding.FragmentSleepBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SleepFragment : Fragment() {
    
    private var _binding: FragmentSleepBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSleepBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: 수면 추적 기능 구현 (Phase 2)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}