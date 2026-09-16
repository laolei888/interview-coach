<template>
  <main class="account-page">
    <header class="page-header">
      <div><span class="eyebrow">账户设置</span><h1>个人中心</h1><p>管理你的公开资料、账户偏好和登录安全。</p></div>
      <el-button :icon="SwitchButton" @click="logout">退出登录</el-button>
    </header>

    <section class="identity-band">
      <div class="identity-main"><button class="avatar-button" type="button" aria-label="更换头像" @click="triggerUpload">
        <img v-if="avatarPreview" :src="avatarPreview" alt="用户头像" />
        <span v-else>{{ initial }}</span>
        <i><el-icon><Camera /></el-icon></i>
      </button>
      <input ref="fileInput" hidden type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="uploadAvatar" />
      <div class="identity-copy"><span>个人训练档案</span><h2>{{ profile.username || '加载中' }}</h2><p>{{ profile.email || '尚未绑定邮箱' }} · {{ memberSince }}</p></div></div>
      <div class="identity-score"><div class="profile-ring" :style="{ '--score': readinessScore }"><span><strong>{{ readinessScore }}</strong><small>准备度</small></span></div><div><span class="account-state"><i></i>账户正常</span><p>训练数据与安全设置已同步</p></div></div>
    </section>

    <section class="metric-strip" v-loading="statsLoading">
      <div><strong>{{ stats.interviewCount }}</strong><span>模拟面试</span></div>
      <div><strong>{{ stats.completedCount }}</strong><span>完成复盘</span></div>
      <div><strong>{{ stats.resumeCount }}</strong><span>简历报告</span></div>
      <div><strong>{{ questionMastery }}%</strong><span>题库掌握率</span></div>
    </section>

    <div class="settings-layout">
      <nav class="settings-nav" aria-label="设置分类">
        <div><span>账户管理</span></div><button :class="{ active: activeSection === 'profile' }" @click="activeSection = 'profile'"><el-icon><User /></el-icon><span><strong>基本资料</strong><small>头像、邮箱与个人信息</small></span><el-icon><ArrowRight /></el-icon></button>
        <button :class="{ active: activeSection === 'security' }" @click="activeSection = 'security'"><el-icon><Lock /></el-icon><span><strong>登录安全</strong><small>密码与会话保护</small></span><el-icon><ArrowRight /></el-icon></button>
      </nav>

      <section v-if="activeSection === 'profile'" class="settings-panel">
        <div class="panel-head"><h2>基本资料</h2><p>这些信息用于完善你的账户资料。</p></div>
        <el-form label-position="top" class="settings-form">
          <el-form-item label="用户名"><el-input :model-value="profile.username" disabled><template #prefix><el-icon><User /></el-icon></template></el-input></el-form-item>
          <el-form-item label="邮箱地址" :error="emailError"><el-input v-model.trim="profile.email" maxlength="100" placeholder="name@example.com"><template #prefix><el-icon><Message /></el-icon></template></el-input></el-form-item>
          <el-form-item label="性别"><el-segmented v-model="profile.gender" :options="['男', '女', '保密']" /></el-form-item>
          <div class="form-actions"><el-button type="primary" :loading="saving" @click="saveProfile">保存更改</el-button></div>
        </el-form>
      </section>

      <section v-else class="settings-panel">
        <div class="panel-head"><h2>登录安全</h2><p>定期更新密码，降低账户被盗用的风险。</p></div>
        <div class="security-grid"><div class="security-status"><el-icon><CircleCheckFilled /></el-icon><div><strong>密码保护已启用</strong><span>密码使用 BCrypt 不可逆摘要存储</span></div></div><div class="security-status session"><el-icon><Key /></el-icon><div><strong>会话撤销已启用</strong><span>修改密码后，其他设备上的旧会话会立即失效</span></div></div></div>
        <el-form label-position="top" class="settings-form password-form">
          <el-form-item label="当前密码"><el-input v-model="password.oldPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
          <div class="password-grid">
            <el-form-item label="新密码"><el-input v-model="password.newPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
            <el-form-item label="确认新密码" :error="confirmError"><el-input v-model="password.confirmPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
          </div>
          <div class="password-meter"><i :style="{ width: passwordStrength + '%' }"></i></div>
          <p class="password-hint">至少 6 位，建议同时包含字母、数字和符号。</p>
          <div class="form-actions"><el-button type="primary" :loading="changingPassword" @click="changePassword">更新密码</el-button></div>
        </el-form>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Camera, CircleCheckFilled, Key, Lock, Message, SwitchButton, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const fileInput = ref(null)
