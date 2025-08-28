import React from 'react';

interface VolumeControlProps {
  volume: number;
  vibration: boolean;
  onVolumeChange: (volume: number) => void;
  onVibrationChange: (enabled: boolean) => void;
}

const VolumeControl: React.FC<VolumeControlProps> = ({
  volume,
  vibration,
  onVolumeChange,
  onVibrationChange
}) => {
  return (
    <div className="bg-gray-800 rounded-2xl p-6">
      <h2 className="font-medium mb-4">볼륨</h2>
      
      <div className="flex items-center mb-6">
        <i className="fas fa-volume-off text-gray-500 mr-2"></i>
        <input 
          type="range" 
          min="0" 
          max="100" 
          value={volume}
          onChange={(e) => onVolumeChange(parseInt(e.target.value))}
          className="w-full mx-2 h-2 bg-gray-600 rounded-lg appearance-none cursor-pointer"
          style={{
            background: `linear-gradient(to right, #00BCD4 0%, #00BCD4 ${volume}%, #4B5563 ${volume}%, #4B5563 100%)`
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
            onChange={(e) => onVibrationChange(e.target.checked)}
          />
          <div className="w-11 h-6 bg-gray-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-accent-cyan"></div>
        </label>
      </div>
    </div>
  );
};

export default VolumeControl;