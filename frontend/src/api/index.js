import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 120000
})

let sessionExpiredNotified = false

function normalizeError(err) {
  if (err.code === 'ECONNABORTED') return '请求超时，请稍后重试'
  if (!err.response) return '无法连接后端服务，请确认服务已启动'
  return err.response.data?.message || err.message || '请求失败，请稍后重试'
}

export const getErrorMessage = (err, fallback = '请求失败，请稍后重试') =>
  err?.normalizedMessage || err?.response?.data?.message || err?.message || fallback

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
      window.dispatchEvent(new CustomEvent('interview-coach:auth-expired'))
      if (!sessionExpiredNotified) {
        sessionExpiredNotified = true
        ElMessage.warning('登录状态已过期，请重新登录')
        setTimeout(() => { sessionExpiredNotified = false }, 1500)
      }
      const current = `${window.location.pathname}${window.location.search}`
      const redirect = current && current !== '/login' ? `?redirect=${encodeURIComponent(current)}` : ''
      window.history.replaceState(null, '', `/login${redirect}`)
      window.dispatchEvent(new PopStateEvent('popstate'))
    }
    err.normalizedMessage = normalizeError(err)
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
export const analyzeResume = (file, context = {}) => {
  const formData = new FormData()
  formData.append('file', file)
  if (context.targetPosition) formData.append('targetPosition', context.targetPosition)
  if (context.jobDescription) formData.append('jobDescription', context.jobDescription)
  return api.post('/resume/analyze', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const getResumeHistory = () => api.get('/resume/history')
export const getResumeDetail = (id) => api.get(`/resume/${id}`)

// ==================== AI 模拟面试 ====================
export const startInterview = (options = {}) =>
  api.get('/interview/start', {
    params: typeof options === 'string' ? { position: options } : options
  })
export const getInterviewHistory = (sessionId) =>
  api.get(`/interview/history/${sessionId}`)
export const endInterview = (sessionId, metrics = {}) =>
  api.post('/interview/end', null, { params: { sessionId, ...metrics } })
export const getSessions = () => api.get('/interview/sessions')
export const getSessionDetail = (id) => api.get(`/interview/session/${id}`)
export const deleteSession = (id) => api.delete(`/interview/session/${id}`)

// SSE：使用 POST 请求体承载回答，避免回答出现在 URL、访问日志或浏览器历史中
export const createChatStreamRequest = (sessionId, userAnswer) => ({
  url: '/api/interview/chat-stream',
  options: {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId, userAnswer })
  }
})

export default api
