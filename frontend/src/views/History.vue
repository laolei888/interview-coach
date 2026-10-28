<template>
  <main class="archive-page">
    <header class="archive-hero">
      <div class="archive-hero-copy">
        <span class="archive-eyebrow"><i></i>YOUR ARCHIVE / 成长档案</span>
        <h1>把每一次练习，沉淀成下一次胜率。</h1>
        <p>回看真实回答、识别能力缺口，再把反馈变成下一次训练的具体动作。</p>
      </div>
      <div class="archive-hero-actions">
        <button type="button" class="archive-refresh" :disabled="loadingSessions || loadingResumes" @click="refreshAll"><el-icon><Refresh /></el-icon>{{ loadingSessions || loadingResumes ? '同步中' : '同步档案' }}</button>
        <el-button type="primary" size="large" :icon="VideoPlay" @click="router.push('/interview')">开始新面试</el-button>
      </div>
    </header>

    <section class="archive-overview">
      <div class="overview-lead">
        <div class="overview-lead-copy">
          <span class="section-kicker">TRAINING HEALTH</span>
          <h2>你的训练正在形成闭环</h2>
          <p>{{ overviewDescription }}</p>
          <button type="button" class="overview-link" @click="activeTab = activeTab === 'interview' ? 'resume' : 'interview'">{{ activeTab === 'interview' ? '查看简历诊断' : '查看面试记录' }}<el-icon><ArrowRight /></el-icon></button>
        </div>
        <div class="overview-ring" :style="{ '--score': archiveReadiness }"><span><strong>{{ archiveReadiness }}</strong><small>档案活跃度</small></span></div>
      </div>
      <div class="overview-stat teal"><span class="stat-icon"><el-icon><ChatLineRound /></el-icon></span><strong>{{ sessions.length }}</strong><small>面试场次</small><em>{{ activeCount ? activeCount + ' 场待继续' : '保持训练节奏' }}</em></div>
      <div class="overview-stat green"><span class="stat-icon"><el-icon><CircleCheck /></el-icon></span><strong>{{ completedCount }}</strong><small>完整复盘</small><em>{{ completionRate }}% 完成率</em></div>
      <div class="overview-stat coral"><span class="stat-icon"><el-icon><Document /></el-icon></span><strong>{{ resumes.length }}</strong><small>简历报告</small><em>{{ averageResumeScore ? '平均 ' + averageResumeScore + ' 分' : '建立第一份基线' }}</em></div>
    </section>

    <section class="archive-shell">
      <header class="archive-toolbar">
        <div class="archive-tabs" role="tablist" aria-label="训练档案分类">
          <button type="button" :class="{ active: activeTab === 'interview' }" role="tab" :aria-selected="activeTab === 'interview'" @click="activeTab = 'interview'"><el-icon><ChatDotRound /></el-icon><span>面试记录</span><b>{{ sessions.length }}</b></button>
          <button type="button" :class="{ active: activeTab === 'resume' }" role="tab" :aria-selected="activeTab === 'resume'" @click="activeTab = 'resume'"><el-icon><Document /></el-icon><span>简历报告</span><b>{{ resumes.length }}</b></button>
        </div>
        <div class="archive-filters">
          <el-input v-model="keyword" clearable :prefix-icon="Search" :placeholder="activeTab === 'interview' ? '搜索岗位名称' : '搜索文件名'" />
          <el-select v-if="activeTab === 'interview'" v-model="statusFilter" clearable placeholder="全部状态">
            <el-option label="全部记录" value="" />
            <el-option label="进行中" value="active" />
            <el-option label="已完成" value="completed" />
          </el-select>
        </div>
      </header>

      <div v-if="activeTab === 'interview'" v-loading="loadingSessions" class="archive-content">
        <div class="content-intro">
          <div><span class="section-kicker">INTERVIEW LOG</span><h2>每一次回答，都是可追踪的进步</h2><p>{{ interviewSummary }}</p></div>
          <div class="status-switch" aria-label="面试记录筛选">
            <button type="button" :class="{ active: statusFilter === '' }" @click="statusFilter = ''">全部 <b>{{ sessions.length }}</b></button>
            <button type="button" :class="{ active: statusFilter === 'active' }" @click="statusFilter = 'active'">待继续 <b>{{ activeCount }}</b></button>
            <button type="button" :class="{ active: statusFilter === 'completed' }" @click="statusFilter = 'completed'">已完成 <b>{{ completedCount }}</b></button>
          </div>
        </div>

        <div v-if="filteredSessions.length" class="timeline-list">
          <article v-for="item in filteredSessions" :key="item.id" :class="['timeline-card', item.status]" @click="item.status === 'completed' ? openSession(item) : continueSession(item.id)">
            <div class="timeline-rail"><span class="rail-dot"></span><i></i></div>
            <div class="timeline-body">
              <div class="timeline-topline">
                <div class="record-heading"><span class="record-type">{{ item.status === 'completed' ? '复盘完成' : '训练未完成' }}</span><h3>{{ item.position || '未命名岗位' }}</h3><el-tag :type="item.status === 'completed' ? 'success' : 'warning'" size="small" effect="plain">{{ item.status === 'completed' ? '已完成' : '进行中' }}</el-tag></div>
                <time>{{ formatDate(item.createdAt, true) }}</time>
              </div>
              <div class="record-meta"><span><el-icon><DataAnalysis /></el-icon>{{ modeText(item) }}</span><span><el-icon><TopRight /></el-icon>{{ difficultyText(item) }}</span><span><el-icon><Aim /></el-icon>{{ feedbackStyleText(item) }}反馈</span><span><el-icon><ChatLineRound /></el-icon>{{ item.targetRounds || 5 }} 轮目标</span></div>
              <div class="timeline-bottom">
                <p>{{ item.status === 'completed' ? '打开完整对话与能力评估，找到下一次训练重点。' : '从上次停下的位置继续，完成后生成结构化评估。' }}</p>
                <div class="record-actions" @click.stop>
                  <el-button v-if="item.status === 'active'" text @click="finishSession(item.id)">结束并生成报告</el-button>
                  <el-button v-if="item.status === 'active'" type="primary" plain @click="continueSession(item.id)">继续训练<el-icon><ArrowRight /></el-icon></el-button>
                  <el-button v-else type="primary" plain @click="openSession(item)">打开复盘<el-icon><ArrowRight /></el-icon></el-button>
                  <button type="button" class="icon-action" aria-label="删除训练记录" @click="removeSession(item)"><el-icon><Delete /></el-icon></button>
                </div>
              </div>
            </div>
          </article>
        </div>
        <div v-else-if="!loadingSessions" class="archive-empty">
          <span class="empty-orb"><el-icon><ChatDotRound /></el-icon></span>
          <span class="section-kicker">NO MATCHES YET</span>
          <h3>{{ keyword || statusFilter ? '没有找到符合条件的记录' : '从一次完整对话开始建立档案' }}</h3>
          <p>{{ keyword || statusFilter ? '换一个关键词或清除筛选条件试试。' : '完成一场模拟面试后，回答评分和能力反馈会在这里持续累积。' }}</p>
          <div><el-button v-if="keyword || statusFilter" plain @click="keyword = ''; statusFilter = ''">清除筛选</el-button><el-button type="primary" @click="router.push('/interview')">开始第一次面试</el-button></div>
        </div>
      </div>

      <div v-else v-loading="loadingResumes" class="archive-content">
        <div class="content-intro resume-intro">
          <div><span class="section-kicker">RESUME LAB</span><h2>让每一版简历都更接近目标岗位</h2><p>{{ resumeSummary }}</p></div>
          <el-button type="primary" plain :icon="Document" @click="router.push('/resume')">分析新简历</el-button>
        </div>

        <div v-if="resumes.length >= 2" class="compare-banner">
          <div class="compare-banner-mark"><el-icon><DataAnalysis /></el-icon></div>
          <div><strong>版本对比实验</strong><span>选择两份报告，确认修改是否真的提升了岗位匹配度。</span></div>
          <el-button :disabled="selectedResumeIds.length !== 2" type="primary" @click="compareResumes">对比所选 {{ selectedResumeIds.length }}/2<el-icon><ArrowRight /></el-icon></el-button>
        </div>

        <div v-if="filteredResumes.length" class="resume-grid">
          <article v-for="(item, index) in filteredResumes" :key="item.id" :class="['resume-card', { selected: selectedResumeIds.includes(item.id) }]" @click="openResume(item)">
            <div class="resume-card-top"><span class="resume-index">REPORT {{ String(index + 1).padStart(2, '0') }}</span><button type="button" class="compare-toggle" :aria-label="selectedResumeIds.includes(item.id) ? '取消选择' : '选择用于对比'" @click.stop="toggleResumeSelection(item.id)"><el-icon><CircleCheck v-if="selectedResumeIds.includes(item.id)" /><Select v-else /></el-icon></button></div>
            <div class="resume-card-main">
              <div class="resume-file-icon"><el-icon><Document /></el-icon></div>
              <h3>{{ item.fileName || '未命名简历' }}</h3>
              <p>{{ item.targetPosition || item.matchPosition || '通用岗位分析' }}</p>
            </div>
            <div class="resume-card-bottom"><div><span>综合评分</span><strong>{{ item.aiScore ?? '--' }}<small v-if="item.aiScore">/100</small></strong></div><time>{{ formatDate(item.createdAt, true) }}</time><el-icon class="resume-arrow"><ArrowRight /></el-icon></div>
          </article>
        </div>
        <div v-else-if="!loadingResumes" class="archive-empty">
          <span class="empty-orb green"><el-icon><Document /></el-icon></span>
          <span class="section-kicker">NO REPORTS YET</span>
          <h3>{{ keyword ? '没有找到匹配的简历报告' : '先建立你的第一份岗位基线' }}</h3>
          <p>{{ keyword ? '换一个文件名关键词试试。' : '上传简历并填写目标岗位，系统会帮你找到最值得优化的部分。' }}</p>
          <div><el-button v-if="keyword" plain @click="keyword = ''">清除搜索</el-button><el-button type="primary" @click="router.push('/resume')">开始简历诊断</el-button></div>
        </div>
      </div>
    </section>

    <ActionConfirmDialog
      v-model="confirmVisible"
      :title="confirmKind === 'delete' ? '删除这条训练记录吗？' : '结束这场面试吗？'"
      :description="confirmKind === 'delete' ? `删除“${confirmTarget?.position || '未命名岗位'}”后，全部对话和评分都无法恢复。` : '结束后会生成能力评估，并锁定当前会话，之后不能继续回答。'"
      :kicker="confirmKind === 'delete' ? 'DELETE RECORD / 删除记录' : 'END PRACTICE / 结束训练'"
      :confirm-text="confirmKind === 'delete' ? '删除训练记录' : '结束并生成报告'"
      :loading-text="confirmKind === 'delete' ? '正在删除' : '正在生成报告'"
      :tone="confirmKind === 'delete' ? 'danger' : 'warning'"
      :icon="confirmKind === 'delete' ? Delete : CircleClose"
      :confirm-icon="confirmKind === 'delete' ? Delete : DocumentChecked"
      :loading="confirmLoading"
      @confirm="confirmAction"
    />

    <el-drawer v-model="sessionDrawer" class="archive-drawer" size="min(780px, 100%)" direction="rtl" destroy-on-close>
      <template #header><div class="drawer-title"><span>面试复盘</span><small>{{ detail?.session?.position || '训练记录' }} · {{ detail ? formatDate(detail.session.createdAt, true) : '' }}</small></div></template>
      <div v-if="detail" class="review-detail">
        <section class="detail-summary">
          <div><span>目标岗位</span><strong>{{ detail.session.position }}</strong></div>
          <div><span>场景 / 难度 / 反馈</span><strong>{{ modeText(detail.session) }} · {{ difficultyText(detail.session) }} · {{ feedbackStyleText(detail.session) }}</strong></div>
          <div><span>对话轮次</span><strong>{{ answerCount }} / {{ detail.session.targetRounds || 5 }} 轮</strong></div>
          <div><span>训练时间</span><strong>{{ formatDate(detail.session.createdAt) }}</strong></div>
        </section>
        <section v-if="summaryMessage" class="assessment"><div class="section-title"><span class="section-kicker">AI ASSESSMENT</span><h2>能力评估</h2><el-button text :icon="Download" @click="downloadInterview">下载报告</el-button></div><article v-html="renderMarkdown(summaryMessage.content.replace('【面试总结】', ''))"></article></section>
        <section class="conversation"><div class="section-title"><span class="section-kicker">FULL TRANSCRIPT</span><h2>完整对话</h2><span class="conversation-count">{{ conversationMessages.length }} 条消息</span></div><div v-for="message in conversationMessages" :key="message.id" :class="['message', message.role]"><span>{{ message.role === 'ai' ? 'AI' : '我' }}</span><article v-html="message.role === 'ai' ? renderInterviewMessage(message.content) : renderMarkdown(message.content)"></article><b v-if="message.role === 'user' && message.score" class="message-score">{{ message.score }}/10</b></div></section>
      </div>
    </el-drawer>

    <el-drawer v-model="resumeDrawer" class="archive-drawer" size="min(720px, 100%)" direction="rtl" destroy-on-close>
      <template #header><div class="drawer-title"><span>简历分析报告</span><small>{{ resumeDetail?.fileName || '报告详情' }}</small></div></template>
      <div v-if="resumeDetail" class="resume-report"><div class="resume-score"><div class="resume-score-ring"><strong>{{ resumeDetail.aiScore ?? '--' }}</strong><span>综合评分</span></div><p>这份报告记录了当前版本的岗位匹配表现，以及下一轮修改最值得投入的方向。</p><el-button :icon="Download" @click="downloadResume">下载报告</el-button></div><article v-html="renderMarkdown(resumeDetail.aiFeedback || '')"></article></div>
    </el-drawer>

    <el-drawer v-model="compareDrawer" class="archive-drawer" size="min(1080px, 100%)" direction="rtl" destroy-on-close>
      <template #header><div class="drawer-title"><span>简历版本对比</span><small>对照修改前后，确认改动是否真正提升岗位匹配度</small></div></template>
      <div v-if="compareReports.length === 2" class="compare-view">
        <section class="compare-summary">
          <div v-for="(report, index) in compareReports" :key="report.id"><span>{{ index === 0 ? '较早版本' : '较新版本' }} · {{ formatDate(report.createdAt, true) }}</span><h2>{{ report.fileName }}</h2><p>{{ report.targetPosition || report.matchPosition || '通用岗位分析' }}</p><strong>{{ report.aiScore ?? '--' }}<small>分</small></strong></div>
          <aside :class="scoreDelta >= 0 ? 'up' : 'down'"><el-icon><TopRight v-if="scoreDelta >= 0" /><BottomRight v-else /></el-icon><strong>{{ scoreDelta > 0 ? '+' : '' }}{{ scoreDelta }}</strong><span>评分变化</span></aside>
        </section>
        <section class="compare-columns"><article v-for="(report, index) in compareReports" :key="report.id"><header><span>版本 {{ index + 1 }}</span><strong>{{ index === 0 ? '较早版本' : '较新版本' }}</strong></header><div v-html="renderMarkdown(report.aiFeedback || '')"></div></article></section>
      </div>
    </el-drawer>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Aim, ArrowRight, BottomRight, ChatDotRound, ChatLineRound, CircleCheck, CircleClose, DataAnalysis, Delete, Document, DocumentChecked, Download, Refresh, Search, Select, TopRight, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { deleteSession, endInterview, getErrorMessage, getResumeDetail, getResumeHistory, getSessionDetail, getSessions } from '../api'
