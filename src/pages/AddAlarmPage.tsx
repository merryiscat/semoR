import React, { useState, useRef, useEffect } from 'react';
import { useAlarmStore } from '@/stores/alarmStore';
import { useAppStore } from '@/stores/appStore';
import { AlarmType, Mission } from '@/types';

const AddAlarmPage: React.FC = () => {
  const { setCurrentPage } = useAppStore();
  const { addAlarm } = useAlarmStore();
  
  const [time, setTime] = useState({ hour: 8, minute: 35, period: 'PM' as 'AM' | 'PM' });
  const [selectedDays, setSelectedDays] = useState<number[]>([1, 2, 3, 4]); // Mon-Thu
  const [isEveryDay, setIsEveryDay] = useState(true);
  const [selectedMissions, setSelectedMissions] = useState<Mission[]>([
    { id: 'workout', name: '운동', icon: 'fas fa-dumbbell' },
    { id: 'math', name: '계산', icon: 'fas fa-calculator' },
  ]);
  const [volume, setVolume] = useState(70);
  const [vibration, setVibration] = useState(true);
  const [soundName] = useState('오르카니');
  const [soundPowerUp] = useState('17개 사용');
  const [snoozeSettings] = useState('5분, 3회');
  
  const periodRef = useRef<HTMLDivElement>(null);
  const hourRef = useRef<HTMLDivElement>(null);
  const minuteRef = useRef<HTMLDivElement>(null);

  const handleSave = () => {
    // Convert 12-hour format to 24-hour format
    let hour24 = time.hour;
    if (time.period === 'PM' && time.hour !== 12) {
      hour24 += 12;
    } else if (time.period === 'AM' && time.hour === 12) {
      hour24 = 0;
    }

    const alarmTime = new Date();
    alarmTime.setHours(hour24, time.minute, 0, 0);

    // Determine repeat days based on selection
    const repeatDays = isEveryDay ? [0, 1, 2, 3, 4, 5, 6] : selectedDays;

    addAlarm({
      title: `${time.period} ${time.hour}:${time.minute.toString().padStart(2, '0')}`,
      description: selectedMissions.length > 0 ? `미션 ${selectedMissions.length}개` : undefined,
      time: alarmTime,
      isActive: true,
      type: AlarmType.BASIC,
      repeatType: isEveryDay ? 'daily' : selectedDays.length === 5 && selectedDays.every(d => [1,2,3,4,5].includes(d)) ? 'weekdays' : 'custom',
      repeatDays,
      hasMission: selectedMissions.length > 0,
      missionCompleted: false,
      missions: selectedMissions,
      volume,
      vibration,
    });

    setCurrentPage('main');
  };

  const toggleDay = (dayIndex: number) => {
    if (selectedDays.includes(dayIndex)) {
      setSelectedDays(selectedDays.filter(d => d !== dayIndex));
    } else {
      setSelectedDays([...selectedDays, dayIndex]);
    }
  };

  const addMission = (index: number) => {
    if (selectedMissions.length < 5) {
      const newMission = { id: `mission_${Date.now()}`, name: '달리기', icon: 'fas fa-running' };
      setSelectedMissions([...selectedMissions, newMission]);
    }
  };

  return (
    <div className="text-white min-h-screen flex flex-col pb-20" style={{ backgroundColor: '#121212' }}>
      <div className="container mx-auto px-4 py-6 max-w-md">
        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <button 
            onClick={() => setCurrentPage('main')}
            className="text-gray-400"
          >
            <i className="fas fa-chevron-left text-xl"></i>
          </button>
          <h1 className="text-xl font-bold">알람 설정</h1>
          <div className="w-6"></div>
        </div>
        
        {/* Time Picker */}
        <div className="time-picker-container rounded-2xl p-6 mb-6 card-shadow card-hover" style={{ backgroundColor: '#1E1E1E' }}>
          <div className="flex justify-center items-center h-40 relative overflow-hidden">
            {/* AM/PM Column */}
            <div ref={periodRef} className="h-full overflow-y-scroll snap-y snap-mandatory w-16 text-center">
              <div className="h-20 flex items-center justify-center opacity-50 text-sm">AM</div>
              <div className="h-20 flex items-center justify-center font-bold scale-110">PM</div>
            </div>
            
            {/* Hour Column */}
            <div ref={hourRef} className="h-full overflow-y-scroll snap-y snap-mandatory w-16 text-center mx-4">
              {[1,2,3,4,5,6,7,8,9,10,11,12].map((hour) => (
                <div key={hour} className={`h-20 flex items-center justify-center ${hour === 8 ? 'font-bold scale-110' : 'opacity-50 text-sm'}`}>
                  {hour}
                </div>
              ))}
            </div>
            
            {/* Minute Column */}
            <div ref={minuteRef} className="h-full overflow-y-scroll snap-y snap-mandatory w-16 text-center">
              {['00','05','10','15','20','25','30','35','40','45','50','55'].map((minute) => (
                <div key={minute} className={`h-20 flex items-center justify-center ${minute === '35' ? 'font-bold scale-110' : 'opacity-50 text-sm'}`}>
                  {minute}
                </div>
              ))}
            </div>
            
            {/* Center highlight line */}
            <div className="center-highlight absolute left-0 right-0 top-1/2 h-1 opacity-30 -translate-y-1/2"></div>
          </div>
        </div>
        
        {/* Repeat Section */}
        <div className="rounded-2xl p-6 mb-6 card-shadow card-hover" style={{ backgroundColor: '#1E1E1E' }}>
          <div className="flex justify-between items-center mb-4">
            <h2 className="font-medium">반복</h2>
            <div className="flex items-center">
              <span className="mr-2">매일</span>
              <label className="relative inline-flex items-center cursor-pointer">
                <input 
                  type="checkbox" 
                  className="sr-only peer" 
                  checked={isEveryDay}
                  onChange={(e) => setIsEveryDay(e.target.checked)}
                />
                <div className="w-11 h-6 bg-gray-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-cyan" style={{ '--tw-bg-opacity': 1 } as any}></div>
              </label>
            </div>
          </div>
          
          <div className="flex justify-between">
            {['일','월','화','수','목','금','토'].map((day, index) => (
              <button 
                key={day}
                onClick={() => toggleDay(index)}
                className={`day-button btn-press w-10 h-10 rounded-full border flex items-center justify-center font-medium ${
                  selectedDays.includes(index) 
                    ? 'selected border-cyan text-cyan' 
                    : 'border-gray-500 text-gray-500'
                }`}
                style={selectedDays.includes(index) ? { borderColor: '#00BCD4', color: '#00BCD4' } : {}}
              >
                {day}
              </button>
            ))}
          </div>
        </div>
        
        {/* Mission Section */}
        <div className="rounded-2xl p-6 mb-6 card-shadow card-hover" style={{ backgroundColor: '#1E1E1E' }}>
          <h2 className="font-medium mb-4">미션 <span className="text-gray-400">({selectedMissions.length}/5)</span></h2>
          
          <div className="flex flex-wrap gap-3">
            {/* Filled mission slots */}
            {selectedMissions.slice(0, 5).map((mission, index) => (
              <div key={mission.id} className="mission-slot filled w-16 h-16 rounded-xl flex items-center justify-center" style={{ backgroundColor: 'rgba(0, 188, 212, 0.2)' }}>
                <i className={`${mission.icon} text-2xl`} style={{ color: '#00BCD4' }}></i>
              </div>
            ))}
            
            {/* Empty mission slots */}
            {Array.from({ length: 5 - selectedMissions.length }).map((_, index) => (
              <div 
                key={`empty-${index}`}
                onClick={() => addMission(index)}
                className="mission-slot empty w-16 h-16 border-2 border-dashed border-gray-500 rounded-xl flex items-center justify-center cursor-pointer btn-press"
              >
                <i className="fas fa-plus text-gray-500 text-xl transition-colors"></i>
              </div>
            ))}
          </div>
        </div>
        
        {/* Volume & Vibration Section */}
        <div className="rounded-2xl p-6 mb-6 card-shadow card-hover" style={{ backgroundColor: '#1E1E1E' }}>
          <h2 className="font-medium mb-4">볼륨</h2>
          
          <div className="flex items-center mb-6">
            <i className="fas fa-volume-off text-gray-500 mr-2"></i>
            <input 
              type="range" 
              min="0" 
              max="100" 
              value={volume} 
              onChange={(e) => setVolume(parseInt(e.target.value))}
              className="w-full mx-2"
              style={{
                background: `linear-gradient(to right, #00BCD4 0%, #00BCD4 ${volume}%, #6B7280 ${volume}%, #6B7280 100%)`
              }}
            />
            <i className="fas fa-volume-up text-gray-500 ml-2"></i>
          </div>
          
          <div className="flex justify-between items-center">
            <div className="flex items-center">
              <i className="fas fa-mobile-alt text-gray-500 mr-2"></i>
              <span>진동</span>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input 
                type="checkbox" 
                className="sr-only peer" 
                checked={vibration}
                onChange={(e) => setVibration(e.target.checked)}
              />
              <div className="toggle-switch-enhanced w-11 h-6 bg-gray-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-cyan" style={vibration ? { backgroundColor: '#00BCD4', boxShadow: '0 2px 8px rgba(0, 188, 212, 0.4)' } : {}}></div>
            </label>
          </div>
        </div>
        
        {/* Sound Section */}
        <div className="rounded-2xl p-6 mb-6 card-shadow card-hover" style={{ backgroundColor: '#1E1E1E' }}>
          <div className="flex justify-between items-center py-4 border-b border-gray-700 btn-press cursor-pointer rounded-lg px-2 hover:bg-gray-800 hover:bg-opacity-30 transition-all">
            <span className="font-medium">사운드</span>
            <div className="flex items-center">
              <span className="text-gray-400 mr-2">오르카니</span>
              <i className="fas fa-chevron-right text-gray-500"></i>
            </div>
          </div>
          
          <div className="flex justify-between items-center py-4 border-b border-gray-700 btn-press cursor-pointer rounded-lg px-2 hover:bg-gray-800 hover:bg-opacity-30 transition-all">
            <span className="font-medium">사운드 파워업</span>
            <div className="flex items-center">
              <span className="text-gray-400 mr-2">17개 사용</span>
              <i className="fas fa-chevron-right text-gray-500"></i>
            </div>
          </div>
          
          <div className="flex justify-between items-center py-4 btn-press cursor-pointer rounded-lg px-2 hover:bg-gray-800 hover:bg-opacity-30 transition-all">
            <span className="font-medium">알람 미루기</span>
            <div className="flex items-center">
              <span className="text-gray-400 mr-2">5분, 3회</span>
              <i className="fas fa-chevron-right text-gray-500"></i>
            </div>
          </div>
        </div>
      </div>
      
      {/* Save Button */}
      <div className="fixed bottom-0 left-0 right-0 p-4 border-t border-gray-800" style={{ backgroundColor: '#121212' }}>
        <button 
          onClick={handleSave}
          className="save-button w-full py-4 rounded-xl font-bold text-white"
        >
          저장
        </button>
      </div>
    </div>
  );
};

export default AddAlarmPage;