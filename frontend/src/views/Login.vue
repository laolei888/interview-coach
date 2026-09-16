<template>
  <main class="auth-page">
    <section class="auth-story">
      <button class="auth-brand" type="button" @click="router.push('/')"><span><el-icon><Aim /></el-icon></span><strong>Interview Coach</strong></button>
      <div class="story-content">
        <span class="story-label">从准备到复盘，一套完整训练路径</span>
        <h1>下一场面试，<br>回答得更有底气</h1>
        <p>围绕真实岗位要求，持续优化简历表达、技术回答和项目叙述。</p>
        <div class="story-proof">
          <div><el-icon><DocumentChecked /></el-icon><span><strong>岗位定向诊断</strong><small>识别 ATS 关键词与能力缺口</small></span></div>
          <div><el-icon><ChatLineRound /></el-icon><span><strong>上下文模拟面试</strong><small>追问、评分与完整会话记录</small></span></div>
          <div><el-icon><TrendCharts /></el-icon><span><strong>结构化训练复盘</strong><small>把每次练习沉淀成行动建议</small></span></div>
        </div>
      </div>
      <p class="story-note">你的训练数据仅对当前账号可见</p>
    </section>

    <section class="auth-form-side">
      <div class="auth-form-wrap">
        <div class="mode-switch" role="tablist" aria-label="账号操作"><button :class="{ active: isLogin }" type="button" @click="switchMode(true)">登录</button><button :class="{ active: !isLogin }" type="button" @click="switchMode(false)">注册</button></div>
        <header class="form-head"><span>{{ isLogin ? '欢迎回来' : '创建训练账号' }}</span><h2>{{ isLogin ? '继续你的求职训练' : '开始建立你的面试优势' }}</h2><p>{{ isLogin ? '登录后可继续未完成的面试和简历诊断。' : '无需邮箱，设置用户名和密码即可开始。' }}</p></header>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
          <el-form-item label="用户名" prop="username"><el-input v-model.trim="form.username" placeholder="3-20 位字母、数字或下划线" size="large" :prefix-icon="User" maxlength="20" autocomplete="username" /></el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="至少 6 位" size="large" :prefix-icon="Lock" maxlength="72" show-password :autocomplete="isLogin ? 'current-password' : 'new-password'" @keydown.enter="isLogin && submit()" />
            <div v-if="!isLogin && form.password" class="password-strength"><i v-for="level in 3" :key="level" :class="{ active: passwordLevel >= level }"></i><span>{{ passwordLabel }}</span></div>
          </el-form-item>
          <el-form-item v-if="!isLogin" label="确认密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" size="large" :prefix-icon="Lock" maxlength="72" show-password autocomplete="new-password" @keydown.enter="submit" /></el-form-item>
          <el-button type="primary" native-type="submit" size="large" :loading="loading" class="submit-btn">{{ isLogin ? '登录并继续' : '创建账号' }}<el-icon v-if="!loading"><ArrowRight /></el-icon></el-button>
        </el-form>
        <p class="form-foot">{{ isLogin ? '还没有账号？' : '已经有账号？' }}<button type="button" @click="switchMode(!isLogin)">{{ isLogin ? '立即注册' : '返回登录' }}</button></p>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Aim, ArrowRight, ChatLineRound, DocumentChecked, Lock, TrendCharts, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const route = useRoute(); const router = useRouter(); const userStore = useUserStore()
