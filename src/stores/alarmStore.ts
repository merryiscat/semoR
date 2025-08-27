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
    }),
    {
      name: 'semor-alarms',
    }
  )
);