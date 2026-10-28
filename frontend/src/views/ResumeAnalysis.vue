<template>
  <main class="resume-workspace">
    <header class="resume-hero">
      <div class="resume-hero-copy">
        <span class="eyebrow"><i></i>RESUME INTELLIGENCE / 简历工作台</span>
        <h1>让每一版简历，都更接近目标岗位。</h1>
        <p>把简历、岗位上下文和 AI 反馈放在同一个工作台里，知道哪里要改，也能验证改完是否真的更好。</p>
      </div>
      <div class="resume-hero-stats">
        <div><span>已分析版本</span><strong>{{ reports.length }}</strong><small>{{ reports.length ? '持续迭代中' : '等待第一份基线' }}</small></div>
        <div><span>历史最高分</span><strong>{{ bestScore }}</strong><small>{{ reports.length ? '从报告中取最高值' : '完成分析后生成' }}</small></div>
        <div><span>当前平均分</span><strong>{{ averageScore }}</strong><small>{{ reports.length ? '全部评分版本' : '暂时没有数据' }}</small></div>
      </div>
    </header>

    <div class="resume-layout">
      <aside ref="briefCard" class="resume-sidebar">
        <section class="brief-card">
          <header class="card-heading"><div><span class="section-kicker">NEW DIAGNOSIS</span><h2>建立一次岗位基线</h2></div><button v-if="draftReady" type="button" class="clear-draft" @click="resetDraft">清空<el-icon><Close /></el-icon></button></header>
          <p class="card-intro">岗位越具体，AI 越能识别真正影响匹配度的关键词和证据。</p>
          <div class="brief-step"><span>01</span><div><strong>目标岗位</strong><small>告诉 AI 你要争取什么机会</small></div></div>
          <el-input v-model="targetPosition" maxlength="100" placeholder="例如：Java 后端开发" class="target-input" />
          <div class="context-toggle-row">
            <div class="brief-step"><span>02</span><div><strong>岗位 JD <em>可选</em></strong><small>补充要求，反馈会更贴近实际</small></div></div>
            <button class="context-toggle" type="button" @click="contextExpanded = !contextExpanded">{{ contextExpanded ? '收起' : '添加 JD' }}<el-icon><component :is="contextExpanded ? 'ArrowUp' : 'ArrowDown'" /></el-icon></button>
          </div>
          <el-input v-if="contextExpanded" v-model="jobDescription" type="textarea" :rows="4" maxlength="6000" show-word-limit resize="vertical" placeholder="粘贴岗位 JD，分析关键词覆盖和能力缺口" class="jd-input" />
          <div class="brief-step"><span>03</span><div><strong>最新简历</strong><small>上传 PDF，保留每次分析版本</small></div></div>
          <el-upload ref="uploadRef" drag :auto-upload="false" :on-change="handleFileChange" :on-exceed="handleFileExceed" :limit="1" accept=".pdf" :disabled="analyzing">
            <div class="drop-content"><span class="drop-icon"><el-icon><UploadFilled /></el-icon></span><strong>{{ selectedFile ? selectedFile.name : '选择或拖入简历' }}</strong><small>{{ selectedFile ? formatSize(selectedFile.size) : 'PDF 格式 · 最大 10MB' }}</small></div>
          </el-upload>
          <div class="brief-readiness" :class="{ ready: selectedFile }"><span class="readiness-dot"><el-icon><CircleCheck v-if="selectedFile" /><UploadFilled v-else /></el-icon></span><div><strong>{{ selectedFile ? '材料已准备好' : '还差一份 PDF 简历' }}</strong><small>{{ selectedFile ? '可以开始生成岗位诊断' : '上传后即可开始分析，不会覆盖历史版本' }}</small></div></div>
          <el-button type="primary" :loading="analyzing" :disabled="!selectedFile" class="analyze-button" @click="startAnalyze"><el-icon v-if="!analyzing"><MagicStick /></el-icon>{{ analyzing ? '正在生成岗位诊断...' : '开始 AI 诊断' }}<el-icon v-if="!analyzing"><ArrowRight /></el-icon></el-button>
          <p class="privacy-note"><el-icon><CircleCheck /></el-icon>每个账号独立保存自己的简历版本</p>
        </section>

        <section class="history-card compact-history">
          <header class="card-heading"><div><span class="section-kicker">VERSION HISTORY</span><h2>历史版本</h2></div><strong class="history-count">{{ reports.length }}</strong></header>
          <div class="history-teaser" :class="{ empty: !reports.length }"><span class="history-teaser-icon"><el-icon><Clock /></el-icon></span><div><strong>{{ reports.length ? '历史报告已保存' : '还没有历史报告' }}</strong><small>{{ reports.length ? `最高 ${bestScore} 分 · 共 ${reports.length} 个版本` : '完成一次分析后，版本会自动归档' }}</small></div></div>
          <button class="history-open" type="button" @click="historyVisible = true"><span>查看历史版本</span><small>{{ reports.length }} 份</small><el-icon><ArrowRight /></el-icon></button>
        </section>
      </aside>

      <section class="report-panel" v-loading="reportLoading" element-loading-text="正在载入报告...">
        <div v-if="analyzing" class="analysis-progress">
          <div class="progress-orb"><el-icon><Loading /></el-icon></div><span class="progress-kicker">AI RESUME REVIEW</span><h2>正在生成你的岗位诊断</h2><p>这通常需要一点时间，系统正在把简历内容与岗位要求逐项对照。</p><div class="analysis-steps"><span class="active"><i></i><b>解析文档结构</b><small>识别经历、项目和技能</small></span><span><i></i><b>匹配岗位关键词</b><small>寻找缺口和可复用证据</small></span><span><i></i><b>生成优化建议</b><small>整理成可执行的修改动作</small></span></div><el-skeleton :rows="9" animated />
        </div>
        <div v-else class="report-empty"><span class="empty-icon"><el-icon><MagicStick /></el-icon></span><span class="section-kicker">NEW REPORT SPACE</span><h2>{{ reports.length ? '准备下一次简历诊断' : '先建立你的第一份基线' }}</h2><p>{{ reports.length ? '当前工作区只服务于下一次分析。历史版本不会自动铺开，点击历史版本后会在右侧浮层中查看。' : '完成一次分析后，评分、亮点和优化建议会在报告浮层中展示，历史版本也会自动归档。' }}</p><div class="empty-actions"><el-button type="primary" :icon="UploadFilled" @click="scrollToBrief">{{ reports.length ? '开始新诊断' : '上传第一份简历' }}</el-button><el-button v-if="reports.length" text @click="historyVisible = true">查看历史版本 <span class="action-count">{{ reports.length }}</span><el-icon><ArrowRight /></el-icon></el-button></div></div>
      </section>
    </div>

    <ActionConfirmDialog v-model="deleteConfirmVisible" title="删除这份分析报告吗？" description="删除后无法恢复，这个简历版本及其岗位诊断内容都会被移除。" kicker="DELETE REPORT / 删除报告" confirm-text="删除报告" loading-text="正在删除" tone="danger" :icon="Delete" :confirm-icon="Delete" :loading="deleteLoading" @confirm="confirmDeleteReport" />

    <el-drawer v-model="historyVisible" title="历史简历分析" direction="rtl" size="min(430px, 92vw)" class="resume-history-drawer" append-to-body>
      <div class="history-drawer-content">
        <div class="drawer-intro"><span class="section-kicker">VERSION ARCHIVE</span><h2>选择一个版本查看</h2><p>历史报告不会占用当前工作区，点击后会在独立浮层中打开。</p></div>
        <div v-if="reports.length" class="history-summary"><div><strong>{{ bestScore }}</strong><span>最高评分</span></div><div><strong>{{ averageScore }}</strong><span>平均评分</span></div><div><strong>{{ reports.length }}</strong><span>分析次数</span></div></div>
        <el-input v-if="reports.length" v-model="historyKeyword" clearable :prefix-icon="Search" placeholder="搜索文件名或岗位" class="history-search" />
        <div v-loading="historyLoading" class="drawer-report-list">
          <button v-for="item in filteredReports" :key="item.id" :class="{ active: activeReport?.id === item.id }" type="button" @click="openReport(item)"><span class="file-icon"><el-icon><Document /></el-icon></span><span class="file-meta"><strong>{{ item.fileName || '未命名简历' }}</strong><small>{{ item.targetPosition || item.matchPosition || '通用岗位' }} · {{ formatDate(item.createdAt) }}</small></span><span :class="['mini-score', scoreTone(item.aiScore)]">{{ item.aiScore ?? '--' }}</span><el-icon class="list-arrow"><ArrowRight /></el-icon></button>
          <div v-if="!historyLoading && !filteredReports.length" class="history-empty"><el-icon><Document /></el-icon><span>{{ historyKeyword ? '没有找到匹配版本' : '暂无分析记录' }}</span><button v-if="historyKeyword" type="button" @click="historyKeyword = ''">清除搜索</button></div>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="reportVisible" direction="rtl" size="min(900px, 94vw)" class="resume-report-drawer" :with-header="false" append-to-body destroy-on-close>
      <div v-if="activeReport" class="report-drawer-shell" v-loading="reportLoading" element-loading-text="正在载入报告...">
        <header class="report-head">
          <div class="score-block" :class="scoreTone(activeReport.aiScore)"><strong>{{ activeReport.aiScore ?? '--' }}</strong><span>综合评分</span></div>
          <div class="report-title"><span>{{ formatDate(activeReport.createdAt, true) }} · 分析已完成</span><h2>{{ activeReport.fileName || '未命名简历' }}</h2><p>{{ activeReport.targetPosition || activeReport.matchPosition || '通用岗位分析' }}</p></div>
          <div class="report-actions"><el-dropdown trigger="click"><el-button text circle aria-label="报告操作"><el-icon><MoreFilled /></el-icon></el-button><template #dropdown><el-dropdown-menu><el-dropdown-item @click="downloadReport">下载 Markdown</el-dropdown-item><el-dropdown-item divided class="danger-item" @click="deleteReport">删除报告</el-dropdown-item></el-dropdown-menu></template></el-dropdown><el-button text circle aria-label="关闭报告" @click="reportVisible = false"><el-icon><Close /></el-icon></el-button></div>
        </header>
        <section class="report-context"><div><span class="context-label">当前判断</span><strong>{{ reportVerdict }}</strong><small>{{ reportVerdictHint }}</small></div><div><span class="context-label">下一步</span><strong>{{ reportNextAction }}</strong><small>结合报告逐项修改，再上传新版本验证</small></div><div class="context-score"><span class="context-label">版本评分</span><strong>{{ activeReport.aiScore ?? '--' }}<small>/100</small></strong></div></section>
        <div class="report-toolbar"><span><el-icon><CircleCheck /></el-icon>分析已完成</span><small>建议从“下一步”开始逐条修改，而不是一次改完所有内容</small><el-button text :icon="Download" @click="downloadReport">下载报告</el-button><el-button type="primary" plain :icon="ChatDotRound" @click="startInterviewFromReport">带着岗位去面试</el-button></div>
        <article v-if="renderedReport" class="report-body" v-html="renderedReport"></article>
        <div v-else class="report-no-content"><el-icon><Document /></el-icon><strong>这份报告暂时没有正文</strong><span>可以重新选择历史版本，或上传新版本进行分析。</span></div>
      </div>
    </el-drawer>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, ArrowRight, ArrowUp, ChatDotRound, CircleCheck, Clock, Close, Delete, Document, Download, Loading, MagicStick, MoreFilled, Search, UploadFilled } from '@element-plus/icons-vue'
