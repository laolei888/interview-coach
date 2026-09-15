<template>
  <div class="interview-page">
    <!-- 未开始 -->
    <div v-if="!sessionId" class="start-view">
      <div class="start-card">
        <div class="start-icon">🤖</div>
        <h1>AI 模拟面试</h1>
        <p>选择目标岗位，DeepSeek 化身专业面试官与你实时对话</p>
        <div class="position-pick">
          <span>面试岗位</span>
          <el-select v-model="position" size="large" style="width:240px">
            <el-option label="Java 后端开发" value="Java后端开发" />
            <el-option label="前端开发" value="前端开发" />
            <el-option label="Python 开发" value="Python开发" />
            <el-option label="测试开发" value="测试开发" />
            <el-option label="数据分析" value="数据分析" />
          </el-select>
        </div>
        <el-button type="primary" size="large" :loading="starting" @click="startInterview" class="go-btn">
          <el-icon><VideoPlay /></el-icon>开始面试
        </el-button>
      </div>
    </div>

    <!-- 面试中 -->
    <div v-if="sessionId" class="room">
      <div class="top-bar">
        <div class="bar-l">
          <div class="room-brand"><span class="live-dot"></span><div><strong>{{ position }}</strong><small>AI 实时模拟面试</small></div></div>
          <span class="round-num">第 {{ Math.max(1, Math.ceil(messages.length / 2)) }} 轮</span>
          <span :class="['connection-state', connectionState]">
            <i></i>{{ connectionLabel }}
          </span>
        </div>
        <el-button type="danger" :loading="ending" :disabled="aiTyping" @click="endInterview" round>结束面试</el-button>
      </div>

      <div class="room-grid">
      <div class="chat-panel" ref="chatPanel">
        <div v-if="messages.length === 0" class="chat-wait">
          <span class="w-dot"></span><p>面试官正在准备问题...</p>
        </div>

        <div v-for="(msg, idx) in messages" :key="idx" :class="['msg', msg.role]">
          <span class="msg-avatar">{{ msg.role === 'ai' ? '🤖' : '👤' }}</span>
          <div class="msg-body">
            <span class="msg-name">{{ msg.role === 'ai' ? '面试官' : '你' }}</span>
            <div class="msg-bubble" v-html="renderMarkdown(msg.content)"></div>
          </div>
        </div>

        <div v-if="aiTyping" class="msg ai">
          <span class="msg-avatar">🤖</span>
          <div class="msg-body">
            <span class="msg-name">面试官</span>
            <div class="msg-bubble typing"><span></span><span></span><span></span></div>
          </div>
        </div>
      </div>

      <div class="input-bar">
        <div class="input-row">
          <el-input v-model="userInput" type="textarea" :rows="2" placeholder="输入回答，Enter 发送..."
            :disabled="aiTyping || ending" @keydown.enter.exact.prevent="sendMessage" resize="none" />
          <div class="input-btns">
            <el-button
              :type="listening ? 'danger' : 'default'"
              :disabled="aiTyping || !voiceSupported"
              @click="toggleVoice"
              round
              :title="voiceSupported ? '语音输入' : '浏览器不支持语音'"
            >
              <el-icon><Microphone /></el-icon>{{ listening ? '录音中...' : '语音' }}
            </el-button>
            <el-button @click="skipQuestion" :disabled="aiTyping" round plain>跳过</el-button>
            <el-button v-if="aiTyping" type="warning" @click="cancelStream" round>停止</el-button>
            <el-button v-else type="primary" :disabled="!userInput.trim()" @click="sendMessage" round>发送</el-button>
          </div>
        </div>
        <div v-if="streamError" class="stream-error">
          <span>{{ streamError }}</span>
          <el-button link type="primary" @click="retryMessage">重试</el-button>
        </div>
      </div>
      <aside class="coach-panel">
        <div class="coach-heading"><span>训练状态</span><el-icon><DataAnalysis /></el-icon></div>
        <div class="coach-score"><strong>{{ Math.max(1, Math.ceil(messages.length / 2)) }}</strong><span>当前轮次</span></div>
        <div class="coach-stat"><span>你的回答</span><strong>{{ answerCount }} 次</strong></div>
        <div class="coach-stat"><span>平均字数</span><strong>{{ averageAnswerLength }} 字</strong></div>
        <div class="coach-stat"><span>回答均分</span><strong>{{ averageScore ? `${averageScore}/10` : '待生成' }}</strong></div>
        <div class="coach-tip"><el-icon><Aim /></el-icon><div><strong>回答建议</strong><p>{{ currentTip }}</p></div></div>
        <div class="coach-steps"><span class="done"></span><span :class="{ active: aiTyping }"></span><span></span><span></span><span></span></div>
        <small class="coach-caption">建议完成 5 轮后结束面试，获得完整评估。</small>
      </aside>
      </div>
    </div>

    <!-- 总结 -->
    <el-dialog v-model="showSummary" title="📊 面试评估报告" width="640px" :close-on-click-modal="false" @close="goHome">
      <div class="summary-content" v-html="renderMarkdown(summary)"></div>
      <template #footer>
        <el-button type="primary" @click="downloadReport" round>
          <el-icon><Download /></el-icon>下载报告（Markdown）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { startInterview as apiStart, endInterview as apiEnd, createChatStreamUrl, getSessionDetail } from '../api'
