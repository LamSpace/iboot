import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getDictDataByType, type DictData } from '@/api/system'

export const useDictStore = defineStore('dict', () => {
  const dictMap = ref<Record<string, DictData[]>>({})

  /**
   * 加载指定字典类型的数据（带缓存，已加载则直接返回）
   */
  async function loadDict(dictType: string): Promise<DictData[]> {
    if (dictMap.value[dictType]) {
      return dictMap.value[dictType]
    }
    const res = await getDictDataByType(dictType)
    if (res.code === 200) {
      dictMap.value[dictType] = res.data
      return res.data
    }
    return []
  }

  /**
   * 批量加载多个字典类型
   */
  async function loadDicts(...dictTypes: string[]): Promise<void> {
    const toLoad = dictTypes.filter(t => !dictMap.value[t])
    await Promise.all(toLoad.map(t => loadDict(t)))
  }

  /**
   * 获取指定字典类型的数据列表（同步，需先 loadDict）
   */
  function getDict(dictType: string): DictData[] {
    return dictMap.value[dictType] || []
  }

  /**
   * 根据字典类型和字典值获取标签文本
   */
  function getDictLabel(dictType: string, dictValue: string | number | undefined): string {
    if (dictValue === undefined || dictValue === null) return ''
    const list = dictMap.value[dictType] || []
    const item = list.find(d => d.dictValue === String(dictValue))
    return item?.dictLabel || String(dictValue)
  }

  /**
   * 根据字典类型和字典值获取 Tag 的 listClass（Element Plus tag type）
   */
  function getDictListClass(dictType: string, dictValue: string | number | undefined): string {
    if (dictValue === undefined || dictValue === null) return ''
    const list = dictMap.value[dictType] || []
    const item = list.find(d => d.dictValue === String(dictValue))
    return item?.listClass || ''
  }

  /**
   * 清除指定字典类型缓存
   */
  function removeDict(dictType: string) {
    delete dictMap.value[dictType]
  }

  /**
   * 清除所有字典缓存
   */
  function clearDict() {
    dictMap.value = {}
  }

  return { dictMap, loadDict, loadDicts, getDict, getDictLabel, getDictListClass, removeDict, clearDict }
})
