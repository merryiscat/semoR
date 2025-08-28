import React from 'react';
import { useAlarmStore } from '@/stores/alarmStore';
import { Alarm } from '@/types';
import { format } from 'date-fns';
import DayIndicator from './DayIndicator';
import ToggleSwitch from './ToggleSwitch';

interface AlarmItemProps {
  alarm: Alarm;
}

export function AlarmItem({ alarm }: AlarmItemProps) {
  const { toggleAlarm, deleteAlarm } = useAlarmStore();
  
  const formatTime = (date: Date) => {
    const hours = date.getHours();
    const minutes = date.getMinutes();
    const period = hours >= 12 ? '오후' : '오전';
    const displayHours = hours > 12 ? hours - 12 : hours === 0 ? 12 : hours;
    
    return `${period} ${displayHours}:${minutes.toString().padStart(2, '0')}`;
  };

  return (
    <div 
      className={`alarm-card rounded-xl p-4 mb-3 transition-all duration-300 ${
        !alarm.isActive ? 'opacity-70' : ''
      }`}
      style={{ backgroundColor: '#1E1E1E' }}
      onClick={(e) => {
        if (!e.target.closest('.toggle-switch') && !e.target.closest('.fa-ellipsis-vertical')) {
          toggleAlarm(alarm.id);
        }
      }}
    >
      <DayIndicator repeatDays={alarm.repeatDays} isActive={alarm.isActive} />
      
      <div className="flex justify-between items-center">
        <div>
          <p className={`text-2xl font-bold ${!alarm.isActive ? 'text-gray-500' : ''}`}>
            {formatTime(alarm.time)}
          </p>
          <div className="flex items-center mt-1">
            <span className={`text-sm mr-2 ${!alarm.isActive ? 'text-gray-500' : 'text-gray-400'}`}>
              미션
            </span>
            {alarm.hasMission ? (
              <i 
                className={`fas fa-check text-xs ${
                  !alarm.isActive 
                    ? 'text-gray-500' 
                    : 'text-primary'
                }`}
                style={{ color: !alarm.isActive ? undefined : '#00BCD4' }}
              ></i>
            ) : (
              <i className={`fas fa-times text-xs ${!alarm.isActive ? 'text-gray-500' : 'text-gray-500'}`}></i>
            )}
          </div>
        </div>
        
        <div className="flex items-center space-x-3">
          <ToggleSwitch
            isChecked={alarm.isActive}
            onChange={() => toggleAlarm(alarm.id)}
          />
          <button 
            className={`${!alarm.isActive ? 'text-gray-500' : 'text-gray-400'}`}
            onClick={(e) => {
              e.stopPropagation();
              if (confirm('이 알람을 삭제하시겠습니까?')) {
                deleteAlarm(alarm.id);
              }
            }}
          >
            <i className="fas fa-ellipsis-vertical"></i>
          </button>
        </div>
      </div>
    </div>
  );
}