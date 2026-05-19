import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    root: fileURLToPath(new URL('./', import.meta.url)),
    fileParallelism: false,
    maxWorkers: 1,
    minWorkers: 1,
  },
})