import { renderMarkdown } from '../utils/markdown'
import { Aim, DataAnalysis } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const position = ref('Java后端开发')
const sessionId = ref(null)
const starting = ref(false)
const ending = ref(false)
const messages = ref([])
const userInput = ref('')
const aiTyping = ref(false)
const showSummary = ref(false)
const summary = ref('')
const chatPanel = ref(null)
const listening = ref(false)
const voiceSupported = ref(false)
const connectionState = ref('idle')
const streamError = ref('')
const lastAnswer = ref('')
const connectionLabel = computed(() => ({ idle: '待机', connecting: '连接中', streaming: '生成中', error: '连接异常' }[connectionState.value] || '待机'))
const answerMessages = computed(() => messages.value.filter(message => message.role === 'user'))
const answerCount = computed(() => answerMessages.value.length)
const averageAnswerLength = computed(() => answerCount.value ? Math.round(answerMessages.value.reduce((sum, item) => sum + (item.content || '').length, 0) / answerCount.value) : 0)
const scoredAnswers = computed(() => answerMessages.value.filter(item => Number.isFinite(item.score)))
const averageScore = computed(() => scoredAnswers.value.length ? (scoredAnswers.value.reduce((sum, item) => sum + item.score, 0) / scoredAnswers.value.length).toFixed(1) : '')
const currentTip = computed(() => answerCount.value === 0 ? '先用 STAR 结构介绍一个最有代表性的项目。' : answerCount.value < 3 ? '先说结论，再补充技术细节和你承担的具体职责。' : '尝试量化结果，并主动说明你的取舍和复盘。')
let abortController = null
let recognition = null

// 语音识别初始化
if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
  voiceSupported.value = true
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  recognition = new SR()
  recognition.lang = 'zh-CN'
  recognition.interimResults = true
  recognition.continuous = true
  recognition.onresult = (e) => {
    let text = ''
    for (let i = 0; i < e.results.length; i++) {
      text += e.results[i][0].transcript
    }
    userInput.value = text
  }
  recognition.onerror = () => { listening.value = false }
  recognition.onend = () => { listening.value = false }
}

const toggleVoice = () => {
  if (listening.value) {
    recognition?.stop()
    listening.value = false
  } else {
    recognition?.start()
    listening.value = true
  }
}

// 支持从历史记录继续面试
onMounted(async () => {
  const resumeId = route.query.sessionId
  if (resumeId) {
    starting.value = true
    try {
      const res = await getSessionDetail(resumeId)
    if (res.data.code === 200) {
        const data = res.data.data
        sessionId.value = data.session.id
        position.value = data.session.position
        messages.value = data.messages || []
        if (data.session.status === 'completed') streamError.value = '该面试已结束，可在历史记录中查看报告。'
        ElMessage.success('已恢复面试')
        await nextTick()
        scroll()
      }
    } catch (e) { ElMessage.error('恢复失败') }
    finally { starting.value = false }
  }
})

const startInterview = async () => {
  starting.value = true
  try {
    const res = await apiStart(position.value)
    if (res.data.code === 200) { sessionId.value = res.data.data.sessionId; messages.value.push({ role: 'ai', content: res.data.data.firstQuestion }); ElMessage.success('面试开始'); await scroll() }
    else ElMessage.error(res.data.message)
  } catch (e) { ElMessage.error('请求失败') }
  finally { starting.value = false }
}

