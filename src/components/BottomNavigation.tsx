import React from 'react';
import { NavTab } from '@/types';

interface BottomNavigationProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
}

const BottomNavigation: React.FC<BottomNavigationProps> = ({ activeTab, onTabChange }) => {
  const navItems = [
    { id: 'alarm' as NavTab, icon: 'fas fa-clock', label: '알람' },
    { id: 'sleep' as NavTab, icon: 'fas fa-moon', label: '수면' },
    { id: 'morning' as NavTab, icon: 'fas fa-sun', label: '아침' },
    { id: 'report' as NavTab, icon: 'fas fa-file-lines', label: '리포트' },
    { id: 'settings' as NavTab, icon: 'fas fa-gear', label: '설정' },
  ];

  return (
    <div className="py-3 px-6 flex justify-around items-center" style={{ backgroundColor: '#1E1E1E' }}>
      {navItems.map((item) => (
        <button
          key={item.id}
          onClick={() => onTabChange(item.id)}
          className="flex flex-col items-center"
          style={{ color: activeTab === item.id ? '#00BCD4' : '#9CA3AF' }}
        >
          <i className={`${item.icon} mb-1`}></i>
          <span className="text-xs">{item.label}</span>
        </button>
      ))}
    </div>
  );
};

export default BottomNavigation;