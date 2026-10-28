<template>
  <section class="workspace-home" v-loading="loading">
    <header class="workspace-header">
      <div>
        <span class="workspace-eyebrow"><i></i>WORKSPACE / {{ dateLabel }}</span>
        <h1>{{ greeting }}，{{ userName || '同学' }}</h1>
        <p>把今天的训练拆成一个清晰动作：<strong>{{ focusTitle }}</strong></p>
      </div>
      <div class="workspace-tools">
        <span class="sync-state"><i></i>{{ loading ? '同步中' : '训练档案已同步' }}</span>
        <button type="button" class="refresh-button" :disabled="loading" @click="loadDashboard"><el-icon><Refresh /></el-icon>刷新</button>
      </div>
    </header>

    <div v-if="loadError" class="workspace-alert" role="alert"><el-icon><WarningFilled /></el-icon><span>{{ loadError }}</span><el-button text @click="loadDashboard">重新加载</el-button></div>

    <section class="readiness-hero">
      <InterviewOrbScene class="hero-orb" />
      <div class="hero-content">
        <span class="hero-label">NEXT BEST ACTION</span>
        <h2>{{ focusTitle }}</h2>
        <p>{{ focusDescription }}</p>
        <div class="hero-actions"><el-button type="primary" size="large" :icon="VideoPlay" @click="router.push(focusPath)">{{ focusAction }}</el-button><button type="button" @click="router.push('/history')">打开成长档案<el-icon><ArrowRight /></el-icon></button></div>
        <div class="hero-facts"><span><strong>{{ weeklyTotal }}</strong>本周训练</span><span><strong>{{ stats.questionProgress.review }}</strong>待复习题</span><span><strong>{{ stats.interviewScores.averageScore || '--' }}</strong>回答均分</span></div>
      </div>
      <div class="readiness-block">
        <div class="readiness-ring" :style="{ '--score': readinessScore }"><span><strong>{{ readinessScore }}</strong><small>准备度</small></span></div>
        <div class="readiness-copy"><span>当前准备度</span><strong>{{ readinessLabel }}</strong><p>{{ readinessDescription }}</p></div>
      </div>
    </section>

    <section class="workflow-panel panel-surface">
      <div class="section-heading"><div><span class="section-kicker">YOUR TRAINING LOOP</span><h2>一条路径，持续变强</h2></div><span class="completion-badge">{{ workflowCompletion }}/3 已完成</span></div>
      <div class="workflow-steps">
        <button v-for="step in workflowSteps" :key="step.key" type="button" :class="['workflow-step', step.state]" :disabled="step.state === 'locked'" @click="router.push(step.path)">
          <span class="step-index">{{ step.index }}</span><span class="step-copy"><small>{{ step.caption }}</small><strong>{{ step.title }}</strong><em>{{ step.description }}</em></span><span class="step-state"><el-icon v-if="step.state === 'done'"><CircleCheckFilled /></el-icon><el-icon v-else><ArrowRight /></el-icon></span>
        </button>
      </div>
    </section>

    <section class="signal-grid">
      <button type="button" class="signal-card teal" @click="router.push('/interview')"><span><el-icon><ChatDotRound /></el-icon>模拟面试</span><strong>{{ stats.interviewCount }}</strong><small>{{ stats.activeCount ? `${stats.activeCount} 场等待继续` : '从一次完整对话开始' }}</small><el-icon class="signal-arrow"><TopRight /></el-icon></button>
      <button type="button" class="signal-card green" @click="router.push('/resume')"><span><el-icon><DocumentChecked /></el-icon>简历诊断</span><strong>{{ stats.resumeCount }}</strong><small>{{ stats.averageResumeScore ? `平均评分 ${stats.averageResumeScore}` : '还没有诊断报告' }}</small><el-icon class="signal-arrow"><TopRight /></el-icon></button>
      <button type="button" class="signal-card amber" @click="router.push('/questions')"><span><el-icon><Collection /></el-icon>题库掌握</span><strong>{{ questionMastery }}<small>%</small></strong><small>{{ stats.questionProgress.review }} 道题等待复习</small><el-icon class="signal-arrow"><TopRight /></el-icon></button>
      <button type="button" class="signal-card coral" @click="router.push('/history')"><span><el-icon><DataAnalysis /></el-icon>完成率</span><strong>{{ completionRate }}<small>%</small></strong><small>{{ stats.completedCount }} / {{ stats.interviewCount }} 场面试完成</small><el-icon class="signal-arrow"><TopRight /></el-icon></button>
    </section>

    <section class="dashboard-grid">
      <article class="panel-surface activity-panel">
        <div class="section-heading"><div><span class="section-kicker">MOMENTUM</span><h2>训练节奏</h2><p>近 7 天创建的模拟面试场次</p></div><strong class="heading-number">{{ weeklyTotal }}<small> 次</small></strong></div>
        <div class="activity-chart" v-if="stats.weeklyTrend.length">
          <svg viewBox="0 0 700 220" role="img" aria-label="近七天训练趋势图"><line v-for="y in [38, 86, 134, 182]" :key="y" x1="32" :y1="y" x2="680" :y2="y" class="chart-grid" /><path :d="areaPath" class="chart-area" /><polyline :points="linePoints" class="chart-line" /><circle v-for="point in chartPoints" :key="point.date" :cx="point.x" :cy="point.y" r="5" class="chart-point"><title>{{ point.date }}：{{ point.count }} 次</title></circle></svg>
          <div class="chart-labels"><span v-for="item in stats.weeklyTrend" :key="item.date">{{ item.date }}</span></div>
        </div>
        <div v-else class="panel-empty"><span class="empty-mark"><el-icon><ChatDotRound /></el-icon></span><strong>从一次训练建立节奏</strong><p>完成第一场模拟面试后，这里会记录你的训练趋势。</p><el-button type="primary" plain @click="router.push('/interview')">开始第一次训练</el-button></div>
      </article>

      <aside class="panel-surface insight-panel">
        <div class="section-heading"><div><span class="section-kicker">SIGNALS</span><h2>今天值得关注</h2></div><el-icon class="heading-icon"><Aim /></el-icon></div>
        <div class="insight-callout"><span class="insight-dot"></span><div><strong>{{ insightTitle }}</strong><p>{{ insightDescription }}</p></div></div>
        <div class="insight-list"><div v-for="item in insightRows" :key="item.label"><span>{{ item.label }}</span><strong :class="item.tone">{{ item.value }}</strong><i><b :style="{ width: `${item.percent}%` }"></b></i></div></div>
        <button type="button" class="text-action" @click="router.push('/history')">查看完整表现分析<el-icon><ArrowRight /></el-icon></button>
      </aside>
    </section>

    <section class="bottom-grid">
      <article class="panel-surface sessions-panel">
        <div class="section-heading"><div><span class="section-kicker">RECENT WORK</span><h2>最近训练</h2><p>从上次停下的位置继续</p></div><el-button text @click="router.push('/history')">全部记录</el-button></div>
        <div v-if="recentSessions.length" class="session-list"><button v-for="item in recentSessions" :key="item.id" type="button" @click="openSession(item)"><span :class="['session-status', item.status]"><i></i></span><span class="session-main"><strong>{{ item.position || '未命名岗位' }}</strong><small>{{ formatDate(item.createdAt) }} · {{ item.status === 'completed' ? '已完成复盘' : '等待继续' }}</small></span><el-tag :type="item.status === 'completed' ? 'success' : 'warning'" size="small" effect="plain">{{ item.status === 'completed' ? '已完成' : '进行中' }}</el-tag><el-icon><ArrowRight /></el-icon></button></div>
        <div v-else class="list-empty"><span>还没有训练记录</span><button type="button" @click="router.push('/interview')">创建第一场面试 <el-icon><ArrowRight /></el-icon></button></div>
      </article>

      <article class="panel-surface coverage-panel"><div class="section-heading"><div><span class="section-kicker">COVERAGE</span><h2>训练覆盖</h2><p>不同面试场景的练习分布</p></div></div><div v-if="stats.modeDistribution.length" class="coverage-list"><div v-for="item in stats.modeDistribution" :key="item.name"><span>{{ item.name }}</span><strong>{{ item.count }}</strong><i><b :style="{ width: `${modeWidth(item.count)}%` }"></b></i></div></div><div v-else class="list-empty compact"><span>完成一场面试后生成覆盖分析</span><button type="button" @click="router.push('/interview')">开始训练 <el-icon><ArrowRight /></el-icon></button></div></article>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Aim, ArrowRight, ChatDotRound, CircleCheckFilled, Collection, DataAnalysis, DocumentChecked, Refresh, TopRight, VideoPlay, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api, { getErrorMessage, getSessions } from '../api'
