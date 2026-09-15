<template>
  <main class="review-page">
    <header class="page-head"><div><span class="eyebrow">成长档案</span><h1>训练记录与复盘</h1><p>查找历史训练，回看完整对话，把反馈转成下一次行动。</p></div><el-button type="primary" :icon="VideoPlay" @click="router.push('/interview')">新面试</el-button></header>

    <section class="metric-strip"><div><strong>{{ sessions.length }}</strong><span>面试场次</span></div><div><strong>{{ completedCount }}</strong><span>完整复盘</span></div><div><strong>{{ activeCount }}</strong><span>进行中</span></div><div><strong>{{ resumes.length }}</strong><span>简历报告</span></div></section>

    <section class="review-shell">
      <div class="toolbar">
        <el-segmented v-model="activeTab" :options="[{ label: '面试记录', value: 'interview' }, { label: '简历报告', value: 'resume' }]" />
        <div class="filters"><el-input v-model="keyword" clearable :prefix-icon="Search" :placeholder="activeTab === 'interview' ? '搜索岗位' : '搜索文件名'" /><el-select v-if="activeTab === 'interview'" v-model="statusFilter" clearable placeholder="全部状态"><el-option label="进行中" value="active" /><el-option label="已完成" value="completed" /></el-select></div>
      </div>

      <div v-if="activeTab === 'interview'" v-loading="loadingSessions" class="record-list">
        <article v-for="item in filteredSessions" :key="item.id" class="record-item" @click="item.status === 'completed' ? openSession(item) : continueSession(item.id)">
          <span class="record-icon"><el-icon><ChatLineRound /></el-icon></span><div class="record-main"><div><h2>{{ item.position }}</h2><el-tag :type="item.status === 'completed' ? 'success' : 'warning'" size="small" effect="plain">{{ item.status === 'completed' ? '已完成' : '进行中' }}</el-tag></div><p>{{ formatDate(item.createdAt, true) }} · {{ item.status === 'completed' ? '查看对话与评估报告' : '继续未完成的训练' }}</p></div>
          <div class="record-actions" @click.stop><el-button v-if="item.status === 'active'" text type="primary" @click="continueSession(item.id)">继续</el-button><el-button v-if="item.status === 'active'" text @click="finishSession(item.id)">结束</el-button><el-button text circle :icon="Delete" aria-label="删除记录" @click="removeSession(item)" /></div>
        </article>
        <el-empty v-if="!loadingSessions && !filteredSessions.length" description="没有匹配的面试记录" />
      </div>

      <div v-else v-loading="loadingResumes" class="record-list">
        <article v-for="item in filteredResumes" :key="item.id" class="record-item" @click="openResume(item)"><span class="record-icon resume"><el-icon><Document /></el-icon></span><div class="record-main"><div><h2>{{ item.fileName }}</h2><span v-if="item.aiScore" class="score">{{ item.aiScore }} 分</span></div><p>{{ formatDate(item.createdAt, true) }}{{ item.matchPosition ? ` · ${item.matchPosition}` : '' }}</p></div><el-icon class="arrow"><ArrowRight /></el-icon></article>
        <el-empty v-if="!loadingResumes && !filteredResumes.length" description="没有匹配的简历报告" />
      </div>
    </section>

    <el-drawer v-model="sessionDrawer" size="min(760px, 100%)" direction="rtl" destroy-on-close>
      <template #header><div class="drawer-title"><span>面试复盘</span><small>{{ detail?.session?.position }}</small></div></template>
      <div v-if="detail" class="review-detail"><section class="detail-summary"><div><span>岗位</span><strong>{{ detail.session.position }}</strong></div><div><span>对话轮次</span><strong>{{ answerCount }} 轮</strong></div><div><span>训练时间</span><strong>{{ formatDate(detail.session.createdAt) }}</strong></div></section>
        <section v-if="summaryMessage" class="assessment"><div class="section-title"><el-icon><DataAnalysis /></el-icon><h2>能力评估</h2><el-button text :icon="Download" @click="downloadInterview">下载报告</el-button></div><article v-html="renderMarkdown(summaryMessage.content.replace('【面试总结】', ''))"></article></section>
        <section class="conversation"><div class="section-title"><el-icon><ChatDotRound /></el-icon><h2>完整对话</h2></div><div v-for="message in conversationMessages" :key="message.id" :class="['message', message.role]"><span>{{ message.role === 'ai' ? 'AI' : '我' }}</span><article v-html="renderMarkdown(message.content)"></article></div></section>
      </div>
    </el-drawer>

    <el-drawer v-model="resumeDrawer" size="min(720px, 100%)" direction="rtl" destroy-on-close><template #header><div class="drawer-title"><span>简历分析报告</span><small>{{ resumeDetail?.fileName }}</small></div></template><div v-if="resumeDetail" class="resume-report"><div class="resume-score"><strong>{{ resumeDetail.aiScore ?? '--' }}</strong><span>综合评分</span><el-button :icon="Download" @click="downloadResume">下载报告</el-button></div><article v-html="renderMarkdown(resumeDetail.aiFeedback || '')"></article></div></el-drawer>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, ChatDotRound, ChatLineRound, DataAnalysis, Delete, Document, Download, Search, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteSession, endInterview, getResumeDetail, getResumeHistory, getSessionDetail, getSessions } from '../api'
