<template>
  <div id="app-container" :class="{ 'has-sidebar': showSidebar }">
    <!-- ======== 侧边栏布局（功能页） ======== -->
    <div v-if="showSidebar" class="layout">
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <button class="workspace-brand" type="button" @click="router.push('/')"><span><el-icon><Aim /></el-icon></span><div v-if="!sidebarCollapsed"><strong>Interview Coach</strong><small>求职训练工作台</small></div></button>
        <div v-if="!sidebarCollapsed" class="nav-label">工作空间</div>
        <div class="sb-user" @click="$router.push('/profile')">
          <img v-if="isImageAvatar" :src="userAvatar" class="sb-avatar" />
          <span v-else class="sb-avatar-d"><el-icon><User /></el-icon></span>
          <div v-if="!sidebarCollapsed" class="sb-info">
            <strong>{{ userStore.username }}</strong>
            <small>个人中心</small>
          </div>
        </div>

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

        <div v-if="!sidebarCollapsed" class="service-state"><i></i><span><strong>服务运行正常</strong><small>AI · 数据库 · Redis</small></span></div>

        <div class="sb-bottom">
          <div class="sb-bottom-item" @click="themeStore.toggle()">
            <el-icon><Sunny v-if="themeStore.dark" /><Moon v-else /></el-icon>
            <span v-if="!sidebarCollapsed">{{ themeStore.dark ? '亮色模式' : '深色模式' }}</span>
          </div>
          <div class="sb-bottom-item" @click="sidebarCollapsed = !sidebarCollapsed">
            <el-icon><component :is="sidebarCollapsed ? 'DArrowRight' : 'DArrowLeft'" /></el-icon>
            <span v-if="!sidebarCollapsed">收起侧栏</span>
          </div>
        </div>
      </aside>

      <main class="main-area">
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
        <router-view />
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
          <button class="theme-toggle" type="button" :title="themeStore.dark ? '亮色模式' : '深色模式'" @click="themeStore.toggle()"><el-icon><Sunny v-if="themeStore.dark" /><Moon v-else /></el-icon></button>
          <template v-if="userStore.isLoggedIn()">
            <el-popover placement="bottom-end" :width="180" trigger="hover" :show-after="200">
              <template #reference>
                <span class="user-chip">
                  <img v-if="isImageAvatar" :src="userAvatar" class="chip-avatar" />
                  <span v-else>{{ userAvatar }}</span>
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
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Aim, ArrowRight, ChatDotRound, Clock, Collection, Document, HomeFilled, Moon, Search, Sunny, User, VideoPlay } from '@element-plus/icons-vue'
import { useUserStore } from './stores/user'
import { useThemeStore } from './stores/theme'
import api from './api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const userAvatar = ref('')
const sidebarCollapsed = ref(false)
const commandVisible = ref(false)
const commandKeyword = ref('')
const selectedCommand = ref(0)
const commandInput = ref(null)
let avatarObjectUrl = ''
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
const isImageAvatar = computed(() => userAvatar.value && (userAvatar.value.startsWith('blob:') || userAvatar.value.startsWith('http')))

// 登录后统一进入工作台布局，访客首页和登录页保持全宽展示。
const showSidebar = computed(() => {
  return userStore.isLoggedIn() && route.path !== '/login'
})

const handleShortcut = event => { if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); commandVisible.value ? commandVisible.value = false : openCommand() } }
onMounted(async () => {
  window.addEventListener('keydown', handleShortcut)
  if (userStore.isLoggedIn()) {
    try {
      const res = await api.get('/user/me')
      if (res.data.code === 200) {
        const avatar = res.data.data.avatar || ''
        if (avatar.startsWith('/api/user/avatar/')) {
          const image = await api.get(avatar.replace('/api', ''), { responseType: 'blob' })
          avatarObjectUrl = URL.createObjectURL(image.data)
          userAvatar.value = avatarObjectUrl
        } else {
          userAvatar.value = avatar
        }
      }
    } catch (e) { /* */ }
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleShortcut)
  if (avatarObjectUrl) URL.revokeObjectURL(avatarObjectUrl)
})

const handleLogout = () => { userStore.logout(); router.push('/') }
const openCommand = () => { commandKeyword.value = ''; selectedCommand.value = 0; commandVisible.value = true }
const focusCommand = () => nextTick(() => commandInput.value?.focus())
const moveCommand = delta => { const length = filteredCommands.value.length; if (length) selectedCommand.value = (selectedCommand.value + delta + length) % length }
const runCommand = item => { commandVisible.value = false; router.push(item.path) }
const runSelected = () => { const item = filteredCommands.value[selectedCommand.value]; if (item) runCommand(item) }
watch(commandKeyword, () => { selectedCommand.value = 0 })
</script>