import { renderInterviewMessage, renderMarkdown } from '../utils/markdown'
import ActionConfirmDialog from '../components/ActionConfirmDialog.vue'

const router = useRouter()
const route = useRoute()
const activeTab = ref('interview')
const keyword = ref('')
const statusFilter = ref('')
const sessions = ref([])
const resumes = ref([])
const loadingSessions = ref(false)
const loadingResumes = ref(false)
const sessionDrawer = ref(false)
const resumeDrawer = ref(false)
const detail = ref(null)
const resumeDetail = ref(null)
const compareDrawer = ref(false)
const selectedResumeIds = ref([])
const compareReports = ref([])
const confirmVisible = ref(false)
const confirmLoading = ref(false)
const confirmKind = ref('finish')
const confirmTarget = ref(null)

const completedCount = computed(() => sessions.value.filter(item => item.status === 'completed').length)
const activeCount = computed(() => sessions.value.filter(item => item.status === 'active').length)
const completionRate = computed(() => sessions.value.length ? Math.round(completedCount.value / sessions.value.length * 100) : 0)
const averageResumeScore = computed(() => {
  const scored = resumes.value.map(item => Number(item.aiScore)).filter(score => Number.isFinite(score))
  return scored.length ? Math.round(scored.reduce((sum, score) => sum + score, 0) / scored.length) : 0
})
const archiveReadiness = computed(() => Math.min(100, Math.round(completionRate.value * .55 + Math.min(100, averageResumeScore.value) * .45)))
const activeSession = computed(() => sessions.value.find(item => item.status === 'active'))
const latestCompleted = computed(() => sessions.value.find(item => item.status === 'completed'))
const overviewDescription = computed(() => activeSession.value ? '你还有一场训练没有走完，先把它完成，系统才能给出完整的能力反馈。' : latestCompleted.value ? '最近一次训练已经完成，打开复盘看看下一次应该优先练什么。' : '从一场面试或一次简历诊断开始，训练数据会在这里逐步形成。')
const interviewSummary = computed(() => {
  if (keyword.value || statusFilter.value) return '当前筛选出 ' + filteredSessions.value.length + ' 条记录，点击卡片即可打开详情或继续训练。'
  return activeCount.value ? '有 ' + activeCount.value + ' 场训练等待继续，完成后会自动生成完整评估。' : completedCount.value ? '你已经完成 ' + completedCount.value + ' 场复盘，继续积累可以看见更清晰的变化曲线。' : '完成第一场模拟面试后，这里会成为你的回答与反馈时间线。'
})
const resumeSummary = computed(() => resumes.value.length ? '共沉淀 ' + resumes.value.length + ' 份版本，平均评分 ' + (averageResumeScore.value || '--') + ' 分。选中两份报告，可以直接对照修改前后的变化。' : '每次上传都会保留一份独立版本，方便你验证修改是否真的有效。')
const filteredSessions = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return sessions.value.filter(item => (!statusFilter.value || item.status === statusFilter.value) && (!query || (item.position || '').toLowerCase().includes(query)))
})
const filteredResumes = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return resumes.value.filter(item => !query || (item.fileName || '').toLowerCase().includes(query) || (item.targetPosition || item.matchPosition || '').toLowerCase().includes(query))
})
const summaryMessage = computed(() => detail.value?.messages?.find(item => item.content?.startsWith('【面试总结】')))
const conversationMessages = computed(() => detail.value?.messages?.filter(item => !item.content?.startsWith('【面试总结】')) || [])
const answerCount = computed(() => conversationMessages.value.filter(item => item.role === 'user').length)
const scoreDelta = computed(() => Number(compareReports.value[1]?.aiScore || 0) - Number(compareReports.value[0]?.aiScore || 0))
const modeMap = { technical: '技术深挖', behavioral: '行为面试', 'system-design': '系统设计', project: '项目复盘' }
const difficultyMap = { junior: '初级', intermediate: '中级', senior: '高级' }
const feedbackStyleMap = { encouraging: '鼓励型', balanced: '平衡型', strict: '严格型' }

