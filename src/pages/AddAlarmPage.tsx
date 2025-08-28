import React, { useState } from 'react';
import { useAlarmStore } from '@/stores/alarmStore';
import { useAppStore } from '@/stores/appStore';
import { AlarmType, Mission } from '@/types';

const AddAlarmPage: React.FC = () => {
  const { setCurrentPage } = useAppStore();
  const { addAlarm } = useAlarmStore();

  const [time, setTime] = useState({ hour: 10, minute: 55, period: 'PM' as 'AM' | 'PM' });
  const [selectedDays, setSelectedDays] = useState<number[]>([1, 2, 3, 4]);
  const [isEveryDay, setIsEveryDay] = useState(true);
  const [selectedMissions, setSelectedMissions] = useState<Mission[]>([]);
  const [volume, setVolume] = useState(70);
  const [vibration, setVibration] = useState(true);

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
      description: selectedMissions.length > 0 ? `미션 ${selectedMissions.length}개` : undefined,
      isActive: true,
      type: AlarmType.BASIC,
      repeatType: isEveryDay ? 'daily' : 'custom',
      repeatDays,
      hasMission: selectedMissions.length > 0,
      missionCompleted: false,
      missions: selectedMissions,
      volume,
      vibration,
    });

    setCurrentPage('main');
  };

  /** 요일 토글 */
  const toggleDay = (dayIndex: number) => {
    setSelectedDays(prev =>
      prev.includes(dayIndex) ? prev.filter(d => d !== dayIndex) : [...prev, dayIndex]
    );
  };

  /** 미션 추가 */
  const addMission = () => {
    if (selectedMissions.length < 5) {
      setSelectedMissions([
        ...selectedMissions,
        { id: `m_${Date.now()}`, name: '달리기', icon: 'fas fa-running' },
      ]);
    }
  };

  return (
    <div className="text-white min-h-screen flex flex-col pb-20 bg-[#121212]">
      <div className="container mx-auto px-4 py-6 max-w-md">

        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <button onClick={() => setCurrentPage('main')} className="text-gray-400">
            <i className="fas fa-chevron-left text-xl"></i>
          </button>
          <h1 className="text-xl font-bold">알람 설정</h1>
          <div className="w-6"></div>
        </div>

        {/* Time Picker - Vertical Scroll */}
        <div className="mb-12">
          <div className="flex justify-center items-center h-48 gap-6">
            {/* AM/PM Picker */}
            <div className="flex flex-col h-full overflow-hidden relative">
              <div 
                className="flex-1 overflow-y-auto" 
                style={{ 
                  scrollSnapType: 'y mandatory',
                  msOverflowStyle: 'none',
                  scrollbarWidth: 'none'
                }}
              >
                <div className="h-16"></div> {/* spacer */}
                {['AM', 'PM'].map((period, index) => (
                  <div
                    key={period}
                    className={`h-16 flex items-center justify-center cursor-pointer transition-all duration-200 ${
                      time.period === period 
                        ? 'text-white text-2xl font-bold' 
                        : 'text-gray-400 text-lg'
                    }`}
                    style={{ scrollSnapAlign: 'center' }}
                    onClick={() => setTime({ ...time, period: period as 'AM' | 'PM' })}
                  >
                    {period}
                  </div>
                ))}
                <div className="h-16"></div> {/* spacer */}
              </div>
            </div>

            {/* Hour Picker */}
            <div className="flex flex-col h-full overflow-hidden relative">
              <div 
                className="flex-1 overflow-y-auto" 
                style={{ 
                  scrollSnapType: 'y mandatory',
                  msOverflowStyle: 'none',
                  scrollbarWidth: 'none'
                }}
              >
                <div className="h-16"></div> {/* spacer */}
                {Array.from({ length: 12 }, (_, i) => i + 1).map((hour) => (
                  <div
                    key={hour}
                    className={`h-16 flex items-center justify-center cursor-pointer transition-all duration-200 ${
                      time.hour === hour 
                        ? 'text-white text-2xl font-bold' 
                        : 'text-gray-400 text-lg'
                    }`}
                    style={{ scrollSnapAlign: 'center' }}
                    onClick={() => setTime({ ...time, hour })}
                  >
                    {hour}
                  </div>
                ))}
                <div className="h-16"></div> {/* spacer */}
              </div>
            </div>

            {/* Minute Picker */}
            <div className="flex flex-col h-full overflow-hidden relative">
              <div 
                className="flex-1 overflow-y-auto" 
                style={{ 
                  scrollSnapType: 'y mandatory',
                  msOverflowStyle: 'none',
                  scrollbarWidth: 'none'
                }}
              >
                <div className="h-16"></div> {/* spacer */}
                {Array.from({ length: 12 }, (_, i) => i * 5).map((minute) => (
                  <div
                    key={minute}
                    className={`h-16 flex items-center justify-center cursor-pointer transition-all duration-200 ${
                      time.minute === minute 
                        ? 'text-white text-2xl font-bold' 
                        : 'text-gray-400 text-lg'
                    }`}
                    style={{ scrollSnapAlign: 'center' }}
                    onClick={() => setTime({ ...time, minute })}
                  >
                    {minute.toString().padStart(2, '0')}
                  </div>
                ))}
                <div className="h-16"></div> {/* spacer */}
              </div>
            </div>
          </div>
          
          {/* Selected time display */}
          <div className="text-center mt-4 text-gray-400">
            선택된 시간: <span className="text-white font-semibold">
              {time.period} {time.hour}:{time.minute.toString().padStart(2, '0')}
            </span>
          </div>
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

        {/* Missions */}
        <div className="mb-8">
          <h2 className="font-medium mb-4">미션 <span className="text-gray-400">({selectedMissions.length}/5)</span></h2>
          <div className="flex flex-wrap gap-3">
            {selectedMissions.map(m => (
              <div key={m.id} className="w-16 h-16 rounded-xl flex items-center justify-center text-white" style={{ backgroundColor: '#00BCD4' }}>
                <i className={m.icon}></i>
              </div>
            ))}
            {Array.from({ length: 5 - selectedMissions.length }).map((_, i) => (
              <div
                key={i}
                onClick={addMission}
                className="w-16 h-16 border-2 border-dashed border-gray-500 rounded-xl flex items-center justify-center cursor-pointer"
              >
                <i className="fas fa-plus text-gray-500 text-xl"></i>
              </div>
            ))}
          </div>
        </div>

        {/* Volume & Vibration */}
        <div className="mb-8">
          <div className="flex items-center mb-4">
            <i className="fas fa-volume-off text-gray-500 mr-2"></i>
            <input
              type="range"
              min="0"
              max="100"
              value={volume}
              onChange={(e) => setVolume(Number(e.target.value))}
              className="w-full mx-2"
              style={{
                background: `linear-gradient(to right, #00BCD4 0%, #00BCD4 ${volume}%, #6B7280 ${volume}%, #6B7280 100%)`
              }}
            />
            <i className="fas fa-volume-up text-gray-500 ml-2"></i>
          </div>
          <div className="flex justify-between items-center">
            <span>진동</span>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                className="sr-only peer"
                checked={vibration}
                onChange={(e) => setVibration(e.target.checked)}
              />
              <div className="w-11 h-6 bg-gray-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-cyan" style={vibration ? { backgroundColor: '#00BCD4' } : {}}></div>
            </label>
          </div>
        </div>

        {/* Sound Settings */}
        <div className="mb-6">
          <div className="rounded-xl bg-[#1E1E1E] divide-y divide-gray-700">
            <div className="flex justify-between items-center py-4 px-4">
              <span>사운드</span>
              <div className="flex items-center text-gray-400">
                <span className="mr-2">오르카니</span>
                <i className="fas fa-chevron-right"></i>
              </div>
            </div>
            <div className="flex justify-between items-center py-4 px-4">
              <span>사운드 파워업</span>
              <div className="flex items-center text-gray-400">
                <span className="mr-2">17개 사용</span>
                <i className="fas fa-chevron-right"></i>
              </div>
            </div>
            <div className="flex justify-between items-center py-4 px-4">
              <span>알람 미루기</span>
              <div className="flex items-center text-gray-400">
                <span className="mr-2">5분, 3회</span>
                <i className="fas fa-chevron-right"></i>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Save Button */}
      <div className="fixed bottom-0 left-0 right-0 p-4 bg-[#121212]">
        <button
          onClick={handleSave}
          className="w-full py-4 rounded-2xl font-bold text-white"
          style={{ backgroundColor: '#FF5722' }}
        >
          저장
        </button>
      </div>
    </div>
  );
};

export default AddAlarmPage;