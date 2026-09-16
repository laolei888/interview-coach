<template>
  <main class="dashboard">
    <template v-if="isLoggedIn">
      <header class="dashboard-head">
        <div><span class="eyebrow">训练驾驶舱</span><h1>{{ greeting }}，{{ userStore.username }}</h1><p>{{ todayText }} · 今天也向目标岗位靠近一点。</p></div>
        <div class="head-status"><i></i><span>训练数据已同步</span></div>
      </header>

      <section class="focus-banner">
        <div class="focus-copy"><span>今日优先任务</span><h2>{{ focusTitle }}</h2><p>{{ focusDescription }}</p><div><el-button type="primary" size="large" :icon="VideoPlay" @click="router.push(focusPath)">{{ focusAction }}</el-button><button type="button" @click="router.push('/history')">查看上次复盘<el-icon><ArrowRight /></el-icon></button></div></div>
        <div class="readiness-score"><div class="readiness-ring" :style="{ '--score': readinessScore }"><span><strong>{{ readinessScore }}</strong><small>准备度</small></span></div><ul><li><span>面试完成</span><b>{{ stats.completedCount }} 场</b></li><li><span>简历表现</span><b>{{ stats.averageResumeScore || '--' }} 分</b></li><li><span>题库掌握</span><b>{{ questionMastery }}%</b></li></ul></div>
      </section>

      <section class="overview-grid" v-loading="loading">
        <div class="metric"><span><el-icon><ChatDotRound /></el-icon>模拟面试</span><strong>{{ stats.interviewCount }}</strong><small>{{ stats.activeCount ? `${stats.activeCount} 场等待继续` : '当前没有未完成会话' }}</small></div>
        <div class="metric"><span><el-icon><DocumentChecked /></el-icon>简历诊断</span><strong>{{ stats.resumeCount }}</strong><small>{{ stats.resumeCount ? `平均评分 ${stats.averageResumeScore || '--'}` : '尚未生成分析报告' }}</small></div>
        <div class="metric"><span><el-icon><Collection /></el-icon>题目练习</span><strong>{{ stats.questionProgress.practiced }}</strong><small>{{ stats.questionProgress.review }} 道需要继续复习</small></div>
        <div class="metric"><span><el-icon><CircleCheck /></el-icon>训练完成率</span><strong>{{ completionRate }}<small>%</small></strong><div class="progress-track"><i :style="{ width: completionRate + '%' }"></i></div></div>
      </section>

      <section class="main-grid">
        <div class="panel trend-panel">
          <div class="panel-head"><div><h2>近 7 天训练趋势</h2><p>每日创建的模拟面试场次</p></div><span class="trend-total">{{ weeklyTotal }} 次</span></div>
          <div class="chart-wrap">
            <svg viewBox="0 0 700 210" role="img" aria-label="近七天面试趋势图">
              <line v-for="y in [35, 85, 135, 185]" :key="y" x1="35" :y1="y" x2="675" :y2="y" class="grid-line" />
              <path :d="areaPath" class="chart-area" />
              <polyline :points="linePoints" class="chart-line" />
              <circle v-for="point in chartPoints" :key="point.date" :cx="point.x" :cy="point.y" r="5" class="chart-dot"><title>{{ point.date }}：{{ point.count }} 次</title></circle>
            </svg>
            <div class="chart-labels"><span v-for="item in stats.weeklyTrend" :key="item.date">{{ item.date }}</span></div>
          </div>
          <div v-if="weeklyTotal === 0" class="chart-empty">本周还没有训练记录，从一次模拟面试开始。</div>
        </div>

        <aside class="panel plan-panel">
          <div class="panel-head"><div><h2>今日训练计划</h2><p>根据当前数据动态推荐</p></div></div>
          <div class="plan-list">
            <button type="button" @click="router.push('/interview')"><span class="plan-icon blue"><el-icon><ChatDotRound /></el-icon></span><span><strong>{{ stats.activeCount ? '继续未完成面试' : '完成一场岗位面试' }}</strong><small>{{ stats.activeCount ? `${stats.activeCount} 场会话等待继续` : '建议训练 20-30 分钟' }}</small></span><el-icon><ArrowRight /></el-icon></button>
            <button type="button" @click="router.push('/questions')"><span class="plan-icon green"><el-icon><Collection /></el-icon></span><span><strong>完成知识点热身</strong><small>从题库随机练习 5 道题</small></span><el-icon><ArrowRight /></el-icon></button>
            <button type="button" @click="router.push('/resume')"><span class="plan-icon amber"><el-icon><DocumentChecked /></el-icon></span><span><strong>{{ stats.resumeCount ? '回顾简历诊断' : '完成首次简历诊断' }}</strong><small>{{ stats.resumeCount ? `当前平均 ${stats.averageResumeScore || '--'} 分` : '发现表达和结构问题' }}</small></span><el-icon><ArrowRight /></el-icon></button>
          </div>
        </aside>
      </section>

      <section class="lower-grid">
        <div class="panel recent-panel">
          <div class="panel-head"><div><h2>最近训练</h2><p>继续会话或进入历史复盘</p></div><el-button text @click="router.push('/history')">全部记录</el-button></div>
          <div v-if="recentSessions.length" class="recent-list">
            <button v-for="item in recentSessions" :key="item.id" type="button" @click="openSession(item)"><span class="recent-icon"><el-icon><ChatLineRound /></el-icon></span><span class="recent-main"><strong>{{ item.position }}</strong><small>{{ formatDate(item.createdAt) }}</small></span><el-tag :type="item.status === 'completed' ? 'success' : 'warning'" size="small" effect="plain">{{ item.status === 'completed' ? '已完成' : '进行中' }}</el-tag><el-icon class="row-arrow"><ArrowRight /></el-icon></button>
          </div>
          <div v-else class="empty-line">暂无面试记录</div>
        </div>

        <div class="panel distribution-panel">
          <div class="panel-head"><div><h2>岗位训练分布</h2><p>你的练习重心</p></div></div>
          <div v-if="stats.positionDistribution.length" class="distribution-list">
            <div v-for="item in stats.positionDistribution" :key="item.name"><div><span>{{ item.name }}</span><strong>{{ item.count }}</strong></div><div class="bar"><i :style="{ width: distributionWidth(item.count) }"></i></div></div>
          </div>
          <div v-else class="empty-line">完成面试后生成岗位分布</div>
        </div>
      </section>
    </template>

    <template v-else>
      <section class="guest-hero">
        <div class="hero-content">
          <span class="eyebrow">面向技术岗位的 AI 求职训练平台</span>
          <h1>把每次准备，变成看得见的进步</h1>
          <p>岗位定向简历诊断、实时模拟面试与逐题复盘，帮你从“刷过题”走到“讲得清”。</p>
          <div class="hero-actions"><el-button type="primary" size="large" @click="router.push('/login')">开始训练<el-icon><ArrowRight /></el-icon></el-button><button type="button" @click="scrollToCapabilities">查看能力</button></div>
          <div class="trust-row"><span><el-icon><CircleCheck /></el-icon> 数据按用户隔离</span><span><el-icon><CircleCheck /></el-icon> 报告可导出</span><span><el-icon><CircleCheck /></el-icon> 支持手机端</span></div>
        </div>
      </section>
      <section id="capabilities" class="capability-band">
        <div><span>01</span><h2>岗位定向诊断</h2><p>结合目标岗位与 JD，检查 ATS 关键词、能力缺口和项目证据。</p></div>
        <div><span>02</span><h2>真实对话训练</h2><p>保留完整面试上下文，每道回答即时评分并继续追问。</p></div>
        <div><span>03</span><h2>结构化复盘</h2><p>从表现趋势到逐题记录，把模糊感受转成下一步行动。</p></div>
      </section>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, ChatDotRound, ChatLineRound, CircleCheck, Collection, DocumentChecked, VideoPlay } from '@element-plus/icons-vue'
