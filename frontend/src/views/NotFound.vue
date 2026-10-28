<template>
  <main class="not-found-page">
    <section class="not-found-card">
      <div class="status-block">
        <span class="eyebrow"><i></i>404 / ROUTE NOT FOUND</span>
        <h1>这个训练页面不存在</h1>
        <p>可能是地址输入有误，或者这个功能已经被移动。你可以回到训练驾驶舱，继续简历、面试和题库练习。</p>
        <div class="actions">
          <el-button type="primary" :icon="HomeFilled" @click="router.push('/')">回到首页</el-button>
          <el-button :icon="Clock" @click="router.push('/history')">查看历史记录</el-button>
        </div>
      </div>
      <div class="route-map" aria-label="可前往功能">
        <button v-for="item in destinations" :key="item.path" type="button" @click="router.push(item.path)">
          <span :class="item.tone"><el-icon><component :is="item.icon" /></el-icon></span>
          <strong>{{ item.title }}</strong>
          <small>{{ item.desc }}</small>
        </button>
      </div>
    </section>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ChatDotRound, Clock, Collection, DocumentChecked, HomeFilled, User } from '@element-plus/icons-vue'

const router = useRouter()
const destinations = [
  { title: '训练驾驶舱', desc: '查看准备度和今日任务', path: '/', icon: HomeFilled, tone: 'teal' },
  { title: '简历分析', desc: '上传 PDF 获取岗位反馈', path: '/resume', icon: DocumentChecked, tone: 'green' },
  { title: '模拟面试', desc: '开始一场 AI 追问训练', path: '/interview', icon: ChatDotRound, tone: 'coral' },
  { title: '面试题库', desc: '复习知识点和参考答案', path: '/questions', icon: Collection, tone: 'amber' },
  { title: '个人中心', desc: '查看训练档案和安全设置', path: '/profile', icon: User, tone: 'gray' }
]
</script>

<style scoped>
.not-found-page { min-height: calc(100vh - 120px); display: grid; place-items: center; color: var(--text); }
.not-found-card { width: min(100%, 980px); display: grid; grid-template-columns: minmax(0, 1fr) 360px; gap: 28px; padding: 34px; border: 1px solid var(--border); border-radius: 8px; background: var(--surface-glass); box-shadow: var(--shadow); backdrop-filter: blur(16px); }
.eyebrow { display: flex; align-items: center; gap: 7px; color: var(--blue); font-size: 11px; font-weight: 800; letter-spacing: .04em; }
.eyebrow i { width: 7px; height: 7px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent) 14%, transparent); }
.status-block h1 { margin: 14px 0 12px; font-size: clamp(30px, 4vw, 46px); line-height: 1.15; letter-spacing: 0; }
.status-block p { max-width: 580px; color: var(--text-secondary); font-size: 14px; line-height: 1.8; }
.actions { display: flex; gap: 10px; margin-top: 24px; flex-wrap: wrap; }
.route-map { display: grid; gap: 9px; }
.route-map button { min-width: 0; display: grid; grid-template-columns: 36px minmax(0, 1fr); grid-template-rows: auto auto; align-items: center; gap: 2px 10px; padding: 12px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); color: var(--text); text-align: left; cursor: pointer; }
.route-map button:hover { border-color: color-mix(in srgb, var(--blue) 36%, var(--border)); background: var(--hover); }
.route-map span { grid-row: 1 / 3; width: 36px; height: 36px; display: grid; place-items: center; border-radius: 7px; }
.route-map span.teal { background: var(--blue-light); color: var(--blue); }
.route-map span.green { background: #e8f6ee; color: var(--success); }
.route-map span.coral { background: #fff0ec; color: var(--accent); }
.route-map span.amber { background: #fff5df; color: #b87813; }
.route-map span.gray { background: var(--hover); color: var(--text-secondary); }
.route-map strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.route-map small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-muted); font-size: 10px; }
@media (max-width: 760px) {
  .not-found-page { min-height: calc(100vh - 160px); align-items: start; padding-top: 16px; }
  .not-found-card { grid-template-columns: 1fr; padding: 24px 18px; }
}
</style>
