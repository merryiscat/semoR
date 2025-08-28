/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        'noto': ['Noto Sans KR', 'sans-serif'],
      },
      colors: {
        'cyan': '#00BCD4',
        'alarmRed': '#F44336',
        'darkGray': '#121212',
        'cardGray': '#1E1E1E',
        // Legacy colors for compatibility
        'dark-bg': '#121212',
        'dark-card': '#1E1E1E',
        'dark-text': '#e0e0e0',
        'accent-cyan': '#00BCD4',
        'accent-red': '#F44336',
      }
    },
  },
  plugins: [],
}