const sendMessage = async () => {
  const a = userInput.value.trim()
  if (!a || aiTyping.value || streamError.value === '该面试已结束，可在历史记录中查看报告。') return
  streamError.value = ''
  lastAnswer.value = a
  messages.value.push({ role: 'user', content: a }); userInput.value = ''; aiTyping.value = true; await scroll()

  const url = createChatStreamUrl(sessionId.value, a)
  abortController = new AbortController()
  const headers = {}
  const token = localStorage.getItem('token')
  if (token) headers['Authorization'] = `Bearer ${token}`

  try {
  connectionState.value = 'connecting'
  const resp = await fetch(url, { signal: abortController.signal, headers })
    if (resp.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      window.location.hash = '#/login'
      throw new Error('登录已过期')
    }
    if (!resp.ok || !resp.body) throw new Error('AI 流式响应不可用')
    connectionState.value = 'streaming'
    const reader = resp.body.getReader(); const dec = new TextDecoder()
    let buf = '', text = ''
    const idx = messages.value.length; messages.value.push({ role: 'ai', content: '' })

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += dec.decode(value, { stream: true })
      const parts = buf.split('\n\n'); buf = parts.pop() || ''
      for (const p of parts) {
        if (p.split('\n').some(line => line.trim() === 'event: error')) {
          const message = p.split('\n').filter(line => line.startsWith('data:')).map(line => line.slice(5).trim()).join(' ')
          throw new Error(message || 'AI 响应失败')
        }
        for (const line of p.split('\n')) {
          if (line.startsWith('data:')) text += line.slice(5).replace(/^ /, '')
        }
      }
      messages.value[idx].content = text
      await scroll()
    }
    if (buf.trim()) {
      for (const line of buf.split('\n')) {
        if (line.startsWith('data:')) text += line.slice(5).replace(/^ /, '')
      }
      messages.value[idx].content = text
    }
    const scoreMatch = text.match(/\[评分[：:]*\s*(10|[1-9])\s*\/\s*10\]/)
    if (scoreMatch) {
      const userMessage = [...messages.value].reverse().find(message => message.role === 'user')
      if (userMessage) userMessage.score = Number(scoreMatch[1])
    }
  } catch (e) {
    if (e.name !== 'AbortError') {
      streamError.value = e.message || 'AI 响应失败，请重试'
      connectionState.value = 'error'
      const last = messages.value[messages.value.length - 1]
      if (last?.role === 'ai' && !last.content) messages.value.pop()
      ElMessage.error(streamError.value)
    } else {
      connectionState.value = 'idle'
    }
  } finally { aiTyping.value = false; abortController = null }
}

const cancelStream = () => {
  abortController?.abort()
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'ai' && !last.content) messages.value.pop()
  connectionState.value = 'idle'
}
const retryMessage = async () => {
  if (!lastAnswer.value || aiTyping.value) return
  userInput.value = lastAnswer.value
  await sendMessage()
}
const skipQuestion = async () => { if (!aiTyping.value) { userInput.value = '请换一个问题'; await sendMessage() } }
const endInterview = async () => {
  try {
    await ElMessageBox.confirm('结束后将生成面试总结，当前会话不能继续作答。确定结束吗？', '结束面试', {
      confirmButtonText: '确认结束', cancelButtonText: '继续面试', type: 'warning'
    })
  } catch { return }
  ending.value = true
  try {
    const res = await apiEnd(sessionId.value)
    if (res.data.code === 200) { summary.value = res.data.data.summary; showSummary.value = true; ElMessage.success('面试结束') }
  } catch (e) { ElMessage.error('请求失败') }
  finally { ending.value = false }
}
const resetInterview = () => {
  showSummary.value = false
  goHome()
}
const downloadReport = () => {
  const text = summary.value
  const blob = new Blob([text], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `面试报告_${new Date().toLocaleDateString()}.md`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('报告已下载')
}
const goHome = () => {
  sessionId.value = null
  messages.value = []
  summary.value = ''
  userInput.value = ''
  router.push('/')
}
const scroll = async () => { await nextTick(); if (chatPanel.value) chatPanel.value.scrollTop = chatPanel.value.scrollHeight }
onBeforeUnmount(() => { if (abortController) abortController.abort() })
</script>

<style scoped>
.interview-page { max-width: 780px; margin: 0 auto; animation: fadeUp .4s ease; }
@keyframes fadeUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }

