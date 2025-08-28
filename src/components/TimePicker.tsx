import React, { useEffect, useRef, useState } from 'react';

interface TimePickerProps {
  value: { hour: number; minute: number; period: 'AM' | 'PM' };
  onChange: (time: { hour: number; minute: number; period: 'AM' | 'PM' }) => void;
}

const TimePicker: React.FC<TimePickerProps> = ({ value, onChange }) => {
  const [selectedPeriod, setSelectedPeriod] = useState(value.period);
  const [selectedHour, setSelectedHour] = useState(value.hour);
  const [selectedMinute, setSelectedMinute] = useState(value.minute);

  const periodRef = useRef<HTMLDivElement>(null);
  const hourRef = useRef<HTMLDivElement>(null);
  const minuteRef = useRef<HTMLDivElement>(null);

  const periods = ['AM', 'PM'];
  const hours = Array.from({ length: 12 }, (_, i) => i + 1);
  const minutes = Array.from({ length: 12 }, (_, i) => i * 5);

  useEffect(() => {
    onChange({ hour: selectedHour, minute: selectedMinute, period: selectedPeriod });
  }, [selectedHour, selectedMinute, selectedPeriod, onChange]);

  const handleScroll = (ref: React.RefObject<HTMLDivElement>, items: any[], setValue: (value: any) => void) => {
    if (!ref.current) return;
    
    const container = ref.current;
    const itemHeight = 80; // h-20 = 5rem = 80px
    const scrollTop = container.scrollTop;
    const selectedIndex = Math.round(scrollTop / itemHeight);
    const clampedIndex = Math.max(0, Math.min(selectedIndex, items.length - 1));
    
    setValue(items[clampedIndex]);
  };

  const scrollToValue = (ref: React.RefObject<HTMLDivElement>, items: any[], value: any) => {
    if (!ref.current) return;
    
    const index = items.indexOf(value);
    if (index >= 0) {
      const itemHeight = 80;
      ref.current.scrollTo({
        top: index * itemHeight,
        behavior: 'smooth'
      });
    }
  };

  useEffect(() => {
    // Initial scroll positions
    const timeoutId = setTimeout(() => {
      scrollToValue(periodRef, periods, selectedPeriod);
      scrollToValue(hourRef, hours, selectedHour);
      scrollToValue(minuteRef, minutes, selectedMinute);
    }, 100);

    return () => clearTimeout(timeoutId);
  }, []);

  const renderTimeColumn = (
    ref: React.RefObject<HTMLDivElement>,
    items: any[],
    selectedValue: any,
    onScroll: () => void,
    formatter?: (item: any) => string
  ) => (
    <div 
      ref={ref}
      className="time-picker h-full overflow-y-scroll snap-y snap-mandatory w-16 text-center scrollbar-hide"
      onScroll={onScroll}
      style={{ scrollSnapType: 'y mandatory' }}
    >
      {items.map((item, index) => (
        <div 
          key={index}
          className={`h-20 flex items-center justify-center time-item transition-all duration-200 ${
            item === selectedValue 
              ? 'selected font-bold text-white scale-120' 
              : 'unselected opacity-50 text-gray-400 scale-80'
          }`}
          style={{ scrollSnapAlign: 'center' }}
        >
          {formatter ? formatter(item) : item}
        </div>
      ))}
    </div>
  );

  return (
    <div className="bg-gray-800 rounded-2xl p-6">
      <div className="flex justify-center items-center h-40 relative overflow-hidden">
        {/* AM/PM Column */}
        {renderTimeColumn(
          periodRef,
          periods,
          selectedPeriod,
          () => handleScroll(periodRef, periods, setSelectedPeriod)
        )}
        
        {/* Hour Column */}
        {renderTimeColumn(
          hourRef,
          hours,
          selectedHour,
          () => handleScroll(hourRef, hours, setSelectedHour)
        )}
        
        {/* Minute Column */}
        {renderTimeColumn(
          minuteRef,
          minutes,
          selectedMinute,
          () => handleScroll(minuteRef, minutes, setSelectedMinute),
          (minute) => minute.toString().padStart(2, '0')
        )}
        
        {/* Center highlight line */}
        <div className="absolute left-0 right-0 top-1/2 h-1 bg-accent-cyan opacity-20 -translate-y-1/2 pointer-events-none"></div>
      </div>
    </div>
  );
};

export default TimePicker;