<style>
/* ======== 全局 ======== */
:root {
  --bg: #f4f6f8; --bg-card: #fff; --bg-input: #fff;
  --text: #17202a; --text-secondary: #53606d; --text-muted: #84909c;
  --border: #e2e7eb; --shadow: 0 10px 30px rgba(23,32,42,.07);
  --nav-bg: rgba(255,255,255,.85); --chat-ai: #f5f7fa;
  --hover: #f0f3f5; --blue: #167d76; --blue-light: #e8f5f3;
  --accent: #df694f; --success: #23845c;
  --sidebar-bg: #fff;
}
html.dark {
  --bg: #111716; --bg-card: #18201f; --bg-input: #202927;
  --text: #eef3f2; --text-secondary: #b5c0be; --text-muted: #7f908d;
  --border: #2b3735; --shadow: 0 12px 34px rgba(0,0,0,.3);
  --nav-bg: rgba(17,23,22,.92); --chat-ai: #202927;
  --hover: #222d2b; --blue: #5fc1b6; --blue-light: #193532;
  --accent: #f08b73; --success: #58b98b; --sidebar-bg: #18201f;
}

* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--bg); color: var(--text);
  -webkit-font-smoothing: antialiased; min-height: 100vh;
  transition: background .3s, color .3s;
}
#app-container { min-height: 100vh; }

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
  background: var(--sidebar-bg); border-right: 1px solid var(--border);
  display: flex; flex-direction: column;
  position: sticky; top: 0; height: 100vh; transition: width .2s; overflow: hidden;
}
.sidebar.collapsed { width: 72px; }
.workspace-brand { height: 76px; display: flex; align-items: center; gap: 11px; padding: 0 17px; border: 0; border-bottom: 1px solid var(--border); background: transparent; color: var(--text); text-align: left; cursor: pointer; white-space: nowrap; }.workspace-brand > span { width: 36px; height: 36px; display: grid; flex: 0 0 36px; place-items: center; border-radius: 8px; background: var(--blue); color: #fff; font-size: 19px; }.workspace-brand strong,.workspace-brand small { display: block; }.workspace-brand strong { font-size: 14px; }.workspace-brand small { margin-top: 3px; color: var(--text-muted); font-size: 9px; }.nav-label { padding: 20px 20px 8px; color: var(--text-muted); font-size: 9px; font-weight: 700; text-transform: uppercase; }

.sb-user {
  display: flex; align-items: center; gap: 12px;
  margin: 4px 9px 12px; padding: 10px; border: 1px solid var(--border); border-radius: 8px; cursor: pointer; transition: background .15s;
}
.sb-user:hover { background: var(--hover); }
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

.sb-bottom {
  padding: 8px;
  display: flex; flex-direction: column; gap: 2px;
  border-top: 1px solid var(--border);
}
.sb-bottom-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; cursor: pointer; border-radius: 8px;
  font-size: 14px; color: var(--text-secondary); transition: background .15s;
  white-space: nowrap;
}
.sb-bottom-item:hover { background: var(--hover); }

.main-area {
  min-width: 0; flex: 1; overflow-y: auto; padding: 30px clamp(22px,3vw,44px) 48px;
  background: var(--bg);
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
.command-dialog { border-radius: 8px !important; overflow: hidden; }.command-dialog .el-dialog__header { display: none; }.command-dialog .el-dialog__body { padding: 0; }.command-input { height: 58px; display: flex; align-items: center; gap: 11px; padding: 0 17px; border-bottom: 1px solid var(--border); color: var(--text-muted); }.command-input > .el-icon { font-size: 19px; }.command-input input { min-width: 0; flex: 1; border: 0; outline: 0; background: transparent; color: var(--text); font: inherit; font-size: 14px; }.command-input input::placeholder { color: var(--text-muted); }.command-input kbd,.command-foot kbd { padding: 2px 5px; border: 1px solid var(--border); border-radius: 4px; background: var(--hover); color: var(--text-muted); font: 9px ui-monospace,monospace; }.command-results { max-height: 420px; overflow-y: auto; padding: 9px; }.command-label { padding: 8px 9px 6px; color: var(--text-muted); font-size: 9px; font-weight: 700; }.command-results button { width: 100%; display: grid; grid-template-columns: 38px 1fr auto 18px; align-items: center; gap: 10px; padding: 10px; border: 0; border-radius: 6px; background: transparent; color: var(--text); text-align: left; cursor: pointer; }.command-results button.active { background: var(--hover); }.command-results button > span:nth-child(2),.command-results button strong,.command-results button small { display: block; }.command-results button strong { font-size: 12px; }.command-results button small { margin-top: 3px; color: var(--text-muted); font-size: 9px; }.command-results button > kbd { color: var(--text-muted); font: 8px ui-monospace,monospace; }.command-results button > .el-icon { color: var(--text-muted); }.command-icon { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 6px; }.command-icon.teal { background: var(--blue-light); color: var(--blue); }.command-icon.green { background: #e8f6ee; color: #23845c; }.command-icon.coral { background: #fff0ec; color: var(--accent); }.command-icon.amber { background: #fff5df; color: #b87813; }.command-icon.blue { background: #ebf3ff; color: #367bc7; }.command-icon.gray { background: var(--hover); color: var(--text-secondary); }.command-empty { display: grid; min-height: 140px; place-items: center; color: var(--text-muted); font-size: 11px; }.command-foot { display: flex; gap: 16px; padding: 9px 17px; border-top: 1px solid var(--border); color: var(--text-muted); font-size: 9px; }.command-foot span { display: flex; align-items: center; gap: 4px; }
</style>
