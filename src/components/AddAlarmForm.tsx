import React, { useState } from 'react';
import { useAlarmStore } from '@/stores/alarmStore';
import { AlarmType, RepeatType } from '@/types';
import { Plus } from 'lucide-react';

export function AddAlarmForm() {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [time, setTime] = useState('');
  const [type, setType] = useState<AlarmType>(AlarmType.BASIC);
  const [repeatType, setRepeatType] = useState<RepeatType>(RepeatType.ONCE);
  
  const addAlarm = useAlarmStore((state) => state.addAlarm);
  
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!title.trim() || !time) {
      alert('제목과 시간을 입력해주세요.');
      return;
    }
    
    const [hours, minutes] = time.split(':').map(Number);
    const alarmTime = new Date();
    alarmTime.setHours(hours, minutes, 0, 0);
    
    addAlarm({
      title: title.trim(),
      description: description.trim() || undefined,
      time: alarmTime,
      isActive: true,
      type,
      repeatType,
    });
    
    setTitle('');
    setDescription('');
    setTime('');
    setType(AlarmType.BASIC);
    setRepeatType(RepeatType.ONCE);
  };
  
  return (
    <div className="card">
      <h2 style={{ marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <Plus size={24} />
        새 알람 추가
      </h2>
      
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="title">알람 제목</label>
          <input
            type="text"
            id="title"
            className="form-control"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="알람 제목을 입력하세요"
          />
        </div>
        
        <div className="form-group">
          <label htmlFor="description">설명 (선택사항)</label>
          <input
            type="text"
            id="description"
            className="form-control"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="알람 설명을 입력하세요"
          />
        </div>
        
        <div className="form-group">
          <label htmlFor="time">시간</label>
          <input
            type="time"
            id="time"
            className="form-control"
            value={time}
            onChange={(e) => setTime(e.target.value)}
          />
        </div>
        
        <div className="form-group">
          <label htmlFor="type">알람 유형</label>
          <select
            id="type"
            className="form-control"
            value={type}
            onChange={(e) => setType(e.target.value as AlarmType)}
          >
            <option value={AlarmType.BASIC}>기본 알람</option>
            <option value={AlarmType.REMINDER}>리마인더</option>
            <option value={AlarmType.MEDICATION}>복약 알림</option>
            <option value={AlarmType.WORKOUT}>운동 알림</option>
            <option value={AlarmType.WORK}>업무 알림</option>
            <option value={AlarmType.CUSTOM}>사용자 정의</option>
          </select>
        </div>
        
        <div className="form-group">
          <label htmlFor="repeat">반복 설정</label>
          <select
            id="repeat"
            className="form-control"
            value={repeatType}
            onChange={(e) => setRepeatType(e.target.value as RepeatType)}
          >
            <option value={RepeatType.ONCE}>한 번만</option>
            <option value={RepeatType.DAILY}>매일</option>
            <option value={RepeatType.WEEKLY}>매주</option>
            <option value={RepeatType.WEEKDAYS}>평일만</option>
            <option value={RepeatType.WEEKENDS}>주말만</option>
          </select>
        </div>
        
        <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
          알람 추가
        </button>
      </form>
    </div>
  );
}