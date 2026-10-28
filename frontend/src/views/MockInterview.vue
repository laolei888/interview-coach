<template>
  <div :class="['interview-page', { 'focus-mode': focusMode }]">
    <!-- 未开始 -->
    <div v-if="!sessionId && starting && route.query.sessionId" class="resume-loading">
      <span class="loading-orb"><el-icon><Loading /></el-icon></span><span class="eyebrow"><i></i>RESTORING SESSION / 恢复面试</span><h2>正在恢复你的面试现场</h2><p>我们正在加载历史对话和训练状态，请稍候。</p><el-skeleton :rows="3" animated />
    </div>
    <div v-else-if="!sessionId" class="start-view">
      <section class="start-intro">
        <span class="eyebrow"><i></i>AI 模拟面试 / LIVE PRACTICE</span><h1>像真实面试一样，<br>完成一场有追问的对话</h1><p>面试官会根据你的回答继续追问，并从表达结构、技术深度和项目证据三个维度给出反馈。</p>
        <div class="interview-features"><div><el-icon><ChatLineRound /></el-icon><span><strong>动态追问</strong><small>根据回答实时调整问题</small></span></div><div><el-icon><DataAnalysis /></el-icon><span><strong>逐题评分</strong><small>每次回答获得即时反馈</small></span></div><div><el-icon><DocumentChecked /></el-icon><span><strong>完整复盘</strong><small>结束后生成能力报告</small></span></div></div>
      </section>
      <section class="start-config">
        <div class="config-head"><span><el-icon><Suitcase /></el-icon></span><div><strong>创建面试会话</strong><small>预计用时 20-30 分钟 · 上次配置会自动保留</small></div></div>
        <section class="preset-block">
          <div class="preset-heading"><div><span>QUICK START</span><strong>先选一个训练方案</strong></div><small>参数仍可继续调整</small></div>
          <div class="preset-grid">
            <button v-for="preset in interviewPresets" :key="preset.key" type="button" :class="['preset-card', { active: isPresetActive(preset) }]" @click="applyPreset(preset)">
              <span class="preset-mark"><el-icon><component :is="preset.icon" /></el-icon></span>
              <span class="preset-copy"><strong>{{ preset.label }}</strong><small>{{ preset.desc }}</small></span>
              <el-icon class="preset-arrow"><ArrowRight /></el-icon>
            </button>
          </div>
        </section>
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
        <label>面试场景</label>
        <div class="mode-grid">
          <button v-for="item in interviewModes" :key="item.value" type="button" :class="{ active: interviewMode === item.value }" @click="interviewMode = item.value">
            <el-icon><component :is="item.icon" /></el-icon><span><strong>{{ item.label }}</strong><small>{{ item.desc }}</small></span>
          </button>
        </div>
        <div class="split-config">
          <div><label>难度档位</label><el-segmented v-model="difficultyLevel" :options="difficultyOptions" /></div>
          <div><label>目标轮次</label><el-input-number v-model="targetRounds" :min="3" :max="8" controls-position="right" /></div>
        </div>
        <div class="feedback-choice">
          <div class="feedback-heading"><div><label>面试官反馈风格</label><small>评分标准不变，只调整点评的直接程度</small></div><span><el-icon><Aim /></el-icon>{{ feedbackStyleLabel }}</span></div>
          <div class="feedback-grid">
            <button v-for="item in feedbackStyles" :key="item.value" type="button" :class="{ active: feedbackStyle === item.value }" @click="feedbackStyle = item.value">
              <span class="feedback-icon"><el-icon><component :is="item.icon" /></el-icon></span><span class="feedback-copy"><strong>{{ item.label }}</strong><small>{{ item.desc }}</small></span><el-icon class="feedback-check"><CircleCheck /></el-icon>
            </button>
          </div>
        </div>
        <div class="presence-choice">
          <div class="presence-heading"><div><label>面试现场</label><small>可随时切回文字模式，回答与进度不会丢失</small></div><span>ANIME PRESENCE</span></div>
          <div class="presence-grid">
            <button type="button" :class="{ active: immersiveMode }" :aria-pressed="immersiveMode" @click="toggleImmersiveMode">
              <span class="presence-icon"><el-icon><VideoPlay /></el-icon></span><span><strong>动漫 3D 面试官</strong><small>VRoid 角色、自然动作与表情嘴型</small></span><i>{{ immersiveMode ? '已开启' : '已关闭' }}</i>
            </button>
            <button type="button" :class="{ active: voicePlaybackEnabled }" :aria-pressed="voicePlaybackEnabled" :disabled="!speechSupported" @click="toggleVoicePlayback">
              <span class="presence-icon"><el-icon><Service /></el-icon></span><span><strong>自然语音播报</strong><small>{{ speechSupported ? '优先使用在线自然中文声线' : '当前浏览器不支持语音播报' }}</small></span><i>{{ voicePlaybackEnabled && speechSupported ? '已开启' : '已关闭' }}</i>
            </button>
          </div>
          <div v-if="speechSupported && voicePlaybackEnabled" class="voice-studio">
            <div class="voice-studio-copy"><span><i></i>{{ selectedVoiceNatural ? '自然声线已就绪' : '当前为系统声线' }}</span><small>{{ voiceQualityHint }}</small></div>
            <el-select v-model="selectedVoiceId" size="small" :disabled="!voiceOptions.length" placeholder="选择中文声线" @change="handleVoiceSelection">
              <el-option v-for="voice in voiceOptions" :key="voice.id" :label="voice.label" :value="voice.id"><span class="voice-option"><b>{{ voice.shortName }}</b><i>{{ voice.natural ? '自然' : voice.lang }}</i></span></el-option>
            </el-select>
            <el-button size="small" :loading="voicePreviewing" @click="previewSelectedVoice"><el-icon><VideoPlay /></el-icon>试听</el-button>
          </div>
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
          <div class="round-progress"><span>进度</span><strong>{{ currentRound }}<small>/ {{ targetRounds }}</small></strong><i><b :style="{ width: `${Math.min(100, answerCount / targetRounds * 100)}%` }"></b></i></div>
          <span class="mode-chip">{{ modeLabel }} · {{ difficultyLabel }} · {{ feedbackStyleLabel }}</span>
          <span :class="['connection-state', connectionState]">
            <i></i>{{ connectionLabel }}
          </span>
        </div>
        <div class="room-actions"><span v-if="!sessionReadOnly" class="save-state"><el-icon><Lock /></el-icon>回答实时保存</span><span class="timer"><el-icon><Timer /></el-icon>{{ elapsedTime }}</span><el-button class="focus-toggle" text :aria-pressed="focusMode" :title="focusMode ? '恢复训练状态面板' : '隐藏状态面板，专注回答'" @click="focusMode = !focusMode"><el-icon><Close v-if="focusMode" /><FullScreen v-else /></el-icon>{{ focusMode ? '退出专注' : '专注模式' }}</el-button><el-button v-if="!sessionReadOnly" class="end-interview-btn" type="danger" plain :loading="ending" :disabled="aiTyping || ending" title="结束后将生成面试复盘，不能继续作答" @click="endInterview"><el-icon v-if="!ending"><CircleClose /></el-icon>{{ ending ? '正在生成报告' : '结束面试' }}</el-button><el-button v-else class="finished-interview-btn" type="success" plain @click="goHistory"><el-icon><DocumentChecked /></el-icon>已结束 · 查看复盘</el-button></div>
      </div>

      <div class="immersive-layout">
        <section class="visual-room" :class="{ 'text-only': !immersiveMode }">
          <AnimeInterviewer v-if="immersiveMode" :state="interviewerState" :speech-text="activeCaptionText" :speech-progress="speechProgress" performance-mode="auto" @ready="handleAvatarReady" @fallback="handleAvatarFallback" />
          <div v-else class="text-room-fallback">
            <span><el-icon><Service /></el-icon></span>
            <strong>文字面试模式</strong>
            <small>三维场景已关闭，当前会话与评分仍会正常保存。</small>
          </div>

          <div class="scene-header">
            <div class="interviewer-profile"><span><img src="/models/interviewer-preview.png" alt="" /></span><div><strong>林屿</strong><small>AI 动漫 · {{ modeLabel }}面试官</small></div></div>
            <span :class="['presence-state', interviewerState]"><i></i>{{ interviewerStateLabel }}</span>
          </div>

          <div class="phase-rail" aria-label="面试状态">
            <span :class="{ active: interviewerState === 'thinking' || interviewerState === 'preparing' }"><i>01</i>思考</span>
            <b></b>
            <span :class="{ active: interviewerState === 'speaking' }"><i>02</i>提问</span>
            <b></b>
            <span :class="{ active: interviewerState === 'listening' || interviewerState === 'idle' }"><i>03</i>倾听</span>
          </div>

          <div :class="['scene-caption', interviewerState]">
            <div><span><i></i>面试官实时字幕</span><small>{{ speechStatusLabel }}</small></div>
            <p v-if="speaking"><span>{{ spokenCaption }}</span><em>{{ pendingCaption }}</em></p>
            <p v-else>{{ activeCaptionText || '正在整理下一道面试问题，请稍候。' }}</p>
          </div>

          <div class="scene-controls">
            <button type="button" :class="['scene-mic', { active: listening }]" :disabled="aiTyping || speaking || sessionReadOnly || !voiceSupported" @click="toggleVoice">
              <span><el-icon><Microphone /></el-icon></span><span><strong>{{ listening ? '结束语音回答' : '开始语音回答' }}</strong><small>{{ voiceSupported ? (listening ? '正在把你的回答转成文字' : '点击后直接说出你的回答') : '当前浏览器不支持语音识别' }}</small></span>
            </button>
            <button type="button" :class="['scene-tool', { active: voicePlaybackEnabled }]" :disabled="!speechSupported" :title="voicePlaybackEnabled ? `${selectedVoiceName} · 点击关闭` : '开启自然语音'" @click="toggleVoicePlayback"><el-icon><Service /></el-icon><span>{{ voicePlaybackEnabled ? '自然语音' : '语音已关' }}</span></button>
            <button type="button" :class="['scene-tool', { active: immersiveMode }]" title="切换三维场景" @click="toggleImmersiveMode"><el-icon><FullScreen /></el-icon><span>{{ immersiveMode ? avatarQualityLabel : '文字模式' }}</span></button>
          </div>
        </section>

        <aside class="interview-console">
          <div class="console-head">
            <div><span>SESSION CONSOLE</span><strong>面试记录</strong></div>
            <div class="console-tabs" role="tablist"><button type="button" :class="{ active: sidePanelTab === 'transcript' }" @click="sidePanelTab = 'transcript'">对话</button><button type="button" :class="{ active: sidePanelTab === 'coach' }" @click="sidePanelTab = 'coach'">状态</button></div>
          </div>

          <div v-show="sidePanelTab === 'transcript'" class="chat-panel" ref="chatPanel">
            <div v-if="messages.length === 0" class="chat-wait">
              <span class="w-dot"></span><p>面试官正在准备问题...</p>
            </div>

            <div v-for="(msg, idx) in messages" :key="idx" :class="['msg', msg.role]">
              <span class="msg-avatar"><el-icon><Service v-if="msg.role === 'ai'" /><User v-else /></el-icon></span>
              <div class="msg-body">
                <span class="msg-name">{{ msg.role === 'ai' ? '面试官' : '你' }}</span>
                <div class="msg-bubble" v-html="msg.role === 'ai' ? renderInterviewMessage(msg.content) : renderMarkdown(msg.content)"></div>
                <span v-if="msg.role === 'user' && msg.score" class="answer-score"><el-icon><DataAnalysis /></el-icon>{{ msg.score }}/10</span>
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

          <div v-show="sidePanelTab === 'coach'" class="coach-panel">
            <div class="coach-heading"><span>训练状态</span><el-icon><DataAnalysis /></el-icon></div>
            <div class="coach-score"><strong>{{ averageScore || '--' }}</strong><span>{{ averageScore ? '当前平均得分' : '完成回答后生成评分' }}</span></div>
            <div class="delivery-panel">
              <div class="delivery-heading"><div><span>DELIVERY SIGNALS</span><strong>表达表现</strong></div><small>{{ deliverySourceLabel }}</small></div>
              <div class="delivery-grid"><div><strong>{{ deliverySpeed || '--' }}</strong><small>字 / 分钟</small></div><div><strong>{{ deliveryFillerCount }}</strong><small>填充词</small></div><div><strong>{{ deliveryAverageDuration || '--' }}</strong><small>平均秒数</small></div></div>
              <p><el-icon><Aim /></el-icon>{{ deliveryTip }}</p>
            </div>
            <div class="coach-stat"><span>训练场景</span><strong>{{ modeLabel }}</strong></div>
            <div class="coach-stat"><span>难度档位</span><strong>{{ difficultyLabel }}</strong></div>
            <div class="coach-stat"><span>反馈风格</span><strong>{{ feedbackStyleLabel }}</strong></div>
            <div class="coach-stat"><span>你的回答</span><strong>{{ answerCount }} 次</strong></div>
            <div class="coach-stat"><span>平均字数</span><strong>{{ averageAnswerLength }} 字</strong></div>
            <div class="coach-stat"><span>回答均分</span><strong>{{ averageScore ? `${averageScore}/10` : '待生成' }}</strong></div>
            <div v-if="userInput.trim() && !sessionReadOnly" class="coach-draft"><span>当前回答预检</span><strong>{{ draftStructureScore }}/3 结构完整度</strong><small>{{ draftTip }}</small></div>
            <div class="coach-tip"><el-icon><Aim /></el-icon><div><strong>回答建议</strong><p>{{ currentTip }}</p></div></div>
            <div class="coach-steps"><span v-for="step in targetRounds" :key="step" :class="{ done: answerCount >= step, active: answerCount + 1 === step && aiTyping }"></span></div>
            <small class="coach-caption">达到目标轮次后结束面试，获得完整评估。</small>
          </div>
        </aside>
      </div>

      <div class="input-bar immersive-input">
        <div class="input-row">
          <el-input v-model="userInput" type="textarea" :rows="3" maxlength="4000" placeholder="先说结论，再说明你的行动、技术选择和结果..."
            :disabled="aiTyping || ending || sessionReadOnly" @keydown.enter.exact.prevent="sendMessage" resize="none" />
          <div class="input-btns">
            <el-button
              :type="listening ? 'danger' : 'default'"
              :disabled="aiTyping || sessionReadOnly || !voiceSupported"
              @click="toggleVoice"
              round
              :title="voiceSupported ? '语音输入' : '浏览器不支持语音'"
            >
              <el-icon><Microphone /></el-icon>{{ listening ? '录音中...' : '语音' }}
            </el-button>
            <el-button @click="skipQuestion" :disabled="aiTyping || sessionReadOnly" round plain>跳过</el-button>
            <el-button v-if="aiTyping" type="warning" @click="cancelStream" round>停止</el-button>
            <el-button v-else type="primary" :disabled="sessionReadOnly || !userInput.trim()" @click="sendMessage" round>发送</el-button>
          </div>
        </div>
        <div class="input-meta"><span>{{ sessionReadOnly ? '该会话已结束' : `${userInput.length} / 4000 字` }}</span><span>{{ sessionReadOnly ? '历史记录中查看完整复盘' : 'Enter 发送 · Shift + Enter 换行' }}</span></div>
        <div v-if="userInput.trim() && !sessionReadOnly" class="draft-signal-row">
          <span class="draft-signal-kicker"><el-icon><Aim /></el-icon>回答预检</span>
          <span class="draft-chip" :class="draftStructureTone">结构 {{ draftStructureScore }}/3</span>
          <span class="draft-chip">约 {{ draftDuration }} 秒</span>
          <span v-if="draftSpeed" class="draft-chip">语速 {{ draftSpeed }} 字/分</span>
          <span class="draft-chip" :class="draftFillerCount > 2 ? 'warn' : 'good'">填充词 {{ draftFillerCount }}</span>
          <small>{{ draftTip }}</small>
        </div>
        <div v-if="sessionReadOnly" class="session-closed"><span>这场面试已经结束，不能继续回答。</span><el-button link type="primary" @click="router.push({ path: '/history', query: { sessionId: sessionId } })">查看复盘</el-button></div>
        <div v-else-if="streamError" class="stream-error">
          <span>{{ streamError }}</span>
          <el-button link type="primary" @click="retryMessage">重试</el-button>
        </div>
      </div>
    </div>

    <ActionConfirmDialog v-model="showEndConfirm" title="确定结束这场面试吗？" description="结束后会锁定当前会话，并根据你的回答生成完整复盘报告。结束后不能继续作答。" kicker="END PRACTICE / 结束训练" confirm-text="结束并生成报告" loading-text="正在生成报告" tone="danger" :loading="ending" @confirm="confirmEndInterview" />

    <!-- 总结 -->
    <el-dialog v-model="showSummary" class="summary-dialog" width="min(860px, calc(100% - 28px))" :close-on-click-modal="false" :show-close="false">
      <template #header>
        <div class="summary-dialog-head"><div class="summary-dialog-title"><span class="summary-success"><el-icon><CircleCheck /></el-icon></span><div><span class="summary-kicker">INTERVIEW COMPLETE</span><h2>这场面试完成了</h2><p>{{ position }} · {{ modeLabel }} · {{ feedbackStyleLabel }} · 用时 {{ elapsedTime }}</p></div></div><button class="summary-close" type="button" aria-label="关闭总结" @click="closeSummary"><el-icon><Close /></el-icon></button></div>
      </template>
      <div class="summary-metrics"><div><span>回答次数</span><strong>{{ answerCount }}</strong><small>次有效回答</small></div><div><span>回答均分</span><strong>{{ averageScore || '--' }}</strong><small>{{ averageScore ? '满分 10 分' : '等待评分' }}</small></div><div><span>训练时长</span><strong>{{ elapsedTime }}</strong><small>{{ difficultyLabel }} · {{ targetRounds }} 轮目标</small></div></div>
      <div v-if="answerMetrics.length" class="summary-delivery"><div><span>表达表现</span><strong>{{ deliverySpeed || '--' }} <small>字/分钟</small></strong></div><div><span>填充词</span><strong>{{ deliveryFillerCount }}</strong></div><div><span>平均回答</span><strong>{{ deliveryAverageDuration || '--' }} <small>秒</small></strong></div></div>
      <div class="summary-content" v-html="renderMarkdown(summary)"></div>
      <template #footer>
        <div class="summary-footer"><span><el-icon><Lock /></el-icon>报告已保存到训练档案</span><div><el-button @click="goHistory">查看复盘<el-icon><ArrowRight /></el-icon></el-button><el-button plain @click="downloadReport"><el-icon><Download /></el-icon>下载报告</el-button><el-button type="primary" @click="goHome">返回工作台</el-button></div></div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { startInterview as apiStart, endInterview as apiEnd, createChatStreamRequest, getErrorMessage, getSessionDetail } from '../api'
