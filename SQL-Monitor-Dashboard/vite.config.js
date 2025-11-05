import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const isProduction = mode === 'production'
  
  return {
    plugins: [vue()],
    
    // 开发服务器配置
    server: {
      host: '0.0.0.0',
      port: 3000,
      open: false, // 自动打开浏览器
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL || 'https://localhost:19198',
          changeOrigin: true
        }
      }
    },
    
    // 构建配置
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: isProduction ? false : 'inline',
      minify: isProduction ? 'terser' : 'esbuild',
      terserOptions: isProduction ? {
        compress: {
          drop_console: true,
          drop_debugger: true
        }
      } : undefined,
      rollupOptions: {
        output: {
          chunkFileNames: 'assets/js/[name]-[hash].js',
          entryFileNames: 'assets/js/[name]-[hash].js',
          assetFileNames: 'assets/[name]-[hash][extname]'
        }
      }
    },
    
    // 基础路径
    base: env.VITE_BASE_URL || '/'
  }
})