const activeSection = ref('profile')
const saving = ref(false)
const changingPassword = ref(false)
const statsLoading = ref(false)
const avatarPreview = ref('')
const avatarPath = ref('')
const profile = reactive({ username: '', email: '', gender: '保密', createdAt: '' })
const password = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const stats = reactive({ interviewCount: 0, completedCount: 0, resumeCount: 0, questionCount: 0, averageResumeScore: 0, questionProgress: { practiced: 0, mastered: 0, review: 0 } })
let objectUrl = ''

const initial = computed(() => (profile.username || 'U').slice(0, 1).toUpperCase())
const memberSince = computed(() => profile.createdAt ? `${new Date(profile.createdAt).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long' })}加入` : '训练账号')
const questionMastery = computed(() => stats.questionProgress.practiced ? Math.round(stats.questionProgress.mastered / stats.questionProgress.practiced * 100) : 0)
const readinessScore = computed(() => Math.round(Math.min(100, (stats.completedCount ? Math.min(100, stats.completedCount * 20) : 0) * .35 + (stats.averageResumeScore || 0) * .35 + questionMastery.value * .3)))
const emailError = computed(() => profile.email && !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(profile.email) ? '请输入有效的邮箱地址' : '')
const confirmError = computed(() => password.confirmPassword && password.newPassword !== password.confirmPassword ? '两次输入的密码不一致' : '')
const passwordStrength = computed(() => {
  const value = password.newPassword
  if (!value) return 0
  let score = Math.min(value.length * 7, 55)
  if (/[A-Za-z]/.test(value) && /\d/.test(value)) score += 20
  if (/[^A-Za-z0-9]/.test(value)) score += 25
  return Math.min(score, 100)
})

onMounted(async () => {
  statsLoading.value = true
  try {
    const [userRes, statsRes] = await Promise.all([api.get('/user/me'), api.get('/stats/dashboard')])
    if (userRes.data.code === 200) {
      const data = userRes.data.data
      Object.assign(profile, { username: data.username, email: data.email || '', gender: data.gender || '保密', createdAt: data.createdAt || '' })
      avatarPath.value = data.avatar || ''
      await loadAvatar(data.avatar)
    }
    if (statsRes.data.code === 200) Object.assign(stats, statsRes.data.data)
  } catch (error) { ElMessage.error(error.response?.data?.message || '账户信息加载失败') }
  finally { statsLoading.value = false }
})

onBeforeUnmount(() => { if (objectUrl) URL.revokeObjectURL(objectUrl) })

async function loadAvatar(path) {
  if (!path?.startsWith('/api/user/avatar/')) return
  const response = await api.get(path.replace('/api', ''), { responseType: 'blob' })
  if (objectUrl) URL.revokeObjectURL(objectUrl)
  objectUrl = URL.createObjectURL(response.data)
  avatarPreview.value = objectUrl
}

function triggerUpload() { fileInput.value?.click() }

async function uploadAvatar(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) return ElMessage.warning('请选择 JPG、PNG、GIF 或 WEBP 图片')
  if (file.size > 2 * 1024 * 1024) return ElMessage.warning('头像大小不能超过 2MB')
  const form = new FormData(); form.append('file', file)
  try {
    const { data } = await api.post('/user/upload-avatar', form)
    if (data.code !== 200) throw new Error(data.message)
    avatarPath.value = data.data.avatar
    await loadAvatar(avatarPath.value)
    ElMessage.success('头像已更新')
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '头像上传失败') }
}

