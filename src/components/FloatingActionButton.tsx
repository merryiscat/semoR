import React from 'react';

interface FloatingActionButtonProps {
  onClick: () => void;
}

const FloatingActionButton: React.FC<FloatingActionButtonProps> = ({ onClick }) => {
  return (
    <button
      onClick={onClick}
      className="fixed w-16 h-16 rounded-full bg-red-500 flex items-center justify-center shadow-lg z-10 hover:bg-red-600 transition-colors"
      style={{ 
        backgroundColor: '#F44336',
        bottom: '88px', // 네비게이션 바(높이 약 72px) + 여백 16px
        right: '20px'
      }}
    >
      <i className="fas fa-plus text-white text-2xl"></i>
    </button>
  );
};

export default FloatingActionButton;