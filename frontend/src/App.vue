<template>
  <div id="app-container" :class="{ 'has-sidebar': showSidebar, 'is-navigating': isNavigating }">
    <div class="route-progress" :class="{ active: isNavigating }" aria-hidden="true"></div>
    <button v-if="showScrollTop" class="scroll-top" type="button" aria-label="回到顶部" @click="scrollToTop"><el-icon><ArrowUp /></el-icon><span>回到顶部</span></button>
    <!-- ======== 侧边栏布局（功能页） ======== -->
    <div v-if="showSidebar" class="layout">
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <button class="workspace-brand" type="button" @click="router.push('/')"><span><el-icon><Aim /></el-icon></span><div v-if="!sidebarCollapsed"><strong>Interview Coach</strong><small>求职训练工作台</small></div></button>
        <div v-if="!sidebarCollapsed" class="nav-label">工作空间</div>
        <button class="sb-user" type="button" :aria-label="`打开 ${userStore.username || '用户'} 的个人中心`" @click="router.push('/profile')">
          <img v-if="isImageAvatar" :src="userAvatar" class="sb-avatar" />
          <span v-else class="sb-avatar-d"><el-icon><User /></el-icon></span>
          <div v-if="!sidebarCollapsed" class="sb-info">
            <strong>{{ userStore.username }}</strong>
            <small>个人中心</small>
          </div>
        </button>

        <el-menu :default-active="route.path" :collapse="sidebarCollapsed" router class="sb-menu">
          <el-menu-item index="/">
            <el-icon><HomeFilled /></el-icon><span>首页</span>
          </el-menu-item>
          <el-menu-item index="/resume">
            <el-icon><Document /></el-icon><span>简历分析</span>
          </el-menu-item>
          <el-menu-item index="/interview">
            <el-icon><ChatDotRound /></el-icon><span>AI模拟面试</span>
          </el-menu-item>
          <el-menu-item index="/questions">
            <el-icon><Collection /></el-icon><span>题库</span>
          </el-menu-item>
          <el-menu-item index="/history">
            <el-icon><Clock /></el-icon><span>历史记录</span>
          </el-menu-item>
        </el-menu>

        <button class="command-trigger" type="button" :title="sidebarCollapsed ? '快速搜索' : ''" @click="openCommand"><el-icon><Search /></el-icon><span v-if="!sidebarCollapsed">快速搜索</span><kbd v-if="!sidebarCollapsed">Ctrl K</kbd></button>

        <button v-if="!sidebarCollapsed" class="sidebar-cta" type="button" @click="router.push('/interview')"><span><el-icon><VideoPlay /></el-icon></span><div><strong>开始模拟面试</strong><small>进入新一轮岗位训练</small></div><el-icon><ArrowRight /></el-icon></button>

        <div v-if="!sidebarCollapsed" class="service-state" :class="serviceStatus" :title="serviceHint"><i></i><span><strong>{{ serviceLabel }}</strong><small>{{ serviceHint }}</small></span></div>

        <div class="sb-bottom">
          <button class="sb-bottom-item" type="button" :title="themeStore.dark ? '切换到亮色模式' : '切换到深色模式'" @click="themeStore.toggle()">
            <el-icon><Sunny v-if="themeStore.dark" /><Moon v-else /></el-icon>
            <span v-if="!sidebarCollapsed">{{ themeStore.dark ? '亮色模式' : '深色模式' }}</span>
          </button>
          <button class="sb-bottom-item" type="button" :title="sidebarCollapsed ? '展开侧栏' : '收起侧栏'" @click="sidebarCollapsed = !sidebarCollapsed">
            <el-icon><component :is="sidebarCollapsed ? 'DArrowRight' : 'DArrowLeft'" /></el-icon>
            <span v-if="!sidebarCollapsed">收起侧栏</span>
          </button>
        </div>
      </aside>

      <main class="main-area" @scroll="handleMainScroll">
        <header class="mobile-header">
          <button class="mobile-brand" type="button" @click="router.push('/')">
            <el-icon><Aim /></el-icon><span>Interview Coach</span>
          </button>
          <div class="mobile-actions">
            <el-button text circle title="快速搜索" @click="openCommand"><el-icon><Search /></el-icon></el-button>
            <el-button text circle :title="themeStore.dark ? '亮色模式' : '深色模式'" @click="themeStore.toggle()">
              <el-icon><Sunny v-if="themeStore.dark" /><Moon v-else /></el-icon>
            </el-button>
            <button class="mobile-avatar" type="button" @click="router.push('/profile')">
              <img v-if="isImageAvatar" :src="userAvatar" alt="用户头像" />
              <el-icon v-else><User /></el-icon>
            </button>
          </div>
        </header>
        <router-view v-slot="{ Component }">
          <Transition name="route" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </Transition>
        </router-view>
      </main>

      <nav class="mobile-nav" aria-label="主要导航">
        <router-link to="/" aria-label="首页"><el-icon><HomeFilled /></el-icon><span>首页</span></router-link>
        <router-link to="/resume" aria-label="简历分析"><el-icon><Document /></el-icon><span>简历</span></router-link>
        <router-link to="/interview" aria-label="模拟面试"><el-icon><ChatDotRound /></el-icon><span>面试</span></router-link>
        <router-link to="/questions" aria-label="题库"><el-icon><Collection /></el-icon><span>题库</span></router-link>
        <router-link to="/history" aria-label="历史记录"><el-icon><Clock /></el-icon><span>历史</span></router-link>
      </nav>

      <el-dialog v-model="commandVisible" width="min(620px, calc(100% - 28px))" class="command-dialog" :show-close="false" append-to-body @opened="focusCommand">
        <div class="command-input"><el-icon><Search /></el-icon><input ref="commandInput" v-model="commandKeyword" placeholder="搜索功能或输入操作名称" @keydown.down.prevent="moveCommand(1)" @keydown.up.prevent="moveCommand(-1)" @keydown.enter.prevent="runSelected" /><kbd>ESC</kbd></div>
        <div class="command-results">
          <div class="command-label">{{ commandKeyword ? '搜索结果' : '快速前往' }}</div>
          <button v-for="(item, index) in filteredCommands" :key="item.path" :class="{ active: selectedCommand === index }" type="button" @mouseenter="selectedCommand = index" @click="runCommand(item)"><span :class="['command-icon', item.tone]"><el-icon><component :is="item.icon" /></el-icon></span><span><strong>{{ item.title }}</strong><small>{{ item.description }}</small></span><kbd>{{ item.shortcut }}</kbd><el-icon><ArrowRight /></el-icon></button>
          <div v-if="!filteredCommands.length" class="command-empty">没有匹配的功能</div>
        </div>
        <div class="command-foot"><span><kbd>↑</kbd><kbd>↓</kbd> 选择</span><span><kbd>Enter</kbd> 打开</span><span><kbd>Esc</kbd> 关闭</span></div>
      </el-dialog>
    </div>

    <!-- ======== 全宽布局（首页 / 登录） ======== -->
    <div v-else class="full-layout">
      <header class="topbar">
        <div class="top-left" @click="$router.push('/')">
          <span class="brand-mark"><el-icon><Aim /></el-icon></span><span class="logo">Interview Coach</span>
        </div>
        <nav class="top-right">
          <div v-if="route.path === '/'" class="public-nav" aria-label="页面导航">
            <a href="#workflow">训练闭环</a>
            <a href="#studio-preview">工作台预览</a>
            <a href="#capabilities">能力边界</a>
          </div>
          <button class="theme-toggle" type="button" :title="themeStore.dark ? '亮色模式' : '深色模式'" @click="themeStore.toggle()"><el-icon><Sunny v-if="themeStore.dark" /><Moon v-else /></el-icon></button>
          <template v-if="userStore.isLoggedIn()">
            <el-popover placement="bottom-end" :width="180" trigger="hover" :show-after="200">
              <template #reference>
                <span class="user-chip">
                  <img v-if="isImageAvatar" :src="userAvatar" class="chip-avatar" />
                  <span v-else>{{ avatarFallback }}</span>
                  {{ userStore.username }}
                </span>
              </template>
              <div class="pop-row"><strong>{{ userStore.username }}</strong></div>
              <div class="pop-divider"></div>
              <div class="pop-row clickable" @click="router.push('/profile')">👤 个人中心</div>
              <div class="pop-divider"></div>
              <div class="pop-row clickable logout" @click="handleLogout">退出登录</div>
            </el-popover>
          </template>
          <router-link v-else to="/login" class="login-link">登录</router-link>
        </nav>
      </header>
      <router-view v-slot="{ Component }">
        <Transition name="route" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </Transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Aim, ArrowRight, ArrowUp, ChatDotRound, Clock, Collection, Document, HomeFilled, Moon, Search, Sunny, User, VideoPlay } from '@element-plus/icons-vue'
