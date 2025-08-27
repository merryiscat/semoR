import { format, isToday, isTomorrow, addDays } from 'date-fns';
import { ko } from 'date-fns/locale';

export function formatAlarmTime(date: Date): string {
  return format(date, 'HH:mm');
}

export function formatAlarmDate(date: Date): string {
  if (isToday(date)) {
    return '오늘';
  } else if (isTomorrow(date)) {
    return '내일';
  } else {
    return format(date, 'M월 d일 (E)', { locale: ko });
  }
}

export function getNextAlarmTime(alarmTime: Date): Date {
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const nextAlarm = new Date(today);
  
  nextAlarm.setHours(alarmTime.getHours(), alarmTime.getMinutes(), 0, 0);
  
  if (nextAlarm <= now) {
    nextAlarm.setDate(nextAlarm.getDate() + 1);
  }
  
  return nextAlarm;
}

export function getTimeUntilAlarm(alarmTime: Date): string {
  const now = new Date();
  const nextAlarm = getNextAlarmTime(alarmTime);
  const diffInMinutes = Math.floor((nextAlarm.getTime() - now.getTime()) / (1000 * 60));
  
  if (diffInMinutes < 60) {
    return `${diffInMinutes}분 후`;
  } else if (diffInMinutes < 24 * 60) {
    const hours = Math.floor(diffInMinutes / 60);
    const minutes = diffInMinutes % 60;
    return minutes > 0 ? `${hours}시간 ${minutes}분 후` : `${hours}시간 후`;
  } else {
    const days = Math.floor(diffInMinutes / (24 * 60));
    const hours = Math.floor((diffInMinutes % (24 * 60)) / 60);
    return hours > 0 ? `${days}일 ${hours}시간 후` : `${days}일 후`;
  }
}