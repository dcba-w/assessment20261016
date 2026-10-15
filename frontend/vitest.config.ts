import { resolve } from 'path'
import { defineConfig } from 'vitest/config'

// 单元测试独立配置。不复用 vite.config.ts，因为它依赖 `--mode` 命令行参数来加载 .env
export default defineConfig({
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  test: {
    environment: 'happy-dom',
    include: ['src/**/__tests__/**/*.spec.ts'],
    restoreMocks: true
  }
})
