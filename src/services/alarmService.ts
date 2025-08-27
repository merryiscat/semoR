import { LocalNotifications } from '@capacitor/local-notifications';
import { Alarm, RepeatType } from '@/types';

export class AlarmService {
  private static instance: AlarmService;

  private constructor() {}

  public static getInstance(): AlarmService {
    if (!AlarmService.instance) {
      AlarmService.instance = new AlarmService();
    }
    return AlarmService.instance;
  }

  public async initializeNotifications(): Promise<void> {
    const permission = await LocalNotifications.requestPermissions();
    if (permission.display === 'granted') {
      console.log('Notification permissions granted');
    }
  }

  public async scheduleAlarm(alarm: Alarm): Promise<void> {
    if (!alarm.isActive) {
      return;
    }

    await this.cancelAlarm(alarm.id);

    const schedule = this.createNotificationSchedule(alarm);
    
    await LocalNotifications.schedule({
      notifications: [{
        title: alarm.title,
        body: alarm.description || '알람이 울렸습니다!',
        id: parseInt(alarm.id.replace(/-/g, '').substring(0, 8), 16),
        schedule,
        sound: 'alarm.wav',
        attachments: [],
        actionTypeId: '',
        extra: {
          alarmId: alarm.id,
          type: alarm.type,
        }
      }]
    });
  }

  private createNotificationSchedule(alarm: Alarm) {
    const alarmTime = new Date(alarm.time);
    
    const schedule: any = {
      at: alarmTime,
    };

    switch (alarm.repeatType) {
      case RepeatType.DAILY:
        schedule.repeats = true;
        schedule.every = 'day';
        break;
        
      case RepeatType.WEEKLY:
        schedule.repeats = true;
        schedule.every = 'week';
        break;
        
      case RepeatType.WEEKDAYS:
        schedule.repeats = true;
        schedule.every = 'day';
        schedule.on = {
          weekday: [2, 3, 4, 5, 6] // Mon-Fri
        };
        break;
        
      case RepeatType.WEEKENDS:
        schedule.repeats = true;
        schedule.every = 'day';
        schedule.on = {
          weekday: [1, 7] // Sat-Sun
        };
        break;
        
      default:
        schedule.repeats = false;
    }

    return schedule;
  }

  public async cancelAlarm(alarmId: string): Promise<void> {
    const numericId = parseInt(alarmId.replace(/-/g, '').substring(0, 8), 16);
    
    await LocalNotifications.cancel({
      notifications: [{
        id: numericId
      }]
    });
  }

  public async scheduleMultipleAlarms(alarms: Alarm[]): Promise<void> {
    for (const alarm of alarms) {
      if (alarm.isActive) {
        await this.scheduleAlarm(alarm);
      }
    }
  }

  public async cancelAllAlarms(): Promise<void> {
    const pending = await LocalNotifications.getPending();
    if (pending.notifications.length > 0) {
      await LocalNotifications.cancel({
        notifications: pending.notifications
      });
    }
  }
}