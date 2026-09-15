<template>
  <div class="resume-workspace">
    <header class="page-head">
      <div><span class="eyebrow">简历工作台</span><h1>诊断和迭代你的简历</h1><p>上传 PDF 获取反馈，并集中管理每一次分析结果。</p></div>
      <el-button v-if="activeReport" @click="downloadReport"><el-icon><Download /></el-icon>下载报告</el-button>
    </header>

    <div class="workspace-grid">
      <aside class="control-panel">
        <section class="upload-section">
          <div class="section-head"><h2>新分析</h2><span>PDF · 最大 10MB</span></div>
          <el-upload ref="uploadRef" drag :auto-upload="false" :on-change="handleFileChange" :on-exceed="handleFileExceed" :limit="1" accept=".pdf" :disabled="analyzing">
            <div class="drop-content"><el-icon><UploadFilled /></el-icon><strong>{{ selectedFile ? selectedFile.name : '选择或拖入简历' }}</strong><small>{{ selectedFile ? formatSize(selectedFile.size) : '仅上传 PDF 文件' }}</small></div>
          </el-upload>
          <el-button type="primary" :loading="analyzing" :disabled="!selectedFile" class="analyze-button" @click="startAnalyze">
            <el-icon v-if="!analyzing"><MagicStick /></el-icon>{{ analyzing ? '正在分析...' : '开始分析' }}
          </el-button>
        </section>

        <section class="history-section">
          <div class="section-head"><h2>历史版本</h2><span>{{ reports.length }} 份</span></div>
          <div v-loading="historyLoading" class="report-list">
            <button v-for="item in reports" :key="item.id" :class="{ active: activeReport?.id === item.id }" type="button" @click="selectReport(item)">
              <span class="file-icon"><el-icon><Document /></el-icon></span>
              <span class="file-meta"><strong>{{ item.fileName }}</strong><small>{{ formatDate(item.createdAt) }}</small></span>
              <span class="mini-score">{{ item.aiScore ?? '--' }}</span>
            </button>
            <div v-if="!historyLoading && !reports.length" class="history-empty"><el-icon><Document /></el-icon><span>暂无分析记录</span></div>
          </div>
        </section>
      </aside>

      <main class="report-panel">
        <div v-if="analyzing" class="analysis-progress">
          <div class="progress-icon"><el-icon><Loading /></el-icon></div><h2>AI 正在分析简历</h2><p>正在检查内容结构、项目表达和岗位匹配度。</p><el-skeleton :rows="7" animated />
        </div>
        <template v-else-if="activeReport">
          <header class="report-head">
            <div class="score-block"><strong>{{ activeReport.aiScore ?? '--' }}</strong><span>综合评分</span></div>
            <div class="report-title"><span>{{ formatDate(activeReport.createdAt, true) }}</span><h2>{{ activeReport.fileName }}</h2><p v-if="activeReport.matchPosition">{{ activeReport.matchPosition }}</p></div>
            <el-dropdown trigger="click">
              <el-button text circle aria-label="报告操作"><el-icon><MoreFilled /></el-icon></el-button>
              <template #dropdown><el-dropdown-menu><el-dropdown-item @click="downloadReport">下载 Markdown</el-dropdown-item><el-dropdown-item divided class="danger-item" @click="deleteReport">删除报告</el-dropdown-item></el-dropdown-menu></template>
            </el-dropdown>
          </header>
          <article class="report-body" v-html="renderedReport"></article>
        </template>
        <div v-else class="report-empty"><span class="empty-icon"><el-icon><Document /></el-icon></span><h2>选择一份简历开始</h2><p>完成分析后，评分、亮点和优化建议会展示在这里。</p></div>
      </main>
    </div>

  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { analyzeResume, getResumeHistory } from '../api'
import api from '../api'
import { renderMarkdown } from '../utils/markdown'
import { ElMessage, ElMessageBox } from 'element-plus'

const uploadRef = ref(null)
const selectedFile = ref(null)
const analyzing = ref(false)
const historyLoading = ref(false)
const reports = ref([])
const activeReport = ref(null)
const MAX_FILE_SIZE = 10 * 1024 * 1024
const renderedReport = computed(() => renderMarkdown(activeReport.value?.aiFeedback || ''))

onMounted(loadHistory)

