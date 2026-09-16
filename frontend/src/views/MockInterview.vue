<template>
  <div class="interview-page">
    <!-- 未开始 -->
    <div v-if="!sessionId" class="start-view">
      <section class="start-intro">
        <span class="eyebrow">AI 模拟面试</span><h1>像真实面试一样，<br>完成一场有追问的对话</h1><p>面试官会根据你的回答继续追问，并从表达结构、技术深度和项目证据三个维度给出反馈。</p>
        <div class="interview-features"><div><el-icon><ChatLineRound /></el-icon><span><strong>动态追问</strong><small>根据回答实时调整问题</small></span></div><div><el-icon><DataAnalysis /></el-icon><span><strong>逐题评分</strong><small>每次回答获得即时反馈</small></span></div><div><el-icon><DocumentChecked /></el-icon><span><strong>完整复盘</strong><small>结束后生成能力报告</small></span></div></div>
      </section>
      <section class="start-config">
        <div class="config-head"><span><el-icon><Suitcase /></el-icon></span><div><strong>创建面试会话</strong><small>预计用时 20-30 分钟</small></div></div>
        <label>目标岗位</label>
        <div class="position-pick">
          <el-select v-model="position" size="large" filterable allow-create default-first-option placeholder="选择或输入目标岗位">
            <el-option label="Java 后端开发" value="Java后端开发" />
            <el-option label="前端开发" value="前端开发" />
            <el-option label="Python 开发" value="Python开发" />
            <el-option label="测试开发" value="测试开发" />
            <el-option label="数据分析" value="数据分析" />
          </el-select>
        </div>
        <div class="session-plan"><span>训练流程</span><div><i>1</i><b>岗位问题</b><em></em><i>2</i><b>技术追问</b><em></em><i>3</i><b>能力评估</b></div></div>
        <el-button type="primary" size="large" :loading="starting" @click="startInterview" class="go-btn"><el-icon><VideoPlay /></el-icon>进入面试间</el-button>
        <p class="config-note"><el-icon><Lock /></el-icon>会话和回答仅当前账号可见</p>
      </section>
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
        <div class="room-actions"><span class="timer"><el-icon><Timer /></el-icon>{{ elapsedTime }}</span><el-button :loading="ending" :disabled="aiTyping" @click="endInterview">结束面试</el-button></div>
      </div>

      <div class="room-grid">
      <div class="chat-panel" ref="chatPanel">
        <div v-if="messages.length === 0" class="chat-wait">
          <span class="w-dot"></span><p>面试官正在准备问题...</p>
        </div>

        <div v-for="(msg, idx) in messages" :key="idx" :class="['msg', msg.role]">
          <span class="msg-avatar"><el-icon><Service v-if="msg.role === 'ai'" /><User v-else /></el-icon></span>
          <div class="msg-body">
            <span class="msg-name">{{ msg.role === 'ai' ? '面试官' : '你' }}</span>
            <div class="msg-bubble" v-html="renderMarkdown(msg.content)"></div>
          </div>
        </div>

        <div v-if="aiTyping" class="msg ai">
          <span class="msg-avatar"><el-icon><Service /></el-icon></span>
          <div class="msg-body">
            <span class="msg-name">面试官</span>
            <div class="msg-bubble typing"><span></span><span></span><span></span></div>
          </div>
        </div>
      </div>

      <div class="input-bar">
        <div class="input-row">
          <el-input v-model="userInput" type="textarea" :rows="3" maxlength="4000" placeholder="先说结论，再说明你的行动、技术选择和结果..."
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
        <div class="input-meta"><span>{{ userInput.length }} / 4000 字</span><span>Enter 发送 · Shift + Enter 换行</span></div>
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
        <div class="coach-steps"><span v-for="step in 5" :key="step" :class="{ done: answerCount >= step, active: answerCount + 1 === step && aiTyping }"></span></div>
        <small class="coach-caption">建议完成 5 轮后结束面试，获得完整评估。</small>
      </aside>
      </div>
    </div>

    <!-- 总结 -->
    <el-dialog v-model="showSummary" title="面试评估报告" width="720px" :close-on-click-modal="false" @close="goHome">
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
import { Aim, ChatLineRound, DataAnalysis, DocumentChecked, Lock, Service, Suitcase, Timer, User } from '@element-plus/icons-vue'
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
const startedAt = ref(null)
const elapsedSeconds = ref(0)
const connectionLabel = computed(() => ({ idle: '待机', connecting: '连接中', streaming: '生成中', error: '连接异常' }[connectionState.value] || '待机'))
const answerMessages = computed(() => messages.value.filter(message => message.role === 'user'))
const answerCount = computed(() => answerMessages.value.length)
const averageAnswerLength = computed(() => answerCount.value ? Math.round(answerMessages.value.reduce((sum, item) => sum + (item.content || '').length, 0) / answerCount.value) : 0)
const scoredAnswers = computed(() => answerMessages.value.filter(item => Number.isFinite(item.score)))
const averageScore = computed(() => scoredAnswers.value.length ? (scoredAnswers.value.reduce((sum, item) => sum + item.score, 0) / scoredAnswers.value.length).toFixed(1) : '')
const currentTip = computed(() => answerCount.value === 0 ? '先用 STAR 结构介绍一个最有代表性的项目。' : answerCount.value < 3 ? '先说结论，再补充技术细节和你承担的具体职责。' : '尝试量化结果，并主动说明你的取舍和复盘。')
const elapsedTime = computed(() => `${String(Math.floor(elapsedSeconds.value / 60)).padStart(2, '0')}:${String(elapsedSeconds.value % 60).padStart(2, '0')}`)
let abortController = null
let recognition = null
let timerId = null
const beginTimer = (value = Date.now()) => { startedAt.value = value; elapsedSeconds.value = Math.max(0, Math.floor((Date.now() - value) / 1000)); clearInterval(timerId); timerId = setInterval(() => { elapsedSeconds.value = Math.floor((Date.now() - startedAt.value) / 1000) }, 1000) }

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
        beginTimer(data.session.createdAt ? new Date(data.session.createdAt).getTime() : Date.now())
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
    if (res.data.code === 200) { sessionId.value = res.data.data.sessionId; beginTimer(); messages.value.push({ role: 'ai', content: res.data.data.firstQuestion }); ElMessage.success('面试开始'); await scroll() }
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
      router.replace('/login')
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
    if (res.data.code === 200) { clearInterval(timerId); summary.value = res.data.data.summary; showSummary.value = true; ElMessage.success('面试结束') }
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
onBeforeUnmount(() => { if (abortController) abortController.abort(); clearInterval(timerId) })
</script>

