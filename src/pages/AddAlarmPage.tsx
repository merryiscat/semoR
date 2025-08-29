import React, { useState } from 'react';
import { useAlarmStore } from '@/stores/alarmStore';
import { useAppStore } from '@/stores/appStore';
import { AlarmType, Mission } from '@/types';
import WheelTimePicker from '@/components/WheelTimePicker';

const AddAlarmPage: React.FC = () => {
  const { setCurrentPage } = useAppStore();
  const { addAlarm } = useAlarmStore();

  const [time, setTime] = useState({ hour: 12, minute: 0, period: 'AM' as 'AM' | 'PM' });
  const [selectedDays, setSelectedDays] = useState<number[]>([1, 2, 3, 4]);
  const [isEveryDay, setIsEveryDay] = useState(true);

  /** 저장 버튼 */
  const handleSave = () => {
    let hour24 = time.hour;
    if (time.period === 'PM' && time.hour !== 12) hour24 += 12;
    if (time.period === 'AM' && time.hour === 12) hour24 = 0;

    const alarmTime = new Date();
    alarmTime.setHours(hour24, time.minute, 0, 0);

    const repeatDays = isEveryDay ? [0, 1, 2, 3, 4, 5, 6] : selectedDays;

    addAlarm({
      title: `${time.period} ${time.hour}:${time.minute.toString().padStart(2, '0')}`,
      time: alarmTime,
      isActive: true,
      type: AlarmType.BASIC,
      repeatType: isEveryDay ? 'daily' : 'custom',
      repeatDays,
    });

    setCurrentPage('main');
  };

  /** 요일 토글 */
  const toggleDay = (dayIndex: number) => {
    setSelectedDays(prev =>
      prev.includes(dayIndex) ? prev.filter(d => d !== dayIndex) : [...prev, dayIndex]
    );
  };


  return (
    <div className="text-white h-screen flex flex-col bg-[#121212]">
      <div className="container mx-auto px-4 py-6 max-w-md flex-1 overflow-y-auto">

        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <button onClick={() => setCurrentPage('main')} className="text-gray-400">
            <i className="fas fa-chevron-left text-xl"></i>
          </button>
          <h1 className="text-xl font-bold">알람 설정</h1>
          <div className="w-6"></div>
        </div>

        {/* Time Picker - Wheel Scroll */}
        <div className="mb-8">
          <WheelTimePicker
            value={time}
            onChange={setTime}
          />
        </div>

        {/* Repeat Days */}
        <div className="rounded-2xl p-6 mb-8 bg-[#1E1E1E]">
          <div className="flex justify-between items-center mb-4">
            <h2 className="font-medium">반복</h2>
            <div className="flex items-center">
              <span className="mr-2">매일</span>
              <input
                type="checkbox"
                checked={isEveryDay}
                onChange={(e) => setIsEveryDay(e.target.checked)}
              />
            </div>
          </div>
          <div className="flex justify-between">
            {['일','월','화','수','목','금','토'].map((day, index) => (
              <button
                key={day}
                onClick={() => toggleDay(index)}
                className={`w-10 h-10 rounded-full border flex items-center justify-center font-medium ${
                  selectedDays.includes(index)
                    ? 'text-white'
                    : 'border-gray-500 text-gray-500'
                }`}
                style={selectedDays.includes(index) ? { borderColor: '#00BCD4', color: '#00BCD4' } : {}}
              >
                {day}
              </button>
            ))}
          </div>
        </div>



      </div>

      {/* Save Button */}
      <div 
        className="flex-none p-4 bg-[#121212]"
        style={{ paddingBottom: 'calc(16px + env(safe-area-inset-bottom, 0px))' }}
      >
        <div className="mx-auto w-full max-w-md">
          <button
            onClick={handleSave}
            className="w-full py-4 rounded-2xl font-bold text-white"
            style={{ backgroundColor: '#FF5722' }}
          >
            저장
          </button>
        </div>
      </div>
    </div>
  );
};

export default AddAlarmPage;