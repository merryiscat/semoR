import React from 'react';
import { useAlarmStore } from '@/stores/alarmStore';
import { Alarm } from '@/types';
import { Power, Trash2, Clock } from 'lucide-react';
import { format } from 'date-fns';

interface AlarmItemProps {
  alarm: Alarm;
}

export function AlarmItem({ alarm }: AlarmItemProps) {
  const { toggleAlarm, deleteAlarm } = useAlarmStore();
  
  const getAlarmTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      basic: '기본',
      reminder: '리마인더',
      medication: '복약',
      workout: '운동',
      work: '업무',
      custom: '사용자 정의',
    };
    return labels[type] || type;
  };
  
  const getRepeatTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      once: '한 번만',
      daily: '매일',
      weekly: '매주',
      weekdays: '평일',
      weekends: '주말',
      custom: '사용자 정의',
    };
    return labels[type] || type;
  };
  
  return (
    <div className={`alarm-item ${!alarm.isActive ? 'inactive' : ''}`}>
      <div className="alarm-info">
        <h3>{alarm.title}</h3>
        {alarm.description && <p>{alarm.description}</p>}
        <p>
          <Clock size={16} style={{ display: 'inline', marginRight: '0.25rem' }} />
          {format(alarm.time, 'HH:mm')} · {getAlarmTypeLabel(alarm.type)} · {getRepeatTypeLabel(alarm.repeatType || 'once')}
        </p>
      </div>
      
      <div className="alarm-actions">
        <button
          className={`btn ${alarm.isActive ? 'btn-secondary' : 'btn-primary'}`}
          onClick={() => toggleAlarm(alarm.id)}
          title={alarm.isActive ? '알람 끄기' : '알람 켜기'}
        >
          <Power size={16} />
        </button>
        
        <button
          className="btn btn-danger"
          onClick={() => {
            if (confirm('이 알람을 삭제하시겠습니까?')) {
              deleteAlarm(alarm.id);
            }
          }}
          title="알람 삭제"
        >
          <Trash2 size={16} />
        </button>
      </div>
    </div>
  );
}