import { renderMarkdown } from '../utils/markdown'

const router = useRouter(); const activeTab = ref('interview'); const keyword = ref(''); const statusFilter = ref('')
const sessions = ref([]); const resumes = ref([]); const loadingSessions = ref(false); const loadingResumes = ref(false)
const sessionDrawer = ref(false); const resumeDrawer = ref(false); const detail = ref(null); const resumeDetail = ref(null)
const completedCount = computed(() => sessions.value.filter(item => item.status === 'completed').length)
const activeCount = computed(() => sessions.value.filter(item => item.status === 'active').length)
const filteredSessions = computed(() => sessions.value.filter(item => (!statusFilter.value || item.status === statusFilter.value) && (!keyword.value || item.position?.toLowerCase().includes(keyword.value.toLowerCase()))))
const filteredResumes = computed(() => resumes.value.filter(item => !keyword.value || item.fileName?.toLowerCase().includes(keyword.value.toLowerCase())))
const summaryMessage = computed(() => detail.value?.messages?.find(item => item.content?.startsWith('【面试总结】')))
const conversationMessages = computed(() => detail.value?.messages?.filter(item => !item.content?.startsWith('【面试总结】')) || [])
const answerCount = computed(() => conversationMessages.value.filter(item => item.role === 'user').length)

