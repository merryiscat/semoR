import React from 'react';
import { useAlarmStore } from '@/stores/alarmStore';
import { AlarmItem } from './AlarmItem';
import { Clock } from 'lucide-react';

export function AlarmList() {
  const alarms = useAlarmStore((state) => state.alarms);
  
  if (alarms.length === 0) {
    return (
      <div className="rounded-xl p-6 text-center" style={{ backgroundColor: '#1E1E1E' }}>
        <Clock size={64} className="mx-auto mb-4 text-gray-400" />
        <h3 className="text-lg font-medium mb-2">아직 알람이 없습니다</h3>
        <p className="text-gray-400">첫 번째 알람을 추가해보세요!</p>
      </div>
    );
  }
  
  return (
    <div>
      {alarms
        .sort((a, b) => a.time.getTime() - b.time.getTime())
        .map((alarm) => (
          <AlarmItem key={alarm.id} alarm={alarm} />
        ))}
    </div>
  );
}