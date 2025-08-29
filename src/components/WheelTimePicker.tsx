import React, { useRef, useEffect, useState, useCallback } from 'react';

interface WheelTimePickerProps {
  value: { hour: number; minute: number; period: 'AM' | 'PM' };
  onChange: (time: { hour: number; minute: number; period: 'AM' | 'PM' }) => void;
}

const WheelTimePicker: React.FC<WheelTimePickerProps> = ({ value, onChange }) => {
  const periodRef = useRef<HTMLDivElement>(null);
  const hourRef = useRef<HTMLDivElement>(null);
  const minuteRef = useRef<HTMLDivElement>(null);

  const [isDragging, setIsDragging] = useState<string | null>(null);
  const [selectedPeriod, setSelectedPeriod] = useState<'AM' | 'PM'>(value.period);
  const [selectedHour, setSelectedHour] = useState<number>(value.hour);
  const [selectedMinute, setSelectedMinute] = useState<number>(value.minute);

  const periods: ('AM' | 'PM')[] = ['AM', 'PM'];
  const hours = Array.from({ length: 12 }, (_, i) => i + 1);
  const minutes = Array.from({ length: 60 }, (_, i) => i); // 0-59분

  const ITEM_HEIGHT = 48; // 각 아이템 높이
  const VISIBLE_ITEMS = 5; // 보이는 아이템 수
  const CONTAINER_HEIGHT = ITEM_HEIGHT * VISIBLE_ITEMS;

  // 값 변경 시 onChange 콜백 호출
  useEffect(() => {
    onChange({ hour: selectedHour, minute: selectedMinute, period: selectedPeriod });
  }, [selectedHour, selectedMinute, selectedPeriod, onChange]);

  // 스크롤을 특정 인덱스로 이동
  const scrollToIndex = useCallback((ref: React.RefObject<HTMLDivElement>, index: number) => {
    if (!ref.current) return;
    
    const scrollTop = index * ITEM_HEIGHT;
    ref.current.scrollTo({
      top: scrollTop,
      behavior: 'smooth'
    });
  }, []);

  // 스크롤 이벤트 핸들러
  const handleScroll = useCallback((
    ref: React.RefObject<HTMLDivElement>, 
    items: any[], 
    setValue: (value: any) => void
  ) => {
    if (!ref.current || isDragging) return;

    const scrollTop = ref.current.scrollTop;
    let index = Math.round(scrollTop / ITEM_HEIGHT);
    index = Math.max(0, Math.min(index, items.length - 1));
    
    setValue(items[index]);
    
    // 정확한 위치로 스냅
    setTimeout(() => {
      if (ref.current) {
        ref.current.scrollTo({
          top: index * ITEM_HEIGHT,
          behavior: 'smooth'
        });
      }
    }, 50);
  }, [isDragging]);

  // 초기 위치 설정
  useEffect(() => {
    const timer = setTimeout(() => {
      if (periodRef.current) {
        scrollToIndex(periodRef, periods.indexOf(selectedPeriod));
      }
      if (hourRef.current) {
        scrollToIndex(hourRef, hours.indexOf(selectedHour));
      }
      if (minuteRef.current) {
        scrollToIndex(minuteRef, selectedMinute);
      }
    }, 100);

    return () => clearTimeout(timer);
  }, []);

  // 터치/마우스 이벤트 핸들러
  const handlePointerDown = (type: string) => {
    setIsDragging(type);
  };

  const handlePointerUp = () => {
    setIsDragging(null);
  };

  useEffect(() => {
    const handleGlobalPointerUp = () => setIsDragging(null);
    document.addEventListener('pointerup', handleGlobalPointerUp);
    document.addEventListener('touchend', handleGlobalPointerUp);
    
    return () => {
      document.removeEventListener('pointerup', handleGlobalPointerUp);
      document.removeEventListener('touchend', handleGlobalPointerUp);
    };
  }, []);

  // 개별 휠 컬럼 렌더링
  const renderWheelColumn = (
    ref: React.RefObject<HTMLDivElement>,
    items: any[],
    selectedValue: any,
    onScroll: () => void,
    formatter?: (item: any) => string,
    type?: string
  ) => (
    <div className="relative flex-1 bg-gray-700 rounded-lg mx-1" style={{ minWidth: '80px' }}>
      <div
        ref={ref}
        className="overflow-y-scroll scrollbar-hide relative"
        style={{ 
          height: `${CONTAINER_HEIGHT}px`,
          scrollSnapType: 'y mandatory'
        }}
        onScroll={onScroll}
        onPointerDown={() => type && handlePointerDown(type)}
        onTouchStart={() => type && handlePointerDown(type)}
      >
        {/* 상단 패딩 */}
        <div style={{ height: `${ITEM_HEIGHT * 2}px` }} />
        
        {items.map((item, index) => (
          <div
            key={index}
            className={`flex items-center justify-center cursor-pointer transition-all duration-200 ${
              item === selectedValue
                ? 'font-bold scale-125'
                : ''
            }`}
            style={{ 
              height: `${ITEM_HEIGHT}px`,
              scrollSnapAlign: 'center',
              fontSize: item === selectedValue ? '50px' : '30px',
              color: item === selectedValue ? '#22D3EE' : '#9CA3AF'
            }}
            onClick={() => {
              if (type === 'period') setSelectedPeriod(item);
              else if (type === 'hour') setSelectedHour(item);
              else if (type === 'minute') setSelectedMinute(item);
              scrollToIndex(ref, index);
            }}
          >
            {formatter ? formatter(item) : item}
          </div>
        ))}
        
        {/* 하단 패딩 */}
        <div style={{ height: `${ITEM_HEIGHT * 2}px` }} />
      </div>
      
      {/* 선택된 아이템 하이라이트 */}
      <div 
        className="absolute left-0 right-0 border-t-2 border-b-2 border-cyan-400 pointer-events-none bg-cyan-400 bg-opacity-30 rounded-md mx-1"
        style={{ 
          top: `${ITEM_HEIGHT * 2}px`,
          height: `${ITEM_HEIGHT}px`
        }}
      />
    </div>
  );

  return (
    <div className="bg-gray-900 rounded-2xl p-6 border border-gray-700">
      <div className="flex bg-gray-800 rounded-xl p-4" style={{ height: `${CONTAINER_HEIGHT}px`, gap: '3rem' }}>
        {/* AM/PM 휠 */}
        {renderWheelColumn(
          periodRef,
          periods,
          selectedPeriod,
          () => handleScroll(periodRef, periods, setSelectedPeriod),
          undefined,
          'period'
        )}
        
        {/* 시간 휠 */}
        {renderWheelColumn(
          hourRef,
          hours,
          selectedHour,
          () => handleScroll(hourRef, hours, setSelectedHour),
          undefined,
          'hour'
        )}
        
        {/* 분 휠 */}
        {renderWheelColumn(
          minuteRef,
          minutes,
          selectedMinute,
          () => handleScroll(minuteRef, minutes, setSelectedMinute),
          (minute) => minute.toString().padStart(2, '0'),
          'minute'
        )}
      </div>
      
      {/* 선택된 시간 표시 */}
      <div className="text-center mt-4">
        <span className="text-gray-400 text-sm">설정 시간 : </span>
        <span className="text-cyan-400 text-lg font-bold">
          {selectedPeriod} {selectedHour}시 {selectedMinute.toString().padStart(2, '0')}분
        </span>
      </div>
    </div>
  );
};

export default WheelTimePicker;