import InterviewOrbScene from './InterviewOrbScene.vue'
import { useRouter } from 'vue-router'

const props = defineProps({ userName: { type: String, default: '' } })
const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const sessions = ref([])
const emptyInterviewScores = () => ({ averageScore: 0, highestScore: 0, lowestScore: 0, scoredAnswerCount: 0, scoreTrend: [], modeScores: [], weakAnswers: [] })
const stats = reactive({ interviewCount: 0, completedCount: 0, activeCount: 0, resumeCount: 0, averageResumeScore: 0, questionCount: 0, questionProgress: { practiced: 0, mastered: 0, review: 0 }, weeklyTrend: [], positionDistribution: [], modeDistribution: [], interviewScores: emptyInterviewScores() })

const userName = computed(() => props.userName)
const greeting = computed(() => new Date().getHours() < 12 ? '早上好' : new Date().getHours() < 18 ? '下午好' : '晚上好')
const dateLabel = computed(() => new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', weekday: 'short' }).toUpperCase())
const completionRate = computed(() => stats.interviewCount ? Math.round(stats.completedCount / stats.interviewCount * 100) : 0)
const weeklyTotal = computed(() => stats.weeklyTrend.reduce((sum, item) => sum + Number(item.count), 0))
const questionMastery = computed(() => stats.questionProgress.practiced ? Math.round(stats.questionProgress.mastered / stats.questionProgress.practiced * 100) : 0)
const readinessScore = computed(() => Math.round(Math.min(100, completionRate.value * .35 + (stats.averageResumeScore || 0) * .35 + questionMastery.value * .3)))
const readinessLabel = computed(() => readinessScore.value >= 80 ? '状态稳定' : readinessScore.value >= 55 ? '正在成形' : '从第一步开始')
const readinessDescription = computed(() => readinessScore.value >= 80 ? '保持节奏，开始针对薄弱项做精练。' : readinessScore.value >= 55 ? '已经有基础，下一场训练会带来更明显的提升。' : '完成一次简历诊断或模拟面试，准备度就会开始有变化。')
const activeSession = computed(() => sessions.value.find(item => item.status === 'active'))
const focusTitle = computed(() => stats.activeCount ? activeSession.value ? `继续「${activeSession.value.position}」模拟面试` : '完成正在进行的模拟面试' : !stats.resumeCount ? '先完成一次岗位定向简历诊断' : stats.questionProgress.review ? `复习 ${stats.questionProgress.review} 道薄弱题目` : '开始一场目标岗位模拟面试')
const focusDescription = computed(() => stats.activeCount ? '从上次停下的位置继续，完成后会生成结构化能力评估。' : !stats.resumeCount ? '上传简历和目标 JD，先明确本轮求职准备的重点。' : stats.questionProgress.review ? '集中处理标记为待复习的知识点，让回答更稳定。' : '用一次完整对话检查技术深度、表达结构和追问应对。')
const focusPath = computed(() => stats.activeCount && activeSession.value ? { path: '/interview', query: { sessionId: activeSession.value.id } } : !stats.resumeCount ? '/resume' : stats.questionProgress.review ? '/questions' : '/interview')
const focusAction = computed(() => stats.activeCount ? '继续面试' : !stats.resumeCount ? '分析简历' : stats.questionProgress.review ? '开始复习' : '开始面试')
const recentSessions = computed(() => sessions.value.slice(0, 4))

const workflowSteps = computed(() => [
  { key: 'resume', index: '01', caption: '定位', title: '明确目标岗位', description: stats.resumeCount ? `${stats.resumeCount} 份诊断报告已建立` : '上传简历，找到能力缺口', state: stats.resumeCount ? 'done' : 'current', path: '/resume' },
  { key: 'interview', index: '02', caption: '练习', title: '完成一场对话', description: stats.activeCount ? `${stats.activeCount} 场面试等待继续` : stats.completedCount ? `已完成 ${stats.completedCount} 场训练` : '让 AI 根据回答继续追问', state: stats.completedCount ? 'done' : stats.resumeCount ? 'current' : 'locked', path: activeSession.value ? { path: '/interview', query: { sessionId: activeSession.value.id } } : '/interview' },
  { key: 'review', index: '03', caption: '复盘', title: '把反馈变成行动', description: stats.interviewScores.scoredAnswerCount ? `${stats.interviewScores.scoredAnswerCount} 道回答已有评分` : '查看评分，决定下一轮练什么', state: stats.interviewScores.scoredAnswerCount ? 'done' : stats.completedCount ? 'current' : 'locked', path: '/history' }
])
const workflowCompletion = computed(() => workflowSteps.value.filter(item => item.state === 'done').length)
const insightTitle = computed(() => stats.activeCount ? '有一场面试正在等待你' : !stats.resumeCount ? '先建立你的第一份基线' : stats.questionProgress.review ? '题库里有一组薄弱点值得优先处理' : stats.interviewScores.scoredAnswerCount ? '你的训练已经开始形成数据' : '下一步可以开始一场完整面试')
const insightDescription = computed(() => stats.activeCount ? '保持上下文，继续完成它，系统会在结束后生成完整评估。' : !stats.resumeCount ? '简历诊断会让后续的面试问题更贴近你的目标岗位。' : stats.questionProgress.review ? `集中复习 ${stats.questionProgress.review} 道题，再回到模拟面试验证表达。` : '继续累积回答评分，准备度会越来越接近真实状态。')
const insightRows = computed(() => [
  { label: '面试完成率', value: `${completionRate.value}%`, percent: completionRate.value, tone: completionRate.value >= 60 ? 'good' : 'normal' },
  { label: '简历表现', value: stats.averageResumeScore ? `${stats.averageResumeScore} 分` : '--', percent: Math.min(100, Number(stats.averageResumeScore) || 0), tone: Number(stats.averageResumeScore) >= 80 ? 'good' : 'normal' },
  { label: '题库掌握度', value: `${questionMastery.value}%`, percent: questionMastery.value, tone: questionMastery.value >= 70 ? 'good' : 'normal' }
])
const chartPoints = computed(() => { const max = Math.max(1, ...stats.weeklyTrend.map(item => Number(item.count))); return stats.weeklyTrend.map((item, index) => ({ ...item, x: 42 + index * 103, y: 182 - Number(item.count) / max * 136 })) })
const linePoints = computed(() => chartPoints.value.map(item => `${item.x},${item.y}`).join(' '))
const areaPath = computed(() => chartPoints.value.length ? `M ${chartPoints.value[0].x} 182 L ${linePoints.value.replaceAll(' ', ' L ')} L ${chartPoints.value.at(-1).x} 182 Z` : '')
const modeWidth = count => Math.max(12, Number(count) / Math.max(1, ...stats.modeDistribution.map(item => Number(item.count))) * 100)
const formatDate = value => value ? new Date(value).toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''

async function loadDashboard() {
  loading.value = true
  loadError.value = ''
  try {
    const [statsRes, sessionsRes] = await Promise.all([api.get('/stats/dashboard'), getSessions()])
    if (statsRes.data.code !== 200) throw new Error(statsRes.data.message || '训练数据加载失败')
    if (sessionsRes.data.code !== 200) throw new Error(sessionsRes.data.message || '面试记录加载失败')
    Object.assign(stats, statsRes.data.data)
    sessions.value = sessionsRes.data.data || []
  } catch (error) {
    loadError.value = getErrorMessage(error, '训练数据加载失败，请稍后重试')
    ElMessage.error(loadError.value)
  } finally { loading.value = false }
}
const openSession = item => item.status === 'completed' ? router.push({ path: '/history', query: { sessionId: item.id } }) : router.push({ path: '/interview', query: { sessionId: item.id } })
onMounted(loadDashboard)
</script>

<style scoped>
.workspace-home { min-width: 0; padding-bottom: 24px; color: var(--text); }
.workspace-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 22px; }.workspace-eyebrow,.section-kicker,.hero-label { color: var(--blue); font: 800 10px ui-monospace, SFMono-Regular, Menlo, monospace; letter-spacing: .08em; }.workspace-eyebrow { display: flex; align-items: center; gap: 8px; }.workspace-eyebrow i { width: 7px; height: 7px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent) 14%, transparent); }.workspace-header h1 { margin: 11px 0 7px; font-size: clamp(27px, 3vw, 38px); letter-spacing: -.04em; }.workspace-header p { color: var(--text-secondary); font-size: 13px; }.workspace-header p strong { color: var(--text); }.workspace-tools { display: flex; align-items: center; gap: 13px; padding-bottom: 4px; }.sync-state { display: flex; align-items: center; gap: 7px; color: var(--text-muted); font-size: 10px; }.sync-state i { width: 7px; height: 7px; border-radius: 50%; background: var(--success); box-shadow: 0 0 0 4px color-mix(in srgb, var(--success) 13%, transparent); }.refresh-button { display: flex; align-items: center; gap: 6px; padding: 7px 10px; border: 1px solid var(--border); border-radius: 6px; background: var(--bg-card); color: var(--text-secondary); cursor: pointer; font: inherit; font-size: 11px; }.refresh-button:hover { border-color: var(--blue); color: var(--blue); }.refresh-button:disabled { cursor: wait; opacity: .6; }
.workspace-alert { display: flex; align-items: center; gap: 9px; margin: -7px 0 16px; padding: 10px 13px; border: 1px solid color-mix(in srgb, var(--accent) 28%, var(--border)); border-radius: 7px; background: color-mix(in srgb, var(--accent) 10%, var(--bg-card)); color: var(--text-secondary); font-size: 11px; }.workspace-alert .el-icon { color: var(--accent); }.workspace-alert span { flex: 1; }.workspace-alert :deep(.el-button) { padding: 3px 0; color: var(--blue); font-size: 11px; }
.readiness-hero { position: relative; min-height: 292px; display: grid; grid-template-columns: minmax(0,1fr) 300px; align-items: center; gap: 28px; overflow: hidden; margin-bottom: 18px; padding: 37px 42px; border-radius: 12px; background: linear-gradient(125deg, #10201e 0%, #1e3934 58%, #274a42 100%); color: #fff; box-shadow: 0 24px 60px rgba(23,32,42,.16); }.readiness-hero::after { content: ''; position: absolute; width: 460px; height: 460px; right: -150px; top: -170px; border-radius: 50%; background: radial-gradient(circle, rgba(120,220,203,.28), transparent 64%); }.hero-orb { position: absolute; right: 8%; bottom: -25%; width: 46%; opacity: .34; pointer-events: none; }.hero-content,.readiness-block { position: relative; z-index: 1; }.hero-label { color: #8ed3ca; }.hero-content h2 { max-width: 680px; margin: 10px 0 9px; font-size: clamp(25px, 3.1vw, 38px); letter-spacing: -.05em; }.hero-content p { max-width: 590px; color: rgba(255,255,255,.66); font-size: 13px; line-height: 1.75; }.hero-actions { display: flex; align-items: center; gap: 18px; margin-top: 22px; }.hero-actions .el-button { height: 42px; border: 0; border-radius: 6px; background: #8ed3ca; color: #10201e; box-shadow: none; font-weight: 800; }.hero-actions > button { display: flex; align-items: center; gap: 5px; border: 0; background: transparent; color: rgba(255,255,255,.72); cursor: pointer; font: inherit; font-size: 11px; }.hero-actions > button:hover { color: #fff; }.hero-facts { display: flex; gap: 24px; margin-top: 28px; }.hero-facts span { color: rgba(255,255,255,.52); font-size: 10px; }.hero-facts strong { margin-right: 4px; color: #fff; font: 700 15px ui-monospace, monospace; }.readiness-block { display: flex; align-items: center; gap: 21px; justify-content: flex-end; }.readiness-ring { --score: 0; width: 145px; height: 145px; display: grid; flex: 0 0 145px; place-items: center; border-radius: 50%; background: conic-gradient(#8ed3ca calc(var(--score) * 1%), rgba(255,255,255,.12) 0); position: relative; }.readiness-ring::after { content: ''; position: absolute; inset: 11px; border-radius: 50%; background: #18312d; }.readiness-ring span { position: relative; z-index: 1; text-align: center; }.readiness-ring strong,.readiness-ring small { display: block; }.readiness-ring strong { font: 700 37px ui-monospace, monospace; }.readiness-ring small { margin-top: 3px; color: rgba(255,255,255,.55); font-size: 9px; }.readiness-copy { min-width: 100px; }.readiness-copy span,.readiness-copy strong,.readiness-copy p { display: block; }.readiness-copy span { color: rgba(255,255,255,.5); font-size: 10px; }.readiness-copy strong { margin-top: 6px; color: #8ed3ca; font-size: 16px; }.readiness-copy p { margin-top: 8px; color: rgba(255,255,255,.55); font-size: 10px; line-height: 1.6; }
.panel-surface { border: 1px solid var(--border); border-radius: 10px; background: color-mix(in srgb, var(--bg-card) 93%, var(--blue-light)); box-shadow: 0 14px 34px rgba(23,32,42,.05); }.section-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 15px; }.section-heading h2 { margin-top: 7px; color: var(--text); font-size: 18px; letter-spacing: -.02em; }.section-heading p { margin-top: 5px; color: var(--text-muted); font-size: 11px; }.completion-badge { padding: 5px 8px; border-radius: 999px; background: var(--blue-light); color: var(--blue); font-size: 10px; font-weight: 700; }.workflow-panel { margin-bottom: 18px; padding: 22px 24px 24px; }.workflow-steps { display: grid; grid-template-columns: repeat(3,1fr); gap: 10px; margin-top: 20px; }.workflow-step { display: grid; grid-template-columns: 32px 1fr 20px; align-items: center; gap: 11px; min-height: 88px; padding: 13px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); color: var(--text); text-align: left; cursor: pointer; transition: transform .18s, border-color .18s, background .18s; }.workflow-step:hover:not(:disabled) { transform: translateY(-2px); border-color: var(--blue); }.workflow-step.current { border-color: color-mix(in srgb, var(--blue) 45%, var(--border)); background: var(--blue-light); }.workflow-step.locked { cursor: not-allowed; opacity: .55; }.workflow-step.done .step-index { background: var(--success); color: #fff; }.step-index { display: grid; place-items: center; width: 29px; height: 29px; border-radius: 7px; background: var(--hover); color: var(--blue); font: 700 10px ui-monospace, monospace; }.step-copy { min-width: 0; }.step-copy small,.step-copy strong,.step-copy em { display: block; }.step-copy small { color: var(--text-muted); font-size: 9px; }.step-copy strong { overflow: hidden; margin-top: 5px; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }.step-copy em { overflow: hidden; margin-top: 4px; color: var(--text-muted); font-size: 9px; font-style: normal; text-overflow: ellipsis; white-space: nowrap; }.step-state { color: var(--text-muted); }.workflow-step.done .step-state { color: var(--success); }
.signal-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 10px; margin-bottom: 18px; }.signal-card { position: relative; min-width: 0; padding: 16px 17px; overflow: hidden; border: 1px solid var(--border); border-radius: 9px; background: var(--bg-card); color: var(--text); text-align: left; cursor: pointer; transition: transform .18s, box-shadow .18s; }.signal-card:hover { transform: translateY(-2px); box-shadow: 0 12px 25px rgba(23,32,42,.08); }.signal-card::before { content: ''; position: absolute; inset: 0 auto 0 0; width: 3px; background: var(--blue); }.signal-card.green::before { background: var(--success); }.signal-card.amber::before { background: #d99a2b; }.signal-card.coral::before { background: var(--accent); }.signal-card span,.signal-card strong,.signal-card small { display: block; }.signal-card > span { display: flex; align-items: center; gap: 6px; color: var(--text-muted); font-size: 10px; }.signal-card > span .el-icon { color: var(--blue); }.signal-card.green > span .el-icon { color: var(--success); }.signal-card.amber > span .el-icon { color: #c88b1b; }.signal-card.coral > span .el-icon { color: var(--accent); }.signal-card strong { margin: 9px 0 4px; font: 700 26px ui-monospace, monospace; }.signal-card strong small { display: inline; margin-left: 2px; color: var(--text-muted); font: 500 12px inherit; }.signal-card > small { color: var(--text-muted); font-size: 10px; }.signal-arrow { position: absolute; top: 16px; right: 15px; color: var(--text-muted); font-size: 14px; }
.dashboard-grid { display: grid; grid-template-columns: minmax(0,1.55fr) minmax(280px,.75fr); gap: 14px; margin-bottom: 14px; }.activity-panel,.insight-panel,.sessions-panel,.coverage-panel { min-width: 0; padding: 22px 24px; }.heading-number { color: var(--blue); font: 700 25px ui-monospace, monospace; }.heading-number small { color: var(--text-muted); font: 500 10px inherit; }.activity-chart { margin-top: 16px; }.activity-chart svg { width: 100%; height: 190px; overflow: visible; }.chart-grid { stroke: var(--border); stroke-dasharray: 3 5; }.chart-area { fill: color-mix(in srgb, var(--blue) 12%, transparent); }.chart-line { fill: none; stroke: var(--blue); stroke-width: 3; stroke-linecap: round; stroke-linejoin: round; }.chart-point { fill: var(--bg-card); stroke: var(--blue); stroke-width: 3; }.chart-labels { display: grid; grid-template-columns: repeat(7,1fr); margin: -1px 7px 0; color: var(--text-muted); font-size: 9px; text-align: center; }.insight-panel { background: linear-gradient(145deg, color-mix(in srgb, var(--blue-light) 38%, var(--bg-card)), var(--bg-card)); }.heading-icon { color: var(--blue); font-size: 19px; }.insight-callout { display: flex; gap: 10px; margin: 22px 0 19px; padding: 12px; border-radius: 8px; background: var(--bg-card); }.insight-dot { width: 7px; height: 7px; flex: 0 0 7px; margin-top: 4px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent) 12%, transparent); }.insight-callout strong { color: var(--text); font-size: 12px; }.insight-callout p { margin-top: 5px; color: var(--text-secondary); font-size: 10px; line-height: 1.6; }.insight-list { display: grid; gap: 15px; }.insight-list > div { display: grid; grid-template-columns: 1fr auto; gap: 6px 12px; align-items: center; }.insight-list span { color: var(--text-muted); font-size: 10px; }.insight-list strong { color: var(--text); font: 700 12px ui-monospace, monospace; }.insight-list strong.good { color: var(--success); }.insight-list i { grid-column: 1 / -1; display: block; height: 5px; overflow: hidden; border-radius: 4px; background: var(--hover); }.insight-list i b { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, var(--blue), #52b882); }.text-action { display: flex; align-items: center; gap: 4px; margin-top: 24px; border: 0; background: transparent; color: var(--blue); cursor: pointer; font-size: 11px; font-weight: 700; }
.bottom-grid { display: grid; grid-template-columns: minmax(0,1.1fr) minmax(280px,.9fr); gap: 14px; }.sessions-panel .section-heading,.coverage-panel .section-heading { margin-bottom: 16px; }.session-list { border-top: 1px solid var(--border); }.session-list button { display: flex; align-items: center; gap: 10px; width: 100%; padding: 13px 2px; border: 0; border-bottom: 1px solid var(--border); background: transparent; color: var(--text); text-align: left; cursor: pointer; }.session-list button:last-child { border-bottom: 0; }.session-list button:hover .session-main strong { color: var(--blue); }.session-status { width: 8px; height: 8px; flex: 0 0 8px; border-radius: 50%; background: var(--success); }.session-status.active { background: #d99a2b; box-shadow: 0 0 0 4px color-mix(in srgb, #d99a2b 13%, transparent); }.session-main { min-width: 0; flex: 1; }.session-main strong,.session-main small { display: block; }.session-main strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }.session-main small { margin-top: 4px; color: var(--text-muted); font-size: 10px; }.session-list > button > .el-icon { color: var(--text-muted); }.coverage-list { display: grid; gap: 16px; margin-top: 22px; }.coverage-list > div { display: grid; grid-template-columns: 1fr auto; gap: 7px 12px; align-items: center; }.coverage-list span { color: var(--text-secondary); font-size: 11px; }.coverage-list strong { color: var(--text); font: 700 12px ui-monospace, monospace; }.coverage-list i { grid-column: 1 / -1; height: 6px; overflow: hidden; border-radius: 4px; background: var(--hover); }.coverage-list b { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, var(--success), #8ed3ca); }.panel-empty,.list-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 190px; color: var(--text-muted); text-align: center; }.panel-empty strong { margin-top: 10px; color: var(--text); font-size: 12px; }.panel-empty p { margin: 5px 0 14px; font-size: 10px; }.empty-mark { display: grid; place-items: center; width: 40px; height: 40px; border-radius: 10px; background: var(--blue-light); color: var(--blue); font-size: 18px; }.list-empty { align-items: flex-start; min-height: 150px; font-size: 11px; }.list-empty button { display: flex; align-items: center; gap: 4px; margin-top: 10px; padding: 0; border: 0; background: transparent; color: var(--blue); cursor: pointer; font: inherit; font-weight: 700; }.list-empty.compact { align-items: center; min-height: 145px; }
@media (max-width: 1100px) { .readiness-hero { grid-template-columns: minmax(0,1fr) 260px; padding-inline: 30px; }.signal-grid { grid-template-columns: repeat(2,1fr); } }
@media (max-width: 860px) { .workspace-header { align-items: flex-start; flex-direction: column; gap: 12px; }.workspace-tools { padding: 0; }.readiness-hero { grid-template-columns: 1fr; }.readiness-block { justify-content: flex-start; }.hero-orb { right: -5%; bottom: 0; width: 55%; opacity: .2; }.dashboard-grid,.bottom-grid { grid-template-columns: 1fr; }.workflow-steps { grid-template-columns: 1fr; }.workflow-step { min-height: 72px; } }
@media (max-width: 600px) { .workspace-home { padding-bottom: 8px; }.workspace-header h1 { font-size: 27px; }.workspace-header p { line-height: 1.6; }.sync-state { display: none; }.readiness-hero { min-height: 0; padding: 25px 21px 22px; border-radius: 9px; }.hero-content h2 { font-size: 25px; }.hero-actions { align-items: flex-start; flex-direction: column; gap: 13px; }.hero-facts { flex-wrap: wrap; gap: 12px 19px; margin-top: 22px; }.readiness-block { align-items: center; gap: 16px; margin-top: 5px; }.readiness-ring { width: 108px; height: 108px; flex-basis: 108px; }.readiness-ring::after { inset: 9px; }.readiness-ring strong { font-size: 29px; }.workflow-panel,.activity-panel,.insight-panel,.sessions-panel,.coverage-panel { padding: 18px 15px; }.section-heading h2 { font-size: 16px; }.signal-grid { gap: 8px; }.signal-card { padding: 14px 12px; }.signal-card strong { font-size: 22px; }.signal-card > small { font-size: 9px; }.activity-chart svg { min-width: 620px; }.activity-chart { overflow-x: auto; padding-bottom: 3px; }.chart-labels { min-width: 620px; }.session-list button { gap: 8px; }.session-list :deep(.el-tag) { display: none; }.workspace-alert { align-items: flex-start; }.workspace-alert :deep(.el-button) { flex-shrink: 0; } }
/* ======== Workspace premium layer ======== */
.workspace-home { position: relative; }
.workspace-header { padding: 5px 2px 0; }
.workspace-header h1 { text-shadow: 0 12px 28px color-mix(in srgb, var(--blue) 9%, transparent); }
.refresh-button { border-radius: 10px; background: var(--surface-glass); box-shadow: 0 10px 24px rgba(16, 54, 47, .05); backdrop-filter: blur(12px); }
.readiness-hero {
  border-radius: 22px;
  background: linear-gradient(135deg, #0b211d 0%, #174239 57%, #2b7164 100%);
  box-shadow: 0 32px 82px rgba(13, 57, 47, .24);
}
.readiness-hero::before { content: ''; position: absolute; inset: 1px; border: 1px solid rgba(255, 255, 255, .12); border-radius: inherit; pointer-events: none; }
.readiness-hero::after { width: 560px; height: 560px; right: -180px; top: -220px; background: radial-gradient(circle, rgba(126, 227, 208, .3), transparent 65%); }
.hero-actions .el-button { border-radius: 12px; box-shadow: 0 14px 26px rgba(107, 208, 192, .18); }
.hero-actions .el-button:hover { box-shadow: 0 18px 34px rgba(107, 208, 192, .28); }
.readiness-ring { filter: drop-shadow(0 14px 26px rgba(0, 0, 0, .16)); }
.panel-surface {
  border-radius: 16px;
  background: var(--surface-glass);
  box-shadow: 0 22px 60px rgba(16, 54, 47, .065);
  backdrop-filter: blur(18px);
}
.workflow-panel, .activity-panel, .insight-panel, .sessions-panel, .coverage-panel { border-color: color-mix(in srgb, var(--blue) 13%, var(--border)); }
.workflow-step, .signal-card, .insight-callout { border-radius: 14px; }
.workflow-step { background: color-mix(in srgb, var(--bg-card) 70%, transparent); box-shadow: 0 10px 26px rgba(16, 54, 47, .035); }
.workflow-step:hover:not(:disabled) { transform: translateY(-4px); box-shadow: 0 18px 34px color-mix(in srgb, var(--blue) 11%, transparent); }
.signal-card { background: color-mix(in srgb, var(--bg-card) 70%, transparent); box-shadow: 0 13px 30px rgba(16, 54, 47, .04); }
.signal-card:hover { box-shadow: 0 20px 40px color-mix(in srgb, var(--blue) 12%, transparent); transform: translateY(-5px); }
.signal-card::before { width: 4px; border-radius: 4px 0 0 4px; }
.insight-panel { background: linear-gradient(145deg, color-mix(in srgb, var(--blue-light) 42%, var(--bg-card)), var(--surface-glass)); }
.activity-chart, .coverage-list, .session-list { position: relative; }
.session-list button { transition: padding .2s, background .2s; border-radius: 10px; }
.session-list button:hover { padding-inline: 10px; background: color-mix(in srgb, var(--blue-light) 36%, transparent); }
@media (max-width: 860px) { .readiness-hero { border-radius: 18px; } }
@media (max-width: 600px) {
  .readiness-hero { border-radius: 17px; box-shadow: 0 24px 54px rgba(13, 57, 47, .2); }
  .panel-surface { border-radius: 14px; }
  .workflow-step, .signal-card, .insight-callout { border-radius: 12px; }
}
@media (max-width: 600px) {
  .hero-orb { display: none; }
  .readiness-hero::after { width: 320px; height: 320px; right: -150px; top: -105px; }
  .hero-content, .readiness-block { z-index: 2; }
}
</style>
