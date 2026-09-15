import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, registerApi } from '../api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')

  const isLoggedIn = () => !!token.value

  const login = async (name, pwd) => {
    const res = await loginApi(name, pwd)
    if (res.data.code === 200) {
      token.value = res.data.data.token
      username.value = res.data.data.username
      localStorage.setItem('token', token.value)
      localStorage.setItem('username', username.value)
    }
    return res.data
  }

  const register = async (name, pwd) => {
    const res = await registerApi(name, pwd)
    return res.data
  }

  const logout = () => {
    token.value = ''
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
  }

  return { token, username, isLoggedIn, login, register, logout }
})
