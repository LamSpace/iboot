import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPublicConfig } from '@/api/system'

export const useAppStore = defineStore('app', () => {
  const systemName = ref<string>('iBoot 后台管理系统')
  const registerEnabled = ref<boolean>(false)
  const configLoaded = ref<boolean>(false)

  async function loadPublicConfig() {
    if (configLoaded.value) return
    
    try {
      const res = await getPublicConfig()
      systemName.value = res.data.systemName
      registerEnabled.value = res.data.registerEnabled
      configLoaded.value = true
    } catch (error) {
      console.error('Failed to load public config:', error)
    }
  }

  return {
    systemName,
    registerEnabled,
    configLoaded,
    loadPublicConfig
  }
})
