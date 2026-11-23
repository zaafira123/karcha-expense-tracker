import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  build: { rollupOptions: { output: { manualChunks: { charts: ['recharts'] } } } },
  server: { proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } } },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    globals: true,
    testTimeout: 10000,
    pool: 'forks',
    maxWorkers: 1,
    fileParallelism: false,
  },
})
