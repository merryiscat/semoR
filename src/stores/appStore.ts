import { create } from 'zustand';
import { AppState, NavTab } from '@/types';

export const useAppStore = create<AppState>((set) => ({
  activeTab: 'alarm',
  setActiveTab: (tab: NavTab) => set({ activeTab: tab }),
  currentPage: 'main',
  setCurrentPage: (page: 'main' | 'add-alarm') => set({ currentPage: page }),
}));