onMounted(async () => {
  await refreshAll(false)
  const sessionId = Number(route.query.sessionId)
  if (Number.isInteger(sessionId) && sessionId > 0) openSession({ id: sessionId })
})

async function loadSessions() {
  loadingSessions.value = true
  try {
    const { data } = await getSessions()
    if (data.code !== 200) throw new Error(data.message || '面试记录加载失败')
    sessions.value = data.data || []
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '面试记录加载失败'))
  } finally {
    loadingSessions.value = false
  }
}

async function loadResumes() {
  loadingResumes.value = true
  try {
    const { data } = await getResumeHistory()
    if (data.code !== 200) throw new Error(data.message || '简历报告加载失败')
    resumes.value = data.data || []
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '简历报告加载失败'))
  } finally {
    loadingResumes.value = false
  }
}

async function refreshAll(showMessage = true) {
  await Promise.all([loadSessions(), loadResumes()])
  if (showMessage) ElMessage.success('训练档案已同步')
}

async function openSession(item) {
  try {
    const { data } = await getSessionDetail(item.id)
    if (data.code === 200) {
      detail.value = data.data
      sessionDrawer.value = true
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '复盘详情加载失败'))
  }
}

async function openResume(item) {
  try {
    const { data } = await getResumeDetail(item.id)
    if (data.code === 200) {
      resumeDetail.value = data.data
      resumeDrawer.value = true
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '简历报告加载失败'))
  }
}