import { healthCheck } from './api'
import { useUserStore } from './stores/user'
import { useThemeStore } from './stores/theme'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const sidebarCollapsed = ref(localStorage.getItem('sidebarCollapsed') === '1')
const isNavigating = ref(false)
const showScrollTop = ref(false)
const serviceStatus = ref('checking')
const commandVisible = ref(false)
const commandKeyword = ref('')
const selectedCommand = ref(0)
const commandInput = ref(null)
const commands = [
  { title: '训练驾驶舱', description: '查看准备度和今日任务', path: '/', icon: HomeFilled, tone: 'teal', shortcut: 'G H', keywords: '首页 看板 dashboard' },
  { title: '岗位简历诊断', description: '上传简历并结合 JD 分析', path: '/resume', icon: Document, tone: 'green', shortcut: 'G R', keywords: '简历 resume ats jd' },
  { title: 'AI 模拟面试', description: '开始或继续实时面试训练', path: '/interview', icon: ChatDotRound, tone: 'coral', shortcut: 'G I', keywords: '面试 interview 对话' },
  { title: '专注题库练习', description: '练习知识点并追踪掌握度', path: '/questions', icon: Collection, tone: 'amber', shortcut: 'G Q', keywords: '题库 练习 question' },
  { title: '训练记录与复盘', description: '回看面试与简历历史', path: '/history', icon: Clock, tone: 'blue', shortcut: 'G L', keywords: '历史 复盘 history report' },
  { title: '账户与安全', description: '管理资料、头像和密码', path: '/profile', icon: User, tone: 'gray', shortcut: 'G P', keywords: '个人 设置 profile security' }
]
const filteredCommands = computed(() => { const keyword = commandKeyword.value.trim().toLowerCase(); return keyword ? commands.filter(item => `${item.title} ${item.description} ${item.keywords}`.toLowerCase().includes(keyword)) : commands })

// 判断头像是不是真实图片路径（非 emoji）
const userAvatar = computed(() => userStore.profile.avatarUrl)
const isImageAvatar = computed(() => userStore.isImageAvatar)
const avatarFallback = computed(() => userStore.avatarFallback)

// 登录后统一进入工作台布局，访客首页和登录页保持全宽展示。
const showSidebar = computed(() => {
  return userStore.isLoggedIn() && route.path !== '/login'
})

