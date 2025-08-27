import React from 'react';
import { useAlarmStore } from '@/stores/alarmStore';
import { AlarmItem } from './AlarmItem';
import { Clock } from 'lucide-react';

export function AlarmList() {
  const alarms = useAlarmStore((state) => state.alarms);
  
  if (alarms.length === 0) {
    return (
      <div className="card">
        <div className="empty-state">
          <Clock size={64} />
          <h3>아직 알람이 없습니다</h3>
          <p>첫 번째 알람을 추가해보세요!</p>
        </div>
      </div>
    );
  }
  
  return (
    <div className="card">
      <h2 style={{ marginBottom: '1.5rem' }}>내 알람 목록</h2>
      <div>
        {alarms
          .sort((a, b) => a.time.getTime() - b.time.getTime())
          .map((alarm) => (
            <AlarmItem key={alarm.id} alarm={alarm} />
          ))}
      </div>
    </div>
  );
}