async function loadHistory(selectFirst = true) {
  historyLoading.value = true
  try {
    const res = await getResumeHistory()
    if (res.data.code === 200) {
      reports.value = res.data.data || []
      if (selectFirst && !activeReport.value && reports.value.length) activeReport.value = reports.value[0]
    }
  } catch (err) { ElMessage.error(err.response?.data?.message || '加载历史报告失败') }
  finally { historyLoading.value = false }
}

const handleFileChange = (file) => {
  const raw = file.raw
  if (!raw || raw.type !== 'application/pdf' && !raw.name.toLowerCase().endsWith('.pdf')) return rejectFile('仅支持 PDF 格式简历')
  if (raw.size > MAX_FILE_SIZE) return rejectFile('简历文件不能超过 10MB')
  selectedFile.value = raw
}
const rejectFile = (message) => { selectedFile.value = null; uploadRef.value?.clearFiles(); ElMessage.error(message) }
const handleFileExceed = () => ElMessage.warning('一次只能选择一份简历')

const startAnalyze = async () => {
  if (!selectedFile.value) return
  analyzing.value = true
  try {
    const res = await analyzeResume(selectedFile.value)
    if (res.data.code !== 200) throw new Error(res.data.message || '分析失败')
    activeReport.value = res.data.data
    reports.value = [res.data.data, ...reports.value.filter(item => item.id !== res.data.data.id)]
    selectedFile.value = null
    uploadRef.value?.clearFiles()
    ElMessage.success('分析完成')
  } catch (err) { ElMessage.error(err.response?.data?.message || err.message || '分析失败，请稍后重试') }
  finally { analyzing.value = false }
}