const serviceLabel = computed(() => ({
  checking: '正在检查服务',
  online: '服务运行正常',
  degraded: '部分服务需关注'
}[serviceStatus.value] || '服务状态未知'))
const serviceHint = computed(() => ({
  checking: '正在检查 AI 与数据服务',
  online: 'AI · 数据库 · Redis',
  degraded: '部分依赖暂不可用，请稍后重试'
}[serviceStatus.value] || '等待服务状态'))

let routeTimer = null
let healthTimer = null
let shortcutTimer = null
const removeRouteStart = router.beforeEach(() => {
  isNavigating.value = true
})
const removeRouteFinish = router.afterEach(() => {
  window.clearTimeout(routeTimer)
  routeTimer = window.setTimeout(() => { isNavigating.value = false }, 180)
})

const checkService = async () => {
  if (!userStore.isLoggedIn()) {
    serviceStatus.value = 'checking'
    return
  }
  serviceStatus.value = 'checking'
  try {
    const response = await healthCheck()
    const data = response.data?.data
    const dependencies = Object.values(data?.dependencies || {})
    const healthy = response.data?.code === 200 && data?.status !== 'DOWN' && (!dependencies.length || dependencies.every(item => item === 'UP'))
    serviceStatus.value = healthy ? 'online' : 'degraded'
  } catch {
    serviceStatus.value = 'degraded'
  }
}

const startHealthMonitor = () => {
  window.clearInterval(healthTimer)
  healthTimer = null
  if (!userStore.isLoggedIn()) return
  checkService()
  healthTimer = window.setInterval(checkService, 60000)
}

const stopHealthMonitor = () => {
  window.clearInterval(healthTimer)
  healthTimer = null
  serviceStatus.value = 'checking'
}

const handleShortcut = event => {
  const key = event.key.toLowerCase()
  const typing = ['INPUT', 'TEXTAREA', 'SELECT'].includes(event.target?.tagName) || event.target?.isContentEditable
  if ((event.ctrlKey || event.metaKey) && key === 'k') {
    event.preventDefault()
    commandVisible.value ? commandVisible.value = false : openCommand()
    return
  }
  if (typing || event.altKey || event.ctrlKey || event.metaKey) return
  if (key === 'g') {
    clearTimeout(shortcutTimer)
    shortcutTimer = window.setTimeout(() => { shortcutTimer = null }, 1100)
    return
  }
  if (shortcutTimer) {
    const command = commands.find(item => item.shortcut.toLowerCase() === `g ${key}`)
    clearTimeout(shortcutTimer)
    shortcutTimer = null
    if (command) { event.preventDefault(); runCommand(command) }
  }
}
const updateScrollState = target => { showScrollTop.value = (target === window ? window.scrollY : target.scrollTop) > 420 }
const handleMainScroll = event => updateScrollState(event.currentTarget)
const handleWindowScroll = () => updateScrollState(window)
const handleAuthExpired = () => {
  if (userStore.isLoggedIn()) userStore.logout()
}
const scrollToTop = () => {
  const target = showSidebar.value ? document.querySelector('.main-area') : window
  target?.scrollTo({ top: 0, behavior: 'smooth' })
}
onMounted(async () => {
  window.addEventListener('keydown', handleShortcut)
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  window.addEventListener('interview-coach:auth-expired', handleAuthExpired)
  try { await userStore.loadProfile() } catch { /* 请求拦截器已经负责提示登录过期 */ }
  startHealthMonitor()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleShortcut)
  window.removeEventListener('scroll', handleWindowScroll)
  window.removeEventListener('interview-coach:auth-expired', handleAuthExpired)
  window.clearTimeout(routeTimer)
  window.clearTimeout(shortcutTimer)
  removeRouteStart()
  removeRouteFinish()
  stopHealthMonitor()
})

const handleLogout = () => { userStore.logout(); router.push('/') }
const openCommand = () => { commandKeyword.value = ''; selectedCommand.value = 0; commandVisible.value = true }
const focusCommand = () => nextTick(() => commandInput.value?.focus())
const moveCommand = delta => { const length = filteredCommands.value.length; if (length) selectedCommand.value = (selectedCommand.value + delta + length) % length }
const runCommand = item => { commandVisible.value = false; router.push(item.path) }
const runSelected = () => { const item = filteredCommands.value[selectedCommand.value]; if (item) runCommand(item) }
watch(commandKeyword, () => { selectedCommand.value = 0 })
watch(sidebarCollapsed, value => localStorage.setItem('sidebarCollapsed', value ? '1' : '0'))
watch(() => route.fullPath, () => {
  commandVisible.value = false
  nextTick(() => {
    document.querySelector('.main-area')?.scrollTo({ top: 0, left: 0, behavior: 'auto' })
    showScrollTop.value = false
  })
})
watch(() => userStore.token, async tokenValue => {
  try { await userStore.loadProfile() } catch { /* 401 由 API 拦截器统一处理 */ }
  if (tokenValue) startHealthMonitor()
  else stopHealthMonitor()
})
</script>

