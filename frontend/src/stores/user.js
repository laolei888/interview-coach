import { defineStore } from 'pinia'
import { computed, reactive, ref } from 'vue'
import api, { loginApi, registerApi } from '../api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const profileVersion = ref(0)
  const profileLoading = ref(false)
  const profile = reactive({ userId: null, username: username.value, email: '', gender: '保密', avatar: '', avatarUrl: '', createdAt: '' })
  let avatarObjectUrl = ''

  const isLoggedIn = () => !!token.value
  const avatarFallback = computed(() => (profile.username || username.value || 'U').slice(0, 1).toUpperCase())
  const isImageAvatar = computed(() => !!profile.avatarUrl)

  const releaseAvatar = () => {
    if (avatarObjectUrl) URL.revokeObjectURL(avatarObjectUrl)
    avatarObjectUrl = ''
    profile.avatarUrl = ''
  }

  const resetProfile = () => {
    releaseAvatar()
    Object.assign(profile, { userId: null, username: '', email: '', gender: '保密', avatar: '', avatarUrl: '', createdAt: '' })
  }

  const loadProfile = async () => {
    if (!isLoggedIn()) {
      resetProfile()
      return null
    }
    profileLoading.value = true
    try {
      const res = await api.get('/user/me')
      if (res.data.code !== 200) throw new Error(res.data.message || '账户信息加载失败')
      const data = res.data.data
      releaseAvatar()
      Object.assign(profile, {
        userId: data.userId,
        username: data.username || username.value,
        email: data.email || '',
        gender: data.gender || '保密',
        avatar: data.avatar || '',
        createdAt: data.createdAt || ''
      })
      username.value = profile.username
      localStorage.setItem('username', username.value)
      if (profile.avatar.startsWith('/api/user/avatar/')) {
        const image = await api.get(profile.avatar.replace('/api', ''), { responseType: 'blob' })
        avatarObjectUrl = URL.createObjectURL(image.data)
        profile.avatarUrl = avatarObjectUrl
      }
      return profile
    } finally {
      profileLoading.value = false
    }
  }

  const login = async (name, pwd) => {
    const res = await loginApi(name, pwd)
    if (res.data.code === 200) {
      token.value = res.data.data.token
      username.value = res.data.data.username
      localStorage.setItem('token', token.value)
      localStorage.setItem('username', username.value)
      profileVersion.value += 1
      await loadProfile()
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
    resetProfile()
    profileVersion.value += 1
  }

  const markProfileChanged = async () => {
    profileVersion.value += 1
    return loadProfile()
  }

  return { token, username, profileVersion, profileLoading, profile, avatarFallback, isImageAvatar, isLoggedIn, login, register, logout, loadProfile, markProfileChanged }
})