import api, { getSessions } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter(); const userStore = useUserStore(); const isLoggedIn = userStore.isLoggedIn()
const loading = ref(false); const recentSessions = ref([])
const stats = reactive({ interviewCount: 0, completedCount: 0, activeCount: 0, resumeCount: 0, averageResumeScore: 0, questionCount: 0, questionProgress: { practiced: 0, mastered: 0, review: 0 }, weeklyTrend: [], positionDistribution: [] })
const greeting = computed(() => new Date().getHours() < 12 ? '早上好' : new Date().getHours() < 18 ? '下午好' : '晚上好')
const todayText = computed(() => new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }))
const completionRate = computed(() => stats.interviewCount ? Math.round(stats.completedCount / stats.interviewCount * 100) : 0)
const weeklyTotal = computed(() => stats.weeklyTrend.reduce((sum, item) => sum + Number(item.count), 0))
const questionMastery = computed(() => stats.questionProgress.practiced ? Math.round(stats.questionProgress.mastered / stats.questionProgress.practiced * 100) : 0)
const readinessScore = computed(() => Math.round(Math.min(100, completionRate.value * .35 + (stats.averageResumeScore || 0) * .35 + questionMastery.value * .3)))
const focusTitle = computed(() => stats.activeCount ? '完成正在进行的模拟面试' : !stats.resumeCount ? '先完成一次岗位定向简历诊断' : stats.questionProgress.review ? `复习 ${stats.questionProgress.review} 道薄弱题目` : '开始一场目标岗位模拟面试')
const focusDescription = computed(() => stats.activeCount ? '保持回答上下文，完成后会生成结构化能力评估。' : !stats.resumeCount ? '上传简历和目标 JD，先明确本轮求职准备的重点。' : stats.questionProgress.review ? '集中处理标记为待复习的知识点，建立稳定表达。' : '用一次完整对话检查技术深度、表达结构和追问应对。')
const focusPath = computed(() => stats.activeCount ? '/interview' : !stats.resumeCount ? '/resume' : stats.questionProgress.review ? '/questions' : '/interview')
const focusAction = computed(() => stats.activeCount ? '继续面试' : !stats.resumeCount ? '分析简历' : stats.questionProgress.review ? '开始复习' : '开始面试')
const chartPoints = computed(() => {
  const max = Math.max(1, ...stats.weeklyTrend.map(item => Number(item.count)))
  return stats.weeklyTrend.map((item, index) => ({ ...item, x: 45 + index * 103, y: 185 - Number(item.count) / max * 135 }))
})
const linePoints = computed(() => chartPoints.value.map(item => `${item.x},${item.y}`).join(' '))
const areaPath = computed(() => chartPoints.value.length ? `M ${chartPoints.value[0].x} 185 L ${linePoints.value.replaceAll(' ', ' L ')} L ${chartPoints.value.at(-1).x} 185 Z` : '')