import { analyzeResume, getErrorMessage, getResumeDetail, getResumeHistory } from '../api'
import api from '../api'
import { renderMarkdown } from '../utils/markdown'
import { ElMessage } from 'element-plus'
import ActionConfirmDialog from '../components/ActionConfirmDialog.vue'

const uploadRef = ref(null)
const briefCard = ref(null)
const router = useRouter()
const selectedFile = ref(null)
const targetPosition = ref('')
const jobDescription = ref('')
const contextExpanded = ref(false)
const historyKeyword = ref('')
const analyzing = ref(false)
const historyLoading = ref(false)
const reportLoading = ref(false)
const historyVisible = ref(false)
const reportVisible = ref(false)
const deleteConfirmVisible = ref(false)
const deleteLoading = ref(false)
const reports = ref([])
const activeReport = ref(null)
const MAX_FILE_SIZE = 10 * 1024 * 1024
const renderedReport = computed(() => renderMarkdown(activeReport.value?.aiFeedback || ''))
const bestScore = computed(() => reports.value.length ? Math.max(...reports.value.map(item => Number(item.aiScore) || 0)) : '--')
const averageScore = computed(() => {
  const scores = reports.value.map(item => Number(item.aiScore)).filter(score => Number.isFinite(score))
  return scores.length ? Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length) : '--'
})
const filteredReports = computed(() => {
  const query = historyKeyword.value.trim().toLowerCase()
  return reports.value.filter(item => !query || (item.fileName || '').toLowerCase().includes(query) || (item.targetPosition || item.matchPosition || '').toLowerCase().includes(query))
})
const draftReady = computed(() => Boolean(selectedFile.value || targetPosition.value.trim() || jobDescription.value.trim()))
const reportVerdict = computed(() => {
  const score = Number(activeReport.value?.aiScore)
  return !Number.isFinite(score) ? '等待评分信息' : score >= 80 ? '岗位匹配基础不错' : score >= 60 ? '有基础，优先补齐关键证据' : '先处理结构与岗位关键词'
})
const reportVerdictHint = computed(() => {
  const score = Number(activeReport.value?.aiScore)
  return !Number.isFinite(score) ? '继续完善目标岗位上下文' : score >= 80 ? '接下来重点打磨表达和项目结果' : '先完成报告中标记的高优先级修改'
})
const reportNextAction = computed(() => {
  const score = Number(activeReport.value?.aiScore)
  return !Number.isFinite(score) || score < 60 ? '补齐岗位关键词' : score < 80 ? '强化项目结果' : '验证下一版提升'
})
const scoreTone = score => Number(score) >= 80 ? 'high' : Number(score) >= 60 ? 'mid' : 'low'

