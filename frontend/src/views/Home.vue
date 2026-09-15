<template>
  <main class="dashboard">
    <template v-if="isLoggedIn">
      <header class="dashboard-head">
        <div><span class="eyebrow">训练驾驶舱</span><h1>{{ greeting }}，{{ userStore.username }}</h1><p>{{ todayText }} · 保持有目标的练习，比一次刷很多题更有效。</p></div>
        <el-button type="primary" size="large" :icon="VideoPlay" @click="router.push('/interview')">开始模拟面试</el-button>
      </header>

      <section class="overview-grid" v-loading="loading">
        <div class="primary-metric"><span>训练完成率</span><strong>{{ completionRate }}<small>%</small></strong><div class="progress-track"><i :style="{ width: completionRate + '%' }"></i></div><p>{{ stats.completedCount }} 场完成，{{ stats.activeCount }} 场待继续</p></div>
        <div class="metric"><span>模拟面试</span><strong>{{ stats.interviewCount }}</strong><small>累计训练场次</small></div>
        <div class="metric"><span>简历均分</span><strong>{{ stats.averageResumeScore || '--' }}</strong><small>来自 {{ stats.resumeCount }} 份报告</small></div>
        <div class="metric"><span>可练习题目</span><strong>{{ stats.questionCount }}</strong><small>公共题与私人题</small></div>
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

    <section v-else class="guest-hero">
      <span class="eyebrow">AI Interview Coach</span><h1>用可复盘的训练，准备下一场技术面试</h1><p>简历诊断、实时模拟面试、结构化能力评估和私人题库，集中在一个训练工作台。</p><div><el-button type="primary" size="large" @click="router.push('/login')">开始训练</el-button><el-button size="large" @click="router.push('/login')">登录</el-button></div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, ChatDotRound, ChatLineRound, Collection, DocumentChecked, VideoPlay } from '@element-plus/icons-vue'
import api, { getSessions } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter(); const userStore = useUserStore(); const isLoggedIn = userStore.isLoggedIn()
const loading = ref(false); const recentSessions = ref([])
const stats = reactive({ interviewCount: 0, completedCount: 0, activeCount: 0, resumeCount: 0, averageResumeScore: 0, questionCount: 0, weeklyTrend: [], positionDistribution: [] })
const greeting = computed(() => new Date().getHours() < 12 ? '早上好' : new Date().getHours() < 18 ? '下午好' : '晚上好')
const todayText = computed(() => new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }))
const completionRate = computed(() => stats.interviewCount ? Math.round(stats.completedCount / stats.interviewCount * 100) : 0)
const weeklyTotal = computed(() => stats.weeklyTrend.reduce((sum, item) => sum + Number(item.count), 0))
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
</script>

