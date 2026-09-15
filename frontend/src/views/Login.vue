<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="login-icon">🎯</div>
        <h2>{{ isLogin ? '欢迎回来' : '创建账号' }}</h2>
        <p>{{ isLogin ? '登录后开始智能面试之旅' : '注册即可体验 AI 面试辅导' }}</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" class="custom-input" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" :prefix-icon="Lock" show-password @keydown.enter="submit" class="custom-input" />
        </el-form-item>

        <el-button type="primary" size="large" :loading="loading" @click="submit" class="submit-btn">
          {{ isLogin ? '登 录' : '注 册' }}
        </el-button>
      </el-form>

      <div class="switch">
        {{ isLogin ? '还没有账号？' : '已有账号？' }}
        <span @click="isLogin = !isLogin">{{ isLogin ? '去注册' : '去登录' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const isLogin = ref(true)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const submit = async () => {
  if (!form.username || !form.password) { ElMessage.warning('请填写完整'); return }
  loading.value = true
  try {
    if (isLogin.value) {
      const data = await userStore.login(form.username, form.password)
      data.code === 200 ? (ElMessage.success('登录成功'), router.push('/')) : ElMessage.error(data.message)
    } else {
      const data = await userStore.register(form.username, form.password)
      data.code === 200 ? (ElMessage.success('注册成功，请登录'), isLogin.value = true) : ElMessage.error(data.message)
    }
  } catch (err) { ElMessage.error('请求失败') }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-page { display: flex; justify-content: center; padding-top: 64px; }
.login-card {
  width: 400px;
  background: #fff;
  border-radius: 20px;
  padding: 44px 40px;
  box-shadow: 0 8px 32px rgba(0,0,0,.06);
  animation: fadeUp .4s ease;
}
@keyframes fadeUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
.login-header { text-align: center; margin-bottom: 36px; }
.login-icon { font-size: 44px; margin-bottom: 12px; }
.login-header h2 { font-size: 24px; color: #1a1a2e; margin-bottom: 6px; }
.login-header p { color: #999; font-size: 14px; }
.submit-btn { width: 100%; height: 44px; border-radius: 10px; font-size: 16px; margin-top: 4px; }
.switch { text-align: center; margin-top: 22px; font-size: 14px; color: #999; }
.switch span { color: #409EFF; cursor: pointer; font-weight: 600; margin-left: 4px; }
.switch span:hover { text-decoration: underline; }

.custom-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e4e7ed inset;
}
.custom-input :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #c0c4cc inset; }
</style>
