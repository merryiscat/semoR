import React from 'react';

interface SoundSettingsProps {
  soundName: string;
  soundPowerUp: string;
  snoozeSettings: string;
  onSoundChange: () => void;
  onSoundPowerUpChange: () => void;
  onSnoozeChange: () => void;
}

const SoundSettings: React.FC<SoundSettingsProps> = ({
  soundName,
  soundPowerUp,
  snoozeSettings,
  onSoundChange,
  onSoundPowerUpChange,
  onSnoozeChange
}) => {
  return (
    <div className="bg-gray-800 rounded-2xl p-6">
      <div className="flex justify-between items-center py-3 border-b border-gray-700">
        <span>사운드</span>
        <button 
          onClick={onSoundChange}
          className="flex items-center hover:text-accent-cyan transition-colors"
        >
          <span className="text-gray-400 mr-2">{soundName}</span>
          <i className="fas fa-chevron-right text-gray-500"></i>
        </button>
      </div>
      
      <div className="flex justify-between items-center py-3 border-b border-gray-700">
        <span>사운드 파워업</span>
        <button 
          onClick={onSoundPowerUpChange}
          className="flex items-center hover:text-accent-cyan transition-colors"
        >
          <span className="text-gray-400 mr-2">{soundPowerUp}</span>
          <i className="fas fa-chevron-right text-gray-500"></i>
        </button>
      </div>
      
      <div className="flex justify-between items-center py-3">
        <span>알람 미루기</span>
        <button 
          onClick={onSnoozeChange}
          className="flex items-center hover:text-accent-cyan transition-colors"
        >
          <span className="text-gray-400 mr-2">{snoozeSettings}</span>
          <i className="fas fa-chevron-right text-gray-500"></i>
        </button>
      </div>
    </div>
  );
};

export default SoundSettings;