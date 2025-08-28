import React from 'react';

interface Mission {
  id: string;
  name: string;
  icon: string;
}

interface MissionSelectorProps {
  selectedMissions: Mission[];
  onChange: (missions: Mission[]) => void;
}

const MissionSelector: React.FC<MissionSelectorProps> = ({ selectedMissions, onChange }) => {
  const availableMissions: Mission[] = [
    { id: 'workout', name: '운동', icon: 'fas fa-dumbbell' },
    { id: 'math', name: '계산', icon: 'fas fa-calculator' },
    { id: 'run', name: '달리기', icon: 'fas fa-running' },
    { id: 'scan', name: 'QR스캔', icon: 'fas fa-qrcode' },
    { id: 'shake', name: '흔들기', icon: 'fas fa-mobile-alt' },
  ];

  const addMission = () => {
    if (selectedMissions.length >= 5) return;
    
    const unusedMissions = availableMissions.filter(
      mission => !selectedMissions.find(selected => selected.id === mission.id)
    );
    
    if (unusedMissions.length > 0) {
      const newMission = unusedMissions[0];
      onChange([...selectedMissions, newMission]);
    }
  };

  const removeMission = (missionId: string) => {
    onChange(selectedMissions.filter(mission => mission.id !== missionId));
  };

  const renderMissionSlot = (index: number) => {
    const mission = selectedMissions[index];
    
    if (mission) {
      return (
        <div 
          key={`filled-${index}`}
          className="w-16 h-16 bg-accent-cyan bg-opacity-20 rounded-xl flex items-center justify-center cursor-pointer hover:bg-opacity-30 transition-colors"
          onClick={() => removeMission(mission.id)}
        >
          <i className={`${mission.icon} text-accent-cyan text-2xl`}></i>
        </div>
      );
    }
    
    return (
      <div 
        key={`empty-${index}`}
        className="w-16 h-16 border-2 border-dashed border-gray-500 rounded-xl flex items-center justify-center cursor-pointer hover:border-gray-400 transition-colors"
        onClick={addMission}
      >
        <i className="fas fa-plus text-gray-500 text-xl"></i>
      </div>
    );
  };

  return (
    <div className="bg-gray-800 rounded-2xl p-6">
      <h2 className="font-medium mb-4">
        미션 <span className="text-gray-400">({selectedMissions.length}/5)</span>
      </h2>
      
      <div className="flex flex-wrap gap-3">
        {Array.from({ length: 5 }, (_, index) => renderMissionSlot(index))}
      </div>
    </div>
  );
};

export default MissionSelector;