onMounted(loadHistory)

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await getResumeHistory()
    if (res.data.code === 200) {
      reports.value = res.data.data || []
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '加载历史报告失败'))
  } finally {
    historyLoading.value = false
  }
}

function handleFileChange(file) {
  const raw = file.raw
  if (!raw || (raw.type !== 'application/pdf' && !raw.name.toLowerCase().endsWith('.pdf'))) return rejectFile('仅支持 PDF 格式简历')
  if (raw.size > MAX_FILE_SIZE) return rejectFile('简历文件不能超过 10MB')
  selectedFile.value = raw
}

function rejectFile(message) {
  selectedFile.value = null
  uploadRef.value?.clearFiles()
  ElMessage.error(message)
}

const handleFileExceed = () => ElMessage.warning('一次只能选择一份简历')

async function startAnalyze() {
  if (!selectedFile.value) return
  analyzing.value = true
  try {
    const res = await analyzeResume(selectedFile.value, { targetPosition: targetPosition.value.trim(), jobDescription: jobDescription.value.trim() })
    if (res.data.code !== 200) throw new Error(res.data.message || '分析失败')
    activeReport.value = res.data.data
    reports.value = [res.data.data, ...reports.value.filter(item => item.id !== res.data.data.id)]
    selectedFile.value = null
    uploadRef.value?.clearFiles()
    reportVisible.value = true
    ElMessage.success('分析完成，已生成新的简历版本')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '分析失败，请稍后重试'))
  } finally {
    analyzing.value = false
  }
}