<style scoped>
.dashboard { max-width: 1180px; margin: 0 auto; color: var(--text); }
.dashboard-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; padding: 10px 0 24px; }
.eyebrow { display: block; margin-bottom: 7px; color: var(--blue); font-size: 12px; font-weight: 700; }
.dashboard-head h1 { margin-bottom: 7px; font-size: 30px; letter-spacing: 0; }.dashboard-head p { color: var(--text-muted); font-size: 13px; }
.overview-grid { display: grid; grid-template-columns: 1.35fr repeat(3, 1fr); min-height: 122px; margin-bottom: 16px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); }
.overview-grid > div { padding: 20px 22px; border-right: 1px solid var(--border); }.overview-grid > div:last-child { border-right: 0; }
.overview-grid span, .overview-grid small { color: var(--text-muted); font-size: 12px; }.overview-grid strong { display: block; margin: 9px 0 4px; font-size: 27px; }.overview-grid strong small { margin-left: 2px; font-size: 14px; }.overview-grid p { margin-top: 8px; color: var(--text-muted); font-size: 11px; }
.progress-track { height: 5px; overflow: hidden; border-radius: 3px; background: var(--hover); }.progress-track i { display: block; height: 100%; border-radius: inherit; background: var(--blue); }
.metric { display: flex; flex-direction: column; justify-content: center; }.metric strong { font-size: 28px; }
.main-grid { display: grid; grid-template-columns: minmax(0, 1.65fr) minmax(300px, .85fr); gap: 16px; margin-bottom: 16px; }.lower-grid { display: grid; grid-template-columns: 1.35fr 1fr; gap: 16px; }
.panel { border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); overflow: hidden; }.panel-head { display: flex; align-items: center; justify-content: space-between; padding: 18px 20px; border-bottom: 1px solid var(--border); }.panel-head h2 { margin-bottom: 4px; font-size: 15px; }.panel-head p { color: var(--text-muted); font-size: 11px; }.trend-total { color: var(--blue); font-size: 13px; font-weight: 700; }
.chart-wrap { position: relative; padding: 14px 18px 10px; }.chart-wrap svg { display: block; width: 100%; height: 210px; overflow: visible; }.grid-line { stroke: var(--border); stroke-width: 1; }.chart-line { fill: none; stroke: var(--blue); stroke-width: 3; stroke-linecap: round; stroke-linejoin: round; }.chart-area { fill: var(--blue-light); opacity: .65; }.chart-dot { fill: var(--bg-card); stroke: var(--blue); stroke-width: 3; }.chart-labels { display: grid; grid-template-columns: repeat(7, 1fr); padding: 0 8px; color: var(--text-muted); font-size: 10px; text-align: center; }.chart-empty { padding: 0 20px 18px; color: var(--text-muted); font-size: 12px; text-align: center; }
.plan-list button, .recent-list button { width: 100%; display: flex; align-items: center; gap: 12px; padding: 15px 18px; border: 0; border-bottom: 1px solid var(--border); background: transparent; color: var(--text); text-align: left; cursor: pointer; }.plan-list button:last-child, .recent-list button:last-child { border-bottom: 0; }.plan-list button:hover, .recent-list button:hover { background: var(--hover); }.plan-list button > span:nth-child(2), .recent-main { min-width: 0; flex: 1; }.plan-list strong, .plan-list small, .recent-main strong, .recent-main small { display: block; }.plan-list strong, .recent-main strong { font-size: 13px; }.plan-list small, .recent-main small { margin-top: 4px; color: var(--text-muted); font-size: 11px; }
.plan-icon, .recent-icon { width: 36px; height: 36px; display: grid; flex: 0 0 36px; place-items: center; border-radius: 7px; }.plan-icon.blue { color: #1677ff; background: #eaf3ff; }.plan-icon.green { color: #289b62; background: #e9f7ef; }.plan-icon.amber { color: #c78517; background: #fff4df; }.recent-icon { color: var(--text-secondary); background: var(--hover); }.row-arrow { color: var(--text-muted); }
.distribution-list { padding: 16px 20px 20px; }.distribution-list > div { margin-bottom: 15px; }.distribution-list > div:last-child { margin-bottom: 0; }.distribution-list > div > div:first-child { display: flex; justify-content: space-between; margin-bottom: 7px; font-size: 12px; }.distribution-list strong { color: var(--text-muted); }.bar { height: 6px; overflow: hidden; border-radius: 3px; background: var(--hover); }.bar i { display: block; height: 100%; border-radius: inherit; background: #36a269; }.empty-line { min-height: 155px; display: grid; place-items: center; color: var(--text-muted); font-size: 12px; }
.guest-hero { max-width: 850px; margin: 0 auto; padding: 110px 20px; text-align: center; }.guest-hero h1 { margin-bottom: 18px; font-size: 44px; line-height: 1.2; }.guest-hero p { margin-bottom: 28px; color: var(--text-secondary); font-size: 17px; line-height: 1.7; }
@media (max-width: 900px) { .overview-grid { grid-template-columns: repeat(2, 1fr); }.overview-grid > div:nth-child(2) { border-right: 0; }.overview-grid > div:nth-child(-n+2) { border-bottom: 1px solid var(--border); }.main-grid, .lower-grid { grid-template-columns: 1fr; } }
@media (max-width: 600px) { .dashboard { width: 100%; padding: 0 2px; }.dashboard-head { align-items: flex-start; flex-direction: column; }.dashboard-head h1 { font-size: 25px; }.overview-grid > div { padding: 16px 14px; }.overview-grid strong { font-size: 23px; }.chart-wrap { padding: 8px 4px; overflow-x: auto; }.chart-wrap svg { min-width: 620px; }.chart-labels { min-width: 620px; }.guest-hero { padding: 60px 8px; text-align: left; }.guest-hero h1 { font-size: 30px; }.guest-hero p { font-size: 15px; } }
</style>
