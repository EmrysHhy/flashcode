import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    host: '0.0.0.0', // 监听所有网络接口，允许通过 IP 地址访问
    port: 80,
    proxy: {
      '/portal': {
        target: 'http://192.168.100.235',
        changeOrigin: true,
        secure: false,
        timeout: 600000,
        proxyTimeout: 600000,
      },
      // 如果需要代理预览页面以解决跨域问题，可以添加如下配置：
      '/preview': {
        target: 'http://192.168.100.235:80', // 替换为实际的预览服务器地址
        changeOrigin: true,
        secure: false,
        // 不重写路径，保留 /preview 前缀
        // rewrite: (path) => path.replace(/^\/preview/, ''),
      },
    },
  },
})