async function selectReport(item) {
  const cached = reports.value.find(report => report.id === item.id)
  if (cached?.aiFeedback) {
    activeReport.value = cached
    return
  }
  reportLoading.value = true
  try {
    const res = await getResumeDetail(item.id)
    if (res.data.code !== 200) throw new Error(res.data.message)
    reports.value = reports.value.map(report => report.id === item.id ? res.data.data : report)
    activeReport.value = res.data.data
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '报告详情加载失败'))
  } finally {
    reportLoading.value = false
  }
}

async function openReport(item) {
  await selectReport(item)
  if (activeReport.value?.id === item.id) {
    historyVisible.value = false
    reportVisible.value = true
  }
}

async function deleteReport() {
  if (!activeReport.value) return
  deleteConfirmVisible.value = true
}

async function confirmDeleteReport() {
  if (!activeReport.value) return
  deleteLoading.value = true
  try {
    const res = await api.delete('/resume/' + activeReport.value.id)
    if (res.data.code !== 200) throw new Error(res.data.message)
    reports.value = reports.value.filter(item => item.id !== activeReport.value.id)
    activeReport.value = null
    reportVisible.value = false
    ElMessage.success('报告已删除')
    deleteConfirmVisible.value = false
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '删除失败'))
  } finally {
    deleteLoading.value = false
  }
}

