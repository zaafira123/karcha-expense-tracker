import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        canvas: 'var(--bg-canvas)', subtle: 'var(--bg-subtle)', hover: 'var(--bg-hover)', 'accent-soft': 'var(--bg-accent-soft)',
        border: 'var(--border-default)', 'border-strong': 'var(--border-strong)', primary: 'var(--text-primary)', secondary: 'var(--text-secondary)', muted: 'var(--text-muted)',
        accent: 'var(--accent-default)', 'accent-hover': 'var(--accent-hover)', 'positive-text': 'var(--status-positive-text)', 'positive-bg': 'var(--status-positive-bg)',
        'warning-text': 'var(--status-warning-text)', 'warning-bg': 'var(--status-warning-bg)', 'danger-text': 'var(--status-danger-text)', 'danger-bg': 'var(--status-danger-bg)',
      },
      fontFamily: { sans: ['Inter', 'system-ui', 'Segoe UI', 'Arial', 'sans-serif'] },
      borderRadius: { control: '8px', card: '12px' },
      fontSize: {
        display: ['32px', { lineHeight: '40px', fontWeight: '600' }], h1: ['24px', { lineHeight: '32px', fontWeight: '600' }],
        h2: ['18px', { lineHeight: '28px', fontWeight: '600' }], body: ['15px', { lineHeight: '24px', fontWeight: '400' }],
        'body-strong': ['15px', { lineHeight: '24px', fontWeight: '500' }], small: ['13px', { lineHeight: '20px', fontWeight: '400' }], caption: ['12px', { lineHeight: '16px', fontWeight: '500' }],
        'landing-h1': ['48px', { lineHeight: '56px', fontWeight: '600' }], 'landing-h2': ['32px', { lineHeight: '40px', fontWeight: '600' }],
      },
    },
  },
  plugins: [],
} satisfies Config