onMounted(async () => {
  if (!isLoggedIn) return
  loading.value = true
  try {
    const [statsRes, sessionsRes] = await Promise.all([api.get('/stats/dashboard'), getSessions()])
    if (statsRes.data.code === 200) Object.assign(stats, statsRes.data.data)
    if (sessionsRes.data.code === 200) recentSessions.value = sessionsRes.data.data.slice(0, 4)
  } finally { loading.value = false }
})
const openSession = item => item.status === 'completed' ? router.push('/history') : router.push({ path: '/interview', query: { sessionId: item.id } })
const distributionWidth = count => `${Math.max(8, count / Math.max(1, ...stats.positionDistribution.map(item => item.count)) * 100)}%`
const formatDate = value => value ? new Date(value).toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''
const scrollToCapabilities = () => document.querySelector('#capabilities')?.scrollIntoView({ behavior: 'smooth' })
</script>

<style scoped>
.dashboard { max-width: 1260px; margin: 0 auto; color: var(--text); }
.dashboard-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; padding: 10px 0 24px; }
.eyebrow { display: block; margin-bottom: 7px; color: var(--blue); font-size: 12px; font-weight: 700; }
.dashboard-head h1 { margin-bottom: 7px; font-size: 32px; letter-spacing: 0; }.dashboard-head p { color: var(--text-muted); font-size: 13px; }.head-status { display: flex; align-items: center; gap: 8px; color: var(--text-muted); font-size: 10px; }.head-status i { width: 7px; height: 7px; border-radius: 50%; background: var(--success); box-shadow: 0 0 0 4px color-mix(in srgb, var(--success) 14%, transparent); }
.focus-banner { min-height: 258px; display: grid; grid-template-columns: 1fr 390px; align-items: center; gap: 40px; margin-bottom: 16px; padding: 34px 38px; border-radius: 8px; position: relative; overflow: hidden; background: #172724; color: #fff; }.focus-banner::after { content: ''; position: absolute; inset: 0 0 0 62%; background: url('https://images.unsplash.com/photo-1552664730-d307ca884978?auto=format&fit=crop&w=900&q=80') center/cover; opacity: .12; }.focus-copy,.readiness-score { position: relative; z-index: 1; }.focus-copy > span { color: #82cfc4; font-size: 10px; font-weight: 700; }.focus-copy h2 { max-width: 600px; margin: 10px 0 9px; font-size: 27px; }.focus-copy > p { max-width: 620px; color: rgba(255,255,255,.62); font-size: 12px; line-height: 1.7; }.focus-copy > div { display: flex; align-items: center; gap: 18px; margin-top: 24px; }.focus-copy > div > button:not(.el-button) { display: flex; align-items: center; gap: 5px; border: 0; background: transparent; color: rgba(255,255,255,.68); cursor: pointer; font: inherit; font-size: 11px; }.readiness-score { display: flex; align-items: center; justify-content: flex-end; gap: 28px; }.readiness-ring { --score: 0; width: 132px; height: 132px; display: grid; flex: 0 0 132px; place-items: center; border-radius: 50%; background: conic-gradient(#62c8ba calc(var(--score) * 1%), rgba(255,255,255,.1) 0); position: relative; }.readiness-ring::after { content: ''; position: absolute; inset: 10px; border-radius: 50%; background: #172724; }.readiness-ring span { position: relative; z-index: 1; text-align: center; }.readiness-ring strong,.readiness-ring small { display: block; }.readiness-ring strong { font-size: 34px; }.readiness-ring small { margin-top: 2px; color: rgba(255,255,255,.5); font-size: 9px; }.readiness-score ul { min-width: 120px; list-style: none; }.readiness-score li { padding: 9px 0; border-bottom: 1px solid rgba(255,255,255,.1); }.readiness-score li:last-child { border: 0; }.readiness-score li span,.readiness-score li b { display: block; }.readiness-score li span { color: rgba(255,255,255,.45); font-size: 9px; }.readiness-score li b { margin-top: 3px; font-size: 12px; }
.overview-grid { display: grid; grid-template-columns: repeat(4,1fr); min-height: 116px; margin-bottom: 16px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); }
.overview-grid > div { padding: 19px 22px; border-right: 1px solid var(--border); }.overview-grid > div:last-child { border-right: 0; }
.overview-grid span, .overview-grid small { color: var(--text-muted); font-size: 11px; }.overview-grid span { display: flex; align-items: center; gap: 6px; }.overview-grid span .el-icon { color: var(--blue); }.overview-grid strong { display: block; margin: 9px 0 4px; font-size: 26px; }.overview-grid strong small { display: inline; margin-left: 2px; font-size: 13px; }
.progress-track { height: 5px; overflow: hidden; border-radius: 3px; background: var(--hover); }.progress-track i { display: block; height: 100%; border-radius: inherit; background: var(--blue); }
.metric { display: flex; flex-direction: column; justify-content: center; }.metric strong { font-size: 28px; }
.main-grid { display: grid; grid-template-columns: minmax(0, 1.65fr) minmax(300px, .85fr); gap: 16px; margin-bottom: 16px; }.lower-grid { display: grid; grid-template-columns: 1.35fr 1fr; gap: 16px; }
.panel { border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); overflow: hidden; }.panel-head { display: flex; align-items: center; justify-content: space-between; padding: 18px 20px; border-bottom: 1px solid var(--border); }.panel-head h2 { margin-bottom: 4px; font-size: 15px; }.panel-head p { color: var(--text-muted); font-size: 11px; }.trend-total { color: var(--blue); font-size: 13px; font-weight: 700; }
.chart-wrap { position: relative; padding: 14px 18px 10px; }.chart-wrap svg { display: block; width: 100%; height: 210px; overflow: visible; }.grid-line { stroke: var(--border); stroke-width: 1; }.chart-line { fill: none; stroke: var(--blue); stroke-width: 3; stroke-linecap: round; stroke-linejoin: round; }.chart-area { fill: var(--blue-light); opacity: .65; }.chart-dot { fill: var(--bg-card); stroke: var(--blue); stroke-width: 3; }.chart-labels { display: grid; grid-template-columns: repeat(7, 1fr); padding: 0 8px; color: var(--text-muted); font-size: 10px; text-align: center; }.chart-empty { padding: 0 20px 18px; color: var(--text-muted); font-size: 12px; text-align: center; }
.plan-list button, .recent-list button { width: 100%; display: flex; align-items: center; gap: 12px; padding: 15px 18px; border: 0; border-bottom: 1px solid var(--border); background: transparent; color: var(--text); text-align: left; cursor: pointer; }.plan-list button:last-child, .recent-list button:last-child { border-bottom: 0; }.plan-list button:hover, .recent-list button:hover { background: var(--hover); }.plan-list button > span:nth-child(2), .recent-main { min-width: 0; flex: 1; }.plan-list strong, .plan-list small, .recent-main strong, .recent-main small { display: block; }.plan-list strong, .recent-main strong { font-size: 13px; }.plan-list small, .recent-main small { margin-top: 4px; color: var(--text-muted); font-size: 11px; }
.plan-icon, .recent-icon { width: 36px; height: 36px; display: grid; flex: 0 0 36px; place-items: center; border-radius: 7px; }.plan-icon.blue { color: #1677ff; background: #eaf3ff; }.plan-icon.green { color: #289b62; background: #e9f7ef; }.plan-icon.amber { color: #c78517; background: #fff4df; }.recent-icon { color: var(--text-secondary); background: var(--hover); }.row-arrow { color: var(--text-muted); }
.distribution-list { padding: 16px 20px 20px; }.distribution-list > div { margin-bottom: 15px; }.distribution-list > div:last-child { margin-bottom: 0; }.distribution-list > div > div:first-child { display: flex; justify-content: space-between; margin-bottom: 7px; font-size: 12px; }.distribution-list strong { color: var(--text-muted); }.bar { height: 6px; overflow: hidden; border-radius: 3px; background: var(--hover); }.bar i { display: block; height: 100%; border-radius: inherit; background: #36a269; }.empty-line { min-height: 155px; display: grid; place-items: center; color: var(--text-muted); font-size: 12px; }
.guest-hero { min-height: min(720px, calc(100vh - 56px)); display: flex; align-items: center; max-width: 100%; margin: 0; padding: 70px max(42px, calc((100vw - 1180px) / 2)) 96px; position: relative; isolation: isolate; background: #eef2f2 url('https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1800&q=88') right center/cover no-repeat; background-blend-mode: screen; }
.guest-hero::after { content: ''; position: absolute; inset: 0 42% 0 0; z-index: -1; background: rgba(244,246,248,.92); }
:global(html.dark) .guest-hero { background-color: #111716; background-blend-mode: multiply; }
:global(html.dark) .guest-hero::after { background: rgba(17,23,22,.9); }
.hero-content { max-width: 650px; }.hero-content h1 { max-width: 620px; margin: 12px 0 20px; font-size: clamp(42px, 5vw, 68px); line-height: 1.06; font-weight: 800; }.hero-content > p { max-width: 590px; color: var(--text-secondary); font-size: 17px; line-height: 1.8; }.hero-actions { display: flex; align-items: center; gap: 18px; margin: 30px 0 24px; }.hero-actions > button:not(.el-button) { border: 0; background: transparent; color: var(--text-secondary); font: inherit; font-size: 14px; cursor: pointer; }.trust-row { display: flex; flex-wrap: wrap; gap: 18px; color: var(--text-muted); font-size: 12px; }.trust-row span { display: flex; align-items: center; gap: 5px; }.trust-row .el-icon { color: var(--success); }
.capability-band { display: grid; grid-template-columns: repeat(3,1fr); max-width: 1180px; margin: 0 auto 60px; border-top: 1px solid var(--border); }.capability-band > div { padding: 30px 32px; border-right: 1px solid var(--border); }.capability-band > div:last-child { border: 0; }.capability-band span { color: var(--accent); font-size: 11px; font-weight: 800; }.capability-band h2 { margin: 10px 0 8px; font-size: 17px; }.capability-band p { color: var(--text-muted); font-size: 12px; line-height: 1.7; }
@media (max-width: 1050px) { .focus-banner { grid-template-columns: 1fr 310px; }.readiness-score { gap: 18px; }.readiness-ring { width: 110px; height: 110px; flex-basis: 110px; } }
@media (max-width: 900px) { .focus-banner { grid-template-columns: 1fr; }.readiness-score { justify-content: flex-start; }.readiness-score ul { display: flex; gap: 25px; }.readiness-score li { border: 0; }.overview-grid { grid-template-columns: repeat(2, 1fr); }.overview-grid > div:nth-child(2) { border-right: 0; }.overview-grid > div:nth-child(-n+2) { border-bottom: 1px solid var(--border); }.main-grid, .lower-grid { grid-template-columns: 1fr; } }
@media (max-width: 820px) { .guest-hero { min-height: auto; padding: 58px 22px 54px; background-position: 62% center; }.guest-hero::after { inset: 0; background: rgba(244,246,248,.82); }:global(html.dark) .guest-hero::after { background: rgba(17,23,22,.84); }.hero-content h1 { font-size: 43px; }.capability-band { grid-template-columns: 1fr; margin: 0 22px 42px; }.capability-band > div { padding: 24px 0; border-right: 0; border-bottom: 1px solid var(--border); } }
@media (max-width: 600px) { .dashboard { width: 100%; padding: 0 2px; }.dashboard-head { align-items: flex-start; flex-direction: column; }.dashboard-head h1 { font-size: 25px; }.head-status { display: none; }.focus-banner { min-height: 0; padding: 26px 22px; }.focus-copy h2 { font-size: 22px; }.focus-copy > div { align-items: flex-start; flex-direction: column; gap: 12px; }.readiness-score { align-items: flex-start; }.readiness-ring { width: 92px; height: 92px; flex-basis: 92px; }.readiness-ring strong { font-size: 27px; }.readiness-score ul { display: block; }.readiness-score li { padding: 4px 0 8px; }.overview-grid > div { padding: 16px 14px; }.overview-grid strong { font-size: 23px; }.chart-wrap { padding: 8px 4px; overflow-x: auto; }.chart-wrap svg { min-width: 620px; }.chart-labels { min-width: 620px; }.guest-hero { padding: 42px 16px 46px; }.hero-content h1 { font-size: 36px; }.hero-content > p { font-size: 15px; }.trust-row { gap: 10px 14px; }.capability-band { margin-inline: 16px; } }
</style>