const selectReport = (item) => { activeReport.value = item }
const deleteReport = async () => {
  try { await ElMessageBox.confirm('删除后无法恢复，确定删除这份分析报告吗？', '删除报告', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }) }
  catch { return }
  try {
    const res = await api.delete(`/resume/${activeReport.value.id}`)
    if (res.data.code !== 200) throw new Error(res.data.message)
    reports.value = reports.value.filter(item => item.id !== activeReport.value.id)
    activeReport.value = reports.value[0] || null
    ElMessage.success('报告已删除')
  } catch (err) { ElMessage.error(err.response?.data?.message || err.message || '删除失败') }
}
const downloadReport = () => {
  if (!activeReport.value) return
  const content = `# ${activeReport.value.fileName} - 简历分析报告\n\n${activeReport.value.aiFeedback || ''}`
  const url = URL.createObjectURL(new Blob([content], { type: 'text/markdown;charset=utf-8' }))
  const link = document.createElement('a'); link.href = url; link.download = `${activeReport.value.fileName.replace(/\.pdf$/i, '')}_分析报告.md`; link.click(); URL.revokeObjectURL(url)
}
const formatSize = bytes => bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`
const formatDate = (value, withTime = false) => value ? new Date(value).toLocaleString('zh-CN', withTime ? { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' } : { month: 'short', day: 'numeric' }) : ''
</script>

<style scoped>
.resume-workspace { max-width: 1180px; margin: 0 auto; animation: enter .35s ease; }
@keyframes enter { from { opacity: 0; transform: translateY(8px); } }
.page-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 20px; margin-bottom: 20px; }
.eyebrow { display: block; color: var(--blue); font-size: 12px; font-weight: 700; margin-bottom: 6px; }
.page-head h1 { color: var(--text); font-size: 28px; margin-bottom: 6px; }
.page-head p { color: var(--text-muted); font-size: 14px; }
.workspace-grid { display: grid; grid-template-columns: 320px minmax(0, 1fr); gap: 16px; align-items: start; }
.control-panel, .report-panel { background: var(--bg-card); border: 1px solid var(--border); border-radius: 8px; }
.upload-section, .history-section { padding: 18px; }
.history-section { border-top: 1px solid var(--border); }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-head h2 { color: var(--text); font-size: 14px; }
.section-head span { color: var(--text-muted); font-size: 11px; }
.upload-section :deep(.el-upload), .upload-section :deep(.el-upload-dragger) { width: 100%; }
.upload-section :deep(.el-upload-dragger) { padding: 0; border-radius: 7px; background: var(--bg); border-color: var(--border); }
.drop-content { min-height: 132px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 7px; padding: 18px; }
.drop-content .el-icon { font-size: 28px; color: var(--blue); }
.drop-content strong { max-width: 100%; color: var(--text); font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.drop-content small { color: var(--text-muted); font-size: 11px; }
.analyze-button { width: 100%; margin-top: 10px; }
.report-list { min-height: 100px; }
.report-list button { width: 100%; display: flex; align-items: center; gap: 10px; padding: 11px 8px; border: 0; border-radius: 6px; background: transparent; color: var(--text); text-align: left; cursor: pointer; }
.report-list button:hover, .report-list button.active { background: var(--hover); }
.report-list button.active { box-shadow: inset 2px 0 var(--blue); }
.file-icon { width: 30px; height: 30px; display: grid; place-items: center; border-radius: 6px; background: var(--blue-light); color: var(--blue); }
.file-meta { min-width: 0; flex: 1; }
.file-meta strong, .file-meta small { display: block; }
.file-meta strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
.file-meta small { color: var(--text-muted); font-size: 10px; margin-top: 3px; }
.mini-score { min-width: 28px; color: var(--blue); font-weight: 700; font-size: 14px; text-align: right; }
.history-empty { min-height: 110px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; color: var(--text-muted); font-size: 12px; }
.history-empty .el-icon { font-size: 24px; }
.report-panel { min-height: 620px; overflow: hidden; }
.report-head { display: grid; grid-template-columns: 82px 1fr 36px; gap: 18px; align-items: center; padding: 22px 26px; border-bottom: 1px solid var(--border); }
.score-block { display: flex; flex-direction: column; align-items: center; justify-content: center; width: 76px; height: 76px; border-radius: 8px; background: var(--blue-light); color: var(--blue); }
.score-block strong { font-size: 28px; line-height: 1; }.score-block span { margin-top: 5px; font-size: 10px; }
.report-title { min-width: 0; }.report-title > span { color: var(--text-muted); font-size: 11px; }.report-title h2 { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text); font-size: 20px; margin: 5px 0; }.report-title p { color: var(--text-secondary); font-size: 12px; }
.report-body { padding: 20px 30px 36px; color: var(--text); font-size: 14px; line-height: 1.85; }
.report-body :deep(h2), .report-body :deep(h3) { color: var(--text); margin: 22px 0 10px; padding-bottom: 7px; border-bottom: 1px solid var(--border); }.report-body :deep(h2) { font-size: 18px; }.report-body :deep(h3) { font-size: 16px; }.report-body :deep(ul), .report-body :deep(ol) { padding-left: 22px; }.report-body :deep(li) { margin: 6px 0; }.report-body :deep(code) { padding: 2px 5px; border-radius: 3px; background: var(--hover); }
.analysis-progress, .report-empty { min-height: 620px; padding: 46px 42px; }.analysis-progress h2, .report-empty h2 { color: var(--text); font-size: 20px; margin: 14px 0 7px; }.analysis-progress p, .report-empty p { color: var(--text-muted); font-size: 13px; margin-bottom: 30px; }.progress-icon, .empty-icon { width: 48px; height: 48px; display: grid; place-items: center; border-radius: 8px; background: var(--blue-light); color: var(--blue); font-size: 24px; }.progress-icon .el-icon { animation: spin 1s linear infinite; }@keyframes spin { to { transform: rotate(360deg); } }.report-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
:global(.danger-item) { color: #f56c6c !important; }
@media (max-width: 860px) { .workspace-grid { grid-template-columns: 1fr; }.control-panel { display: grid; grid-template-columns: 1fr 1fr; }.history-section { border-top: 0; border-left: 1px solid var(--border); }.report-panel { min-height: 480px; }.analysis-progress, .report-empty { min-height: 480px; } }
@media (max-width: 600px) { .page-head { align-items: flex-start; flex-direction: column; }.page-head h1 { font-size: 24px; }.control-panel { display: block; }.history-section { border-left: 0; border-top: 1px solid var(--border); }.report-head { grid-template-columns: 64px 1fr 30px; padding: 16px 14px; gap: 10px; }.score-block { width: 60px; height: 60px; }.score-block strong { font-size: 23px; }.report-title h2 { font-size: 16px; }.report-body { padding: 14px 16px 28px; }.report-panel { min-height: 400px; }.analysis-progress, .report-empty { min-height: 400px; padding: 30px 20px; } }
</style>
