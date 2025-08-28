import React from 'react';
import { AlarmList } from '@/components/AlarmList';
import NextAlarmCard from '@/components/NextAlarmCard';
import FloatingActionButton from '@/components/FloatingActionButton';
import BottomNavigation from '@/components/BottomNavigation';
import AddAlarmPage from '@/pages/AddAlarmPage';
import { useAlarmManager } from '@/utils/useAlarmManager';
import { useAlarmStore } from '@/stores/alarmStore';
import { useAppStore } from '@/stores/appStore';

function App() {
  useAlarmManager();
  const { getNextAlarm } = useAlarmStore();
  const { activeTab, setActiveTab, currentPage, setCurrentPage } = useAppStore();
  
  const nextAlarm = getNextAlarm();

  // If we're on the add alarm page, render it
  if (currentPage === 'add-alarm') {
    return <AddAlarmPage />;
  }

  const renderActiveTab = () => {
    switch (activeTab) {
      case 'alarm':
        return (
          <>
            <NextAlarmCard nextAlarm={nextAlarm} />
            <div className="space-y-3">
              <AlarmList />
            </div>
          </>
        );
      case 'sleep':
        return (
          <div className="flex items-center justify-center h-64">
            <p className="text-gray-400">수면 기능 개발 예정</p>
          </div>
        );
      case 'morning':
        return (
          <div className="flex items-center justify-center h-64">
            <p className="text-gray-400">아침 기능 개발 예정</p>
          </div>
        );
      case 'report':
        return (
          <div className="flex items-center justify-center h-64">
            <p className="text-gray-400">리포트 기능 개발 예정</p>
          </div>
        );
      case 'settings':
        return (
          <div className="flex items-center justify-center h-64">
            <p className="text-gray-400">설정 기능 개발 예정</p>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="flex flex-col h-screen text-white font-noto" style={{ backgroundColor: '#121212' }}>
      <div className="container mx-auto px-4 py-6 flex-1 overflow-y-auto max-w-md">
        {/* Header Section */}
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold">알람</h1>
        </div>
        
        {renderActiveTab()}
      </div>
      
      {/* Show FAB only on alarm tab */}
      {activeTab === 'alarm' && (
        <FloatingActionButton onClick={() => setCurrentPage('add-alarm')} />
      )}
      
      <BottomNavigation activeTab={activeTab} onTabChange={setActiveTab} />
    </div>
  );
}

export default App;