function toggleResumeSelection(id) {
  selectedResumeIds.value = selectedResumeIds.value.includes(id) ? selectedResumeIds.value.filter(item => item !== id) : [...selectedResumeIds.value.slice(-1), id]
}

async function compareResumes() {
  if (selectedResumeIds.value.length !== 2) return
  try {
    const responses = await Promise.all(selectedResumeIds.value.map(getResumeDetail))
    compareReports.value = responses.map(item => item.data.data).sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    compareDrawer.value = true
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '版本对比加载失败'))
  }
}

const continueSession = id => router.push({ path: '/interview', query: { sessionId: id } })

function finishSession(id) {
  confirmKind.value = 'finish'
  confirmTarget.value = sessions.value.find(item => item.id === id) || { id }
  confirmVisible.value = true
}

function removeSession(item) {
  confirmKind.value = 'delete'
  confirmTarget.value = item
  confirmVisible.value = true
}

async function confirmAction() {
  if (!confirmTarget.value) return
  confirmLoading.value = true
  try {
    if (confirmKind.value === 'delete') {
      const { data } = await deleteSession(confirmTarget.value.id)
      if (data.code !== 200) throw new Error(data.message)
      sessions.value = sessions.value.filter(row => row.id !== confirmTarget.value.id)
      if (detail.value?.session?.id === confirmTarget.value.id) {
        detail.value = null
        sessionDrawer.value = false
      }
      ElMessage.success('训练记录已删除')
    } else {
      const { data } = await endInterview(confirmTarget.value.id)
      if (data.code !== 200) throw new Error(data.message)
      await loadSessions()
      ElMessage.success('面试已结束，评估报告已生成')
    }
    confirmVisible.value = false
  } catch (error) {
    ElMessage.error(getErrorMessage(error, confirmKind.value === 'delete' ? '删除失败' : '结束失败'))
  } finally {
    confirmLoading.value = false
  }
}