<style>
/* ======== 全局 ======== */
:root {
  --bg: #edf3f1; --bg-card: #fff; --bg-input: rgba(255,255,255,.92);
  --bg-soft: #e4eeeb; --surface-glass: rgba(255,255,255,.72);
  --text: #10221f; --text-secondary: #405a53; --text-muted: #5b716a;
  --border: rgba(16,54,47,.16); --shadow: 0 24px 70px rgba(16,54,47,.11);
  --nav-bg: rgba(248,252,250,.78); --chat-ai: #f1f6f4;
  --hover: #dceae5; --blue: #0b6f65; --blue-light: #d5efe8;
  --accent: #b9533f; --success: #247a55;
  --sidebar-bg: #112823;
  --el-color-primary: var(--blue); --el-color-primary-light-3: color-mix(in srgb, var(--blue) 66%, white); --el-color-primary-light-5: color-mix(in srgb, var(--blue) 48%, white); --el-color-primary-light-7: color-mix(in srgb, var(--blue) 30%, white); --el-color-primary-light-8: color-mix(in srgb, var(--blue) 20%, white); --el-color-primary-light-9: color-mix(in srgb, var(--blue) 10%, var(--bg-card)); --el-color-primary-dark-2: color-mix(in srgb, var(--blue) 82%, black);
  --el-color-success: var(--success); --el-color-warning: #b87813; --el-color-danger: var(--accent); --el-color-info: var(--text-muted);
  --el-bg-color: var(--bg-card); --el-bg-color-page: var(--bg); --el-fill-color-blank: var(--bg-card); --el-fill-color-light: var(--hover); --el-fill-color-lighter: color-mix(in srgb, var(--hover) 70%, var(--bg-card));
  --el-text-color-primary: var(--text); --el-text-color-regular: var(--text-secondary); --el-text-color-secondary: var(--text-muted); --el-text-color-placeholder: var(--text-muted); --el-border-color: var(--border); --el-border-color-light: color-mix(in srgb, var(--border) 70%, transparent);
}
html.dark {
  --bg: #0a1412; --bg-card: #13221f; --bg-input: #1a2b27;
  --bg-soft: #0f1d1a; --surface-glass: rgba(19,34,31,.78);
  --text: #eff8f5; --text-secondary: #c3d7d0; --text-muted: #9ebbb2;
  --border: rgba(185,226,215,.2); --shadow: 0 24px 70px rgba(0,0,0,.34);
  --nav-bg: rgba(10,20,18,.78); --chat-ai: #1a2b27;
  --hover: #203a34; --blue: #83dfd0; --blue-light: #1d4b43;
  --accent: #f3a083; --success: #78d6a5; --sidebar-bg: #091713;
  --el-color-primary: var(--blue); --el-color-primary-light-3: color-mix(in srgb, var(--blue) 72%, var(--bg-card)); --el-color-primary-light-5: color-mix(in srgb, var(--blue) 54%, var(--bg-card)); --el-color-primary-light-7: color-mix(in srgb, var(--blue) 36%, var(--bg-card)); --el-color-primary-light-8: color-mix(in srgb, var(--blue) 26%, var(--bg-card)); --el-color-primary-light-9: color-mix(in srgb, var(--blue) 16%, var(--bg-card)); --el-color-primary-dark-2: color-mix(in srgb, var(--blue) 82%, black);
  --el-color-success: var(--success); --el-color-warning: #e0ad55; --el-color-danger: var(--accent); --el-color-info: var(--text-muted);
  --el-bg-color: var(--bg-card); --el-bg-color-page: var(--bg); --el-fill-color-blank: var(--bg-card); --el-fill-color-light: var(--hover); --el-fill-color-lighter: color-mix(in srgb, var(--hover) 72%, var(--bg-card));
  --el-text-color-primary: var(--text); --el-text-color-regular: var(--text-secondary); --el-text-color-secondary: var(--text-muted); --el-text-color-placeholder: var(--text-muted); --el-border-color: var(--border); --el-border-color-light: color-mix(in srgb, var(--border) 72%, transparent);
}