onMounted(() => { loadSessions(); loadResumes() })
async function loadSessions() { loadingSessions.value = true; try { const { data } = await getSessions(); if (data.code === 200) sessions.value = data.data } catch (e) { ElMessage.error('面试记录加载失败') } finally { loadingSessions.value = false } }
async function loadResumes() { loadingResumes.value = true; try { const { data } = await getResumeHistory(); if (data.code === 200) resumes.value = data.data } catch (e) { ElMessage.error('简历报告加载失败') } finally { loadingResumes.value = false } }
async function openSession(item) { try { const { data } = await getSessionDetail(item.id); if (data.code === 200) { detail.value = data.data; sessionDrawer.value = true } } catch (e) { ElMessage.error('复盘详情加载失败') } }
async function openResume(item) { try { const { data } = await getResumeDetail(item.id); if (data.code === 200) { resumeDetail.value = data.data; resumeDrawer.value = true } } catch (e) { ElMessage.error('简历报告加载失败') } }
const continueSession = id => router.push({ path: '/interview', query: { sessionId: id } })
async function finishSession(id) { try { await ElMessageBox.confirm('结束后将生成能力评估，且不能继续回答。', '结束面试', { type: 'warning' }); const { data } = await endInterview(id); if (data.code !== 200) throw new Error(data.message); await loadSessions(); ElMessage.success('面试已结束') } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e.response?.data?.message || e.message || '结束失败') } }
async function removeSession(item) { try { await ElMessageBox.confirm(`确定删除“${item.position}”及全部对话吗？`, '删除训练记录', { type: 'warning' }); const { data } = await deleteSession(item.id); if (data.code !== 200) throw new Error(data.message); sessions.value = sessions.value.filter(row => row.id !== item.id); ElMessage.success('训练记录已删除') } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e.response?.data?.message || e.message || '删除失败') } }
function downloadText(name, content) { const url = URL.createObjectURL(new Blob([content], { type: 'text/markdown;charset=utf-8' })); const link = document.createElement('a'); link.href = url; link.download = name; link.click(); URL.revokeObjectURL(url) }
const downloadInterview = () => downloadText(`${detail.value.session.position}_面试报告.md`, summaryMessage.value?.content || '')
const downloadResume = () => downloadText(`${resumeDetail.value.fileName.replace(/\.pdf$/i, '')}_分析报告.md`, resumeDetail.value.aiFeedback || '')
const formatDate = (value, withTime = false) => value ? new Date(value).toLocaleString('zh-CN', withTime ? { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' } : { month: 'short', day: 'numeric' }) : ''
</script>

<style scoped>
.review-page { max-width: 1120px; margin: 0 auto; color: var(--text); }.page-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 20px; margin-bottom: 22px; }.eyebrow { color: var(--blue); font-size: 12px; font-weight: 700; }.page-head h1 { margin: 5px 0 7px; font-size: 29px; }.page-head p { color: var(--text-muted); font-size: 13px; }
.metric-strip { display: grid; grid-template-columns: repeat(4,1fr); margin-bottom: 16px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); }.metric-strip div { padding: 17px 20px; border-right: 1px solid var(--border); }.metric-strip div:last-child { border: 0; }.metric-strip strong,.metric-strip span { display: block; }.metric-strip strong { font-size: 23px; }.metric-strip span { margin-top: 3px; color: var(--text-muted); font-size: 11px; }
.review-shell { overflow: hidden; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); }.toolbar { display: flex; align-items: center; justify-content: space-between; gap: 15px; padding: 14px 18px; border-bottom: 1px solid var(--border); }.filters { display: flex; gap: 9px; }.filters :deep(.el-input) { width: 230px; }.filters :deep(.el-select) { width: 130px; }
.record-list { min-height: 280px; }.record-item { display: flex; align-items: center; gap: 14px; padding: 18px 20px; border-bottom: 1px solid var(--border); cursor: pointer; }.record-item:last-child { border: 0; }.record-item:hover { background: var(--hover); }.record-icon { width: 40px; height: 40px; display: grid; flex: 0 0 40px; place-items: center; border-radius: 8px; background: var(--blue-light); color: var(--blue); }.record-icon.resume { color: #289b62; background: #e9f7ef; }.record-main { min-width: 0; flex: 1; }.record-main > div { display: flex; align-items: center; gap: 9px; }.record-main h2 { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }.record-main p { margin-top: 5px; color: var(--text-muted); font-size: 11px; }.record-actions { display: flex; align-items: center; }.score { color: var(--blue); font-size: 13px; font-weight: 700; }.arrow { color: var(--text-muted); }
.drawer-title span,.drawer-title small { display: block; }.drawer-title span { color: var(--text); font-size: 17px; font-weight: 700; }.drawer-title small { margin-top: 3px; color: var(--text-muted); font-size: 11px; }.review-detail,.resume-report { color: var(--text); }.detail-summary { display: grid; grid-template-columns: repeat(3,1fr); margin-bottom: 18px; border: 1px solid var(--border); border-radius: 7px; }.detail-summary div { padding: 13px; border-right: 1px solid var(--border); }.detail-summary div:last-child { border: 0; }.detail-summary span,.detail-summary strong { display: block; }.detail-summary span { color: var(--text-muted); font-size: 10px; }.detail-summary strong { margin-top: 5px; font-size: 13px; }.assessment,.conversation { margin-bottom: 20px; border: 1px solid var(--border); border-radius: 8px; overflow: hidden; }.section-title { display: flex; align-items: center; gap: 8px; padding: 14px 16px; border-bottom: 1px solid var(--border); }.section-title h2 { flex: 1; font-size: 14px; }.section-title > .el-icon { color: var(--blue); }.assessment > article { padding: 8px 20px 24px; font-size: 13px; line-height: 1.8; }.assessment :deep(h2) { margin: 18px 0 8px; font-size: 15px; }.assessment :deep(ul) { padding-left: 20px; }
.message { display: flex; gap: 10px; padding: 14px 16px; border-bottom: 1px solid var(--border); }.message:last-child { border: 0; }.message > span { width: 28px; height: 28px; display: grid; flex: 0 0 28px; place-items: center; border-radius: 50%; background: var(--hover); color: var(--text-muted); font-size: 10px; }.message.user { background: var(--blue-light); }.message article { min-width: 0; font-size: 13px; line-height: 1.75; }.resume-score { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; padding: 16px; border: 1px solid var(--border); border-radius: 8px; }.resume-score strong { color: var(--blue); font-size: 30px; }.resume-score span { flex: 1; color: var(--text-muted); font-size: 11px; }.resume-report > article { font-size: 13px; line-height: 1.85; }.resume-report :deep(h2),.resume-report :deep(h3) { margin: 20px 0 8px; }
@media(max-width:650px){.page-head{align-items:flex-start;flex-direction:column}.metric-strip{grid-template-columns:repeat(2,1fr)}.metric-strip div:nth-child(2){border-right:0}.metric-strip div:nth-child(-n+2){border-bottom:1px solid var(--border)}.toolbar{align-items:stretch;flex-direction:column}.toolbar :deep(.el-segmented){width:100%}.filters{display:grid;grid-template-columns:1fr 120px}.filters :deep(.el-input),.filters :deep(.el-select){width:100%}.record-item{align-items:flex-start;padding:15px 12px}.record-actions{flex-wrap:wrap;justify-content:flex-end}.detail-summary{grid-template-columns:1fr}.detail-summary div{border-right:0;border-bottom:1px solid var(--border)}.page-head h1{font-size:25px}}
</style>
