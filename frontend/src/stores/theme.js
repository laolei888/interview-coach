import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const dark = ref(localStorage.getItem('dark') === '1')

  const apply = () => {
    document.documentElement.classList.toggle('dark', dark.value)
    localStorage.setItem('dark', dark.value ? '1' : '0')
  }

  const toggle = () => { dark.value = !dark.value }
  watch(dark, apply)
  apply()

  return { dark, toggle }
})