* { margin: 0; padding: 0; box-sizing: border-box; }
html { overflow-x: hidden; }
button, input, textarea, select { font: inherit; }
button:focus-visible, a:focus-visible, input:focus-visible, textarea:focus-visible, select:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--blue) 35%, transparent);
  outline-offset: 2px;
}
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background:
    radial-gradient(circle at 12% 0%, color-mix(in srgb, var(--blue) 14%, transparent), transparent 29%),
    radial-gradient(circle at 92% 18%, color-mix(in srgb, var(--accent) 10%, transparent), transparent 25%),
    var(--bg);
  color: var(--text);
  -webkit-font-smoothing: antialiased; min-height: 100vh; overflow-x: hidden;
  transition: background .3s, color .3s;
}
body::before { content: ''; position: fixed; inset: 0; z-index: 0; pointer-events: none; opacity: .42; background-image: linear-gradient(color-mix(in srgb, var(--blue) 4%, transparent) 1px, transparent 1px), linear-gradient(90deg, color-mix(in srgb, var(--blue) 4%, transparent) 1px, transparent 1px); background-size: 72px 72px; mask-image: linear-gradient(to bottom, #000, transparent 72%); }
#app, #app-container { position: relative; z-index: 1; min-height: 100vh; }
::selection { background: color-mix(in srgb, var(--blue) 24%, transparent); color: var(--text); }
::-webkit-scrollbar { width: 9px; height: 9px; }
::-webkit-scrollbar-thumb { border: 3px solid transparent; border-radius: 999px; background: color-mix(in srgb, var(--text-muted) 42%, transparent); background-clip: padding-box; }
::-webkit-scrollbar-track { background: transparent; }
.el-button { border-radius: 10px; transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease, background .2s ease; }
.el-button--primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 14px 28px color-mix(in srgb, var(--blue) 24%, transparent); }
.el-input__wrapper { border-radius: 10px; transition: box-shadow .2s ease, border-color .2s ease; }
.el-textarea__inner { border-radius: 10px; }
.route-progress {
  position: fixed; inset: 0 auto auto 0; z-index: 1000; width: 32%; height: 3px;
  opacity: 0; pointer-events: none; transform: translateX(-110%);
  background: linear-gradient(90deg, var(--blue), #55bf9e, var(--accent));
  box-shadow: 0 1px 8px color-mix(in srgb, var(--blue) 42%, transparent);
}
.route-progress.active { opacity: 1; animation: routeProgress 1.1s ease-in-out infinite; }
@keyframes routeProgress { 0% { transform: translateX(-110%); } 55% { transform: translateX(210%); } 100% { transform: translateX(340%); } }
.route-enter-active, .route-leave-active { transition: opacity .18s ease, transform .18s ease; }
.route-enter-from { opacity: 0; transform: translateY(7px); }
.route-leave-to { opacity: 0; transform: translateY(-4px); }
.scroll-top {
  position: fixed; right: 28px; bottom: 28px; z-index: 120;
  display: inline-flex; align-items: center; gap: 7px; min-height: 38px;
  padding: 0 13px; border: 1px solid color-mix(in srgb, var(--blue) 22%, var(--border));
  border-radius: 999px; background: color-mix(in srgb, var(--bg-card) 82%, transparent);
  color: var(--blue); cursor: pointer; font-size: 11px; font-weight: 700;
  box-shadow: 0 16px 36px rgba(16, 54, 47, .14); backdrop-filter: blur(16px);
  animation: scrollTopIn .2s ease;
}
.scroll-top:hover { transform: translateY(-3px); box-shadow: 0 20px 42px color-mix(in srgb, var(--blue) 18%, transparent); }
@keyframes scrollTopIn { from { opacity: 0; transform: translateY(7px); } to { opacity: 1; transform: translateY(0); } }
.el-button--primary {
  --el-button-bg-color: var(--blue) !important;
  --el-button-border-color: var(--blue) !important;
  --el-button-text-color: #fff !important;
  --el-button-hover-bg-color: color-mix(in srgb, var(--blue) 86%, white) !important;
  --el-button-hover-border-color: color-mix(in srgb, var(--blue) 86%, white) !important;
  color: #fff !important;
  box-shadow: 0 10px 22px color-mix(in srgb, var(--blue) 18%, transparent);
}
.page-head,.dashboard-head,.review-page,.question-page,.interview-page,.account-page,.resume-workspace {
  animation: pageIn .32s ease;
}
@keyframes pageIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

/* ======== 全宽顶栏 ======== */
.full-layout { min-height: 100vh; }
.topbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 32px; height: 56px;
  background: var(--nav-bg); backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
  position: sticky; top: 0; z-index: 99;
}
.top-left { display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 18px; font-weight: 700; color: var(--text); user-select: none; }
.brand-mark { width: 30px; height: 30px; display: grid; place-items: center; border-radius: 7px; background: var(--blue); color: #fff; }
.top-right { display: flex; align-items: center; gap: 12px; }
.public-nav { display: flex; align-items: center; gap: 24px; margin-right: 8px; }
.public-nav a { color: var(--text-muted); font-size: 12px; text-decoration: none; transition: color .2s; }
.public-nav a:hover { color: var(--blue); }
.theme-toggle { width: 32px; height: 32px; display: grid; place-items: center; border: 0; border-radius: 6px; background: transparent; color: var(--text-secondary); cursor: pointer; font-size: 17px; }
.theme-toggle:hover { background: var(--hover); }
.user-chip { display: flex; align-items: center; gap: 6px; cursor: pointer; padding: 4px 10px; border-radius: 8px; font-size: 14px; color: var(--text-secondary); }
.user-chip:hover { background: var(--hover); }
.chip-avatar { width: 24px; height: 24px; border-radius: 50%; object-fit: cover; }
.login-link { padding: 8px 15px; border-radius: 6px; background: var(--text); color: var(--bg-card); text-decoration: none; font-weight: 600; font-size: 13px; }
.pop-row { padding: 6px 4px; font-size: 14px; color: var(--text); }
.pop-row.clickable { cursor: pointer; border-radius: 6px; padding: 8px; }
.pop-row.clickable:hover { background: var(--hover); }
.pop-row.logout { color: #f56c6c; }
.pop-divider { height: 1px; background: var(--border); margin: 4px 0; }

/* ======== 侧边栏布局 ======== */
.layout { display: flex; min-height: 100vh; }
.sidebar {
  width: 256px; flex-shrink: 0;
  background: linear-gradient(180deg, var(--sidebar-bg), color-mix(in srgb, var(--sidebar-bg) 88%, var(--blue-light)));
  border-right: 1px solid var(--border);
  display: flex; flex-direction: column;
  position: sticky; top: 0; height: 100vh; transition: width .2s; overflow: hidden;
}
.sidebar.collapsed { width: 72px; }
.workspace-brand { height: 76px; display: flex; align-items: center; gap: 11px; padding: 0 17px; border: 0; border-bottom: 1px solid var(--border); background: transparent; color: var(--text); text-align: left; cursor: pointer; white-space: nowrap; }.workspace-brand > span { width: 36px; height: 36px; display: grid; flex: 0 0 36px; place-items: center; border-radius: 8px; background: linear-gradient(135deg,var(--blue),#36a269); color: #fff; font-size: 19px; box-shadow: 0 10px 22px color-mix(in srgb, var(--blue) 28%, transparent); }.workspace-brand strong,.workspace-brand small { display: block; }.workspace-brand strong { font-size: 14px; }.workspace-brand small { margin-top: 3px; color: var(--text-muted); font-size: 9px; }.nav-label { padding: 20px 20px 8px; color: var(--text-muted); font-size: 9px; font-weight: 700; text-transform: uppercase; }

.sb-user {
  display: flex; align-items: center; gap: 12px;
  margin: 4px 9px 12px; padding: 10px; border: 1px solid var(--border); border-radius: 8px; cursor: pointer; transition: background .15s, transform .15s;
  width: calc(100% - 18px); background: var(--surface-glass); color: var(--text); text-align: left; box-shadow: 0 10px 26px rgba(23,32,42,.04);
}
.sb-user:hover { background: var(--hover); transform: translateY(-1px); }
.sb-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.sb-avatar-d { width: 36px; height: 36px; display: grid; flex-shrink: 0; place-items: center; border-radius: 7px; background: var(--blue-light); color: var(--blue); font-size: 18px; }
.sb-info strong { display: block; font-size: 14px; color: var(--text); }
.sb-info small { font-size: 12px; color: var(--text-muted); }

.sb-menu {
  flex: 1; border-right: none !important;
  background: transparent !important;
}
.sb-menu :deep(.el-menu-item) {
  color: var(--text-secondary);
  height: 45px; line-height: 45px; margin: 3px 9px; border-radius: 7px;
}
.sb-menu :deep(.el-menu-item:hover) { background: var(--hover); color: var(--text); }
.sb-menu :deep(.el-menu-item.is-active) {
  background: var(--text); color: var(--bg-card); font-weight: 600;
}
.command-trigger { height: 39px; display: flex; align-items: center; gap: 10px; margin: 4px 10px 8px; padding: 0 12px; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-card); color: var(--text-muted); cursor: pointer; font-size: 11px; white-space: nowrap; }.command-trigger span { flex: 1; text-align: left; }.command-trigger kbd { padding: 2px 5px; border: 1px solid var(--border); border-radius: 4px; background: var(--hover); color: var(--text-muted); font: 8px ui-monospace,monospace; }.sidebar.collapsed .command-trigger { width: 42px; justify-content: center; margin-inline: 15px; padding: 0; }
.sidebar-cta { display: flex; align-items: center; gap: 10px; margin: 10px 12px; padding: 13px 12px; border: 0; border-radius: 8px; background: var(--blue); color: #fff; text-align: left; cursor: pointer; }.sidebar-cta > span { width: 30px; height: 30px; display: grid; flex: 0 0 30px; place-items: center; border-radius: 6px; background: rgba(255,255,255,.16); }.sidebar-cta > div { min-width: 0; flex: 1; }.sidebar-cta strong,.sidebar-cta small { display: block; }.sidebar-cta strong { font-size: 11px; }.sidebar-cta small { margin-top: 3px; color: rgba(255,255,255,.7); font-size: 8px; }.service-state { display: flex; align-items: center; gap: 9px; margin: 3px 17px 12px; }.service-state i { width: 7px; height: 7px; border-radius: 50%; background: #37b87c; box-shadow: 0 0 0 4px rgba(55,184,124,.12); }.service-state span,.service-state strong,.service-state small { display: block; }.service-state strong { color: var(--text-secondary); font-size: 9px; }.service-state small { margin-top: 2px; color: var(--text-muted); font-size: 8px; }

.service-state.checking i { background: #d99a2b; box-shadow: 0 0 0 4px rgba(217,154,43,.12); animation: statusPulse 1.2s ease-in-out infinite; }
.service-state.degraded i { background: #df694f; box-shadow: 0 0 0 4px rgba(223,105,79,.12); }
@keyframes statusPulse { 50% { opacity: .38; transform: scale(.8); } }

.sb-bottom {
  padding: 8px;
  display: flex; flex-direction: column; gap: 2px;
  border-top: 1px solid var(--border);
}
.sb-bottom-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; cursor: pointer; border-radius: 8px;
  width: 100%; border: 0; background: transparent; font-size: 14px; color: var(--text-secondary); text-align: left; transition: background .15s;
  white-space: nowrap;
}
.sb-bottom-item:hover { background: var(--hover); }

.main-area {
  min-width: 0; flex: 1; overflow-y: auto; padding: 30px clamp(22px,3vw,44px) 48px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--blue-light) 38%, transparent), transparent 260px),
    var(--bg);
}
.mobile-header, .mobile-nav { display: none; }

@media (max-width: 760px) {
  .layout { display: block; min-height: 100vh; }
  .sidebar { display: none; }
  .main-area { min-height: 100vh; padding: 72px 14px 88px; overflow: visible; }
  .mobile-header {
    display: flex; align-items: center; justify-content: space-between;
    position: fixed; inset: 0 0 auto 0; height: 56px; padding: 0 14px;
    background: var(--nav-bg); backdrop-filter: blur(14px);
    border-bottom: 1px solid var(--border); z-index: 100;
  }
  .mobile-brand {
    display: flex; align-items: center; gap: 8px; border: 0; background: transparent;
    color: var(--text); font-size: 15px; font-weight: 700; cursor: pointer;
  }
  .mobile-brand .el-icon { color: var(--blue); font-size: 20px; }
  .mobile-actions { display: flex; align-items: center; gap: 4px; }
  .mobile-avatar {
    width: 32px; height: 32px; display: grid; place-items: center; padding: 0;
    border: 1px solid var(--border); border-radius: 50%; background: var(--bg-card);
    color: var(--text-secondary); overflow: hidden; cursor: pointer;
  }
  .mobile-avatar img { width: 100%; height: 100%; object-fit: cover; }
  .mobile-nav {
    display: grid; grid-template-columns: repeat(5, 1fr);
    position: fixed; inset: auto 0 0 0; z-index: 100;
    height: calc(64px + env(safe-area-inset-bottom)); padding: 6px 6px env(safe-area-inset-bottom);
    background: var(--nav-bg); backdrop-filter: blur(14px); border-top: 1px solid var(--border);
  }
  .mobile-nav a {
    min-width: 0; display: flex; flex-direction: column; align-items: center; justify-content: center;
    gap: 3px; color: var(--text-muted); text-decoration: none; font-size: 11px;
  }
  .mobile-nav a .el-icon { font-size: 20px; }
  .mobile-nav a.router-link-active { color: var(--blue); font-weight: 600; }
  .topbar { padding: 0 16px; }
  .logo { font-size: 15px; }
  .public-nav { display: none; }
  .scroll-top { right: 14px; bottom: calc(76px + env(safe-area-inset-bottom)); padding-inline: 11px; }
}

/* ======== Element Plus 深色覆盖 ======== */
html.dark .el-card, html.dark .el-dialog, html.dark .el-table, html.dark .el-tabs,
html.dark .el-input__wrapper, html.dark .el-textarea__inner, html.dark .el-select .el-input__wrapper,
html.dark .el-select-dropdown, html.dark .el-popper, html.dark .el-button--default,
html.dark .el-upload-dragger, html.dark .el-pagination button, html.dark .el-pager li {
  background: var(--bg-card) !important; color: var(--text) !important; border-color: var(--border) !important;
}
html.dark .el-input__inner, html.dark .el-textarea__inner { background: var(--bg-input) !important; color: var(--text) !important; }
html.dark .el-table th.el-table__cell { background: var(--bg-card); }
html.dark .el-table td { border-bottom-color: var(--border); }
html.dark .el-tabs__item { color: var(--text-secondary); }
html.dark .el-tabs__item.is-active { color: var(--blue); }
html.dark .el-tabs__nav-wrap::after { background: var(--border); }
html.dark .el-radio__label, html.dark .el-checkbox__label { color: var(--text); }
html.dark .el-form-item__label { color: var(--text-secondary); }
html.dark .el-dialog__title { color: var(--text); }
html.dark .el-menu-item { color: var(--text-secondary) !important; }
html.dark .el-menu-item:hover { background: var(--hover) !important; }
html.dark .el-menu-item.is-active { background: var(--text) !important; color: var(--bg) !important; }
html.dark .el-divider__text { background: var(--bg-card); color: var(--text-muted); }
.el-input__inner, .el-textarea__inner, .el-select__selected-item, .el-select__placeholder { color: var(--text) !important; }
.el-input__inner::placeholder, .el-textarea__inner::placeholder { color: var(--text-muted) !important; opacity: 1; }
.el-form-item__label, .el-checkbox__label, .el-radio__label { color: var(--text-secondary) !important; }
.el-button--default { color: var(--text-secondary); border-color: var(--border); background: var(--bg-card); }
.el-button--default:hover { color: var(--blue); border-color: var(--blue); background: var(--blue-light); }
.el-button--primary.is-disabled, .el-button--primary:disabled { color: rgba(255,255,255,.82) !important; }
.command-dialog { border-radius: 8px !important; overflow: hidden; }.command-dialog .el-dialog__header { display: none; }.command-dialog .el-dialog__body { padding: 0; }.command-input { height: 58px; display: flex; align-items: center; gap: 11px; padding: 0 17px; border-bottom: 1px solid var(--border); color: var(--text-muted); }.command-input > .el-icon { font-size: 19px; }.command-input input { min-width: 0; flex: 1; border: 0; outline: 0; background: transparent; color: var(--text); font: inherit; font-size: 14px; }.command-input input::placeholder { color: var(--text-muted); }.command-input kbd,.command-foot kbd { padding: 2px 5px; border: 1px solid var(--border); border-radius: 4px; background: var(--hover); color: var(--text-muted); font: 9px ui-monospace,monospace; }.command-results { max-height: 420px; overflow-y: auto; padding: 9px; }.command-label { padding: 8px 9px 6px; color: var(--text-muted); font-size: 9px; font-weight: 700; }.command-results button { width: 100%; display: grid; grid-template-columns: 38px 1fr auto 18px; align-items: center; gap: 10px; padding: 10px; border: 0; border-radius: 6px; background: transparent; color: var(--text); text-align: left; cursor: pointer; }.command-results button.active { background: var(--hover); }.command-results button > span:nth-child(2),.command-results button strong,.command-results button small { display: block; }.command-results button strong { font-size: 12px; }.command-results button small { margin-top: 3px; color: var(--text-muted); font-size: 9px; }.command-results button > kbd { color: var(--text-muted); font: 8px ui-monospace,monospace; }.command-results button > .el-icon { color: var(--text-muted); }.command-icon { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 6px; }.command-icon.teal { background: var(--blue-light); color: var(--blue); }.command-icon.green { background: #e8f6ee; color: #23845c; }.command-icon.coral { background: #fff0ec; color: var(--accent); }.command-icon.amber { background: #fff5df; color: #b87813; }.command-icon.blue { background: #ebf3ff; color: #367bc7; }.command-icon.gray { background: var(--hover); color: var(--text-secondary); }.command-empty { display: grid; min-height: 140px; place-items: center; color: var(--text-muted); font-size: 11px; }.command-foot { display: flex; gap: 16px; padding: 9px 17px; border-top: 1px solid var(--border); color: var(--text-muted); font-size: 9px; }.command-foot span { display: flex; align-items: center; gap: 4px; }
/* ======== 高级视觉系统覆盖 ======== */
.sidebar {
  background:
    radial-gradient(circle at 14% 0%, rgba(94, 205, 188, .18), transparent 31%),
    radial-gradient(circle at 95% 72%, rgba(31, 105, 92, .22), transparent 35%),
    linear-gradient(180deg, #102722 0%, #0a1a16 100%);
  border-right-color: rgba(185, 226, 215, .13);
  box-shadow: 18px 0 50px rgba(11, 39, 32, .12);
}
.workspace-brand { border-bottom-color: rgba(185, 226, 215, .12); color: #eff8f5; }
.workspace-brand > span { border-radius: 11px; background: linear-gradient(135deg, #72d4c4, #318f80); box-shadow: 0 12px 28px rgba(61, 174, 155, .24); }
.workspace-brand small, .nav-label, .sb-info small { color: rgba(220, 243, 237, .72); }
.workspace-brand strong, .sb-info strong { color: #eff8f5; }
.sb-user { background: rgba(255, 255, 255, .055); border-color: rgba(185, 226, 215, .13); color: #eff8f5; box-shadow: 0 12px 28px rgba(0, 0, 0, .12); }
.sb-user:hover { background: rgba(255, 255, 255, .1); }
.sb-avatar-d { background: rgba(104, 208, 192, .17); color: #8ed3ca; }
.sb-menu .el-menu-item { color: rgba(226, 244, 239, .84); }
.sb-menu .el-menu-item:hover { background: rgba(255, 255, 255, .07); color: #fff; }
.sb-menu .el-menu-item.is-active { background: linear-gradient(135deg, rgba(142, 211, 202, .24), rgba(62, 143, 130, .17)); color: #fff; box-shadow: inset 3px 0 #8ed3ca, 0 9px 24px rgba(0, 0, 0, .13); }
.command-trigger { background: rgba(255, 255, 255, .075); border-color: rgba(185, 226, 215, .2); color: rgba(226, 244, 239, .86); }
.command-trigger kbd { background: rgba(255, 255, 255, .12); border-color: rgba(185, 226, 215, .22); color: rgba(226, 244, 239, .86); }
.sidebar-cta { background: linear-gradient(135deg, #5cc7b7, #318f80); box-shadow: 0 14px 28px rgba(71, 182, 164, .18); }
.sidebar-cta small { color: rgba(255, 255, 255, .86); }
.service-state strong { color: rgba(226, 244, 239, .88); }
.service-state small { color: rgba(226, 244, 239, .68); }
.sb-bottom { border-top-color: rgba(185, 226, 215, .12); }
.sb-bottom-item { color: rgba(226, 244, 239, .84); }
.sb-bottom-item:hover { background: rgba(255, 255, 255, .07); }
.main-area {
  background:
    radial-gradient(circle at 74% -8%, color-mix(in srgb, var(--blue) 12%, transparent), transparent 29%),
    linear-gradient(180deg, color-mix(in srgb, var(--blue-light) 20%, transparent), transparent 340px),
    var(--bg);
}
.topbar { box-shadow: 0 8px 30px rgba(16, 54, 47, .05); }
.top-left .brand-mark { border-radius: 10px; box-shadow: 0 10px 24px color-mix(in srgb, var(--blue) 28%, transparent); }
.theme-toggle, .user-chip { border-radius: 10px; }
.login-link { border-radius: 10px; box-shadow: 0 10px 22px rgba(16, 54, 47, .12); }
.command-dialog { border-radius: 16px !important; box-shadow: 0 30px 90px rgba(16, 54, 47, .22); }
.el-dialog, .el-message-box { border-radius: 16px !important; box-shadow: 0 30px 90px rgba(16, 54, 47, .18); }
html { scroll-behavior: smooth; }
button, a, input, textarea, select { -webkit-tap-highlight-color: transparent; }
:focus-visible { outline: 3px solid color-mix(in srgb, var(--blue) 42%, transparent); outline-offset: 3px; }
.el-button { min-height: 34px; font-weight: 600; }
.el-button--primary { border-radius: 11px !important; background: linear-gradient(135deg, color-mix(in srgb, var(--blue) 92%, white), var(--blue)) !important; }
.el-button--primary.is-disabled, .el-button--primary:disabled { opacity: .58; box-shadow: none; }
.el-button.is-text { border-radius: 9px; }
.el-button.is-text:hover { background: color-mix(in srgb, var(--blue-light) 68%, transparent); }
.el-input__wrapper, .el-textarea__inner, .el-select .el-input__wrapper, .el-input-number { box-shadow: 0 0 0 1px var(--border) inset, 0 8px 20px rgba(16, 54, 47, .025); }
.el-input__wrapper:hover, .el-textarea__inner:hover, .el-select .el-input__wrapper:hover { box-shadow: 0 0 0 1px color-mix(in srgb, var(--blue) 35%, var(--border)) inset, 0 10px 24px rgba(16, 54, 47, .04); }
.el-input__wrapper.is-focus, .el-textarea__inner:focus, .el-select .el-input__wrapper.is-focus { box-shadow: 0 0 0 1px var(--blue) inset, 0 0 0 4px color-mix(in srgb, var(--blue) 10%, transparent) !important; }
.el-tag { border-radius: 999px; }
.el-popper, .el-dropdown-menu, .el-select-dropdown, .el-autocomplete-suggestion { border-radius: 12px !important; box-shadow: 0 20px 55px rgba(16, 54, 47, .14) !important; }
.el-message { border-radius: 12px !important; box-shadow: 0 16px 38px rgba(16, 54, 47, .14) !important; }
.el-loading-mask { background: color-mix(in srgb, var(--bg) 72%, transparent) !important; backdrop-filter: blur(5px); }
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { scroll-behavior: auto !important; animation-duration: .01ms !important; animation-iteration-count: 1 !important; transition-duration: .01ms !important; }
}
@media (max-width: 600px) {
  .el-dialog { width: min(calc(100% - 28px), 560px) !important; margin-top: 6vh !important; }
  .el-dialog__body { max-height: 70vh; overflow-y: auto; }
  .el-message-box { width: min(calc(100% - 28px), 460px) !important; }
  .el-button:not(.is-text):not(.el-button--small) { min-height: 40px; }
  .el-button.is-text, .el-button--small { min-height: 32px; }
}
</style>
