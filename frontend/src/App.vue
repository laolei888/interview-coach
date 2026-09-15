<template>
  <div id="app-container" :class="{ 'has-sidebar': showSidebar }">
    <!-- ======== 侧边栏布局（功能页） ======== -->
    <div v-if="showSidebar" class="layout">
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <!-- 用户头像 -->
        <div class="sb-user" @click="$router.push('/profile')">
          <img v-if="isImageAvatar" :src="userAvatar" class="sb-avatar" />
          <span v-else class="sb-avatar-d">{{ isImageAvatar ? '' : userAvatar }}</span>
          <div v-if="!sidebarCollapsed" class="sb-info">
            <strong>{{ userStore.username }}</strong>
            <small>个人中心</small>
          </div>
        </div>

        <el-divider style="margin:8px 0" />

        <!-- 菜单 -->
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from './stores/user'
import { useThemeStore } from './stores/theme'
import api from './api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const userAvatar = ref('')
const sidebarCollapsed = ref(false)
let avatarObjectUrl = ''

// 判断头像是不是真实图片路径（非 emoji）
const isImageAvatar = computed(() => userAvatar.value && (userAvatar.value.startsWith('blob:') || userAvatar.value.startsWith('http')))

// 首页和登录页用全宽布局，其他页用侧边栏
const showSidebar = computed(() => {
  const fullPages = ['/', '/login']
  return userStore.isLoggedIn() && !fullPages.includes(route.path)
})

onMounted(async () => {
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
  if (avatarObjectUrl) URL.revokeObjectURL(avatarObjectUrl)
})

const handleLogout = () => { userStore.logout(); router.push('/') }
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
  width: 240px; flex-shrink: 0;
  background: var(--sidebar-bg); border-right: 1px solid var(--border);
  display: flex; flex-direction: column;
  transition: width .2s; overflow: hidden;
}
.sidebar.collapsed { width: 64px; }

.sb-user {
  display: flex; align-items: center; gap: 12px;
  padding: 16px; cursor: pointer; transition: background .15s;
}
.sb-user:hover { background: var(--hover); }
.sb-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.sb-avatar-d { font-size: 28px; flex-shrink: 0; }
.sb-info strong { display: block; font-size: 14px; color: var(--text); }
.sb-info small { font-size: 12px; color: var(--text-muted); }

.sb-menu {
  flex: 1; border-right: none !important;
  background: transparent !important;
}
.sb-menu :deep(.el-menu-item) {
  color: var(--text-secondary);
  height: 44px; line-height: 44px; margin: 2px 8px; border-radius: 8px;
}
.sb-menu :deep(.el-menu-item:hover) { background: var(--hover); color: var(--text); }
.sb-menu :deep(.el-menu-item.is-active) {
  background: var(--blue-light); color: var(--blue); font-weight: 600;
}

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
  flex: 1; overflow-y: auto; padding: 24px 28px;
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
html.dark .el-menu-item.is-active { background: var(--blue-light) !important; color: var(--blue) !important; }
html.dark .el-divider__text { background: var(--bg-card); color: var(--text-muted); }
</style>
