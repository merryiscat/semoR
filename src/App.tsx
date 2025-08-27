import React from 'react';
import { AlarmList } from '@/components/AlarmList';
import { AddAlarmForm } from '@/components/AddAlarmForm';
import { Header } from '@/components/Header';
import { useAlarmManager } from '@/utils/useAlarmManager';
import './App.css';

function App() {
  useAlarmManager();
  
  return (
    <div className="app">
      <Header />
      <main className="main-content">
        <AddAlarmForm />
        <AlarmList />
      </main>
    </div>
  );
}

export default App;