<style scoped>
.interview-page { max-width: 1260px; margin: 0 auto; animation: fadeUp .4s ease; color: var(--text); }
@keyframes fadeUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }

/* Start */
.start-view { min-height: calc(100vh - 110px); display: grid; grid-template-columns: minmax(0,1.1fr) minmax(380px,.75fr); align-items: center; gap: clamp(40px,7vw,100px); padding: 42px 3vw; }
.start-intro { max-width: 650px; }.eyebrow { color: var(--blue); font-size: 11px; font-weight: 700; }.start-intro h1 { margin: 14px 0 20px; font-size: clamp(38px,4vw,56px); line-height: 1.12; }.start-intro > p { max-width: 600px; color: var(--text-secondary); font-size: 15px; line-height: 1.8; }.interview-features { display: grid; grid-template-columns: repeat(3,1fr); gap: 12px; margin-top: 34px; }.interview-features > div { display: flex; align-items: center; gap: 10px; padding: 14px 0; border-top: 1px solid var(--border); }.interview-features .el-icon { color: var(--blue); font-size: 20px; }.interview-features span,.interview-features strong,.interview-features small { display: block; }.interview-features strong { font-size: 11px; }.interview-features small { margin-top: 3px; color: var(--text-muted); font-size: 9px; }
.start-config { padding: 28px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); box-shadow: var(--shadow); }.config-head { display: flex; align-items: center; gap: 12px; margin-bottom: 26px; }.config-head > span { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 7px; background: var(--blue-light); color: var(--blue); font-size: 20px; }.config-head strong,.config-head small { display: block; }.config-head strong { font-size: 15px; }.config-head small { margin-top: 4px; color: var(--text-muted); font-size: 10px; }.start-config > label { display: block; margin-bottom: 8px; color: var(--text-secondary); font-size: 11px; font-weight: 700; }.position-pick { margin-bottom: 22px; }.position-pick :deep(.el-select) { width: 100%; }.session-plan { padding: 15px; margin-bottom: 20px; border: 1px solid var(--border); border-radius: 7px; background: var(--hover); }.session-plan > span { color: var(--text-muted); font-size: 9px; }.session-plan > div { display: flex; align-items: center; margin-top: 11px; }.session-plan i { width: 21px; height: 21px; display: grid; place-items: center; border-radius: 50%; background: var(--blue-light); color: var(--blue); font-size: 9px; font-style: normal; }.session-plan b { margin-left: 5px; font-size: 9px; white-space: nowrap; }.session-plan em { height: 1px; flex: 1; margin: 0 8px; background: var(--border); }.go-btn { width: 100%; height: 46px; border-radius: 7px; font-weight: 700; }.config-note { display: flex; align-items: center; justify-content: center; gap: 5px; margin-top: 13px; color: var(--text-muted); font-size: 9px; }

