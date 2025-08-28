import React from 'react';

interface DayIndicatorProps {
  repeatDays?: number[];
  isActive?: boolean;
}

const DayIndicator: React.FC<DayIndicatorProps> = ({ repeatDays = [], isActive = true }) => {
  const dayLabels = ['일', '월', '화', '수', '목', '금', '토'];
  
  return (
    <div className="flex justify-between w-full mb-3">
      {dayLabels.map((day, index) => {
        const isDayActive = repeatDays.includes(index);
        const dayClasses = `
          w-6 h-6 flex items-center justify-center text-xs rounded-full
          ${!isActive ? 'text-gray-500' : isDayActive ? 'text-white' : 'text-gray-400'}
        `.trim();
        
        const dayStyle = isDayActive && isActive ? { backgroundColor: '#00BCD4', color: 'white' } : {};
        
        return (
          <span key={index} className={dayClasses} style={dayStyle}>
            {day}
          </span>
        );
      })}
    </div>
  );
};

export default DayIndicator;