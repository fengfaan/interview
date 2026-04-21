import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        surface: '#FCF8F9',
        'surface-container-low': '#F6F3F4',
        'surface-container': '#F0EDEE',
        'surface-container-high': '#EAE7E8',
        'surface-container-highest': '#E5E2E3',
        'surface-container-lowest': '#FFFFFF',
        'inverse-surface': '#303031',
        primary: '#0058BE',
        'primary-container': '#2170E4',
        'primary-fixed': '#D8E2FF',
        secondary: '#006C49',
        'secondary-container': '#6CF8BB',
        error: '#BA1A1A',
        'error-container': '#FFDAD6',
        'on-surface': '#1B1B1C',
        'on-surface-variant': '#424754',
        'on-primary': '#FFFFFF',
        'on-primary-fixed': '#001A42',
        'on-secondary-container': '#00714D',
        'on-error-container': '#93000A',
        'outline-variant': '#C2C6D6',
      },
      fontFamily: {
        headline: ['Manrope', 'sans-serif'],
        body: ['Inter', 'sans-serif'],
        label: ['Inter', 'sans-serif'],
      },
    },
  },
  plugins: [],
} satisfies Config
