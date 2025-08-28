import React from 'react';
import { Alarm } from '@/types';

interface NextAlarmCardProps {
  nextAlarm: Alarm | null;
}

const NextAlarmCard: React.FC<NextAlarmCardProps> = ({ nextAlarm }) => {
  const getTimeUntilAlarm = (alarmTime: Date): string => {
    const now = new Date();
    const diff = alarmTime.getTime() - now.getTime();
    
    if (diff <= 0) return '지나간 알람';
    
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    
    if (hours > 0) {
      return `${hours}시간 ${minutes}분 후에 울려요`;
    } else {
      return `${minutes}분 후에 울려요`;
    }
  };

  if (!nextAlarm) {
    return (
      <div className="rounded-xl p-4 mb-6" style={{ backgroundColor: '#1E1E1E' }}>
        <div className="flex justify-between items-center">
          <div>
            <p className="text-gray-400 text-sm">Next Alarm</p>
            <p className="font-medium">예정된 알람이 없습니다</p>
          </div>
          <i className="fas fa-bell-slash text-gray-400"></i>
        </div>
      </div>
    );
  }

  return (
    <div className="rounded-xl p-4 mb-6" style={{ backgroundColor: '#1E1E1E' }}>
      <div className="flex justify-between items-center">
        <div>
          <p className="text-gray-400 text-sm">Next Alarm</p>
          <p className="font-medium">{getTimeUntilAlarm(nextAlarm.time)}</p>
        </div>
        <i className="fas fa-bullhorn" style={{ color: '#00BCD4' }}></i>
      </div>
    </div>
  );
};

export default NextAlarmCard;