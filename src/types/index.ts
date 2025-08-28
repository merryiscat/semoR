export interface Alarm {
  id: string;
  title: string;
  description?: string;
  time: Date;
  isActive: boolean;
  type: AlarmType;
  repeatType?: RepeatType;
  repeatDays?: number[];
  soundUrl?: string;
  hasMission?: boolean;
  missionCompleted?: boolean;
  missions?: Mission[];
  volume?: number;
  vibration?: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export enum AlarmType {
  BASIC = 'basic',
  REMINDER = 'reminder',
  MEDICATION = 'medication',
  WORKOUT = 'workout',
  WORK = 'work',
  CUSTOM = 'custom'
}

export enum RepeatType {
  ONCE = 'once',
  DAILY = 'daily',
  WEEKLY = 'weekly',
  WEEKDAYS = 'weekdays',
  WEEKENDS = 'weekends',
  CUSTOM = 'custom'
}

export interface AlarmStore {
  alarms: Alarm[];
  addAlarm: (alarm: Omit<Alarm, 'id' | 'createdAt' | 'updatedAt'>) => void;
  updateAlarm: (id: string, updates: Partial<Alarm>) => void;
  deleteAlarm: (id: string) => void;
  toggleAlarm: (id: string) => void;
  getActiveAlarms: () => Alarm[];
  getNextAlarm: () => Alarm | null;
}

export type NavTab = 'alarm' | 'sleep' | 'morning' | 'report' | 'settings';

export interface AppState {
  activeTab: NavTab;
  setActiveTab: (tab: NavTab) => void;
  currentPage: 'main' | 'add-alarm';
  setCurrentPage: (page: 'main' | 'add-alarm') => void;
}

export interface Mission {
  id: string;
  name: string;
  icon: string;
}