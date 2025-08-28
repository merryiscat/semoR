import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { Alarm, AlarmStore } from '@/types';

export const useAlarmStore = create<AlarmStore>()(
  persist(
    (set, get) => ({
      alarms: [],
      
      addAlarm: (alarmData) => {
        const newAlarm: Alarm = {
          ...alarmData,
          id: crypto.randomUUID(),
          createdAt: new Date(),
          updatedAt: new Date(),
        };
        
        set((state) => ({
          alarms: [...state.alarms, newAlarm],
        }));
      },
      
      updateAlarm: (id, updates) => {
        set((state) => ({
          alarms: state.alarms.map((alarm) =>
            alarm.id === id
              ? { ...alarm, ...updates, updatedAt: new Date() }
              : alarm
          ),
        }));
      },
      
      deleteAlarm: (id) => {
        set((state) => ({
          alarms: state.alarms.filter((alarm) => alarm.id !== id),
        }));
      },
      
      toggleAlarm: (id) => {
        set((state) => ({
          alarms: state.alarms.map((alarm) =>
            alarm.id === id
              ? { ...alarm, isActive: !alarm.isActive, updatedAt: new Date() }
              : alarm
          ),
        }));
      },
      
      getActiveAlarms: () => {
        return get().alarms.filter((alarm) => alarm.isActive);
      },
      
      getNextAlarm: () => {
        const activeAlarms = get().alarms.filter((alarm) => alarm.isActive);
        if (activeAlarms.length === 0) return null;
        
        const now = new Date();
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        
        // Find the earliest upcoming alarm
        const upcomingAlarms = activeAlarms
          .map(alarm => {
            const alarmTime = new Date(alarm.time);
            let nextAlarmTime = new Date(today);
            nextAlarmTime.setHours(alarmTime.getHours(), alarmTime.getMinutes(), 0, 0);
            
            // If alarm time has passed today, set for tomorrow
            if (nextAlarmTime <= now) {
              nextAlarmTime.setDate(nextAlarmTime.getDate() + 1);
            }
            
            return { ...alarm, nextTime: nextAlarmTime };
          })
          .sort((a, b) => a.nextTime.getTime() - b.nextTime.getTime());
        
        return upcomingAlarms.length > 0 ? upcomingAlarms[0] : null;
      },
    }),
    {
      name: 'semor-alarms',
    }
  )
);