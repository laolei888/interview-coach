import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 120000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      window.location.hash = '#/login'
    }
    return Promise.reject(err)
  }
)

// ==================== 用户 ====================
export const loginApi = (username, password) =>
  api.post('/user/login', { username, password })
export const registerApi = (username, password) =>
  api.post('/user/register', { username, password })
export const getUserInfo = () => api.get('/user/me')

// ==================== 健康检查 ====================
export const healthCheck = () => api.get('/health')

// ==================== 简历分析 ====================
export const analyzeResume = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/resume/analyze', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const getResumeHistory = () => api.get('/resume/history')
export const getResumeDetail = (id) => api.get(`/resume/${id}`)

// ==================== AI 模拟面试 ====================
export const startInterview = (position = 'Java后端开发') =>
  api.get('/interview/start', { params: { position } })
export const getInterviewHistory = (sessionId) =>
  api.get(`/interview/history/${sessionId}`)
export const endInterview = (sessionId) =>
  api.post('/interview/end', null, { params: { sessionId } })
export const getSessions = () => api.get('/interview/sessions')
export const getSessionDetail = (id) => api.get(`/interview/session/${id}`)
export const deleteSession = (id) => api.delete(`/interview/session/${id}`)

// SSE
export const createChatStreamUrl = (sessionId, userAnswer) =>
  `/api/interview/chat-stream?sessionId=${sessionId}&userAnswer=${encodeURIComponent(userAnswer)}`

export default api
