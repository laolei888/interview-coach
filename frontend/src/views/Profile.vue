<template>
  <main class="account-page">
    <header class="page-header">
      <div><span class="eyebrow">账户设置</span><h1>个人中心</h1><p>管理你的公开资料、账户偏好和登录安全。</p></div>
      <el-button :icon="SwitchButton" @click="logout">退出登录</el-button>
    </header>

    <section class="identity-band">
      <button class="avatar-button" type="button" aria-label="更换头像" @click="triggerUpload">
        <img v-if="avatarPreview" :src="avatarPreview" alt="用户头像" />
        <span v-else>{{ initial }}</span>
        <i><el-icon><Camera /></el-icon></i>
      </button>
      <input ref="fileInput" hidden type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="uploadAvatar" />
      <div class="identity-copy"><h2>{{ profile.username || '加载中' }}</h2><p>{{ profile.email || '尚未绑定邮箱' }}</p></div>
      <div class="account-state"><span></span>账户正常</div>
    </section>

    <section class="metric-strip" v-loading="statsLoading">
      <div><strong>{{ stats.interviewCount }}</strong><span>模拟面试</span></div>
      <div><strong>{{ stats.completedCount }}</strong><span>完成复盘</span></div>
      <div><strong>{{ stats.resumeCount }}</strong><span>简历报告</span></div>
      <div><strong>{{ stats.questionCount }}</strong><span>可练习题目</span></div>
    </section>

    <div class="settings-layout">
      <nav class="settings-nav" aria-label="设置分类">
        <button :class="{ active: activeSection === 'profile' }" @click="activeSection = 'profile'"><el-icon><User /></el-icon><span>基本资料</span><el-icon><ArrowRight /></el-icon></button>
        <button :class="{ active: activeSection === 'security' }" @click="activeSection = 'security'"><el-icon><Lock /></el-icon><span>登录安全</span><el-icon><ArrowRight /></el-icon></button>
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
        <div class="security-status"><el-icon><CircleCheckFilled /></el-icon><div><strong>密码保护已启用</strong><span>你的密码以不可逆方式加密存储</span></div></div>
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
import { ArrowRight, Camera, CircleCheckFilled, Lock, Message, SwitchButton, User } from '@element-plus/icons-vue'
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
const profile = reactive({ username: '', email: '', gender: '保密' })
const password = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const stats = reactive({ interviewCount: 0, completedCount: 0, resumeCount: 0, questionCount: 0 })
let objectUrl = ''

const initial = computed(() => (profile.username || 'U').slice(0, 1).toUpperCase())
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
      Object.assign(profile, { username: data.username, email: data.email || '', gender: data.gender || '保密' })
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
.account-page { max-width: 1080px; margin: 0 auto; color: var(--text); }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 24px; }
.eyebrow { color: var(--blue); font-size: 12px; font-weight: 700; }
.page-header h1 { margin: 5px 0 7px; font-size: 30px; letter-spacing: 0; }
.page-header p { color: var(--text-muted); font-size: 14px; }
.identity-band { display: flex; align-items: center; gap: 18px; padding: 24px 0; border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); }
.avatar-button { position: relative; width: 76px; height: 76px; flex: 0 0 76px; padding: 0; border: 1px solid var(--border); border-radius: 50%; overflow: hidden; background: var(--blue-light); color: var(--blue); font-size: 28px; font-weight: 700; cursor: pointer; }
.avatar-button img { width: 100%; height: 100%; object-fit: cover; }
.avatar-button i { position: absolute; right: 0; bottom: 0; width: 27px; height: 27px; display: grid; place-items: center; border: 2px solid var(--bg); border-radius: 50%; background: var(--text); color: var(--bg-card); font-size: 13px; }
.identity-copy { min-width: 0; flex: 1; }
.identity-copy h2 { margin-bottom: 5px; font-size: 20px; }
.identity-copy p { color: var(--text-muted); font-size: 13px; overflow: hidden; text-overflow: ellipsis; }
.account-state { display: flex; align-items: center; gap: 7px; color: #36a269; font-size: 13px; }
.account-state span { width: 8px; height: 8px; border-radius: 50%; background: #36a269; }
.metric-strip { display: grid; grid-template-columns: repeat(4, 1fr); min-height: 84px; margin: 18px 0; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); }
.metric-strip div { padding: 17px 20px; border-right: 1px solid var(--border); }
.metric-strip div:last-child { border-right: 0; }
.metric-strip strong, .metric-strip span { display: block; }
.metric-strip strong { margin-bottom: 3px; font-size: 23px; }
.metric-strip span { color: var(--text-muted); font-size: 12px; }
.settings-layout { display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: 18px; align-items: start; }
.settings-nav, .settings-panel { border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); }
.settings-nav { padding: 8px; }
.settings-nav button { width: 100%; display: grid; grid-template-columns: 22px 1fr 18px; align-items: center; gap: 8px; padding: 12px; border: 0; border-radius: 6px; background: transparent; color: var(--text-secondary); text-align: left; cursor: pointer; }
.settings-nav button:hover { background: var(--hover); }
.settings-nav button.active { background: var(--blue-light); color: var(--blue); font-weight: 600; }
.settings-panel { overflow: hidden; }
.panel-head { padding: 22px 24px; border-bottom: 1px solid var(--border); }
.panel-head h2 { margin-bottom: 5px; font-size: 18px; }
.panel-head p { color: var(--text-muted); font-size: 13px; }
.settings-form { max-width: 650px; padding: 24px; }
.settings-form :deep(.el-form-item) { margin-bottom: 21px; }
.form-actions { display: flex; justify-content: flex-end; padding-top: 5px; }
.security-status { display: flex; align-items: center; gap: 12px; margin: 24px 24px 0; padding: 14px 16px; border: 1px solid #bce3cd; border-radius: 7px; background: rgba(54, 162, 105, .07); color: #268653; }
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
  .identity-band { flex-wrap: wrap; }
  .account-state { width: 100%; padding-left: 94px; }
  .metric-strip { grid-template-columns: repeat(2, 1fr); }
  .metric-strip div:nth-child(2) { border-right: 0; }
  .metric-strip div:nth-child(-n+2) { border-bottom: 1px solid var(--border); }
  .settings-layout { grid-template-columns: 1fr; }
  .settings-nav { display: grid; grid-template-columns: 1fr 1fr; }
  .settings-nav button .el-icon:last-child { display: none; }
  .password-grid { grid-template-columns: 1fr; gap: 0; }
}
@media (max-width: 420px) { .settings-form, .panel-head { padding-left: 16px; padding-right: 16px; } }
</style>
