import { useEffect } from 'react';
import { useAlarmStore } from '@/stores/alarmStore';
import { AlarmService } from '@/services/alarmService';

export function useAlarmManager() {
  const alarms = useAlarmStore((state) => state.alarms);
  const alarmService = AlarmService.getInstance();

  useEffect(() => {
    const initializeServices = async () => {
      await alarmService.initializeNotifications();
      await alarmService.cancelAllAlarms();
      await alarmService.scheduleMultipleAlarms(alarms);
    };

    initializeServices();

    return () => {
      alarmService.cancelAllAlarms();
    };
  }, [alarms, alarmService]);

  useEffect(() => {
    const handleVisibilityChange = async () => {
      if (document.visibilityState === 'visible') {
        await alarmService.cancelAllAlarms();
        await alarmService.scheduleMultipleAlarms(alarms);
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [alarms, alarmService]);

  return {
    alarmService,
    activeAlarmsCount: alarms.filter(alarm => alarm.isActive).length,
  };
}