function downloadReport() {
  if (!activeReport.value) return
  const content = '# ' + (activeReport.value.fileName || '简历') + ' - 简历分析报告\n\n' + (activeReport.value.aiFeedback || '')
  const url = URL.createObjectURL(new Blob([content], { type: 'text/markdown;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = (activeReport.value.fileName || '简历').replace(/\.pdf$/i, '') + '_分析报告.md'
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('报告已下载')
}

function startInterviewFromReport() {
  const targetPosition = activeReport.value?.targetPosition || activeReport.value?.matchPosition || ''
  reportVisible.value = false
  router.push({ path: '/interview', query: targetPosition ? { position: targetPosition } : {} })
}

function resetDraft() {
  selectedFile.value = null
  targetPosition.value = ''
  jobDescription.value = ''
  contextExpanded.value = false
  uploadRef.value?.clearFiles()
}

function scrollToBrief() {
  briefCard.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const formatSize = bytes => bytes < 1024 * 1024 ? (bytes / 1024).toFixed(1) + ' KB' : (bytes / 1024 / 1024).toFixed(1) + ' MB'
const formatDate = (value, withTime = false) => value ? new Date(value).toLocaleString('zh-CN', withTime ? { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' } : { month: 'short', day: 'numeric' }) : ''
</script>

<style scoped>
.resume-workspace { max-width: 1220px; margin: 0 auto; padding-bottom: 24px; animation: enter .35s ease; color: var(--text); }
@keyframes enter { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.resume-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 25px; margin-bottom: 19px; padding: 27px 30px; border: 1px solid var(--border); border-radius: 10px; background: linear-gradient(120deg, var(--bg-card), color-mix(in srgb, var(--blue-light) 48%, var(--bg-card))); box-shadow: 0 15px 34px rgba(23,32,42,.05); }.eyebrow,.section-kicker { color: var(--blue); font: 800 10px ui-monospace, SFMono-Regular, Menlo, monospace; letter-spacing: .08em; }.eyebrow { display: flex; align-items: center; gap: 8px; }.eyebrow i { width: 7px; height: 7px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent) 14%, transparent); }.resume-hero h1 { margin: 11px 0 8px; font-size: clamp(27px, 3.2vw, 39px); letter-spacing: -.05em; line-height: 1.16; }.resume-hero p { max-width: 660px; color: var(--text-secondary); font-size: 12px; line-height: 1.7; }.resume-hero-stats { display: flex; flex: 0 0 380px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-card); overflow: hidden; }.resume-hero-stats > div { min-width: 0; flex: 1; padding: 13px 14px; border-right: 1px solid var(--border); }.resume-hero-stats > div:last-child { border: 0; }.resume-hero-stats span,.resume-hero-stats strong,.resume-hero-stats small { display: block; }.resume-hero-stats span { color: var(--text-muted); font-size: 9px; }.resume-hero-stats strong { margin-top: 5px; color: var(--blue); font: 700 22px ui-monospace, monospace; }.resume-hero-stats small { margin-top: 4px; overflow: hidden; color: var(--text-muted); text-overflow: ellipsis; white-space: nowrap; font-size: 8px; }
.resume-layout { display: grid; grid-template-columns: 354px minmax(0, 1fr); gap: 17px; align-items: start; }.resume-sidebar { display: grid; gap: 14px; }.brief-card,.history-card,.report-panel { border: 1px solid var(--border); border-radius: 9px; background: var(--surface-glass); box-shadow: 0 16px 38px rgba(23,32,42,.055); backdrop-filter: blur(16px); }.brief-card { padding: 19px; }.card-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }.card-heading h2 { margin-top: 7px; font-size: 16px; letter-spacing: -.025em; }.clear-draft { display: flex; align-items: center; gap: 3px; border: 0; background: transparent; color: var(--text-muted); cursor: pointer; font: inherit; font-size: 10px; }.clear-draft:hover { color: var(--accent); }.card-intro { margin: 10px 0 16px; color: var(--text-secondary); font-size: 11px; line-height: 1.65; }.brief-step { display: flex; align-items: center; gap: 9px; margin: 13px 0 8px; }.brief-step > span { width: 24px; height: 24px; display: grid; flex: 0 0 24px; place-items: center; border-radius: 6px; background: var(--blue-light); color: var(--blue); font: 800 9px ui-monospace,monospace; }.brief-step strong,.brief-step small { display: block; }.brief-step strong { font-size: 11px; }.brief-step em { margin-left: 4px; color: var(--text-muted); font-size: 8px; font-style: normal; font-weight: 400; }.brief-step small { margin-top: 3px; color: var(--text-muted); font-size: 9px; }.target-input,.jd-input { margin-bottom: 3px; }.brief-card :deep(.el-upload),.brief-card :deep(.el-upload-dragger) { width: 100%; }.brief-card :deep(.el-upload-dragger) { padding: 0; border-radius: 7px; border-color: var(--border); background: linear-gradient(180deg,var(--bg-card),var(--bg-soft)); }.drop-content { min-height: 128px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 7px; padding: 16px; }.drop-icon { width: 38px; height: 38px; display: grid; place-items: center; margin-bottom: 2px; border-radius: 9px; background: var(--blue-light); color: var(--blue); font-size: 21px; }.drop-content strong { max-width: 100%; overflow: hidden; color: var(--text); text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }.drop-content small { color: var(--text-muted); font-size: 10px; }.analyze-button { width: 100%; height: 42px; margin-top: 11px; font-weight: 700; }.privacy-note { display: flex; align-items: center; justify-content: center; gap: 4px; margin-top: 11px; color: var(--text-muted); font-size: 9px; }.privacy-note .el-icon { color: var(--success); }
.history-card { padding: 17px 15px 12px; }.history-count { min-width: 24px; height: 24px; display: grid; place-items: center; border-radius: 50%; background: var(--blue-light); color: var(--blue); font: 700 11px ui-monospace,monospace; }.history-summary { display: grid; grid-template-columns: repeat(3,1fr); margin: 14px 0 10px; border: 1px solid var(--border); border-radius: 7px; overflow: hidden; background: var(--hover); }.history-summary > div { padding: 10px 8px; border-right: 1px solid var(--border); }.history-summary > div:last-child { border: 0; }.history-summary strong,.history-summary span { display: block; }.history-summary strong { color: var(--blue); font: 700 18px ui-monospace,monospace; }.history-summary span { margin-top: 4px; color: var(--text-muted); font-size: 9px; }.history-search { margin-bottom: 8px; }.report-list { min-height: 88px; }.report-list button { width: 100%; display: flex; align-items: center; gap: 8px; padding: 10px 7px; border: 1px solid transparent; border-radius: 6px; background: transparent; color: var(--text); cursor: pointer; text-align: left; }.report-list button:hover,.report-list button.active { border-color: var(--border); background: var(--hover); }.report-list button.active { box-shadow: inset 3px 0 var(--blue); background: var(--blue-light); }.file-icon { width: 29px; height: 29px; display: grid; flex: 0 0 29px; place-items: center; border-radius: 6px; background: var(--blue-light); color: var(--blue); }.file-meta { min-width: 0; flex: 1; }.file-meta strong,.file-meta small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.file-meta strong { font-size: 11px; }.file-meta small { margin-top: 3px; color: var(--text-muted); font-size: 9px; }.mini-score { min-width: 25px; font: 700 13px ui-monospace,monospace; text-align: right; }.mini-score.high { color: var(--success); }.mini-score.mid { color: #b87813; }.mini-score.low { color: var(--accent); }.list-arrow { color: var(--text-muted); font-size: 12px; }.history-empty { min-height: 95px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; color: var(--text-muted); font-size: 11px; }.history-empty .el-icon { font-size: 22px; }.history-empty button { border: 0; background: transparent; color: var(--blue); cursor: pointer; font: inherit; font-size: 10px; }
.report-panel { min-height: 700px; overflow: hidden; }.analysis-progress { min-height: 700px; padding: 46px 44px; }.progress-orb { width: 52px; height: 52px; display: grid; place-items: center; border-radius: 50%; background: var(--blue-light); color: var(--blue); font-size: 25px; }.progress-orb .el-icon { animation: spin 1s linear infinite; }@keyframes spin { to { transform: rotate(360deg); } }.progress-kicker { display: block; margin-top: 17px; color: var(--blue); font: 800 10px ui-monospace,monospace; letter-spacing: .08em; }.analysis-progress h2 { margin: 8px 0 7px; font-size: 21px; }.analysis-progress > p { color: var(--text-muted); font-size: 12px; }.analysis-steps { display: grid; grid-template-columns: repeat(3,1fr); gap: 9px; margin: 24px 0 30px; }.analysis-steps span { display: flex; flex-direction: column; gap: 5px; padding: 12px; border: 1px solid var(--border); border-radius: 7px; color: var(--text-muted); }.analysis-steps i { width: 7px; height: 7px; border-radius: 50%; background: var(--border); }.analysis-steps b { font-size: 10px; }.analysis-steps small { font-size: 9px; line-height: 1.4; }.analysis-steps span.active { border-color: color-mix(in srgb,var(--blue) 24%,var(--border)); background: var(--blue-light); color: var(--blue); }.analysis-steps span.active i { background: var(--blue); box-shadow: 0 0 0 4px color-mix(in srgb,var(--blue) 12%,transparent); }.report-head { display: grid; grid-template-columns: 82px minmax(0,1fr) 36px; gap: 18px; align-items: center; padding: 24px 28px; border-bottom: 1px solid var(--border); background: linear-gradient(180deg,var(--bg-card),transparent); }.score-block { width: 76px; height: 76px; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 9px; background: var(--blue-light); color: var(--blue); }.score-block.high { background: #e8f6ee; color: var(--success); }.score-block.mid { background: #fff5df; color: #b87813; }.score-block.low { background: #fff0ec; color: var(--accent); }.score-block strong { font: 700 28px ui-monospace,monospace; line-height: 1; }.score-block span { margin-top: 5px; font-size: 9px; }.report-title { min-width: 0; }.report-title > span { color: var(--text-muted); font-size: 10px; }.report-title h2 { overflow: hidden; margin: 5px 0; text-overflow: ellipsis; white-space: nowrap; font-size: 20px; }.report-title p { color: var(--text-secondary); font-size: 11px; }.report-context { display: grid; grid-template-columns: 1fr 1fr 130px; gap: 0; border-bottom: 1px solid var(--border); background: var(--bg-card); }.report-context > div { min-height: 95px; padding: 16px 20px; border-right: 1px solid var(--border); }.report-context > div:last-child { border: 0; }.context-label,.report-context strong,.report-context small { display: block; }.context-label { color: var(--text-muted); font: 800 9px ui-monospace,monospace; letter-spacing: .06em; }.report-context strong { margin-top: 8px; font-size: 13px; }.report-context small { margin-top: 4px; color: var(--text-secondary); font-size: 9px; line-height: 1.5; }.context-score strong { color: var(--blue); font: 700 25px ui-monospace,monospace; }.context-score strong small { display: inline; margin-left: 3px; color: var(--text-muted); font: 400 10px inherit; }.report-toolbar { display: flex; align-items: center; gap: 10px; padding: 11px 28px; border-bottom: 1px solid var(--border); background: var(--hover); }.report-toolbar span { display: flex; align-items: center; gap: 5px; color: var(--success); font-size: 11px; font-weight: 700; }.report-toolbar span .el-icon { font-size: 14px; }.report-toolbar small { flex: 1; color: var(--text-muted); font-size: 10px; }.report-toolbar :deep(.el-button) { font-size: 10px; }.report-body { max-width: 850px; padding: 24px 36px 48px; color: var(--text); font-size: 13px; line-height: 1.9; }.report-body :deep(h2),.report-body :deep(h3) { margin: 23px 0 10px; padding-bottom: 7px; border-bottom: 1px solid var(--border); }.report-body :deep(h2) { font-size: 18px; }.report-body :deep(h3) { font-size: 15px; }.report-body :deep(ul),.report-body :deep(ol) { padding-left: 22px; }.report-body :deep(li) { margin: 6px 0; }.report-body :deep(code) { padding: 2px 5px; border-radius: 4px; background: var(--hover); }.report-empty { min-height: 700px; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 30px; text-align: center; }.empty-icon { width: 58px; height: 58px; display: grid; place-items: center; margin-bottom: 17px; border-radius: 50%; background: var(--blue-light); color: var(--blue); font-size: 27px; }.report-empty h2 { margin: 9px 0 7px; font-size: 20px; }.report-empty p { max-width: 360px; margin-bottom: 18px; color: var(--text-muted); font-size: 11px; line-height: 1.7; }
:global(.danger-item) { color: #f56c6c !important; }
@media (max-width: 1000px) { .resume-hero { align-items: flex-start; flex-direction: column; }.resume-hero-stats { width: 100%; flex-basis: auto; }.resume-layout { grid-template-columns: 1fr; }.resume-sidebar { display: grid; grid-template-columns: minmax(0,1fr) minmax(0,1fr); align-items: start; }.brief-card,.history-card { grid-column: auto; grid-row: auto; }.report-panel { min-height: 560px; }.analysis-progress,.report-empty { min-height: 560px; } }
@media (max-width: 820px) { .resume-sidebar { display: flex; flex-direction: column; }.brief-card,.history-card { grid-column: auto; grid-row: auto; }.report-panel { min-height: 520px; }.analysis-progress,.report-empty { min-height: 520px; } }
@media (max-width: 600px) { .resume-hero { padding: 21px 16px; }.resume-hero h1 { font-size: 27px; }.resume-hero-stats { display: grid; grid-template-columns: repeat(3,1fr); }.resume-hero-stats > div { padding: 10px 8px; }.resume-hero-stats strong { font-size: 18px; }.resume-hero-stats small { display: none; }.resume-layout { gap: 12px; }.resume-sidebar { display: flex; flex-direction: column; }.brief-card,.history-card { padding: 15px; }.history-card { order: 2; }.report-head { grid-template-columns: 61px minmax(0,1fr) 29px; gap: 10px; padding: 16px 14px; }.score-block { width: 60px; height: 60px; }.score-block strong { font-size: 22px; }.report-title h2 { font-size: 16px; }.report-context { grid-template-columns: 1fr 1fr; }.report-context > div { min-height: 88px; padding: 13px 14px; }.context-score { grid-column: 1 / -1; border-top: 1px solid var(--border); }.report-toolbar { align-items: flex-start; flex-wrap: wrap; padding: 10px 14px; }.report-toolbar small { min-width: calc(100% - 25px); order: 3; }.report-toolbar :deep(.el-button) { margin-left: auto; }.report-body { padding: 16px 15px 30px; font-size: 13px; }.analysis-progress { min-height: 520px; padding: 30px 20px; }.analysis-steps { grid-template-columns: 1fr; gap: 7px; margin: 18px 0 24px; }.analysis-steps span { display: grid; grid-template-columns: 9px 1fr; align-items: center; padding: 9px; }.analysis-steps small { grid-column: 2; }.report-empty { min-height: 430px; padding: 24px 16px; } }

/* 产品级阅读与输入层：让首屏只突出一个主任务，报告正文保持稳定的阅读节奏。 */
.resume-layout { grid-template-columns: minmax(300px, 330px) minmax(0, 1fr); gap: 20px; }
.brief-card { padding: 22px; }
.context-toggle-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.context-toggle-row .brief-step { min-width: 0; flex: 1; }
.context-toggle { display: inline-flex; align-items: center; gap: 3px; flex: 0 0 auto; padding: 5px 7px; border: 1px solid var(--border); border-radius: 5px; background: var(--bg-card); color: var(--blue); cursor: pointer; font-size: 10px; }
.context-toggle:hover { border-color: var(--blue); background: var(--blue-light); }
.brief-readiness { display: flex; align-items: center; gap: 9px; margin-top: 12px; padding: 10px 11px; border: 1px solid var(--border); border-radius: 7px; background: var(--hover); }
.brief-readiness.ready { border-color: color-mix(in srgb, var(--success) 32%, var(--border)); background: color-mix(in srgb, var(--success) 7%, var(--bg-card)); }
.readiness-dot { width: 25px; height: 25px; display: grid; flex: 0 0 25px; place-items: center; border-radius: 50%; background: var(--bg-card); color: var(--text-muted); font-size: 13px; }
.brief-readiness.ready .readiness-dot { color: var(--success); }
.brief-readiness strong, .brief-readiness small { display: block; }
.brief-readiness strong { font-size: 10px; }
.brief-readiness small { margin-top: 3px; color: var(--text-muted); font-size: 9px; line-height: 1.4; }
.report-panel { min-height: 620px; background: var(--bg-card); }
.report-head { position: sticky; top: 0; z-index: 3; background: color-mix(in srgb, var(--bg-card) 93%, transparent); backdrop-filter: blur(14px); }
.report-body { max-width: 880px; margin: 0 auto; padding: 30px 44px 58px; }
.report-body :deep(p) { margin: 0 0 13px; }
.report-body :deep(h2:first-child), .report-body :deep(h3:first-child) { margin-top: 0; }
.report-body :deep(strong) { color: var(--text); font-weight: 750; }
.report-body :deep(blockquote) { margin: 15px 0; padding: 12px 16px; border-left: 3px solid var(--blue); border-radius: 0 6px 6px 0; background: var(--blue-light); color: var(--text-secondary); }
.report-body :deep(table) { display: block; width: 100%; margin: 17px 0; overflow-x: auto; border-collapse: collapse; font-size: 12px; }
.report-body :deep(th), .report-body :deep(td) { min-width: 120px; padding: 10px 12px; border: 1px solid var(--border); text-align: left; vertical-align: top; }
.report-body :deep(th) { background: var(--hover); color: var(--text); font-weight: 700; }
.report-body :deep(tr:nth-child(even) td) { background: color-mix(in srgb, var(--hover) 42%, transparent); }
.report-body :deep(hr) { margin: 25px 0; border: 0; border-top: 1px solid var(--border); }
.report-body :deep(a) { color: var(--blue); }
.report-body :deep(pre) { margin: 15px 0; padding: 14px; overflow-x: auto; border-radius: 7px; background: var(--hover); font-size: 12px; line-height: 1.6; }
.report-no-content { min-height: 280px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; padding: 30px; color: var(--text-muted); text-align: center; }
.report-no-content .el-icon { font-size: 30px; color: var(--blue); }
.report-no-content strong { color: var(--text); font-size: 15px; }
.report-no-content span { font-size: 11px; }
@media (max-width: 1000px) {
  .resume-layout { grid-template-columns: 1fr; }
  .report-panel, .analysis-progress, .report-empty { min-height: 560px; }
}
@media (max-width: 600px) {
  .brief-card { padding: 17px 15px; }
  .report-panel, .analysis-progress { min-height: 500px; }
  .report-head { position: static; }
  .report-body { padding: 22px 15px 36px; }
  .report-no-content { min-height: 220px; }
}
.compact-history { padding: 18px; }
.history-teaser { display: flex; align-items: center; gap: 10px; margin: 16px 0 14px; padding: 12px; border: 1px solid var(--border); border-radius: 8px; background: var(--hover); }
.history-teaser.empty { background: color-mix(in srgb, var(--blue-light) 38%, var(--bg-card)); }
.history-teaser-icon { width: 32px; height: 32px; display: grid; flex: 0 0 32px; place-items: center; border-radius: 8px; background: var(--blue-light); color: var(--blue); font-size: 16px; }
.history-teaser strong, .history-teaser small { display: block; }
.history-teaser strong { font-size: 11px; }
.history-teaser small { margin-top: 4px; color: var(--text-muted); font-size: 9px; }
.history-open { width: 100%; display: flex; align-items: center; gap: 7px; padding: 10px 11px; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-card); color: var(--text); cursor: pointer; text-align: left; }
.history-open:hover { border-color: var(--blue); background: var(--blue-light); }
.history-open span { flex: 1; font-size: 11px; font-weight: 700; }
.history-open small { color: var(--text-muted); font-size: 10px; }
.history-open .el-icon { color: var(--blue); }
.empty-actions { display: flex; align-items: center; justify-content: center; gap: 12px; flex-wrap: wrap; }
.empty-actions .el-button + .el-button { margin-left: 0; }
.empty-actions .el-button--primary { min-width: 150px; }
.action-count { display: inline-grid; min-width: 18px; height: 18px; margin-left: 3px; place-items: center; border-radius: 9px; background: var(--blue-light); color: var(--blue); font-size: 9px; }
.report-actions { display: flex; align-items: center; justify-content: flex-end; gap: 2px; }
.report-drawer-shell { min-height: 100%; background: var(--bg-card); color: var(--text); }
.report-drawer-shell .report-head { grid-template-columns: 76px minmax(0, 1fr) auto; }
.history-drawer-content { min-height: 100%; padding: 4px 2px 24px; color: var(--text); }
.drawer-intro { padding: 4px 0 16px; }
.drawer-intro h2 { margin: 8px 0 6px; font-size: 20px; }
.drawer-intro p { color: var(--text-muted); font-size: 11px; line-height: 1.6; }
.drawer-report-list { min-height: 120px; }
.drawer-report-list button { width: 100%; display: flex; align-items: center; gap: 9px; padding: 12px 9px; border: 1px solid transparent; border-radius: 7px; background: transparent; color: var(--text); cursor: pointer; text-align: left; }
.drawer-report-list button:hover, .drawer-report-list button.active { border-color: var(--border); background: var(--hover); }
.drawer-report-list button.active { box-shadow: inset 3px 0 var(--blue); background: var(--blue-light); }
:global(.resume-history-drawer .el-drawer__header) { margin-bottom: 0; padding: 22px 24px 16px; border-bottom: 1px solid var(--border); }
:global(.resume-history-drawer .el-drawer__title) { color: var(--text); font-size: 16px; font-weight: 750; }
:global(.resume-history-drawer .el-drawer__body) { padding: 18px 24px 0; background: var(--bg-card); }
:global(.resume-report-drawer .el-drawer__body) { padding: 0; background: var(--bg-card); }
@media (max-width: 600px) {
  .history-drawer-content { padding-top: 0; }
  :global(.resume-history-drawer .el-drawer__header) { padding: 18px 16px 14px; }
  :global(.resume-history-drawer .el-drawer__body) { padding: 16px 16px 0; }
  .report-drawer-shell .report-head { grid-template-columns: 61px minmax(0, 1fr) auto; gap: 10px; padding: 16px 14px; }
  .report-actions .el-button { padding: 6px; }
}
/* ======== Resume studio premium layer ======== */
.resume-hero {
  border-radius: 20px;
  border-color: color-mix(in srgb, var(--blue) 16%, var(--border));
  background: linear-gradient(135deg, color-mix(in srgb, var(--blue-light) 48%, var(--bg-card)), var(--surface-glass) 58%, var(--bg-card));
  box-shadow: 0 26px 68px rgba(16, 54, 47, .08);
  backdrop-filter: blur(16px);
}
.resume-hero-stats { border-radius: 14px; background: color-mix(in srgb, var(--bg-card) 68%, transparent); }
.brief-card, .history-card, .report-panel { border-radius: 17px; border-color: color-mix(in srgb, var(--blue) 13%, var(--border)); box-shadow: 0 22px 60px rgba(16, 54, 47, .065); }
.brief-card { background: linear-gradient(150deg, var(--surface-glass), color-mix(in srgb, var(--blue-light) 16%, var(--bg-card))); }
.history-card { background: var(--surface-glass); }
.brief-card :deep(.el-upload-dragger) { border-radius: 13px; background: linear-gradient(145deg, color-mix(in srgb, var(--blue-light) 28%, var(--bg-card)), var(--bg-soft)); }
.drop-icon { border-radius: 12px; box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--blue) 12%, transparent); }
.history-teaser, .brief-readiness { border-radius: 12px; }
.report-panel { background: var(--surface-glass); backdrop-filter: blur(16px); }
.report-head { background: color-mix(in srgb, var(--bg-card) 72%, transparent); backdrop-filter: blur(18px); }
.score-block { border-radius: 14px; box-shadow: 0 12px 26px color-mix(in srgb, var(--blue) 12%, transparent); }
.report-context { background: color-mix(in srgb, var(--bg-card) 68%, transparent); }
.report-toolbar { background: color-mix(in srgb, var(--blue-light) 38%, transparent); }
.report-body :deep(blockquote) { border-radius: 0 12px 12px 0; }
.report-body :deep(pre) { border-radius: 12px; }
.report-drawer-shell { background: var(--surface-glass); backdrop-filter: blur(18px); }
.drawer-report-list button { border-radius: 10px; }
@media (max-width: 600px) {
  .resume-hero, .brief-card, .history-card, .report-panel { border-radius: 14px; }
  .resume-hero { box-shadow: 0 20px 46px rgba(16, 54, 47, .07); }
}
</style>