function downloadText(name, content) {
  const url = URL.createObjectURL(new Blob([content], { type: 'text/markdown;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = name
  link.click()
  URL.revokeObjectURL(url)
}

const downloadInterview = () => downloadText((detail.value?.session?.position || '面试') + '_面试报告.md', summaryMessage.value?.content || '')
const downloadResume = () => downloadText((resumeDetail.value?.fileName || '简历').replace(/\.pdf$/i, '') + '_分析报告.md', resumeDetail.value?.aiFeedback || '')
const formatDate = (value, withTime = false) => value ? new Date(value).toLocaleString('zh-CN', withTime ? { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' } : { month: 'short', day: 'numeric' }) : ''
const modeText = item => modeMap[item?.interviewMode] || '技术深挖'
const difficultyText = item => difficultyMap[item?.difficultyLevel] || '中级'
const feedbackStyleText = item => feedbackStyleMap[item?.feedbackStyle] || '平衡型'
</script>

<style scoped>
.archive-page { max-width: 1180px; margin: 0 auto; padding-bottom: 24px; color: var(--text); }
.archive-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 22px; }
.archive-eyebrow,.section-kicker { color: var(--blue); font: 800 10px ui-monospace, SFMono-Regular, Menlo, monospace; letter-spacing: .08em; }
.archive-eyebrow { display: flex; align-items: center; gap: 8px; }
.archive-eyebrow i { width: 7px; height: 7px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent) 14%, transparent); }
.archive-hero h1 { max-width: 760px; margin: 11px 0 8px; font-size: clamp(28px, 3.5vw, 42px); letter-spacing: -.055em; line-height: 1.15; }
.archive-hero p { color: var(--text-secondary); font-size: 13px; line-height: 1.7; }
.archive-hero-actions { display: flex; align-items: center; gap: 10px; padding-bottom: 3px; white-space: nowrap; }
.archive-refresh { display: flex; align-items: center; gap: 6px; height: 40px; padding: 0 12px; border: 1px solid var(--border); border-radius: 6px; background: var(--bg-card); color: var(--text-secondary); cursor: pointer; font: inherit; font-size: 11px; }
.archive-refresh:hover { border-color: var(--blue); color: var(--blue); }.archive-refresh:disabled { cursor: wait; opacity: .58; }
.archive-overview { display: grid; grid-template-columns: minmax(330px, 1.75fr) repeat(3, minmax(130px, 1fr)); gap: 10px; margin-bottom: 18px; }
.overview-lead,.overview-stat { min-height: 152px; border: 1px solid var(--border); border-radius: 10px; background: var(--bg-card); box-shadow: 0 14px 30px rgba(23,32,42,.045); }
.overview-lead { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 23px 25px; background: linear-gradient(125deg, var(--bg-card), color-mix(in srgb, var(--blue-light) 55%, var(--bg-card))); }
.overview-lead-copy { min-width: 0; }.overview-lead h2 { margin: 8px 0 7px; font-size: 19px; letter-spacing: -.035em; }.overview-lead p { max-width: 300px; color: var(--text-secondary); font-size: 11px; line-height: 1.7; }.overview-link { display: flex; align-items: center; gap: 5px; margin-top: 13px; border: 0; background: transparent; color: var(--blue); cursor: pointer; font: 700 11px inherit; }.overview-link:hover { gap: 8px; }
.overview-ring { width: 100px; height: 100px; display: grid; flex: 0 0 100px; place-items: center; border-radius: 50%; background: conic-gradient(var(--blue) calc(var(--score) * 1%), var(--border) 0); position: relative; }.overview-ring::after { content: ''; position: absolute; inset: 7px; border-radius: 50%; background: var(--bg-card); }.overview-ring span { position: relative; z-index: 1; text-align: center; }.overview-ring strong,.overview-ring small { display: block; }.overview-ring strong { color: var(--blue); font-size: 25px; line-height: 1; }.overview-ring small { margin-top: 4px; color: var(--text-muted); font-size: 9px; }
.overview-stat { position: relative; display: flex; flex-direction: column; justify-content: center; padding: 20px 18px; overflow: hidden; }.overview-stat::after { content: ''; position: absolute; width: 70px; height: 70px; right: -24px; bottom: -24px; border-radius: 50%; background: currentColor; opacity: .055; }.overview-stat.teal { color: var(--blue); }.overview-stat.green { color: var(--success); }.overview-stat.coral { color: var(--accent); }.stat-icon { width: 29px; height: 29px; display: grid; place-items: center; margin-bottom: 12px; border-radius: 7px; background: color-mix(in srgb, currentColor 11%, var(--bg-card)); }.overview-stat strong,.overview-stat small,.overview-stat em { display: block; }.overview-stat strong { color: var(--text); font-size: 25px; line-height: 1; }.overview-stat small { margin-top: 6px; color: var(--text-secondary); font-size: 11px; }.overview-stat em { margin-top: 6px; color: var(--text-muted); font-size: 9px; font-style: normal; white-space: nowrap; }
.archive-shell { overflow: hidden; border: 1px solid var(--border); border-radius: 10px; background: color-mix(in srgb, var(--bg-card) 95%, var(--blue-light)); box-shadow: 0 18px 40px rgba(23,32,42,.055); }.archive-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; min-height: 71px; padding: 13px 20px; border-bottom: 1px solid var(--border); background: var(--bg-card); }.archive-tabs { display: flex; align-items: center; gap: 5px; }.archive-tabs button { display: flex; align-items: center; gap: 8px; height: 38px; padding: 0 12px; border: 0; border-radius: 6px; background: transparent; color: var(--text-muted); cursor: pointer; font: inherit; font-size: 12px; }.archive-tabs button:hover { background: var(--hover); color: var(--text); }.archive-tabs button.active { background: var(--text); color: var(--bg-card); }.archive-tabs button b { min-width: 20px; padding: 2px 5px; border-radius: 10px; background: color-mix(in srgb, currentColor 12%, transparent); font-size: 9px; }.archive-tabs button.active b { background: rgba(255,255,255,.18); }.archive-filters { display: flex; gap: 8px; }.archive-filters :deep(.el-input) { width: 230px; }.archive-filters :deep(.el-select) { width: 130px; }
.archive-content { min-height: 320px; padding: 25px 26px 30px; }.content-intro { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 22px; }.content-intro h2 { margin: 8px 0 6px; font-size: 20px; letter-spacing: -.035em; }.content-intro p { color: var(--text-secondary); font-size: 11px; line-height: 1.7; }.status-switch { display: flex; align-items: center; gap: 3px; padding: 3px; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-card); }.status-switch button { padding: 6px 9px; border: 0; border-radius: 5px; background: transparent; color: var(--text-muted); cursor: pointer; font: inherit; font-size: 10px; }.status-switch button.active { background: var(--blue-light); color: var(--blue); font-weight: 700; }.status-switch b { margin-left: 3px; font-size: 9px; }
.timeline-list { display: flex; flex-direction: column; gap: 10px; }.timeline-card { position: relative; display: grid; grid-template-columns: 24px minmax(0, 1fr); gap: 10px; padding: 18px 18px 18px 15px; border: 1px solid var(--border); border-radius: 9px; background: var(--bg-card); cursor: pointer; transition: border-color .18s, box-shadow .18s, transform .18s; }.timeline-card:hover { border-color: color-mix(in srgb, var(--blue) 46%, var(--border)); box-shadow: 0 12px 28px rgba(23,32,42,.07); transform: translateY(-1px); }.timeline-card.active { border-color: color-mix(in srgb, var(--accent) 35%, var(--border)); background: linear-gradient(110deg, color-mix(in srgb, var(--accent) 7%, var(--bg-card)), var(--bg-card) 38%); }.timeline-rail { display: flex; flex-direction: column; align-items: center; padding-top: 3px; }.rail-dot { width: 11px; height: 11px; border: 3px solid var(--blue); border-radius: 50%; background: var(--bg-card); z-index: 1; }.timeline-card.active .rail-dot { border-color: var(--accent); }.timeline-rail i { width: 1px; flex: 1; margin-top: 5px; background: var(--border); }.timeline-card:last-child .timeline-rail i { display: none; }.timeline-body { min-width: 0; }.timeline-topline { display: flex; align-items: flex-start; justify-content: space-between; gap: 15px; }.record-heading { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; min-width: 0; }.record-type { color: var(--blue); font: 800 9px ui-monospace,monospace; letter-spacing: .06em; text-transform: uppercase; }.timeline-card.active .record-type { color: var(--accent); }.record-heading h3 { max-width: 420px; overflow: hidden; color: var(--text); text-overflow: ellipsis; white-space: nowrap; font-size: 15px; }.timeline-topline time { flex: 0 0 auto; color: var(--text-muted); font-size: 10px; }.record-meta { display: flex; flex-wrap: wrap; gap: 14px; margin-top: 10px; color: var(--text-muted); font-size: 10px; }.record-meta span { display: flex; align-items: center; gap: 4px; }.record-meta .el-icon { color: var(--blue); }.timeline-bottom { display: flex; align-items: center; justify-content: space-between; gap: 15px; margin-top: 15px; padding-top: 12px; border-top: 1px solid var(--border); }.timeline-bottom p { color: var(--text-secondary); font-size: 11px; line-height: 1.5; }.record-actions { display: flex; align-items: center; flex: 0 0 auto; gap: 2px; }.record-actions :deep(.el-button) { font-size: 11px; }.icon-action { width: 28px; height: 28px; display: grid; place-items: center; margin-left: 4px; border: 0; border-radius: 5px; background: transparent; color: var(--text-muted); cursor: pointer; }.icon-action:hover { background: color-mix(in srgb, var(--accent) 10%, transparent); color: var(--accent); }
.resume-intro { align-items: center; }.compare-banner { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; padding: 13px 15px; border: 1px solid color-mix(in srgb, var(--blue) 24%, var(--border)); border-radius: 8px; background: var(--blue-light); }.compare-banner-mark { width: 32px; height: 32px; display: grid; flex: 0 0 32px; place-items: center; border-radius: 6px; background: var(--bg-card); color: var(--blue); }.compare-banner > div:nth-child(2) { display: flex; flex: 1; flex-direction: column; gap: 3px; }.compare-banner strong { font-size: 12px; }.compare-banner span { color: var(--text-secondary); font-size: 10px; }.compare-banner :deep(.el-button) { font-size: 11px; }.resume-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }.resume-card { min-height: 218px; display: flex; flex-direction: column; padding: 16px; border: 1px solid var(--border); border-radius: 9px; background: var(--bg-card); cursor: pointer; transition: border-color .18s, box-shadow .18s, transform .18s; }.resume-card:hover,.resume-card.selected { border-color: color-mix(in srgb, var(--blue) 48%, var(--border)); box-shadow: 0 12px 28px rgba(23,32,42,.07); transform: translateY(-1px); }.resume-card.selected { background: linear-gradient(140deg, var(--blue-light), var(--bg-card) 52%); }.resume-card-top { display: flex; align-items: center; justify-content: space-between; }.resume-index { color: var(--text-muted); font: 800 9px ui-monospace,monospace; letter-spacing: .08em; }.compare-toggle { width: 27px; height: 27px; display: grid; place-items: center; border: 1px solid var(--border); border-radius: 6px; background: var(--bg-card); color: var(--text-muted); cursor: pointer; }.compare-toggle:hover { border-color: var(--blue); color: var(--blue); }.resume-card.selected .compare-toggle { border-color: var(--blue); background: var(--blue); color: #fff; }.resume-card-main { flex: 1; padding-top: 21px; }.resume-file-icon { width: 38px; height: 38px; display: grid; place-items: center; margin-bottom: 15px; border-radius: 8px; background: #e8f6ee; color: var(--success); }.resume-card h3 { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }.resume-card-main p { overflow: hidden; margin-top: 6px; color: var(--text-muted); text-overflow: ellipsis; white-space: nowrap; font-size: 10px; }.resume-card-bottom { display: flex; align-items: flex-end; gap: 12px; padding-top: 14px; border-top: 1px solid var(--border); }.resume-card-bottom > div { display: flex; flex: 1; flex-direction: column; gap: 3px; }.resume-card-bottom span { color: var(--text-muted); font-size: 9px; }.resume-card-bottom strong { color: var(--blue); font-size: 21px; line-height: 1; }.resume-card-bottom strong small { margin-left: 2px; color: var(--text-muted); font-size: 9px; font-weight: 500; }.resume-card-bottom time { color: var(--text-muted); font-size: 9px; }.resume-arrow { color: var(--text-muted); }
.archive-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; text-align: center; }.empty-orb { width: 62px; height: 62px; display: grid; place-items: center; margin-bottom: 17px; border-radius: 50%; background: var(--blue-light); color: var(--blue); font-size: 25px; }.empty-orb.green { background: #e8f6ee; color: var(--success); }.archive-empty h3 { margin: 9px 0 6px; font-size: 16px; }.archive-empty p { max-width: 350px; color: var(--text-muted); font-size: 11px; line-height: 1.7; }.archive-empty > div { display: flex; gap: 8px; margin-top: 17px; }.archive-empty :deep(.el-button) { font-size: 11px; }
.drawer-title span,.drawer-title small { display: block; }.drawer-title span { color: var(--text); font-size: 17px; font-weight: 700; }.drawer-title small { margin-top: 4px; color: var(--text-muted); font-size: 10px; }.review-detail,.resume-report { color: var(--text); }.detail-summary { display: grid; grid-template-columns: 1.2fr 1fr .9fr 1fr; margin-bottom: 18px; border: 1px solid var(--border); border-radius: 7px; overflow: hidden; background: var(--bg-card); }.detail-summary div { padding: 13px; border-right: 1px solid var(--border); }.detail-summary div:last-child { border: 0; }.detail-summary span,.detail-summary strong { display: block; }.detail-summary span { color: var(--text-muted); font-size: 10px; }.detail-summary strong { margin-top: 5px; font-size: 13px; }.assessment,.conversation { margin-bottom: 20px; border: 1px solid var(--border); border-radius: 8px; overflow: hidden; background: var(--bg-card); }.section-title { display: flex; align-items: center; gap: 9px; padding: 14px 16px; border-bottom: 1px solid var(--border); background: var(--hover); }.section-title h2 { flex: 1; font-size: 14px; }.section-title > .section-kicker { color: var(--blue); }.conversation-count { color: var(--text-muted); font-size: 10px; }.assessment > article { padding: 8px 20px 24px; font-size: 13px; line-height: 1.8; }.assessment :deep(h2) { margin: 18px 0 8px; font-size: 15px; }.assessment :deep(ul) { padding-left: 20px; }.message { display: flex; gap: 10px; padding: 14px 16px; border-bottom: 1px solid var(--border); }.message:last-child { border: 0; }.message > span { width: 28px; height: 28px; display: grid; flex: 0 0 28px; place-items: center; border-radius: 50%; background: var(--hover); color: var(--text-muted); font-size: 10px; }.message.user { background: var(--blue-light); }.message article { min-width: 0; flex: 1; font-size: 13px; line-height: 1.75; }.message-score { align-self: flex-start; flex-shrink: 0; padding: 3px 7px; border-radius: 4px; background: var(--bg-card); color: var(--blue); font-size: 10px; }.resume-score { display: flex; align-items: center; gap: 16px; margin-bottom: 18px; padding: 15px; border: 1px solid var(--border); border-radius: 8px; }.resume-score-ring { width: 70px; height: 70px; display: flex; flex: 0 0 70px; flex-direction: column; align-items: center; justify-content: center; border-radius: 50%; background: var(--blue-light); }.resume-score-ring strong { color: var(--blue); font-size: 25px; line-height: 1; }.resume-score-ring span { margin-top: 4px; color: var(--text-muted); font-size: 9px; }.resume-score p { flex: 1; color: var(--text-secondary); font-size: 11px; line-height: 1.65; }.resume-report > article { font-size: 13px; line-height: 1.85; }.resume-report :deep(h2),.resume-report :deep(h3) { margin: 20px 0 8px; }.compare-summary { display: grid; grid-template-columns: 1fr 1fr 140px; margin-bottom: 18px; border: 1px solid var(--border); border-radius: 8px; overflow: hidden; }.compare-summary > div { position: relative; min-width: 0; padding: 18px 20px; border-right: 1px solid var(--border); }.compare-summary span,.compare-summary h2,.compare-summary p { display: block; }.compare-summary span { color: var(--text-muted); font-size: 10px; }.compare-summary h2 { overflow: hidden; margin: 6px 0; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }.compare-summary p { color: var(--text-secondary); font-size: 11px; }.compare-summary > div > strong { position: absolute; top: 18px; right: 18px; color: var(--blue); font-size: 24px; }.compare-summary strong small { font-size: 10px; }.compare-summary aside { display: flex; flex-direction: column; align-items: center; justify-content: center; }.compare-summary aside .el-icon { font-size: 20px; }.compare-summary aside strong { margin: 3px 0; font-size: 20px; }.compare-summary aside span { font-size: 9px; }.compare-summary aside.up { color: var(--success); }.compare-summary aside.down { color: var(--accent); }.compare-columns { display: grid; grid-template-columns: 1fr 1fr; border: 1px solid var(--border); border-radius: 8px; overflow: hidden; }.compare-columns > article { min-width: 0; }.compare-columns > article + article { border-left: 1px solid var(--border); }.compare-columns header { display: flex; justify-content: space-between; padding: 12px 18px; border-bottom: 1px solid var(--border); background: var(--hover); font-size: 11px; }.compare-columns header span { color: var(--text-muted); }.compare-columns > article > div { padding: 10px 20px 28px; font-size: 12px; line-height: 1.75; }.compare-columns :deep(h2),.compare-columns :deep(h3) { margin: 18px 0 7px; font-size: 14px; }.compare-columns :deep(ul) { padding-left: 18px; }
@media (max-width: 980px) { .archive-overview { grid-template-columns: 1.4fr 1fr 1fr; }.overview-lead { grid-column: 1 / -1; }.resume-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 680px) { .archive-hero { align-items: flex-start; flex-direction: column; gap: 16px; }.archive-hero h1 { font-size: 29px; }.archive-hero-actions { width: 100%; }.archive-hero-actions .el-button { flex: 1; }.archive-overview { grid-template-columns: repeat(2, minmax(0, 1fr)); }.overview-lead { grid-column: 1 / -1; min-height: 175px; padding: 20px; }.overview-stat { min-height: 128px; padding: 15px; }.archive-toolbar { align-items: stretch; flex-direction: column; padding: 12px; }.archive-tabs button { flex: 1; justify-content: center; }.archive-filters { display: grid; grid-template-columns: 1fr 120px; }.archive-filters :deep(.el-input),.archive-filters :deep(.el-select) { width: 100%; }.archive-content { padding: 20px 13px 22px; }.content-intro { align-items: flex-start; flex-direction: column; margin-bottom: 17px; }.content-intro h2 { font-size: 18px; }.status-switch { width: 100%; }.status-switch button { flex: 1; }.timeline-card { padding: 15px 12px 15px 10px; }.timeline-topline { align-items: flex-start; flex-direction: column; gap: 7px; }.record-heading h3 { max-width: 220px; }.timeline-bottom { align-items: flex-start; flex-direction: column; gap: 10px; }.record-actions { width: 100%; justify-content: flex-end; }.resume-intro :deep(.el-button) { width: 100%; }.compare-banner { align-items: flex-start; flex-wrap: wrap; }.compare-banner > div:nth-child(2) { min-width: calc(100% - 50px); }.compare-banner :deep(.el-button) { width: 100%; margin-left: 44px; }.resume-grid { grid-template-columns: 1fr; }.detail-summary { grid-template-columns: 1fr; }.detail-summary div { border-right: 0; border-bottom: 1px solid var(--border); }.detail-summary div:last-child { border-bottom: 0; }.resume-score { align-items: flex-start; flex-wrap: wrap; }.resume-score p { min-width: calc(100% - 86px); }.resume-score :deep(.el-button) { width: 100%; margin-left: 86px; }.compare-summary { grid-template-columns: 1fr 1fr; }.compare-summary aside { grid-column: 1 / -1; flex-direction: row; gap: 8px; padding: 10px; border-top: 1px solid var(--border); }.compare-columns { grid-template-columns: 1fr; }.compare-columns > article + article { border-top: 1px solid var(--border); border-left: 0; } }
/* ======== History premium layer ======== */
.archive-hero h1 { text-shadow: 0 12px 28px color-mix(in srgb, var(--blue) 9%, transparent); }
.archive-refresh { border-radius: 10px; background: var(--surface-glass); box-shadow: 0 10px 24px rgba(16, 54, 47, .05); backdrop-filter: blur(12px); }
.overview-lead, .overview-stat { border-radius: 16px; background: var(--surface-glass); box-shadow: 0 20px 50px rgba(16, 54, 47, .06); backdrop-filter: blur(16px); }
.overview-lead { border-color: color-mix(in srgb, var(--blue) 16%, var(--border)); }
.overview-ring { filter: drop-shadow(0 12px 24px color-mix(in srgb, var(--blue) 13%, transparent)); }
.archive-shell { border-radius: 18px; border-color: color-mix(in srgb, var(--blue) 13%, var(--border)); background: var(--surface-glass); box-shadow: 0 24px 64px rgba(16, 54, 47, .07); backdrop-filter: blur(16px); }
.archive-toolbar { background: color-mix(in srgb, var(--bg-card) 72%, transparent); }
.archive-tabs button { border-radius: 10px; }
.archive-tabs button.active { box-shadow: 0 10px 22px rgba(16, 54, 47, .12); }
.timeline-list { gap: 14px; }
.timeline-card, .resume-card { border-radius: 15px; background: var(--surface-glass); box-shadow: 0 18px 46px rgba(16, 54, 47, .05); backdrop-filter: blur(14px); }
.timeline-card:hover, .resume-card:hover, .resume-card.selected { box-shadow: 0 24px 56px color-mix(in srgb, var(--blue) 11%, transparent); transform: translateY(-4px); }
.timeline-card.active { background: linear-gradient(110deg, color-mix(in srgb, var(--accent) 8%, var(--surface-glass)), var(--surface-glass) 45%); }
.detail-summary, .assessment, .conversation, .resume-score, .compare-summary, .compare-columns { border-radius: 14px; }
.section-title { background: color-mix(in srgb, var(--blue-light) 42%, var(--bg-card)); }
@media (max-width: 680px) {
  .overview-lead, .overview-stat, .archive-shell, .timeline-card, .resume-card { border-radius: 13px; }
}
</style>