/* Room */
.room { animation: fadeUp .3s ease; }
.top-bar { display: flex; justify-content: space-between; align-items: center; padding: 4px 0 18px; }
.bar-l { display: flex; align-items: center; gap: 14px; }
.room-brand { display: flex; align-items: center; gap: 10px; }.room-brand > div { display: flex; flex-direction: column; gap: 2px; }.room-brand strong { color: var(--text); font-size: 15px; }.room-brand small { color: var(--text-muted); font-size: 11px; }.live-dot { width: 10px; height: 10px; border-radius: 50%; background: #36a269; box-shadow: 0 0 0 4px rgba(54,162,105,.13); }
.round-num { font-size: 13px; color: #aaa; }
.connection-state { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; color: #909399; }
.connection-state i { width: 7px; height: 7px; border-radius: 50%; background: #909399; }
.connection-state.connecting i, .connection-state.streaming i { background: #409EFF; animation: pulse 1.2s infinite; }
.connection-state.error { color: #f56c6c; }
.connection-state.error i { background: #f56c6c; }
.room-actions { display: flex; align-items: center; gap: 12px; }.timer { display: flex; align-items: center; gap: 5px; color: var(--text-muted); font: 600 12px ui-monospace, SFMono-Regular, Menlo, monospace; }

/* Chat */
.chat-panel {
  background: var(--bg-card); border: 1px solid var(--border); border-radius: 8px; padding: 28px 30px; height: calc(100vh - 315px); min-height: 440px;
  overflow-y: auto; margin-bottom: 14px;
  display: flex; flex-direction: column; gap: 18px;
}
.room-grid { display: grid; grid-template-columns: minmax(0, 1fr) 250px; gap: 16px; align-items: start; }
.room-grid .chat-panel { grid-column: 1; grid-row: 1; }
.room-grid .input-bar { grid-column: 1; grid-row: 2; }
.coach-panel { grid-column: 2; grid-row: 1 / span 2; min-height: 440px; padding: 20px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); position: sticky; top: 30px; }
.coach-heading { display: flex; justify-content: space-between; align-items: center; color: var(--text); font-size: 13px; font-weight: 700; }.coach-heading .el-icon { color: var(--blue); font-size: 18px; }
.coach-score { display: flex; flex-direction: column; align-items: center; padding: 22px 0 18px; margin: 10px 0 14px; border-bottom: 1px solid var(--border); }.coach-score strong { color: var(--blue); font-size: 40px; line-height: 1; }.coach-score span { margin-top: 6px; color: var(--text-muted); font-size: 11px; }
.coach-stat { display: flex; justify-content: space-between; padding: 9px 0; color: var(--text-secondary); font-size: 12px; }.coach-stat strong { color: var(--text); font-size: 13px; }
.coach-tip { display: flex; gap: 8px; margin-top: 15px; padding: 11px; border-radius: 7px; background: var(--blue-light); color: var(--blue); }.coach-tip > .el-icon { flex-shrink: 0; margin-top: 2px; }.coach-tip strong { display: block; font-size: 11px; }.coach-tip p { margin-top: 5px; color: var(--text-secondary); font-size: 11px; line-height: 1.55; }
.coach-steps { display: flex; gap: 5px; margin: 20px 0 8px; }.coach-steps span { height: 4px; flex: 1; border-radius: 2px; background: var(--border); }.coach-steps span.done { background: var(--blue); }.coach-steps span.active { background: #77b8ff; animation: pulse 1.2s infinite; }.coach-caption { color: var(--text-muted); font-size: 10px; line-height: 1.5; }
.chat-wait { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: var(--text-muted); }.w-dot { width: 12px; height: 12px; border-radius: 50%; background: var(--blue); animation: pulse 1.2s ease infinite; margin-bottom: 12px; }
@keyframes pulse { 0%,100%{opacity:.3;transform:scale(.8)} 50%{opacity:1;transform:scale(1.2)} }

.msg { display: flex; gap: 10px; animation: msgIn .3s ease; }
@keyframes msgIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }
.msg.user { flex-direction: row-reverse; }
.msg-avatar { width: 34px; height: 34px; border-radius: 7px; display: flex; align-items: center; justify-content: center; font-size: 17px; flex-shrink: 0; background: var(--hover); color: var(--text-secondary); }.msg.ai .msg-avatar { background: var(--blue-light); color: var(--blue); }.msg.user .msg-avatar { background: var(--text); color: var(--bg-card); }
.msg-body { flex: 1; min-width: 0; }
.msg-name { font-size: 10px; color: var(--text-muted); margin-bottom: 5px; display: block; padding-left: 2px; }
.msg.user .msg-name { text-align: right; padding-right: 2px; }
.msg-bubble { display: inline-block; max-width: 92%; padding: 13px 16px; border-radius: 8px; font-size: 14px; line-height: 1.8; word-break: break-word; }
.msg.ai .msg-bubble { background: var(--hover); border-top-left-radius: 2px; color: var(--text); }
.msg.user .msg-bubble { background: var(--text); color: var(--bg-card); border-top-right-radius: 2px; }

.msg-bubble :deep(p) { margin: 4px 0; }
.msg-bubble :deep(h3) { font-size: 15px; margin: 14px 0 6px; padding-bottom: 4px; border-bottom: 1px solid var(--border); color: var(--text); }
.msg-bubble :deep(h3:first-child) { margin-top: 0; }
.msg-bubble :deep(strong) { font-weight: 700; }
.msg-bubble :deep(ul), .msg-bubble :deep(ol) { padding-left: 20px; margin: 6px 0; }
.msg-bubble :deep(li) { margin: 4px 0; line-height: 1.7; }
.msg-bubble :deep(li::marker) { color: var(--blue); }.msg-bubble :deep(code) { background: var(--bg-card); padding: 2px 6px; border-radius: 4px; font-size: 12px; color: var(--accent); }
.msg.user .msg-bubble :deep(h3) { color: #fff; border-bottom-color: rgba(255,255,255,.25); }
.msg.user .msg-bubble :deep(code) { background: rgba(255,255,255,.2); color: #fff; }

.typing span { display: inline-block; width: 7px; height: 7px; border-radius: 50%; background: #bbb; margin-right: 4px; animation: bounce 1.4s ease infinite; }
.typing span:nth-child(2) { animation-delay: .2s; }
.typing span:nth-child(3) { animation-delay: .4s; }
@keyframes bounce { 0%,60%,100%{transform:translateY(0)} 30%{transform:translateY(-5px)} }

/* Input */
.input-bar { background: var(--bg-card); border: 1px solid var(--border); border-radius: 8px; padding: 14px 16px 10px; }
.input-row { display: flex; gap: 12px; align-items: flex-end; }
.input-row :deep(.el-textarea) { flex: 1; }
.input-btns { display: flex; gap: 8px; flex-shrink: 0; }
.input-meta { display: flex; justify-content: space-between; margin-top: 7px; color: var(--text-muted); font-size: 9px; }
.stream-error { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; padding: 8px 10px; color: #f56c6c; background: #fef0f0; border-radius: 6px; font-size: 13px; }

/* Summary */
.summary-content { line-height: 2; font-size: 14px; color: var(--text); }.summary-content :deep(h3) { margin: 16px 0 8px; color: var(--text); padding-bottom: 4px; border-bottom: 1px solid var(--border); }
.summary-content :deep(strong) { font-weight: 700; }
.summary-content :deep(ul) { padding-left: 20px; }
.summary-content :deep(li) { margin: 6px 0; }
@media (max-width: 600px) {
  .start-view { display: block; min-height: 0; padding: 8px 0 20px; }.start-intro h1 { font-size: 32px; }.start-intro > p { font-size: 13px; }.interview-features { grid-template-columns: 1fr; gap: 0; margin: 24px 0; }.interview-features > div { padding: 10px 0; }.start-config { padding: 22px 18px; }
  .top-bar { align-items: flex-start; gap: 10px; }
  .bar-l { flex-wrap: wrap; gap: 8px; }
  .room-actions .timer { display: none; }.room-grid { display: flex; flex-direction: column; }.coach-panel { order: 0; width: 100%; min-height: auto; padding: 12px 14px; position: static; }.coach-heading { margin-bottom: 0; }.coach-score, .coach-stat, .coach-tip, .coach-steps, .coach-caption { display: none; }.chat-panel { order: 1; width: 100%; height: 55vh; min-height: 320px; padding: 16px 12px; }.input-bar { order: 2; width: 100%; }
  .input-bar { padding: 12px; }
  .input-row { display: block; }
  .input-btns { justify-content: flex-end; margin-top: 8px; flex-wrap: wrap; }
  .msg-bubble { padding: 10px 12px; font-size: 14px; }
}
</style>