/* Start */
.start-view { display: flex; justify-content: center; padding-top: 48px; }
.start-card {
  background: #fff; border-radius: 20px; padding: 52px 48px; text-align: center;
  box-shadow: 0 8px 32px rgba(0,0,0,.06); width: 100%; max-width: 480px;
}
.start-icon { font-size: 60px; margin-bottom: 14px; }
.start-card h1 { font-size: 26px; color: #1a1a2e; margin-bottom: 8px; }
.start-card > p { color: #888; margin-bottom: 32px; line-height: 1.6; }
.position-pick { display: flex; align-items: center; justify-content: center; gap: 14px; margin-bottom: 32px; }
.position-pick span { font-size: 15px; font-weight: 600; color: #333; }
.go-btn { min-width: 150px; height: 46px; font-size: 16px; border-radius: 10px; }

/* Room */
.room { animation: fadeUp .3s ease; }
.top-bar { display: flex; justify-content: space-between; align-items: center; padding-bottom: 14px; }
.bar-l { display: flex; align-items: center; gap: 14px; }
.room-brand { display: flex; align-items: center; gap: 10px; }.room-brand > div { display: flex; flex-direction: column; gap: 2px; }.room-brand strong { color: var(--text); font-size: 15px; }.room-brand small { color: var(--text-muted); font-size: 11px; }.live-dot { width: 10px; height: 10px; border-radius: 50%; background: #36a269; box-shadow: 0 0 0 4px rgba(54,162,105,.13); }
.round-num { font-size: 13px; color: #aaa; }
.connection-state { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; color: #909399; }
.connection-state i { width: 7px; height: 7px; border-radius: 50%; background: #909399; }
.connection-state.connecting i, .connection-state.streaming i { background: #409EFF; animation: pulse 1.2s infinite; }
.connection-state.error { color: #f56c6c; }
.connection-state.error i { background: #f56c6c; }

/* Chat */
.chat-panel {
  background: #fff; border-radius: 16px; padding: 24px; height: 50vh; min-height: 380px;
  overflow-y: auto; box-shadow: 0 2px 12px rgba(0,0,0,.04); margin-bottom: 14px;
  display: flex; flex-direction: column; gap: 18px;
}
.room-grid { display: grid; grid-template-columns: minmax(0, 1fr) 210px; gap: 14px; align-items: start; }
.room-grid .chat-panel { grid-column: 1; grid-row: 1; }
.room-grid .input-bar { grid-column: 1; grid-row: 2; }
.coach-panel { grid-column: 2; grid-row: 1 / span 2; min-height: 380px; padding: 18px; border: 1px solid var(--border); border-radius: 10px; background: var(--bg-card); }
.coach-heading { display: flex; justify-content: space-between; align-items: center; color: var(--text); font-size: 13px; font-weight: 700; }.coach-heading .el-icon { color: var(--blue); font-size: 18px; }
.coach-score { display: flex; flex-direction: column; align-items: center; padding: 22px 0 18px; margin: 10px 0 14px; border-bottom: 1px solid var(--border); }.coach-score strong { color: var(--blue); font-size: 40px; line-height: 1; }.coach-score span { margin-top: 6px; color: var(--text-muted); font-size: 11px; }
.coach-stat { display: flex; justify-content: space-between; padding: 9px 0; color: var(--text-secondary); font-size: 12px; }.coach-stat strong { color: var(--text); font-size: 13px; }
.coach-tip { display: flex; gap: 8px; margin-top: 15px; padding: 11px; border-radius: 7px; background: var(--blue-light); color: var(--blue); }.coach-tip > .el-icon { flex-shrink: 0; margin-top: 2px; }.coach-tip strong { display: block; font-size: 11px; }.coach-tip p { margin-top: 5px; color: var(--text-secondary); font-size: 11px; line-height: 1.55; }
.coach-steps { display: flex; gap: 5px; margin: 20px 0 8px; }.coach-steps span { height: 4px; flex: 1; border-radius: 2px; background: var(--border); }.coach-steps span.done { background: var(--blue); }.coach-steps span.active { background: #77b8ff; animation: pulse 1.2s infinite; }.coach-caption { color: var(--text-muted); font-size: 10px; line-height: 1.5; }
.chat-wait { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #bbb; }
.w-dot { width: 12px; height: 12px; border-radius: 50%; background: #409EFF; animation: pulse 1.2s ease infinite; margin-bottom: 12px; }
@keyframes pulse { 0%,100%{opacity:.3;transform:scale(.8)} 50%{opacity:1;transform:scale(1.2)} }

.msg { display: flex; gap: 10px; animation: msgIn .3s ease; }
@keyframes msgIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }
.msg.user { flex-direction: row-reverse; }
.msg-avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; background: #f5f7fa; }
.msg-body { flex: 1; min-width: 0; }
.msg-name { font-size: 11px; color: #bbb; margin-bottom: 3px; display: block; padding-left: 2px; }
.msg.user .msg-name { text-align: right; padding-right: 2px; }
.msg-bubble { display: inline-block; max-width: 100%; padding: 12px 18px; border-radius: 16px; font-size: 15px; line-height: 1.8; word-break: break-word; }
.msg.ai .msg-bubble { background: #f5f7fa; border-bottom-left-radius: 4px; }
.msg.user .msg-bubble { background: #409EFF; color: #fff; border-bottom-right-radius: 4px; }

.msg-bubble :deep(p) { margin: 4px 0; }
.msg-bubble :deep(h3) { font-size: 16px; margin: 14px 0 6px; padding-bottom: 4px; border-bottom: 1px solid #e4e7ed; color: #1a1a2e; }
.msg-bubble :deep(h3:first-child) { margin-top: 0; }
.msg-bubble :deep(strong) { font-weight: 700; }
.msg-bubble :deep(ul), .msg-bubble :deep(ol) { padding-left: 20px; margin: 6px 0; }
.msg-bubble :deep(li) { margin: 4px 0; line-height: 1.7; }
.msg-bubble :deep(li::marker) { color: #409EFF; }
.msg-bubble :deep(code) { background: #e8eaed; padding: 2px 6px; border-radius: 4px; font-size: 13px; color: #d63384; }
.msg.user .msg-bubble :deep(h3) { color: #fff; border-bottom-color: rgba(255,255,255,.25); }
.msg.user .msg-bubble :deep(code) { background: rgba(255,255,255,.2); color: #fff; }

.typing span { display: inline-block; width: 7px; height: 7px; border-radius: 50%; background: #bbb; margin-right: 4px; animation: bounce 1.4s ease infinite; }
.typing span:nth-child(2) { animation-delay: .2s; }
.typing span:nth-child(3) { animation-delay: .4s; }
@keyframes bounce { 0%,60%,100%{transform:translateY(0)} 30%{transform:translateY(-5px)} }

/* Input */
.input-bar { background: #fff; border-radius: 16px; padding: 16px 20px; box-shadow: 0 2px 12px rgba(0,0,0,.04); }
.input-row { display: flex; gap: 12px; align-items: flex-end; }
.input-row :deep(.el-textarea) { flex: 1; }
.input-btns { display: flex; gap: 8px; flex-shrink: 0; }
.stream-error { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; padding: 8px 10px; color: #f56c6c; background: #fef0f0; border-radius: 6px; font-size: 13px; }

/* Summary */
.summary-content { line-height: 2; font-size: 15px; color: #333; }
.summary-content :deep(h3) { margin: 16px 0 8px; color: #1a1a2e; padding-bottom: 4px; border-bottom: 1px solid #eee; }
.summary-content :deep(strong) { font-weight: 700; }
.summary-content :deep(ul) { padding-left: 20px; }
.summary-content :deep(li) { margin: 6px 0; }
@media (max-width: 600px) {
  .start-view { padding-top: 20px; }
  .start-card { padding: 36px 20px; }
  .position-pick { flex-direction: column; gap: 8px; }
  .position-pick .el-select { width: 100% !important; }
  .top-bar { align-items: flex-start; gap: 10px; }
  .bar-l { flex-wrap: wrap; gap: 8px; }
  .room-grid { display: flex; flex-direction: column; }.coach-panel { order: 0; width: 100%; min-height: auto; padding: 12px 14px; }.coach-heading { margin-bottom: 0; }.coach-score, .coach-stat, .coach-tip, .coach-steps, .coach-caption { display: none; }.chat-panel { order: 1; width: 100%; height: 55vh; min-height: 320px; padding: 16px 12px; }.input-bar { order: 2; width: 100%; }
  .input-bar { padding: 12px; }
  .input-row { display: block; }
  .input-btns { justify-content: flex-end; margin-top: 8px; flex-wrap: wrap; }
  .msg-bubble { padding: 10px 12px; font-size: 14px; }
}
</style>