import { renderInterviewMessage, renderMarkdown } from '../utils/markdown'
import { Aim, ArrowRight, ChatLineRound, CircleCheck, CircleClose, Close, DataAnalysis, DocumentChecked, Download, Files, FullScreen, Loading, Lock, Microphone, Operation, Service, Suitcase, Timer, User, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ActionConfirmDialog from '../components/ActionConfirmDialog.vue'
import AnimeInterviewer from '../components/AnimeInterviewer.vue'

const route = useRoute()
const router = useRouter()
const interviewPreferenceKey = 'interviewCoach:interview-preferences'
const presencePreferenceKey = 'interviewCoach:presence-preferences'
const readInterviewPreferences = () => {
  try {
    const stored = JSON.parse(localStorage.getItem(interviewPreferenceKey) || '{}')
    return stored && typeof stored === 'object' ? stored : {}
  } catch {
    return {}
  }
}
const savedPreferences = readInterviewPreferences()
const readPresencePreferences = () => {
  try {
    const stored = JSON.parse(localStorage.getItem(presencePreferenceKey) || '{}')
    return stored && typeof stored === 'object' ? stored : {}
  } catch {
    return {}
  }
}
const savedPresencePreferences = readPresencePreferences()
const queryPosition = typeof route.query.position === 'string' ? route.query.position.trim().slice(0, 100) : ''
const position = ref(queryPosition || (typeof savedPreferences.position === 'string' && savedPreferences.position ? savedPreferences.position : 'Java后端开发'))
const interviewMode = ref(['technical', 'project', 'system-design', 'behavioral'].includes(savedPreferences.interviewMode) ? savedPreferences.interviewMode : 'technical')
const difficultyLevel = ref(['junior', 'intermediate', 'senior'].includes(savedPreferences.difficultyLevel) ? savedPreferences.difficultyLevel : 'intermediate')
const targetRounds = ref(Math.min(8, Math.max(3, Number(savedPreferences.targetRounds) || 5)))
const feedbackStyle = ref(['encouraging', 'balanced', 'strict'].includes(savedPreferences.feedbackStyle) ? savedPreferences.feedbackStyle : 'balanced')
const sessionId = ref(null)
const starting = ref(false)
const ending = ref(false)
const focusMode = ref(false)
const immersiveMode = ref(savedPresencePreferences.immersiveMode !== false)
const voicePlaybackEnabled = ref(savedPresencePreferences.voicePlaybackEnabled !== false)
const speechSupported = ref('speechSynthesis' in window && 'SpeechSynthesisUtterance' in window)
const speaking = ref(false)
const currentSpeechText = ref('')
const speechCharIndex = ref(0)
const selectedVoiceId = ref(typeof savedPresencePreferences.selectedVoiceId === 'string' ? savedPresencePreferences.selectedVoiceId : '')
const voicePreviewing = ref(false)
const speechVoices = ref([])
const sidePanelTab = ref('transcript')
const avatarQuality = ref('detecting')
const avatarFallback = ref(false)
const messages = ref([])
const userInput = ref('')
const aiTyping = ref(false)
const showSummary = ref(false)
const showEndConfirm = ref(false)
const summary = ref('')
const chatPanel = ref(null)
const listening = ref(false)
const voiceSupported = ref(false)
const voiceStartedAt = ref(null)
const voiceLastDuration = ref(0)
const voiceDraftText = ref('')
const answerMetrics = ref([])
const connectionState = ref('idle')
const streamError = ref('')
const sessionReadOnly = ref(false)
const lastAnswer = ref('')
const startedAt = ref(null)
const elapsedSeconds = ref(0)
const connectionLabel = computed(() => ({ idle: '待机', connecting: '连接中', streaming: '生成中', error: '连接异常' }[connectionState.value] || '待机'))
const sanitizeInterviewSpeech = value => (value || '')
  .replace(/\[评分[：:]*\s*(?:10|[1-9])\s*\/\s*10\]/g, '')
  .replace(/【面试总结】/g, '')
  .replace(/#{1,6}\s*/g, '')
  .replace(/[*_`>]/g, '')
  .replace(/简短点评\s*[：:]\s*/g, '')
  .replace(/下一问\s*[：:]\s*/g, '\n接下来我想问，')
  .replace(/(?:^|\n)\s*[-•]\s*/g, '\n')
  .replace(/\n{3,}/g, '\n\n')
  .trim()
const speechVoiceId = voice => voice ? `${voice.voiceURI || voice.name}::${voice.lang || ''}` : ''
const isNaturalVoice = voice => /natural|neural|online|xiaoxiao|xiaoyi|xiaohan|xiaomo|xiaorui|xiaoshuang|yunxi|yunyang|yunjian|晓晓|晓伊|晓涵|晓墨|晓睿|晓双|云希|云扬|云健/i.test(voice?.name || '') || voice?.localService === false
const speechVoiceScore = voice => {
  const name = voice?.name || ''
  const lang = voice?.lang || ''
  let score = /^zh-CN/i.test(lang) ? 80 : /^zh/i.test(lang) ? 55 : 0
  if (isNaturalVoice(voice)) score += 120
  if (/xiaoxiao|晓晓/i.test(name)) score += 34
  if (/xiaoyi|晓伊/i.test(name)) score += 28
  if (/yunxi|云希/i.test(name)) score += 25
  if (/microsoft|google/i.test(name)) score += 12
  if (/huihui|tingting|desktop|compact|legacy/i.test(name)) score -= 24
  return score
}
const voiceOptions = computed(() => {
  const chineseVoices = speechVoices.value.filter(voice => /^zh(?:-|_)/i.test(voice.lang || '') || /mandarin|普通话|中文|xiaoxiao|xiaoyi|yunxi|晓晓|晓伊|云希/i.test(voice.name || ''))
  const candidates = chineseVoices.length ? chineseVoices : speechVoices.value
  return [...candidates]
    .sort((left, right) => speechVoiceScore(right) - speechVoiceScore(left) || (left.name || '').localeCompare(right.name || ''))
    .map(voice => ({
      id: speechVoiceId(voice),
      label: `${voice.name} · ${voice.lang || '未知语言'}${isNaturalVoice(voice) ? ' · 自然' : ''}`,
      shortName: (voice.name || '默认声线').replace(/^Microsoft\s+/i, '').replace(/\s*\(.+?\)\s*/g, ' ').trim(),
      lang: voice.lang || '默认',
      natural: isNaturalVoice(voice),
      voice
    }))
})
const selectedVoiceOption = computed(() => voiceOptions.value.find(option => option.id === selectedVoiceId.value) || voiceOptions.value[0])
const selectedVoiceNatural = computed(() => Boolean(selectedVoiceOption.value?.natural))
const selectedVoiceName = computed(() => selectedVoiceOption.value?.shortName || '浏览器默认中文声线')
const voiceQualityHint = computed(() => selectedVoiceNatural.value
  ? `${selectedVoiceName.value} · 已优化停顿和语速`
  : voiceOptions.value.length ? `${selectedVoiceName.value} · 可在右侧切换其他中文声线` : '正在检测浏览器中的中文声线')
const latestAiMessage = computed(() => [...messages.value].reverse().find(message => message.role === 'ai' && !String(message.content || '').startsWith('【面试总结】')))
const latestInterviewerText = computed(() => sanitizeInterviewSpeech(latestAiMessage.value?.content || currentSpeechText.value))
const activeCaptionText = computed(() => currentSpeechText.value || latestInterviewerText.value)
const spokenCaption = computed(() => activeCaptionText.value.slice(0, speechCharIndex.value))
const pendingCaption = computed(() => activeCaptionText.value.slice(speechCharIndex.value))
const speechProgress = computed(() => activeCaptionText.value.length ? Math.min(1, speechCharIndex.value / activeCaptionText.value.length) : 0)
const interviewerState = computed(() => {
  if (sessionReadOnly.value) return 'ended'
  if (streamError.value) return 'error'
  if (ending.value) return 'thinking'
  if (speaking.value) return 'speaking'
  if (listening.value) return 'listening'
  if (aiTyping.value || connectionState.value === 'connecting' || connectionState.value === 'streaming') return 'thinking'
  if (!messages.value.length) return 'preparing'
  return 'idle'
})
const interviewerStateLabel = computed(() => ({
  preparing: '正在准备',
  thinking: '正在思考',
  speaking: '正在提问',
  listening: '正在倾听',
  idle: '等待回答',
  ended: '面试已结束',
  error: '连接异常'
}[interviewerState.value] || '等待回答'))
const speechStatusLabel = computed(() => {
  if (!speechSupported.value) return '语音不可用'
  if (!voicePlaybackEnabled.value) return '自动播报已关闭'
  if (speaking.value) return '正在同步嘴型'
  if (aiTyping.value) return '正在生成追问'
  return selectedVoiceNatural.value ? '自然声线与嘴型已同步' : '字幕与语音已同步'
})
const avatarQualityLabel = computed(() => ({ quality: '动漫 VRM', reduced: '流畅 VRM', detecting: '角色加载中', fallback: '头像模式' }[avatarQuality.value] || '动漫场景'))
const interviewModes = [
  { label: '技术深挖', value: 'technical', desc: '原理、编码、工程取舍', icon: Operation },
  { label: '项目复盘', value: 'project', desc: '贡献、结果、复杂问题', icon: Files },
  { label: '系统设计', value: 'system-design', desc: '容量、架构、边界', icon: DataAnalysis },
  { label: '行为面试', value: 'behavioral', desc: '协作、冲突、STAR 证据', icon: ChatLineRound }
]
const difficultyOptions = [
  { label: '初级', value: 'junior' },
  { label: '中级', value: 'intermediate' },
  { label: '高级', value: 'senior' }
]
const feedbackStyles = [
  { label: '鼓励型', value: 'encouraging', desc: '先看亮点，再温和修正', icon: Aim },
  { label: '平衡型', value: 'balanced', desc: '客观直接，兼顾风险', icon: DataAnalysis },
  { label: '严格型', value: 'strict', desc: '按淘汰标准指出缺口', icon: Operation }
]
const interviewPresets = [
  { key: 'technical-sprint', label: '技术冲刺', desc: '原理深挖 · 5 轮', interviewMode: 'technical', difficultyLevel: 'intermediate', targetRounds: 5, feedbackStyle: 'balanced', icon: Operation },
  { key: 'project-review', label: '项目深挖', desc: '追问取舍 · 高压', interviewMode: 'project', difficultyLevel: 'senior', targetRounds: 5, feedbackStyle: 'strict', icon: Files },
  { key: 'behavior-story', label: '行为表达', desc: 'STAR 结构 · 4 轮', interviewMode: 'behavioral', difficultyLevel: 'intermediate', targetRounds: 4, feedbackStyle: 'encouraging', icon: ChatLineRound }
]
const modeLabel = computed(() => interviewModes.find(item => item.value === interviewMode.value)?.label || '技术深挖')
const difficultyLabel = computed(() => difficultyOptions.find(item => item.value === difficultyLevel.value)?.label || '中级')
const feedbackStyleLabel = computed(() => feedbackStyles.find(item => item.value === feedbackStyle.value)?.label || '平衡型')
const isPresetActive = preset => preset.interviewMode === interviewMode.value && preset.difficultyLevel === difficultyLevel.value && preset.targetRounds === targetRounds.value && preset.feedbackStyle === feedbackStyle.value
const applyPreset = preset => {
  interviewMode.value = preset.interviewMode
  difficultyLevel.value = preset.difficultyLevel
  targetRounds.value = preset.targetRounds
  feedbackStyle.value = preset.feedbackStyle
}
const saveInterviewPreferences = () => {
  localStorage.setItem(interviewPreferenceKey, JSON.stringify({
    position: position.value,
    interviewMode: interviewMode.value,
    difficultyLevel: difficultyLevel.value,
    targetRounds: targetRounds.value,
    feedbackStyle: feedbackStyle.value
  }))
}
const savePresencePreferences = () => {
  localStorage.setItem(presencePreferenceKey, JSON.stringify({
    immersiveMode: immersiveMode.value,
    voicePlaybackEnabled: voicePlaybackEnabled.value,
    selectedVoiceId: selectedVoiceId.value
  }))
}
let speechUtterance = null
let speechSequence = 0
const loadSpeechVoices = () => {
  if (!speechSupported.value) return
  speechVoices.value = window.speechSynthesis.getVoices()
  if (!voiceOptions.value.some(option => option.id === selectedVoiceId.value)) {
    selectedVoiceId.value = voiceOptions.value[0]?.id || ''
    savePresencePreferences()
  }
}
const preferredSpeechVoice = () => selectedVoiceOption.value?.voice || null
const splitSpeechSegments = content => {
  const sentences = (content.replace(/\n+/g, '。').match(/[^。！？!?；;]+[。！？!?；;]?/g) || [content]).map(item => item.trim()).filter(Boolean)
  const result = []
  sentences.forEach(sentence => {
    let remaining = sentence
    while (remaining.length > 68) {
      const windowText = remaining.slice(0, 68)
      const punctuationIndex = Math.max(windowText.lastIndexOf('，'), windowText.lastIndexOf(','), windowText.lastIndexOf('、'), windowText.lastIndexOf('：'), windowText.lastIndexOf(':'))
      const cut = punctuationIndex >= 32 ? punctuationIndex + 1 : 68
      result.push(remaining.slice(0, cut).trim())
      remaining = remaining.slice(cut).trim()
    }
    if (remaining) result.push(remaining)
  })
  return result
}
const stopSpeech = () => {
  speechSequence += 1
  if (speechSupported.value) window.speechSynthesis.cancel()
  if (speechUtterance) {
    speechUtterance.onstart = null
    speechUtterance.onboundary = null
    speechUtterance.onend = null
    speechUtterance.onerror = null
  }
  speechUtterance = null
  voicePreviewing.value = false
  speaking.value = false
  currentSpeechText.value = ''
  speechCharIndex.value = 0
}
const speakInterviewer = text => {
  const content = sanitizeInterviewSpeech(text)
  if (!content || !speechSupported.value || !voicePlaybackEnabled.value || sessionReadOnly.value) return
  stopSpeech()
  const sequence = ++speechSequence
  const segments = splitSpeechSegments(content)
  const voice = preferredSpeechVoice()
  currentSpeechText.value = content
  const speakSegment = (index, offset) => {
    if (sequence !== speechSequence || index >= segments.length) return
    const segment = segments[index]
    const utterance = new SpeechSynthesisUtterance(segment)
    if (voice) utterance.voice = voice
    utterance.lang = voice?.lang || 'zh-CN'
    utterance.rate = isNaturalVoice(voice) ? .98 : .93
    utterance.pitch = isNaturalVoice(voice) ? 1.01 : .98
    utterance.volume = 1
    utterance.onstart = () => {
      if (sequence !== speechSequence) return
      speaking.value = true
      speechCharIndex.value = offset
    }
    utterance.onboundary = event => {
      if (sequence === speechSequence && speechUtterance === utterance && Number.isFinite(event.charIndex)) {
        speechCharIndex.value = Math.min(content.length, offset + event.charIndex)
      }
    }
    utterance.onend = () => {
      if (sequence !== speechSequence || speechUtterance !== utterance) return
      const nextOffset = Math.min(content.length, offset + segment.length)
      speechCharIndex.value = nextOffset
      speechUtterance = null
      if (index < segments.length - 1) {
        window.setTimeout(() => speakSegment(index + 1, nextOffset), 75)
        return
      }
      speaking.value = false
      currentSpeechText.value = ''
      speechCharIndex.value = 0
    }
    utterance.onerror = event => {
      if (sequence !== speechSequence || speechUtterance !== utterance) return
      speaking.value = false
      currentSpeechText.value = ''
      speechCharIndex.value = 0
      speechUtterance = null
      if (event.error && !['canceled', 'interrupted'].includes(event.error)) ElMessage.warning('语音播报暂时不可用，已保留文字字幕')
    }
    speechUtterance = utterance
    window.speechSynthesis.speak(utterance)
  }
  speakSegment(0, 0)
}
const handleVoiceSelection = () => {
  savePresencePreferences()
  if (voicePreviewing.value) stopSpeech()
}
const previewSelectedVoice = () => {
  if (!speechSupported.value) return
  stopSpeech()
  const sequence = ++speechSequence
  const utterance = new SpeechSynthesisUtterance('你好，我是你的模拟面试官林屿。接下来，我会根据你的回答自然追问。准备好后，我们就开始吧。')
  const voice = preferredSpeechVoice()
  if (voice) utterance.voice = voice
  utterance.lang = voice?.lang || 'zh-CN'
  utterance.rate = isNaturalVoice(voice) ? .98 : .93
  utterance.pitch = isNaturalVoice(voice) ? 1.01 : .98
  utterance.onstart = () => { if (sequence === speechSequence) voicePreviewing.value = true }
  const finishPreview = () => {
    if (sequence !== speechSequence) return
    voicePreviewing.value = false
    speechUtterance = null
  }
  utterance.onend = finishPreview
  utterance.onerror = finishPreview
  speechUtterance = utterance
  window.speechSynthesis.speak(utterance)
}
const toggleVoicePlayback = () => {
  if (!speechSupported.value) {
    ElMessage.warning('当前浏览器不支持语音播报')
    return
  }
  voicePlaybackEnabled.value = !voicePlaybackEnabled.value
  savePresencePreferences()
  if (!voicePlaybackEnabled.value) stopSpeech()
  else if (sessionId.value && latestAiMessage.value?.content && !sessionReadOnly.value) speakInterviewer(latestAiMessage.value.content)
}
const toggleImmersiveMode = () => {
  immersiveMode.value = !immersiveMode.value
  savePresencePreferences()
}
const handleAvatarReady = payload => {
  avatarFallback.value = false
  avatarQuality.value = payload?.quality || 'quality'
}
const handleAvatarFallback = () => {
  avatarFallback.value = true
  avatarQuality.value = 'fallback'
}
const answerMessages = computed(() => messages.value.filter(message => message.role === 'user'))
const answerCount = computed(() => answerMessages.value.length)
const currentRound = computed(() => Math.min(targetRounds.value, Math.max(1, answerCount.value + (aiTyping.value ? 1 : 0))))
const averageAnswerLength = computed(() => answerCount.value ? Math.round(answerMessages.value.reduce((sum, item) => sum + (item.content || '').length, 0) / answerCount.value) : 0)
const scoredAnswers = computed(() => answerMessages.value.filter(item => Number.isFinite(item.score)))
const averageScore = computed(() => scoredAnswers.value.length ? (scoredAnswers.value.reduce((sum, item) => sum + item.score, 0) / scoredAnswers.value.length).toFixed(1) : '')
const currentTip = computed(() => answerCount.value === 0 ? '先用 STAR 结构介绍一个最有代表性的项目。' : answerCount.value < 3 ? '先说结论，再补充技术细节和你承担的具体职责。' : '尝试量化结果，并主动说明你的取舍和复盘。')
const analyzeAnswerText = text => {
  const value = (text || '').trim()
  const hanCount = (value.match(/[\u4e00-\u9fff]/g) || []).length
  const latinWords = (value.match(/[A-Za-z0-9]+(?:['’-][A-Za-z0-9]+)?/g) || []).length
  const fillerPattern = /嗯+|呃+|那个|就是|然后|其实|可能|大概|um+|uh+|like/gi
  const fillers = value.match(fillerPattern) || []
  const hasConclusion = /(^|[。！？.!?\n])\s*(结论|核心|我认为|我负责|主要是|可以概括为)/i.test(value) || value.length >= 42
  const hasAction = /(负责|设计|实现|搭建|采用|通过|解决|优化|协作|推动|排查|落地)/i.test(value)
  const hasResult = /(结果|最终|提升|降低|减少|增加|达到|上线|稳定|效率|性能|\d+\s*[%％万千百十]?)/i.test(value)
  const speechUnits = hanCount + latinWords * 2
  const duration = Math.max(1, Math.round(hanCount / 3.4 + latinWords / 2.3))
  return { structureScore: [hasConclusion, hasAction, hasResult].filter(Boolean).length, duration, fillerCount: fillers.length, speechUnits }
}
const draftMetrics = computed(() => {
  return analyzeAnswerText(userInput.value)
})
const draftStructureScore = computed(() => draftMetrics.value.structureScore)
const draftStructureTone = computed(() => draftStructureScore.value >= 3 ? 'good' : draftStructureScore.value === 2 ? 'mid' : 'warn')
const draftDuration = computed(() => draftMetrics.value.duration)
const draftFillerCount = computed(() => draftMetrics.value.fillerCount)
const draftSpeed = computed(() => draftMetrics.value.speechUnits ? Math.round(draftMetrics.value.speechUnits / draftMetrics.value.duration * 60) : 0)
const draftTip = computed(() => {
  if (draftStructureScore.value < 2) return '先给结论，再补充你具体做了什么。'
  if (draftStructureScore.value === 2 && !/(结果|最终|提升|降低|减少|增加|达到|上线|\d+)/i.test(userInput.value)) return '再补一个结果或数据，回答会更有说服力。'
  if (draftFillerCount.value > 2) return '内容已经成形，可以删掉几个“然后 / 就是”再发送。'
  if (draftDuration.value < 15) return '回答略短，补充一个技术取舍或项目细节。'
  return '结构完整，可以发送；记得保持结论先行。'
})
const deliveryStats = computed(() => {
  const items = answerMetrics.value
  const totalSeconds = items.reduce((sum, item) => sum + (item.duration || 0), 0)
  const totalUnits = items.reduce((sum, item) => sum + (item.speechUnits || 0), 0)
  return {
    count: items.length,
    speed: totalSeconds ? Math.round(totalUnits / totalSeconds * 60) : 0,
    fillerCount: items.reduce((sum, item) => sum + (item.fillerCount || 0), 0),
    averageDuration: items.length ? Math.round(totalSeconds / items.length) : 0,
    voiceCount: items.filter(item => item.voice).length
  }
})
const deliverySpeed = computed(() => deliveryStats.value.speed)
const deliveryFillerCount = computed(() => deliveryStats.value.fillerCount)
const deliveryAverageDuration = computed(() => deliveryStats.value.averageDuration)
const deliverySourceLabel = computed(() => deliveryStats.value.voiceCount ? `已记录 ${deliveryStats.value.voiceCount} 次语音` : deliveryStats.value.count ? '文字回答估算' : '等待第一题')
const deliveryTip = computed(() => {
  if (!deliveryStats.value.count) return '完成第一题后，会记录你的语速和填充词。'
  if (deliverySpeed.value > 300) return '语速偏快，试着在结论和结果之间停半拍。'
  if (deliverySpeed.value < 120) return '语速偏慢，可以减少铺垫，先把结论说出来。'
  if (deliveryFillerCount.value >= deliveryStats.value.count * 3) return '填充词偏多，发送前删掉几个“然后 / 就是”，表达会更利落。'
  return deliveryStats.value.voiceCount ? '语速处于稳定区间，继续保持自然停顿。' : '当前是文字回答估算，开启语音输入后会更接近真实表达。'
})
const deliveryMetricsPayload = computed(() => ({ averageSpeakingRate: deliverySpeed.value, fillerWordCount: deliveryFillerCount.value, averageAnswerSeconds: deliveryAverageDuration.value }))
const elapsedTime = computed(() => `${String(Math.floor(elapsedSeconds.value / 60)).padStart(2, '0')}:${String(elapsedSeconds.value % 60).padStart(2, '0')}`)
let abortController = null
let recognition = null
let recognitionBaseText = ''
let timerId = null
let voiceRefreshTimer = null
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
    userInput.value = [recognitionBaseText, text.trim()].filter(Boolean).join(recognitionBaseText ? '\n' : '')
    voiceDraftText.value = text
  }
  recognition.onerror = () => { finalizeVoiceCapture(); listening.value = false }
  recognition.onend = () => { finalizeVoiceCapture(); listening.value = false }
}

const finalizeVoiceCapture = () => {
  if (!voiceStartedAt.value) return
  voiceLastDuration.value = Math.max(1, Math.round((Date.now() - voiceStartedAt.value) / 1000))
  voiceStartedAt.value = null
}
const stopVoiceCapture = () => {
  finalizeVoiceCapture()
  recognition?.stop()
  listening.value = false
}
const toggleVoice = () => {
  if (listening.value) {
    stopVoiceCapture()
  } else {
    try {
      stopSpeech()
      sidePanelTab.value = 'transcript'
      voiceLastDuration.value = 0
      voiceDraftText.value = ''
      recognitionBaseText = userInput.value.trim()
      voiceStartedAt.value = Date.now()
      recognition?.start()
      listening.value = true
    } catch {
      finalizeVoiceCapture()
      ElMessage.warning('语音输入暂时不可用，请稍后重试')
    }
  }
}

// 支持从历史记录继续面试
onMounted(async () => {
  if (speechSupported.value) {
    loadSpeechVoices()
    voiceRefreshTimer = window.setTimeout(loadSpeechVoices, 350)
    window.speechSynthesis.addEventListener?.('voiceschanged', loadSpeechVoices)
  } else {
    voicePlaybackEnabled.value = false
  }
  const resumeId = route.query.sessionId
  if (resumeId) {
    starting.value = true
    try {
      const res = await getSessionDetail(resumeId)
    if (res.data.code === 200) {
        const data = res.data.data
        sessionId.value = data.session.id
        position.value = data.session.position
        interviewMode.value = data.session.interviewMode || 'technical'
        difficultyLevel.value = data.session.difficultyLevel || 'intermediate'
        targetRounds.value = data.session.targetRounds || 5
        feedbackStyle.value = data.session.feedbackStyle || 'balanced'
        messages.value = data.messages || []
        answerMetrics.value = messages.value.filter(message => message.role === 'user').map(message => analyzeAnswerText(message.content))
        sessionReadOnly.value = data.session.status === 'completed'
        const sessionStart = data.session.createdAt ? new Date(data.session.createdAt).getTime() : Date.now()
        if (sessionReadOnly.value) {
          clearInterval(timerId)
          elapsedSeconds.value = Math.max(0, Math.floor((Date.now() - sessionStart) / 1000))
          connectionState.value = 'idle'
        } else {
          beginTimer(sessionStart)
        }
        ElMessage.success('已恢复面试')
        await nextTick()
        scroll()
      }
    } catch (e) { ElMessage.error(getErrorMessage(e, '恢复失败')) }
    finally { starting.value = false }
  }
})

onBeforeRouteLeave(async () => {
  if (!sessionId.value || sessionReadOnly.value || ending.value) return true
  try {
    await ElMessageBox.confirm('离开后本场面试会保留为进行中，你可以从历史记录继续。确定现在离开吗？', '离开面试现场', {
      confirmButtonText: '离开并保留进度', cancelButtonText: '继续面试', type: 'warning', closeOnClickModal: false, distinguishCancelAndClose: true
    })
    stopSpeech()
    stopVoiceCapture()
    return true
  } catch {
    return false
  }
})

const startInterview = async () => {
  starting.value = true
  try {
    const res = await apiStart({ position: position.value, interviewMode: interviewMode.value, difficultyLevel: difficultyLevel.value, targetRounds: targetRounds.value, feedbackStyle: feedbackStyle.value })
    if (res.data.code === 200) {
      sessionId.value = res.data.data.sessionId
      sessionReadOnly.value = false
      streamError.value = ''
      connectionState.value = 'idle'
      interviewMode.value = res.data.data.interviewMode
      difficultyLevel.value = res.data.data.difficultyLevel
      targetRounds.value = res.data.data.targetRounds
      feedbackStyle.value = res.data.data.feedbackStyle || feedbackStyle.value
      saveInterviewPreferences()
      savePresencePreferences()
      answerMetrics.value = []
      beginTimer()
      const firstQuestion = res.data.data.firstQuestion
      messages.value.push({ role: 'ai', content: firstQuestion })
      sidePanelTab.value = 'transcript'
      ElMessage.success('面试开始')
      await scroll()
      speakInterviewer(firstQuestion)
    } else ElMessage.error(res.data.message)
  } catch (e) { ElMessage.error(getErrorMessage(e)) }
  finally { starting.value = false }
}

const sendMessage = async (recordMetric = true) => {
  const a = userInput.value.trim()
  if (!a || aiTyping.value || sessionReadOnly.value) return
  stopSpeech()
  if (listening.value) stopVoiceCapture()
  if (recordMetric) {
    const voice = Boolean(voiceDraftText.value && voiceLastDuration.value && (a === voiceDraftText.value || a.includes(voiceDraftText.value) || voiceDraftText.value.includes(a)))
    const draft = analyzeAnswerText(a)
    answerMetrics.value.push({ ...draft, duration: voice ? voiceLastDuration.value : draft.duration, voice })
  }
  voiceDraftText.value = ''
  voiceLastDuration.value = 0
  streamError.value = ''
  lastAnswer.value = a
  messages.value.push({ role: 'user', content: a }); userInput.value = ''; aiTyping.value = true; await scroll()

  const streamRequest = createChatStreamRequest(sessionId.value, a)
  abortController = new AbortController()
  const headers = { ...streamRequest.options.headers }
  const token = localStorage.getItem('token')
  if (token) headers['Authorization'] = `Bearer ${token}`

  try {
  connectionState.value = 'connecting'
  const resp = await fetch(streamRequest.url, { ...streamRequest.options, signal: abortController.signal, headers })
    if (resp.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      window.dispatchEvent(new CustomEvent('interview-coach:auth-expired'))
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
    connectionState.value = 'idle'
    speakInterviewer(text)
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
  stopSpeech()
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'ai' && !last.content) messages.value.pop()
  connectionState.value = 'idle'
}
const retryMessage = async () => {
  if (!lastAnswer.value || aiTyping.value) return
  userInput.value = lastAnswer.value
  await sendMessage(false)
}
const skipQuestion = async () => { if (!aiTyping.value) { userInput.value = '请换一个问题'; await sendMessage(false) } }
const endInterview = () => {
  if (!ending.value && !aiTyping.value) showEndConfirm.value = true
}
const confirmEndInterview = async () => {
  showEndConfirm.value = false
  ending.value = true
  stopSpeech()
  if (listening.value) stopVoiceCapture()
  try {
    const res = await apiEnd(sessionId.value, deliveryMetricsPayload.value)
    if (res.data.code !== 200) throw new Error(res.data.message || '结束面试失败')
    clearInterval(timerId)
    sessionReadOnly.value = true
    focusMode.value = false
    connectionState.value = 'idle'
    summary.value = res.data.data.summary || ''
    showSummary.value = true
    ElMessage.success('面试已结束，复盘报告已生成')
  } catch (e) { ElMessage.error(getErrorMessage(e)) }
  finally { ending.value = false }
}
const closeSummary = () => {
  showSummary.value = false
  sessionReadOnly.value = true
}
const goHistory = () => {
  const finishedSessionId = sessionId.value
  showSummary.value = false
  resetRoomState()
  router.push({ path: '/history', query: finishedSessionId ? { sessionId: finishedSessionId } : {} })
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
const resetRoomState = () => {
  stopSpeech()
  if (listening.value) stopVoiceCapture()
  clearInterval(timerId)
  sessionId.value = null
  sessionReadOnly.value = false
  focusMode.value = false
  feedbackStyle.value = 'balanced'
  messages.value = []
  summary.value = ''
  userInput.value = ''
  streamError.value = ''
  connectionState.value = 'idle'
  lastAnswer.value = ''
  startedAt.value = null
  elapsedSeconds.value = 0
  voiceStartedAt.value = null
  voiceLastDuration.value = 0
  voiceDraftText.value = ''
  answerMetrics.value = []
  sidePanelTab.value = 'transcript'
}
const goHome = () => {
  resetRoomState()
  router.push('/')
}
const scroll = async () => { await nextTick(); if (chatPanel.value) chatPanel.value.scrollTop = chatPanel.value.scrollHeight }
onBeforeUnmount(() => {
  if (abortController) abortController.abort()
  clearInterval(timerId)
  clearTimeout(voiceRefreshTimer)
  stopSpeech()
  recognition?.abort?.()
  window.speechSynthesis?.removeEventListener?.('voiceschanged', loadSpeechVoices)
})
</script>

<style scoped>
.interview-page { max-width: 1260px; margin: 0 auto; animation: fadeUp .4s ease; color: var(--text); }
@keyframes fadeUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }

/* Start */
.resume-loading { width: min(560px, 100%); min-height: calc(100vh - 150px); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; margin: 0 auto; padding: 30px; text-align: center; }
.loading-orb { width: 58px; height: 58px; display: grid; place-items: center; margin-bottom: 5px; border-radius: 50%; background: var(--blue-light); color: var(--blue); font-size: 25px; }
.loading-orb .el-icon { animation: spin 1s linear infinite; }
.resume-loading h2 { margin-top: 2px; font-size: 21px; }
.resume-loading p { color: var(--text-muted); font-size: 12px; }
.resume-loading :deep(.el-skeleton) { width: min(390px, 100%); margin-top: 13px; }
.start-view { min-height: calc(100vh - 110px); display: grid; grid-template-columns: minmax(0,1.1fr) minmax(380px,.75fr); align-items: center; gap: clamp(40px,7vw,100px); padding: 42px 3vw; }
.start-intro { max-width: 650px; }.eyebrow { display: inline-flex; align-items: center; gap: 7px; color: var(--blue); font-size: 11px; font-weight: 800; letter-spacing: .04em; }.eyebrow i { width: 7px; height: 7px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent) 14%, transparent); }.start-intro h1 { margin: 14px 0 20px; font-size: clamp(38px,4vw,56px); line-height: 1.12; }.start-intro > p { max-width: 600px; color: var(--text-secondary); font-size: 15px; line-height: 1.8; }.interview-features { display: grid; grid-template-columns: repeat(3,1fr); gap: 12px; margin-top: 34px; }.interview-features > div { display: flex; align-items: center; gap: 10px; padding: 14px 0; border-top: 1px solid var(--border); }.interview-features .el-icon { color: var(--blue); font-size: 20px; }.interview-features span,.interview-features strong,.interview-features small { display: block; }.interview-features strong { font-size: 11px; }.interview-features small { margin-top: 3px; color: var(--text-muted); font-size: 9px; }
.start-config { padding: 28px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); box-shadow: var(--shadow); }.config-head { display: flex; align-items: center; gap: 12px; margin-bottom: 26px; }.config-head > span { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 7px; background: var(--blue-light); color: var(--blue); font-size: 20px; }.config-head strong,.config-head small { display: block; }.config-head strong { font-size: 15px; }.config-head small { margin-top: 4px; color: var(--text-muted); font-size: 10px; }.start-config label { display: block; margin-bottom: 8px; color: var(--text-secondary); font-size: 11px; font-weight: 700; }.position-pick { margin-bottom: 18px; }.position-pick :deep(.el-select) { width: 100%; }
.mode-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 18px; }.mode-grid button { display: flex; align-items: center; gap: 9px; min-height: 62px; padding: 10px; border: 1px solid var(--border); border-radius: 7px; background: transparent; color: var(--text); text-align: left; cursor: pointer; }.mode-grid button:hover,.mode-grid button.active { border-color: var(--blue); background: var(--blue-light); }.mode-grid .el-icon { flex: 0 0 20px; color: var(--blue); font-size: 18px; }.mode-grid span,.mode-grid strong,.mode-grid small { display: block; }.mode-grid strong { font-size: 12px; }.mode-grid small { margin-top: 3px; color: var(--text-muted); font-size: 9px; line-height: 1.35; }
.split-config { display: grid; grid-template-columns: 1fr 120px; gap: 12px; margin-bottom: 18px; }.split-config :deep(.el-segmented) { width: 100%; }.split-config :deep(.el-input-number) { width: 100%; }
.feedback-choice { margin: -2px 0 18px; padding: 14px; border: 1px solid color-mix(in srgb, var(--blue) 14%, var(--border)); border-radius: 14px; background: color-mix(in srgb, var(--blue-light) 28%, var(--bg-card)); }
.feedback-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 10px; }.feedback-heading label { margin-bottom: 3px; }.feedback-heading small { display: block; color: var(--text-muted); font-size: 9px; line-height: 1.4; }.feedback-heading > span { display: inline-flex; align-items: center; gap: 4px; padding: 4px 7px; border-radius: 999px; background: var(--blue-light); color: var(--blue); font-size: 9px; font-weight: 700; white-space: nowrap; }.feedback-heading > span .el-icon { font-size: 11px; }
.feedback-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 7px; }.feedback-grid button { position: relative; display: flex; align-items: flex-start; gap: 7px; min-width: 0; min-height: 66px; padding: 10px 9px; border: 1px solid var(--border); border-radius: 10px; background: color-mix(in srgb, var(--bg-card) 70%, transparent); color: var(--text); text-align: left; cursor: pointer; transition: transform .2s, border-color .2s, background .2s, box-shadow .2s; }.feedback-grid button:hover { transform: translateY(-1px); border-color: color-mix(in srgb, var(--blue) 48%, var(--border)); }.feedback-grid button.active { border-color: var(--blue); background: var(--bg-card); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--blue) 18%, transparent), 0 8px 18px color-mix(in srgb, var(--blue) 9%, transparent); }.feedback-icon { display: grid; flex: 0 0 23px; width: 23px; height: 23px; place-items: center; border-radius: 7px; background: var(--hover); color: var(--blue); font-size: 13px; }.feedback-copy { min-width: 0; }.feedback-copy strong, .feedback-copy small { display: block; }.feedback-copy strong { font-size: 11px; }.feedback-copy small { margin-top: 3px; color: var(--text-muted); font-size: 9px; line-height: 1.35; }.feedback-check { position: absolute; top: 8px; right: 7px; color: var(--blue); font-size: 13px; opacity: 0; transform: scale(.7); transition: opacity .2s, transform .2s; }.feedback-grid button.active .feedback-check { opacity: 1; transform: scale(1); }
.session-plan { padding: 15px; margin-bottom: 20px; border: 1px solid var(--border); border-radius: 7px; background: var(--hover); }.session-plan > span { color: var(--text-muted); font-size: 9px; }.session-plan > div { display: flex; align-items: center; margin-top: 11px; }.session-plan i { width: 21px; height: 21px; display: grid; place-items: center; border-radius: 50%; background: var(--blue-light); color: var(--blue); font-size: 9px; font-style: normal; }.session-plan b { margin-left: 5px; font-size: 9px; white-space: nowrap; }.session-plan em { height: 1px; flex: 1; margin: 0 8px; background: var(--border); }.go-btn { width: 100%; height: 46px; border-radius: 7px; font-weight: 700; }.config-note { display: flex; align-items: center; justify-content: center; gap: 5px; margin-top: 13px; color: var(--text-muted); font-size: 9px; }

/* Room */
.room { animation: fadeUp .3s ease; }
.top-bar { display: flex; justify-content: space-between; align-items: center; padding: 4px 0 18px; }
.bar-l { display: flex; align-items: center; gap: 14px; }
.room-brand { display: flex; align-items: center; gap: 10px; }.room-brand > div { display: flex; flex-direction: column; gap: 2px; }.room-brand strong { color: var(--text); font-size: 15px; }.room-brand small { color: var(--text-muted); font-size: 11px; }.live-dot { width: 10px; height: 10px; border-radius: 50%; background: #36a269; box-shadow: 0 0 0 4px rgba(54,162,105,.13); }
.round-progress { display: flex; align-items: center; gap: 8px; min-width: 132px; padding-left: 12px; border-left: 1px solid var(--border); }.round-progress > span { color: var(--text-muted); font-size: 10px; }.round-progress strong { color: var(--blue); font: 700 17px ui-monospace, monospace; }.round-progress strong small { color: var(--text-muted); font-size: 10px; }.round-progress i { width: 55px; height: 4px; overflow: hidden; border-radius: 3px; background: var(--border); }.round-progress b { display: block; height: 100%; background: var(--blue); transition: width .3s ease; }
.mode-chip { max-width: 190px; overflow: hidden; padding: 4px 8px; border: 1px solid var(--border); border-radius: 999px; color: var(--text-secondary); font-size: 11px; background: var(--bg-card); text-overflow: ellipsis; white-space: nowrap; }
.connection-state { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; color: #909399; }
.connection-state i { width: 7px; height: 7px; border-radius: 50%; background: #909399; }
.connection-state.connecting i, .connection-state.streaming i { background: #409EFF; animation: pulse 1.2s infinite; }
.connection-state.error { color: #f56c6c; }
.connection-state.error i { background: #f56c6c; }
.room-actions { display: flex; align-items: center; gap: 12px; }.save-state { display: inline-flex; align-items: center; gap: 4px; color: var(--success); font-size: 10px; white-space: nowrap; }.save-state .el-icon { font-size: 12px; }.timer { display: flex; align-items: center; gap: 5px; color: var(--text-muted); font: 600 12px ui-monospace, SFMono-Regular, Menlo, monospace; }

/* Chat */
.chat-panel {
  background: var(--bg-card); border: 1px solid var(--border); border-radius: 8px; padding: 28px 30px; height: calc(100vh - 315px); min-height: 440px;
  overflow-y: auto; margin-bottom: 14px;
  display: flex; flex-direction: column; gap: 18px;
}
.room-grid { display: grid; grid-template-columns: minmax(0, 1fr) 270px; gap: 18px; align-items: start; }
.room-grid .chat-panel { grid-column: 1; grid-row: 1; }
.room-grid .input-bar { grid-column: 1; grid-row: 2; }
.coach-panel { grid-column: 2; grid-row: 1 / span 2; min-height: 440px; padding: 20px; border: 1px solid var(--border); border-radius: 8px; background: color-mix(in srgb, var(--bg-card) 94%, var(--blue-light)); position: sticky; top: 30px; box-shadow: 0 14px 30px rgba(23,32,42,.05); }
.coach-heading { display: flex; justify-content: space-between; align-items: center; color: var(--text); font-size: 13px; font-weight: 700; }.coach-heading .el-icon { color: var(--blue); font-size: 18px; }
.coach-score { display: flex; flex-direction: column; align-items: center; padding: 22px 0 18px; margin: 10px 0 14px; border-bottom: 1px solid var(--border); }.coach-score strong { color: var(--blue); font: 700 38px ui-monospace, monospace; line-height: 1; }.coach-score span { margin-top: 6px; color: var(--text-muted); font-size: 11px; }
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
.answer-score { display: inline-flex; align-items: center; gap: 4px; margin-top: 6px; padding: 3px 7px; border: 1px solid color-mix(in srgb, var(--blue) 22%, var(--border)); border-radius: 4px; background: var(--blue-light); color: var(--blue); font-size: 10px; font-weight: 700; }
.msg.user .answer-score { float: right; clear: both; }

.msg-bubble :deep(p) { margin: 0 0 10px; }.msg-bubble :deep(p:last-child) { margin-bottom: 0; }
.msg-bubble :deep(h1),.msg-bubble :deep(h2),.msg-bubble :deep(h3),.msg-bubble :deep(h4) { margin: 10px 0 5px; font: inherit; font-weight: 700; color: inherit; }
.msg-bubble :deep(strong) { font-weight: 700; }
.msg-bubble :deep(ul), .msg-bubble :deep(ol) { padding-left: 20px; margin: 6px 0; }
.msg-bubble :deep(li) { margin: 4px 0; line-height: 1.7; }
.msg-bubble :deep(li::marker) { color: var(--blue); }.msg-bubble :deep(code) { background: transparent; padding: 0; border-radius: 0; color: inherit; font: inherit; font-weight: 600; }
.msg.user .msg-bubble :deep(h3) { color: #fff; border-bottom-color: rgba(255,255,255,.25); }
.msg.user .msg-bubble :deep(code) { background: rgba(255,255,255,.2); color: #fff; }

.typing span { display: inline-block; width: 7px; height: 7px; border-radius: 50%; background: #bbb; margin-right: 4px; animation: bounce 1.4s ease infinite; }
.typing span:nth-child(2) { animation-delay: .2s; }
.typing span:nth-child(3) { animation-delay: .4s; }
@keyframes bounce { 0%,60%,100%{transform:translateY(0)} 30%{transform:translateY(-5px)} }

/* Input */
.input-bar { background: var(--bg-card); border: 1px solid var(--border); border-radius: 8px; padding: 14px 16px 10px; box-shadow: 0 12px 26px rgba(23,32,42,.04); }
.input-row { display: flex; gap: 12px; align-items: flex-end; }
.input-row :deep(.el-textarea) { flex: 1; }
.input-btns { display: flex; gap: 8px; flex-shrink: 0; }
.input-meta { display: flex; justify-content: space-between; margin-top: 7px; color: var(--text-muted); font-size: 9px; }
.stream-error { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; padding: 8px 10px; color: #f56c6c; background: #fef0f0; border-radius: 6px; font-size: 13px; }
.session-closed { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 8px; padding: 9px 11px; border: 1px solid color-mix(in srgb, var(--blue) 18%, var(--border)); border-radius: 6px; background: var(--blue-light); color: var(--text-secondary); font-size: 11px; }

/* Summary */
.summary-content { line-height: 2; font-size: 14px; color: var(--text); }.summary-content :deep(h3) { margin: 16px 0 8px; color: var(--text); padding-bottom: 4px; border-bottom: 1px solid var(--border); }
.summary-content :deep(strong) { font-weight: 700; }
.summary-content :deep(ul) { padding-left: 20px; }
.summary-content :deep(li) { margin: 6px 0; }
.end-interview-btn { min-width: 112px; border-radius: 6px; font-size: 11px; font-weight: 700; }
.end-interview-btn:not(:disabled):hover { box-shadow: 0 8px 18px rgba(245,108,108,.16); }
.finished-interview-btn { min-width: 138px; border-radius: 6px; font-size: 11px; font-weight: 700; }
.summary-dialog-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.summary-dialog-title { display: flex; align-items: center; gap: 12px; }
.summary-success { width: 42px; height: 42px; display: grid; flex: 0 0 42px; place-items: center; border-radius: 50%; background: #e8f6ee; color: var(--success); font-size: 22px; }
.summary-kicker { color: var(--blue); font: 800 9px ui-monospace,monospace; letter-spacing: .08em; }
.summary-dialog-title h2 { margin: 6px 0 4px; color: var(--text); font-size: 21px; }
.summary-dialog-title p { color: var(--text-muted); font-size: 11px; }
.summary-close { width: 30px; height: 30px; display: grid; place-items: center; border: 0; border-radius: 6px; background: transparent; color: var(--text-muted); cursor: pointer; }
.summary-close:hover { background: var(--hover); color: var(--text); }
.summary-metrics { display: grid; grid-template-columns: repeat(3, 1fr); margin: 4px 0 18px; border: 1px solid var(--border); border-radius: 8px; overflow: hidden; background: var(--hover); }
.summary-metrics > div { min-width: 0; padding: 14px 16px; border-right: 1px solid var(--border); }
.summary-metrics > div:last-child { border-right: 0; }
.summary-metrics span, .summary-metrics strong, .summary-metrics small { display: block; }
.summary-metrics span { color: var(--text-muted); font-size: 10px; }
.summary-metrics strong { margin: 6px 0 3px; color: var(--blue); font: 700 21px ui-monospace,monospace; }
.summary-metrics small { overflow: hidden; color: var(--text-muted); text-overflow: ellipsis; white-space: nowrap; font-size: 9px; }
.summary-content { max-height: min(48vh, 460px); overflow-y: auto; padding: 18px 20px 22px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); }
.summary-footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.summary-footer > span { display: flex; align-items: center; gap: 5px; color: var(--text-muted); font-size: 10px; }
.summary-footer > span .el-icon { color: var(--success); }
.summary-footer > div { display: flex; align-items: center; gap: 8px; }
.summary-footer :deep(.el-button) { margin-left: 0 !important; font-size: 11px; }
:global(.summary-dialog .el-dialog) { overflow: hidden; border-radius: 12px; background: var(--bg-card); }
:global(.summary-dialog .el-dialog__header) { margin: 0; padding: 22px 24px 0; }
:global(.summary-dialog .el-dialog__body) { padding: 18px 24px 8px; }
:global(.summary-dialog .el-dialog__footer) { padding: 0 24px 22px; }
@media (max-width: 1050px) {
  .start-view { grid-template-columns: 1fr; max-width: 760px; margin: 0 auto; gap: 30px; padding: 28px 12px; }
  .start-intro { max-width: none; }
  .start-intro h1 { font-size: clamp(34px, 5vw, 48px); }
  .room-grid { display: flex; flex-direction: column; }
  .coach-panel { order: 0; width: 100%; min-height: 0; position: static; }
  .chat-panel { order: 1; width: 100%; }
  .input-bar { order: 2; width: 100%; }
}
@media (max-width: 600px) {
  .start-view { display: block; min-height: 0; padding: 8px 0 20px; }.start-intro h1 { font-size: 32px; }.start-intro > p { font-size: 13px; }.interview-features { grid-template-columns: 1fr; gap: 0; margin: 24px 0; }.interview-features > div { padding: 10px 0; }.start-config { padding: 22px 18px; }.mode-grid,.split-config,.feedback-grid { grid-template-columns: 1fr; }.feedback-choice { padding: 12px; }.feedback-grid button { min-height: 54px; }
  .top-bar { align-items: flex-start; flex-wrap: wrap; gap: 10px; }
  .bar-l { min-width: 0; flex: 1; flex-wrap: wrap; gap: 8px; }
  .room-actions { margin-left: auto; }
  .room-actions .el-button { font-size: 11px; }
  .round-progress { order: 3; width: 100%; min-width: 0; padding-left: 0; border-left: 0; }
  .round-progress i { flex: 1; }
  .room-actions .timer, .room-actions .save-state { display: none; }.room-grid { display: flex; flex-direction: column; }.coach-panel { order: 0; width: 100%; min-height: auto; padding: 12px 14px; position: static; }.coach-heading { margin-bottom: 0; }.coach-score, .coach-stat, .coach-tip, .coach-steps, .coach-caption { display: none; }.chat-panel { order: 1; width: 100%; height: 55vh; min-height: 320px; padding: 16px 12px; }.input-bar { order: 2; width: 100%; }
  .input-bar { padding: 12px; }
  .input-row { display: block; }
  .input-btns { justify-content: flex-end; margin-top: 8px; flex-wrap: wrap; }
  .input-meta { flex-wrap: wrap; gap: 4px 10px; line-height: 1.4; }
  .msg-bubble { padding: 10px 12px; font-size: 14px; }
  .end-interview-btn, .finished-interview-btn { min-width: 0; padding-inline: 9px; }
  .summary-dialog-title h2 { font-size: 18px; }
  .summary-dialog-title p { max-width: 210px; line-height: 1.5; }
  .summary-metrics > div { padding: 12px 10px; }
  .summary-metrics strong { font-size: 18px; }
  .summary-footer { align-items: stretch; flex-direction: column; }
  .summary-footer > div { display: grid; grid-template-columns: 1fr 1fr; }
  .summary-footer > div .el-button:last-child { grid-column: 1 / -1; }
  :global(.summary-dialog .el-dialog__header) { padding: 18px 16px 0; }
  :global(.summary-dialog .el-dialog__body) { padding: 16px 16px 6px; }
  :global(.summary-dialog .el-dialog__footer) { padding: 0 16px 18px; }
}
/* ======== Interview room premium layer ======== */
.start-config {
  border-radius: 22px;
  border-color: color-mix(in srgb, var(--blue) 15%, var(--border));
  background: linear-gradient(145deg, var(--surface-glass), color-mix(in srgb, var(--blue-light) 24%, var(--bg-card)));
  box-shadow: 0 30px 82px rgba(16, 54, 47, .13);
  backdrop-filter: blur(18px);
}
.config-head > span { border-radius: 12px; box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--blue) 14%, transparent); }
.mode-grid button { border-radius: 12px; background: color-mix(in srgb, var(--bg-card) 58%, transparent); transition: transform .2s, border-color .2s, background .2s, box-shadow .2s; }
.mode-grid button:hover { transform: translateY(-2px); box-shadow: 0 12px 26px color-mix(in srgb, var(--blue) 10%, transparent); }
.mode-grid button.active { background: linear-gradient(135deg, color-mix(in srgb, var(--blue-light) 92%, var(--bg-card)), var(--bg-card)); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--blue) 12%, transparent); }
.session-plan { border-radius: 13px; background: color-mix(in srgb, var(--blue-light) 54%, var(--bg-card)); }
.top-bar { padding-bottom: 22px; }
.chat-panel {
  border-radius: 18px;
  border-color: color-mix(in srgb, var(--blue) 13%, var(--border));
  background: var(--surface-glass);
  box-shadow: 0 24px 64px rgba(16, 54, 47, .075);
  backdrop-filter: blur(16px);
}
.coach-panel {
  border-radius: 18px;
  border-color: color-mix(in srgb, var(--blue) 13%, var(--border));
  background: linear-gradient(160deg, color-mix(in srgb, var(--blue-light) 48%, var(--bg-card)), var(--surface-glass));
  box-shadow: 0 22px 54px rgba(16, 54, 47, .075);
  backdrop-filter: blur(16px);
}
.coach-score { margin-inline: 3px; }
.coach-tip { border-radius: 12px; box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--blue) 10%, transparent); }
.msg-bubble { border-radius: 14px; box-shadow: 0 8px 20px rgba(16, 54, 47, .045); }
.msg.ai .msg-bubble { border-top-left-radius: 4px; }
.msg.user .msg-bubble { border-top-right-radius: 4px; box-shadow: 0 10px 24px color-mix(in srgb, var(--text) 13%, transparent); }
.input-bar { border-radius: 16px; background: var(--surface-glass); box-shadow: 0 18px 42px rgba(16, 54, 47, .075); backdrop-filter: blur(16px); }
.input-row :deep(.el-textarea__inner) { border-radius: 12px; }
.summary-metrics, .summary-content { border-radius: 14px; }
.summary-success { box-shadow: 0 10px 22px color-mix(in srgb, var(--success) 16%, transparent); }
@media (max-width: 600px) {
  .start-config { border-radius: 18px; box-shadow: 0 22px 54px rgba(16, 54, 47, .11); }
  .chat-panel, .coach-panel { border-radius: 14px; }
  .input-bar { border-radius: 14px; }
}
/* ======== Answer coaching signals ======== */
.draft-signal-row { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-top: 10px; padding-top: 10px; border-top: 1px solid var(--border); }
.draft-signal-kicker { display: inline-flex; align-items: center; gap: 4px; margin-right: 3px; color: var(--blue); font-size: 10px; font-weight: 800; }
.draft-signal-kicker .el-icon { font-size: 12px; }
.draft-chip { display: inline-flex; align-items: center; min-height: 22px; padding: 0 8px; border: 1px solid var(--border); border-radius: 999px; background: color-mix(in srgb, var(--bg-card) 62%, transparent); color: var(--text-secondary); font-size: 9px; }
.draft-chip.good { border-color: color-mix(in srgb, var(--success) 30%, var(--border)); background: color-mix(in srgb, var(--success) 8%, var(--bg-card)); color: var(--success); }
.draft-chip.mid { border-color: color-mix(in srgb, #c88b1b 32%, var(--border)); background: color-mix(in srgb, #c88b1b 8%, var(--bg-card)); color: #b87813; }
.draft-chip.warn { border-color: color-mix(in srgb, var(--accent) 32%, var(--border)); background: color-mix(in srgb, var(--accent) 8%, var(--bg-card)); color: var(--accent); }
.draft-signal-row small { flex: 1 1 100%; color: var(--text-muted); font-size: 9px; line-height: 1.5; }
.coach-draft { display: flex; flex-direction: column; gap: 5px; margin: 15px 0 2px; padding: 12px; border: 1px solid color-mix(in srgb, var(--blue) 16%, var(--border)); border-radius: 12px; background: color-mix(in srgb, var(--blue-light) 40%, var(--bg-card)); }
.coach-draft span { color: var(--text-muted); font-size: 9px; }
.coach-draft strong { color: var(--blue); font-size: 12px; }
.coach-draft small { color: var(--text-secondary); font-size: 10px; line-height: 1.5; }
.delivery-panel { margin: 8px 0 14px; padding: 12px; border: 1px solid color-mix(in srgb, var(--accent) 19%, var(--border)); border-radius: 13px; background: linear-gradient(145deg, color-mix(in srgb, var(--accent) 8%, var(--bg-card)), color-mix(in srgb, var(--blue-light) 35%, var(--bg-card))); }
.delivery-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; }.delivery-heading > div { display: flex; flex-direction: column; gap: 3px; }.delivery-heading span { color: var(--accent); font: 800 8px ui-monospace, monospace; letter-spacing: .08em; }.delivery-heading strong { color: var(--text); font-size: 12px; }.delivery-heading small { color: var(--text-muted); font-size: 9px; text-align: right; }
.delivery-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 5px; margin: 11px 0 10px; }.delivery-grid > div { min-width: 0; padding: 8px 5px; border-radius: 8px; background: color-mix(in srgb, var(--bg-card) 65%, transparent); text-align: center; }.delivery-grid strong, .delivery-grid small { display: block; }.delivery-grid strong { overflow: hidden; color: var(--blue); font: 700 16px ui-monospace, monospace; text-overflow: ellipsis; white-space: nowrap; }.delivery-grid small { margin-top: 3px; color: var(--text-muted); font-size: 8px; }.delivery-panel > p { display: flex; gap: 5px; margin: 0; color: var(--text-secondary); font-size: 9px; line-height: 1.5; }.delivery-panel > p .el-icon { flex: 0 0 auto; margin-top: 1px; color: var(--accent); }
.summary-delivery { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin: 0 0 14px; }.summary-delivery > div { padding: 11px 13px; border: 1px solid color-mix(in srgb, var(--blue) 14%, var(--border)); border-radius: 10px; background: color-mix(in srgb, var(--blue-light) 24%, var(--bg-card)); }.summary-delivery span, .summary-delivery strong { display: block; }.summary-delivery span { color: var(--text-muted); font-size: 9px; }.summary-delivery strong { margin-top: 5px; color: var(--blue); font: 700 17px ui-monospace, monospace; }.summary-delivery strong small { color: var(--text-muted); font-size: 9px; font-weight: 500; }
@media (max-width: 600px) {
  .draft-signal-row { align-items: flex-start; }
  .draft-signal-row small { flex-basis: 100%; }
  .delivery-panel { margin: 9px 0 0; }.delivery-grid { gap: 4px; }.delivery-grid strong { font-size: 14px; }.summary-delivery { gap: 5px; }.summary-delivery > div { padding: 9px 8px; }.summary-delivery strong { font-size: 14px; }
}

/* ======== Focus mode ======== */
.focus-toggle { min-height: 32px; padding-inline: 8px; border-radius: 9px; color: var(--text-muted); font-size: 10px; }
.focus-toggle:hover, .focus-toggle[aria-pressed='true'] { background: var(--blue-light); color: var(--blue); }
.focus-mode { max-width: 1440px; }
.focus-mode .top-bar { max-width: 1120px; margin-inline: auto; }
.focus-mode .room-grid { grid-template-columns: minmax(0, 1040px); max-width: 1040px; margin-inline: auto; }
.focus-mode .coach-panel { display: none; }
.focus-mode .input-bar { max-width: 1040px; margin-inline: auto; }
@media (min-width: 601px) {
  .focus-mode .chat-panel { min-height: calc(100vh - 272px); }
}
@media (max-width: 600px) {
  .focus-toggle { padding-inline: 6px; }
  .focus-toggle span { display: none; }
  .focus-mode .top-bar { padding-bottom: 12px; }
  .focus-mode .chat-panel { height: 62vh; }
}

/* ======== Quick start presets ======== */
.preset-block { margin: -7px 0 23px; padding: 13px; border: 1px solid color-mix(in srgb, var(--blue) 16%, var(--border)); border-radius: 14px; background: color-mix(in srgb, var(--blue-light) 22%, var(--bg-card)); }
.preset-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.preset-heading > div { display: flex; flex-direction: column; gap: 4px; }
.preset-heading span { color: var(--blue); font: 800 8px ui-monospace, monospace; letter-spacing: .1em; }
.preset-heading strong { color: var(--text); font-size: 11px; }
.preset-heading > small { color: var(--text-muted); font-size: 9px; }
.preset-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 7px; }
.preset-card { position: relative; display: flex; align-items: flex-start; gap: 7px; min-width: 0; min-height: 66px; padding: 9px 8px; border: 1px solid var(--border); border-radius: 10px; background: color-mix(in srgb, var(--bg-card) 70%, transparent); color: var(--text); cursor: pointer; text-align: left; transition: transform .2s, border-color .2s, background .2s, box-shadow .2s; }
.preset-card:hover { transform: translateY(-1px); border-color: color-mix(in srgb, var(--blue) 48%, var(--border)); box-shadow: 0 8px 18px color-mix(in srgb, var(--blue) 9%, transparent); }
.preset-card.active { border-color: var(--blue); background: var(--bg-card); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--blue) 16%, transparent); }
.preset-mark { display: grid; flex: 0 0 23px; width: 23px; height: 23px; place-items: center; border-radius: 7px; background: var(--hover); color: var(--blue); font-size: 12px; }
.preset-copy { min-width: 0; padding-right: 7px; }
.preset-copy strong, .preset-copy small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.preset-copy strong { font-size: 10px; }
.preset-copy small { margin-top: 4px; color: var(--text-muted); font-size: 8px; }
.preset-arrow { position: absolute; top: 9px; right: 7px; color: var(--text-muted); font-size: 11px; opacity: .7; }
.preset-card.active .preset-arrow { color: var(--blue); opacity: 1; }
@media (max-width: 600px) {
  .preset-block { margin-top: -3px; padding: 11px; }
  .preset-grid { grid-template-columns: 1fr; gap: 6px; }
  .preset-card { min-height: 52px; align-items: center; }
  .preset-arrow { top: 50%; transform: translateY(-50%); }
}

/* ======== Immersive interviewer MVP ======== */
.presence-choice { margin: -2px 0 18px; padding: 14px; border: 1px solid color-mix(in srgb, var(--blue) 14%, var(--border)); border-radius: 14px; background: color-mix(in srgb, var(--bg-card) 58%, transparent); }
.presence-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.presence-heading label { margin-bottom: 3px; }
.presence-heading small { display: block; color: var(--text-muted); font-size: 9px; line-height: 1.45; }
.presence-heading > span { color: var(--blue); font: 800 8px ui-monospace, monospace; letter-spacing: .09em; }
.presence-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 7px; }
.presence-grid button { display: grid; grid-template-columns: 30px minmax(0, 1fr); align-items: center; gap: 8px; position: relative; min-height: 62px; padding: 9px 10px; border: 1px solid var(--border); border-radius: 11px; background: color-mix(in srgb, var(--bg-card) 72%, transparent); color: var(--text); text-align: left; cursor: pointer; transition: border-color .2s, background .2s, transform .2s, box-shadow .2s; }
.presence-grid button:hover:not(:disabled) { transform: translateY(-1px); border-color: color-mix(in srgb, var(--blue) 45%, var(--border)); }
.presence-grid button.active { border-color: var(--blue); background: color-mix(in srgb, var(--blue-light) 46%, var(--bg-card)); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--blue) 10%, transparent); }
.presence-grid button:disabled { cursor: not-allowed; opacity: .58; }
.presence-grid button > span:nth-child(2), .presence-grid button strong, .presence-grid button small { display: block; min-width: 0; }
.presence-grid button strong { padding-right: 42px; font-size: 10px; }
.presence-grid button small { margin-top: 4px; color: var(--text-muted); font-size: 8px; line-height: 1.4; }
.presence-grid button > i { position: absolute; top: 8px; right: 8px; color: var(--text-muted); font-size: 8px; font-style: normal; }
.presence-grid button.active > i { color: var(--blue); }
.presence-icon { width: 30px; height: 30px; display: grid !important; place-items: center; border-radius: 9px; background: var(--hover); color: var(--blue); font-size: 15px; }
.voice-studio { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 9px; margin-top: 9px; padding: 10px; border: 1px solid color-mix(in srgb, #8d98ef 20%, var(--border)); border-radius: 11px; background: linear-gradient(120deg, color-mix(in srgb, #858fe6 7%, var(--bg-card)), color-mix(in srgb, var(--bg-card) 88%, transparent)); }
.voice-studio-copy { min-width: 0; grid-column: 1 / -1; }.voice-studio-copy span, .voice-studio-copy small { display: block; }.voice-studio-copy span { display: flex; align-items: center; gap: 6px; color: var(--text); font-size: 9px; font-weight: 800; }.voice-studio-copy span i { width: 6px; height: 6px; border-radius: 50%; background: #7e8ded; box-shadow: 0 0 0 4px color-mix(in srgb, #7e8ded 12%, transparent); }.voice-studio-copy small { margin-top: 4px; overflow: hidden; color: var(--text-muted); font-size: 8px; text-overflow: ellipsis; white-space: nowrap; }
.voice-studio :deep(.el-select) { width: 100%; }.voice-studio :deep(.el-select__wrapper) { min-height: 30px; border-radius: 8px; background: var(--bg-card); box-shadow: 0 0 0 1px var(--border) inset; }.voice-studio :deep(.el-button) { min-height: 30px; border-radius: 8px; }
.voice-option { display: flex; align-items: center; justify-content: space-between; gap: 12px; width: 100%; }.voice-option b { overflow: hidden; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.voice-option i { color: #6674d8; font-size: 9px; font-style: normal; }

.immersive-layout { display: grid; grid-template-columns: minmax(0, 1fr) 350px; gap: 18px; align-items: stretch; }
.visual-room { position: relative; min-width: 0; min-height: clamp(520px, 63vh, 690px); overflow: hidden; border: 1px solid rgba(174, 227, 215, .2); border-radius: 24px; background: #0e211d; box-shadow: 0 30px 80px rgba(7, 31, 25, .24); isolation: isolate; }
.visual-room::after { content: ''; position: absolute; inset: 0; z-index: 1; pointer-events: none; box-shadow: inset 0 0 0 1px rgba(255,255,255,.035), inset 0 -90px 110px rgba(3, 12, 10, .2); border-radius: inherit; }
.text-room-fallback { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; padding: 30px; background: radial-gradient(circle at 50% 28%, rgba(99, 197, 180, .26), transparent 30%), linear-gradient(145deg, #173c34, #0a1714); color: #e8f6f2; text-align: center; }
.text-room-fallback > span { width: 66px; height: 66px; display: grid; place-items: center; border: 1px solid rgba(174, 227, 215, .25); border-radius: 50%; background: rgba(255,255,255,.08); color: #91ded1; font-size: 28px; }
.text-room-fallback strong { margin-top: 4px; font-size: 19px; }
.text-room-fallback small { max-width: 320px; color: rgba(226, 244, 239, .66); font-size: 11px; line-height: 1.6; }
.scene-header { position: absolute; inset: 18px 18px auto; z-index: 3; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.interviewer-profile { display: flex; align-items: center; gap: 10px; color: #f1faf7; }
.interviewer-profile > span { width: 38px; height: 38px; display: grid; place-items: center; border: 1px solid rgba(179, 232, 220, .24); border-radius: 12px; background: rgba(7, 26, 22, .72); color: #8ed9cd; font: 800 10px ui-monospace, monospace; box-shadow: 0 12px 28px rgba(0,0,0,.2); backdrop-filter: blur(12px); }
.interviewer-profile > span img { width: 100%; height: 100%; display: block; object-fit: cover; border-radius: 11px; }
.interviewer-profile strong, .interviewer-profile small { display: block; }
.interviewer-profile strong { font-size: 12px; }
.interviewer-profile small { margin-top: 3px; color: rgba(226, 244, 239, .62); font-size: 9px; }
.presence-state { display: inline-flex; align-items: center; gap: 6px; min-height: 29px; padding: 0 10px; border: 1px solid rgba(179, 232, 220, .18); border-radius: 999px; background: rgba(7, 26, 22, .66); color: rgba(238, 250, 247, .82); font-size: 9px; backdrop-filter: blur(12px); }
.presence-state i { width: 7px; height: 7px; border-radius: 50%; background: #78a399; box-shadow: 0 0 0 4px rgba(120,163,153,.12); }
.presence-state.speaking i { background: #78e0c8; box-shadow: 0 0 0 4px rgba(120,224,200,.14); animation: presencePulse 1s infinite; }
.presence-state.listening i { background: #f1a17f; box-shadow: 0 0 0 4px rgba(241,161,127,.14); animation: presencePulse 1s infinite; }
.presence-state.thinking i, .presence-state.preparing i { background: #e2bd6e; box-shadow: 0 0 0 4px rgba(226,189,110,.14); animation: presencePulse 1.3s infinite; }
.presence-state.error i { background: #f07c6a; }
@keyframes presencePulse { 50% { opacity: .35; transform: scale(.72); } }
.phase-rail { position: absolute; top: 76px; right: 18px; z-index: 3; display: flex; align-items: center; gap: 7px; padding: 7px 9px; border: 1px solid rgba(179, 232, 220, .14); border-radius: 999px; background: rgba(7, 26, 22, .56); color: rgba(226, 244, 239, .42); backdrop-filter: blur(12px); }
.phase-rail span { display: inline-flex; align-items: center; gap: 4px; font-size: 8px; transition: color .2s; }
.phase-rail span i { color: inherit; font: 700 7px ui-monospace, monospace; font-style: normal; }
.phase-rail span.active { color: #8ed9cd; }
.phase-rail b { width: 13px; height: 1px; background: rgba(179, 232, 220, .18); }
.scene-caption { position: absolute; z-index: 3; left: 18px; right: 18px; bottom: 82px; padding: 15px 17px 16px; border: 1px solid rgba(179, 232, 220, .19); border-radius: 16px; background: linear-gradient(135deg, rgba(5, 20, 17, .82), rgba(13, 42, 36, .68)); color: #f2faf8; box-shadow: 0 18px 38px rgba(0,0,0,.22); backdrop-filter: blur(18px); }
.scene-caption > div { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.scene-caption > div span { display: inline-flex; align-items: center; gap: 6px; color: #8ed9cd; font-size: 9px; font-weight: 800; }
.scene-caption > div span i { width: 6px; height: 6px; border-radius: 50%; background: #8ed9cd; }
.scene-caption > div small { color: rgba(226, 244, 239, .58); font-size: 8px; }
.scene-caption p { max-height: 91px; margin-top: 8px; overflow-y: auto; color: #f5fbf9; font-size: clamp(13px, 1.25vw, 16px); line-height: 1.7; white-space: pre-line; }
.scene-caption p span { color: #f6fbfa; }
.scene-caption p em { color: rgba(226, 244, 239, .52); font-style: normal; transition: color .16s; }
.scene-caption.speaking { border-color: rgba(142, 217, 205, .38); box-shadow: 0 18px 42px rgba(36, 135, 118, .2); }
.scene-controls { position: absolute; z-index: 3; left: 18px; right: 18px; bottom: 17px; display: flex; align-items: center; gap: 8px; }
.scene-controls button { border: 1px solid rgba(179, 232, 220, .18); background: rgba(7, 26, 22, .68); color: rgba(239, 250, 247, .78); cursor: pointer; backdrop-filter: blur(14px); transition: transform .2s, border-color .2s, background .2s; }
.scene-controls button:hover:not(:disabled) { transform: translateY(-2px); border-color: rgba(142, 217, 205, .48); }
.scene-controls button:disabled { cursor: not-allowed; opacity: .45; }
.scene-mic { min-width: 0; flex: 1; display: flex; align-items: center; gap: 10px; min-height: 48px; padding: 6px 12px 6px 7px; border-radius: 14px; text-align: left; }
.scene-mic > span:first-child { width: 34px; height: 34px; display: grid; flex: 0 0 34px; place-items: center; border-radius: 10px; background: rgba(142, 217, 205, .14); color: #8ed9cd; font-size: 17px; }
.scene-mic > span:nth-child(2), .scene-mic strong, .scene-mic small { display: block; }
.scene-mic strong { color: #f3faf8; font-size: 10px; }
.scene-mic small { margin-top: 2px; color: rgba(226, 244, 239, .55); font-size: 8px; }
.scene-mic.active { border-color: rgba(241, 161, 127, .48); background: rgba(88, 39, 29, .72); }
.scene-mic.active > span:first-child { background: rgba(241, 161, 127, .16); color: #f1a17f; animation: presencePulse 1s infinite; }
.scene-tool { width: 70px; height: 48px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 3px; flex: 0 0 70px; border-radius: 14px; font-size: 13px; }
.scene-tool span { font-size: 7px; white-space: nowrap; }
.scene-tool.active { background: rgba(36, 105, 93, .72); color: #9be3d7; }

.interview-console { min-width: 0; height: clamp(520px, 63vh, 690px); display: flex; flex-direction: column; overflow: hidden; border: 1px solid color-mix(in srgb, var(--blue) 14%, var(--border)); border-radius: 22px; background: color-mix(in srgb, var(--bg-card) 90%, transparent); box-shadow: 0 24px 64px rgba(16, 54, 47, .09); backdrop-filter: blur(18px); }
.console-head { min-height: 66px; display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 12px 14px 11px 16px; border-bottom: 1px solid var(--border); }
.console-head > div:first-child span, .console-head > div:first-child strong { display: block; }
.console-head > div:first-child span { color: var(--blue); font: 800 8px ui-monospace, monospace; letter-spacing: .09em; }
.console-head > div:first-child strong { margin-top: 4px; color: var(--text); font-size: 13px; }
.console-tabs { display: flex; padding: 3px; border-radius: 10px; background: var(--hover); }
.console-tabs button { min-width: 45px; height: 28px; border: 0; border-radius: 8px; background: transparent; color: var(--text-muted); cursor: pointer; font-size: 9px; }
.console-tabs button.active { background: var(--bg-card); color: var(--blue); font-weight: 800; box-shadow: 0 4px 12px rgba(16,54,47,.08); }
.interview-console .chat-panel { min-height: 0; height: auto; flex: 1; margin: 0; padding: 18px 15px; border: 0; border-radius: 0; background: transparent; box-shadow: none; backdrop-filter: none; }
.interview-console .coach-panel { min-height: 0; height: auto; flex: 1; position: static; overflow-y: auto; padding: 17px; border: 0; border-radius: 0; background: transparent; box-shadow: none; backdrop-filter: none; }
.interview-console .msg { gap: 7px; }
.interview-console .msg-avatar { width: 28px; height: 28px; border-radius: 9px; font-size: 13px; }
.interview-console .msg-bubble { max-width: 100%; padding: 10px 11px; font-size: 11px; line-height: 1.65; }
.interview-console .msg.user .msg-bubble { background: var(--text); }
.interview-console .msg-name { font-size: 8px; }
.immersive-input { margin-top: 18px; }

.focus-mode .immersive-layout { grid-template-columns: minmax(0, 1100px); max-width: 1100px; margin-inline: auto; }
.focus-mode .interview-console { display: none; }
.focus-mode .immersive-input { max-width: 1100px; }
.focus-mode .visual-room { min-height: clamp(590px, 70vh, 760px); }

@media (max-width: 1100px) {
  .immersive-layout { grid-template-columns: minmax(0, 1fr) 310px; }
  .scene-caption p { font-size: 13px; }
}
@media (max-width: 900px) {
  .immersive-layout { grid-template-columns: 1fr; }
  .visual-room { min-height: 570px; }
  .interview-console { height: 460px; }
  .console-head { min-height: 60px; }
}
@media (max-width: 600px) {
  .presence-grid { grid-template-columns: 1fr; }
  .voice-studio-copy small { white-space: normal; }
  .visual-room { min-height: 520px; border-radius: 18px; }
  .scene-header { inset: 13px 13px auto; }
  .phase-rail { display: none; }
  .interviewer-profile > span { width: 34px; height: 34px; }
  .scene-caption { left: 12px; right: 12px; bottom: 80px; padding: 12px 13px; border-radius: 13px; }
  .scene-caption > div small { display: none; }
  .scene-caption p { max-height: 108px; font-size: 12px; line-height: 1.65; }
  .scene-controls { left: 12px; right: 12px; bottom: 12px; gap: 6px; }
  .scene-mic { min-height: 50px; }
  .scene-mic small { display: none; }
  .scene-tool { width: 52px; flex-basis: 52px; }
  .scene-tool span { display: none; }
  .interview-console { height: 430px; border-radius: 17px; }
  .immersive-input { margin-top: 12px; }
  .focus-mode .visual-room { min-height: 560px; }
}
</style>