async function saveProfile() {
  if (emailError.value) return ElMessage.warning(emailError.value)
  saving.value = true
  try {
    const { data } = await api.post('/user/update-profile', { email: profile.email, gender: profile.gender })
    if (data.code !== 200) throw new Error(data.message)
    ElMessage.success('个人资料已保存')
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '保存失败') }
  finally { saving.value = false }
}

async function changePassword() {
  if (!password.oldPassword || password.newPassword.length < 6) return ElMessage.warning('请填写当前密码，新密码至少 6 位')
  if (confirmError.value || !password.confirmPassword) return ElMessage.warning('请确认两次输入的新密码一致')
  changingPassword.value = true
  try {
    const { data } = await api.post('/user/change-password', { oldPassword: password.oldPassword, newPassword: password.newPassword })
    if (data.code !== 200) throw new Error(data.message)
    ElMessage.success('密码已更新，请重新登录')
    setTimeout(logout, 600)
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '密码更新失败') }
  finally { changingPassword.value = false }
}

function logout() { userStore.logout(); router.push('/login') }
</script>

<style scoped>
.account-page { max-width: 1180px; margin: 0 auto; color: var(--text); }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 24px; }
.eyebrow { color: var(--blue); font-size: 12px; font-weight: 700; }
.page-header h1 { margin: 5px 0 7px; font-size: 30px; letter-spacing: 0; }
.page-header p { color: var(--text-muted); font-size: 14px; }
.identity-band { min-height: 156px; display: flex; align-items: center; justify-content: space-between; gap: 28px; padding: 26px 30px; border-radius: 8px; position: relative; overflow: hidden; background: #172724; color: #fff; }.identity-band::after { content: ''; position: absolute; inset: 0 0 0 65%; background: url('https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&w=700&q=80') center/cover; opacity: .1; }.identity-main,.identity-score { position: relative; z-index: 1; display: flex; align-items: center; gap: 18px; }
.avatar-button { position: relative; width: 76px; height: 76px; flex: 0 0 76px; padding: 0; border: 1px solid var(--border); border-radius: 50%; overflow: hidden; background: var(--blue-light); color: var(--blue); font-size: 28px; font-weight: 700; cursor: pointer; }
.avatar-button img { width: 100%; height: 100%; object-fit: cover; }
.avatar-button i { position: absolute; right: 0; bottom: 0; width: 27px; height: 27px; display: grid; place-items: center; border: 2px solid var(--bg); border-radius: 50%; background: var(--text); color: var(--bg-card); font-size: 13px; }
.identity-copy { min-width: 0; flex: 1; }
.identity-copy > span { color: #82cfc4; font-size: 9px; font-weight: 700; }.identity-copy h2 { margin: 5px 0; color: #fff; font-size: 23px; }.identity-copy p { color: rgba(255,255,255,.55); font-size: 11px; overflow: hidden; text-overflow: ellipsis; }.profile-ring { --score: 0; width: 84px; height: 84px; display: grid; place-items: center; border-radius: 50%; position: relative; background: conic-gradient(#62c8ba calc(var(--score) * 1%),rgba(255,255,255,.1) 0); }.profile-ring::after { content: ''; position: absolute; inset: 7px; border-radius: 50%; background: #172724; }.profile-ring span { position: relative; z-index: 1; text-align: center; }.profile-ring strong,.profile-ring small { display: block; }.profile-ring strong { font-size: 22px; }.profile-ring small { color: rgba(255,255,255,.5); font-size: 8px; }.account-state { display: flex; align-items: center; gap: 7px; color: #82cfc4; font-size: 11px; font-weight: 700; }.account-state i { width: 7px; height: 7px; border-radius: 50%; background: #56ca91; }.identity-score p { margin-top: 5px; color: rgba(255,255,255,.48); font-size: 9px; }
.metric-strip { display: grid; grid-template-columns: repeat(4, 1fr); min-height: 84px; margin: 16px 0; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); }
.metric-strip div { padding: 17px 20px; border-right: 1px solid var(--border); }
.metric-strip div:last-child { border-right: 0; }
.metric-strip strong, .metric-strip span { display: block; }
.metric-strip strong { margin-bottom: 3px; font-size: 23px; }
.metric-strip span { color: var(--text-muted); font-size: 12px; }
.settings-layout { display: grid; grid-template-columns: 248px minmax(0, 1fr); gap: 16px; align-items: start; }
.settings-nav, .settings-panel { border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); }
.settings-nav { padding: 8px; }.settings-nav > div { padding: 10px 12px 7px; color: var(--text-muted); font-size: 9px; font-weight: 700; }.settings-nav button { width: 100%; display: grid; grid-template-columns: 25px 1fr 18px; align-items: center; gap: 8px; padding: 12px; border: 0; border-radius: 6px; background: transparent; color: var(--text-secondary); text-align: left; cursor: pointer; }
.settings-nav button:hover { background: var(--hover); }
.settings-nav button.active { background: var(--blue-light); color: var(--blue); font-weight: 600; }
.settings-nav button span,.settings-nav button strong,.settings-nav button small { display: block; }.settings-nav button strong { font-size: 11px; }.settings-nav button small { margin-top: 3px; color: var(--text-muted); font-size: 8px; font-weight: 400; }
.settings-panel { overflow: hidden; }
.panel-head { padding: 22px 24px; border-bottom: 1px solid var(--border); }
.panel-head h2 { margin-bottom: 5px; font-size: 18px; }
.panel-head p { color: var(--text-muted); font-size: 13px; }
.settings-form { max-width: 650px; padding: 24px; }
.settings-form :deep(.el-form-item) { margin-bottom: 21px; }
.form-actions { display: flex; justify-content: flex-end; padding-top: 5px; }
.security-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin: 24px 24px 0; }.security-status { display: flex; align-items: center; gap: 12px; padding: 14px 16px; border: 1px solid #bce3cd; border-radius: 7px; background: rgba(54,162,105,.07); color: #268653; }.security-status.session { border-color: color-mix(in srgb,var(--blue) 35%,var(--border)); background: var(--blue-light); color: var(--blue); }
.security-status > .el-icon { font-size: 24px; }
.security-status strong, .security-status span { display: block; }
.security-status strong { font-size: 14px; }
.security-status span { margin-top: 3px; color: var(--text-muted); font-size: 12px; }
.password-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.password-meter { height: 4px; margin-top: -8px; border-radius: 2px; overflow: hidden; background: var(--border); }
.password-meter i { display: block; height: 100%; background: var(--blue); transition: width .2s; }
.password-hint { margin-top: 8px; color: var(--text-muted); font-size: 12px; }
@media (max-width: 720px) {
  .page-header { align-items: flex-start; flex-direction: column; }
  .identity-band { align-items: flex-start; flex-direction: column; padding: 22px 18px; }.identity-score { width: 100%; padding-top: 16px; border-top: 1px solid rgba(255,255,255,.1); }.profile-ring { width: 68px; height: 68px; }
  .metric-strip { grid-template-columns: repeat(2, 1fr); }
  .metric-strip div:nth-child(2) { border-right: 0; }
  .metric-strip div:nth-child(-n+2) { border-bottom: 1px solid var(--border); }
  .settings-layout { grid-template-columns: 1fr; }
  .settings-nav { display: grid; grid-template-columns: 1fr 1fr; }.settings-nav > div { display: none; }
  .settings-nav button .el-icon:last-child { display: none; }
  .password-grid { grid-template-columns: 1fr; gap: 0; }
  .security-grid { grid-template-columns: 1fr; margin: 16px 16px 0; }
}
@media (max-width: 420px) { .settings-form, .panel-head { padding-left: 16px; padding-right: 16px; } }
</style>