const formRef = ref(null); const isLogin = ref(route.query.mode !== 'register'); const loading = ref(false)
const form = reactive({ username: '', password: '', confirmPassword: '' })
const validateUsername = (_, value, callback) => !value ? callback(new Error('请输入用户名')) : !/^[A-Za-z0-9_]{3,20}$/.test(value) ? callback(new Error('仅支持 3-20 位字母、数字或下划线')) : callback()
const validateConfirmPassword = (_, value, callback) => !isLogin.value && value !== form.password ? callback(new Error('两次输入的密码不一致')) : callback()
const rules = { username: [{ validator: validateUsername, trigger: ['blur', 'change'] }], password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 72, message: '密码长度为 6-72 位', trigger: 'blur' }], confirmPassword: [{ validator: validateConfirmPassword, trigger: ['blur', 'change'] }] }
const passwordLevel = computed(() => { const value = form.password; if (!value) return 0; let score = value.length >= 8 ? 1 : 0; if (/[A-Za-z]/.test(value) && /\d/.test(value)) score += 1; if (/[^A-Za-z0-9]/.test(value) || value.length >= 12) score += 1; return Math.max(1, Math.min(3, score)) })
const passwordLabel = computed(() => ['', '基础', '良好', '较强'][passwordLevel.value])
function switchMode(loginMode) { isLogin.value = loginMode; form.password = ''; form.confirmPassword = ''; formRef.value?.clearValidate(); router.replace({ path: '/login', query: { ...route.query, mode: loginMode ? undefined : 'register' } }) }
async function submit() {
  try { await formRef.value?.validate() } catch { return }
  loading.value = true
  try {
    if (isLogin.value) { const data = await userStore.login(form.username, form.password); if (data.code !== 200) throw new Error(data.message); ElMessage.success('登录成功'); const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') ? route.query.redirect : '/'; router.replace(redirect) }
    else { const data = await userStore.register(form.username, form.password); if (data.code !== 200) throw new Error(data.message); ElMessage.success('账号创建成功，请登录'); switchMode(true) }
  } catch (err) { ElMessage.error(err.response?.data?.message || err.message || '请求失败，请稍后重试') }
  finally { loading.value = false }
}
</script>

<style scoped>
.auth-page { min-height: calc(100vh - 56px); display: grid; grid-template-columns: minmax(420px,.95fr) minmax(520px,1.05fr); background: var(--bg-card); }
.auth-story { min-height: calc(100vh - 56px); display: flex; flex-direction: column; padding: 38px clamp(36px,6vw,86px); position: relative; overflow: hidden; background: #152725 url('https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1400&q=85') center/cover; color: #fff; isolation: isolate; }.auth-story::after { content: ''; position: absolute; inset: 0; z-index: -1; background: rgba(13,35,32,.84); }
.auth-brand { align-self: flex-start; display: flex; align-items: center; gap: 10px; border: 0; background: transparent; color: #fff; cursor: pointer; }.auth-brand > span { width: 32px; height: 32px; display: grid; place-items: center; border-radius: 7px; background: #fff; color: #167d76; }.auth-brand strong { font-size: 15px; }
.story-content { max-width: 560px; margin: auto 0; padding: 64px 0; }.story-label { color: #8ed3ca; font-size: 12px; font-weight: 700; }.story-content h1 { margin: 16px 0 20px; font-size: clamp(38px,4vw,58px); line-height: 1.12; }.story-content > p { max-width: 490px; color: rgba(255,255,255,.72); font-size: 15px; line-height: 1.8; }
.story-proof { display: grid; gap: 14px; margin-top: 38px; }.story-proof > div { display: flex; align-items: center; gap: 14px; }.story-proof > div > .el-icon { width: 38px; height: 38px; padding: 9px; border: 1px solid rgba(255,255,255,.18); border-radius: 7px; color: #8ed3ca; }.story-proof span,.story-proof strong,.story-proof small { display: block; }.story-proof strong { font-size: 13px; }.story-proof small { margin-top: 3px; color: rgba(255,255,255,.58); font-size: 11px; }.story-note { color: rgba(255,255,255,.45); font-size: 11px; }
.auth-form-side { display: grid; place-items: center; padding: 50px clamp(28px,7vw,100px); background: var(--bg-card); }.auth-form-wrap { width: min(100%,430px); }.mode-switch { display: inline-grid; grid-template-columns: repeat(2,1fr); width: 170px; padding: 3px; border-radius: 7px; background: var(--hover); }.mode-switch button { padding: 7px 16px; border: 0; border-radius: 5px; background: transparent; color: var(--text-muted); cursor: pointer; font-size: 12px; }.mode-switch button.active { background: var(--bg-card); box-shadow: 0 1px 4px rgba(0,0,0,.08); color: var(--text); font-weight: 700; }
.form-head { margin: 28px 0 32px; }.form-head > span { color: var(--blue); font-size: 12px; font-weight: 700; }.form-head h2 { margin: 8px 0 9px; color: var(--text); font-size: 28px; }.form-head p { color: var(--text-muted); font-size: 13px; line-height: 1.6; }.auth-form-wrap :deep(.el-form-item) { margin-bottom: 20px; }.auth-form-wrap :deep(.el-form-item__label) { padding-bottom: 7px; color: var(--text-secondary); font-size: 12px; font-weight: 600; }.auth-form-wrap :deep(.el-input__wrapper) { min-height: 46px; border-radius: 7px; background: var(--bg-input); box-shadow: 0 0 0 1px var(--border) inset; }.auth-form-wrap :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--blue) inset; }.submit-btn { width: 100%; height: 46px; margin-top: 4px; border-radius: 7px; font-weight: 700; }
.password-strength { width: 100%; display: grid; grid-template-columns: repeat(3,1fr) auto; align-items: center; gap: 5px; margin-top: 8px; }.password-strength i { height: 3px; border-radius: 2px; background: var(--border); }.password-strength i.active { background: var(--blue); }.password-strength span { min-width: 26px; margin-left: 4px; color: var(--text-muted); font-size: 10px; }.form-foot { margin-top: 22px; color: var(--text-muted); font-size: 12px; text-align: center; }.form-foot button { margin-left: 5px; border: 0; background: transparent; color: var(--blue); cursor: pointer; font: inherit; font-weight: 700; }
@media (max-width: 900px) { .auth-page { grid-template-columns: minmax(320px,.8fr) 1.2fr; }.auth-story { padding-inline: 30px; }.story-content h1 { font-size: 38px; } }
@media (max-width: 720px) { .auth-page { display: block; }.auth-story { min-height: 230px; padding: 24px 20px; }.story-content { margin: auto 0 0; padding: 36px 0 4px; }.story-content h1 { margin: 10px 0 0; font-size: 30px; }.story-content > p,.story-proof,.story-note { display: none; }.auth-form-side { padding: 34px 20px 50px; }.form-head { margin: 24px 0 28px; }.form-head h2 { font-size: 24px; } }
</style>
