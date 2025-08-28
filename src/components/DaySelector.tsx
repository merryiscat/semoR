import React from 'react';

interface DaySelectorProps {
  selectedDays: number[];
  onChange: (days: number[]) => void;
  isEveryDay: boolean;
  onEveryDayChange: (enabled: boolean) => void;
}

const DaySelector: React.FC<DaySelectorProps> = ({ 
  selectedDays, 
  onChange, 
  isEveryDay, 
  onEveryDayChange 
}) => {
  const dayLabels = ['일', '월', '화', '수', '목', '금', '토'];

  const toggleDay = (dayIndex: number) => {
    if (isEveryDay) return; // Disable individual selection when every day is enabled
    
    const newSelectedDays = selectedDays.includes(dayIndex)
      ? selectedDays.filter(day => day !== dayIndex)
      : [...selectedDays, dayIndex];
    
    onChange(newSelectedDays);
  };

  const handleEveryDayToggle = (checked: boolean) => {
    onEveryDayChange(checked);
    if (checked) {
      onChange([0, 1, 2, 3, 4, 5, 6]); // Select all days
    }
  };

  const isDaySelected = (dayIndex: number) => {
    return isEveryDay || selectedDays.includes(dayIndex);
  };

  return (
    <div className="bg-gray-800 rounded-2xl p-6">
      <div className="flex justify-between items-center mb-4">
        <h2 className="font-medium">반복</h2>
        <div className="flex items-center">
          <span className="mr-2">매일</span>
          <label className="relative inline-flex items-center cursor-pointer">
            <input 
              type="checkbox" 
              className="sr-only peer" 
              checked={isEveryDay}
              onChange={(e) => handleEveryDayToggle(e.target.checked)}
            />
            <div className="w-11 h-6 bg-gray-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-accent-cyan"></div>
          </label>
        </div>
      </div>
      
      <div className="flex justify-between">
        {dayLabels.map((day, index) => {
          const selected = isDaySelected(index);
          return (
            <button
              key={index}
              onClick={() => toggleDay(index)}
              className={`w-10 h-10 rounded-full border flex items-center justify-center transition-colors ${
                selected
                  ? 'border-accent-cyan text-accent-cyan'
                  : 'border-gray-500 text-gray-500'
              } ${isEveryDay ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
              disabled={isEveryDay}
            >
              {day}
            </button>
          );
        })}
      </div>
    </div>
  );
};

export default DaySelector;