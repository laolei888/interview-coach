<template>
  <main class="practice-page">
    <header class="page-header">
      <div>
        <span class="eyebrow">练习中心</span>
        <h1>面试题库</h1>
        <p>筛选知识点，逐题作答，再展开参考答案校准思路。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Plus" @click="openCreate">新建题目</el-button>
        <el-button type="primary" :icon="MagicStick" @click="aiVisible = true">AI 出题</el-button>
      </div>
    </header>

    <section class="filter-band">
      <el-segmented v-model="source" :options="sourceOptions" @change="resetAndLoad" />
      <div class="filters">
        <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索题目或答案" @clear="resetAndLoad" @keydown.enter="resetAndLoad" />
        <el-select v-model="category" clearable placeholder="全部分类" @change="resetAndLoad">
          <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="difficulty" clearable placeholder="全部难度" @change="resetAndLoad">
          <el-option v-for="item in difficulties" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button :icon="Refresh" :disabled="!questions.length" @click="randomPractice">随机练习</el-button>
      </div>
    </section>

    <div class="result-bar">
      <span>共 {{ total }} 道可见题目</span>
      <span v-if="activeFilters">已应用筛选条件</span>
    </div>

    <div v-loading="loading" class="question-area">
      <el-empty v-if="!loading && !questions.length" description="没有匹配的题目">
        <el-button type="primary" @click="clearFilters">清空筛选</el-button>
      </el-empty>

      <section v-else class="question-list">
        <article v-for="(item, index) in questions" :id="`question-${item.id}`" :key="item.id" class="question-card">
          <div class="question-index">{{ String((page - 1) * pageSize + index + 1).padStart(2, '0') }}</div>
          <div class="question-content">
            <div class="meta-row">
              <span class="category">{{ item.category }}</span>
              <el-tag :type="difficultyType(item.difficulty)" size="small" effect="plain">{{ item.difficulty }}</el-tag>
              <span class="source-label">{{ item.userId ? '我的题目' : '公共题' }}</span>
              <el-dropdown v-if="item.userId" trigger="click" class="item-menu" @command="handleCommand($event, item)">
                <el-button text circle :icon="MoreFilled" aria-label="题目操作" />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit" :icon="Edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="delete" :icon="Delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <h2>{{ item.question }}</h2>
            <button class="answer-toggle" type="button" @click="toggleAnswer(item.id)">
              <el-icon><View v-if="!expanded.has(item.id)" /><Hide v-else /></el-icon>
              {{ expanded.has(item.id) ? '收起参考答案' : '查看参考答案' }}
            </button>
            <div v-if="expanded.has(item.id)" class="answer-panel">
              <div class="answer-title">参考答案</div>
              <div class="markdown-body" v-html="renderMarkdown(item.answer || '暂无参考答案')"></div>
            </div>
          </div>
        </article>
      </section>
    </div>

    <footer v-if="total > pageSize" class="pagination-wrap">
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" background layout="prev, pager, next" @current-change="load" />
    </footer>

    <el-dialog v-model="editorVisible" :title="editingId ? '编辑我的题目' : '新建私有题目'" width="620px" destroy-on-close>
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="技术分类"><el-input v-model="editor.category" maxlength="50" placeholder="例如：Java 并发" /></el-form-item>
          <el-form-item label="难度"><el-segmented v-model="editor.difficulty" :options="difficulties" /></el-form-item>
        </div>
        <el-form-item label="题目"><el-input v-model="editor.question" type="textarea" :rows="4" maxlength="2000" show-word-limit /></el-form-item>
        <el-form-item label="参考答案"><el-input v-model="editor.answer" type="textarea" :rows="8" maxlength="10000" show-word-limit placeholder="支持 Markdown" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveQuestion">保存题目</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiVisible" title="AI 生成练习题" width="520px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="技术方向"><el-input v-model="aiForm.category" maxlength="50" placeholder="例如：Spring Boot、系统设计、React" /></el-form-item>
        <el-form-item label="难度"><el-segmented v-model="aiForm.difficulty" :options="difficulties" /></el-form-item>
        <el-form-item label="生成数量">
          <el-input-number v-model="aiForm.count" :min="1" :max="5" />
          <span class="field-hint">一次最多生成 5 道，生成后仅自己可见</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" :icon="MagicStick" @click="generateQuestions">开始生成</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Hide, MagicStick, MoreFilled, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { renderMarkdown } from '../utils/markdown'

const categories = ['Java', 'Spring', '数据库', 'Redis', '计算机网络', '操作系统', '设计模式', '微服务', '前端', '算法']
const difficulties = ['简单', '中等', '困难']
const sourceOptions = [{ label: '全部', value: 'all' }, { label: '公共题', value: 'public' }, { label: '我的题目', value: 'mine' }]

const questions = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')
const category = ref('')
const difficulty = ref('')
const source = ref('all')
const loading = ref(false)
const expanded = ref(new Set())
const editorVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const aiVisible = ref(false)
const generating = ref(false)
const editor = reactive({ category: '', difficulty: '中等', question: '', answer: '' })
const aiForm = reactive({ category: 'Java', difficulty: '中等', count: 3 })
const activeFilters = computed(() => keyword.value || category.value || difficulty.value || source.value !== 'all')

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/question/list', { params: { keyword: keyword.value || undefined, category: category.value || undefined, difficulty: difficulty.value || undefined, source: source.value, page: page.value, size: pageSize } })
    if (data.code === 200) {
      questions.value = data.data.records
      total.value = data.data.total
      expanded.value = new Set()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '题目加载失败')
  } finally {
    loading.value = false
  }
}

function resetAndLoad() { page.value = 1; load() }
function clearFilters() { keyword.value = ''; category.value = ''; difficulty.value = ''; source.value = 'all'; resetAndLoad() }
function difficultyType(value) { return value === '简单' ? 'success' : value === '中等' ? 'warning' : 'danger' }
function toggleAnswer(id) { const next = new Set(expanded.value); next.has(id) ? next.delete(id) : next.add(id); expanded.value = next }

function randomPractice() {
  const item = questions.value[Math.floor(Math.random() * questions.value.length)]
  expanded.value = new Set()
  document.getElementById(`question-${item.id}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function openCreate() {
  editingId.value = null
  Object.assign(editor, { category: '', difficulty: '中等', question: '', answer: '' })
  editorVisible.value = true
}

function openEdit(item) {
  editingId.value = item.id
  Object.assign(editor, { category: item.category || '', difficulty: item.difficulty || '中等', question: item.question || '', answer: item.answer || '' })
  editorVisible.value = true
}

async function saveQuestion() {
  if (!editor.category.trim() || !editor.question.trim()) return ElMessage.warning('请填写技术分类和题目')
  saving.value = true
  try {
    const request = editingId.value ? api.put(`/question/${editingId.value}`, editor) : api.post('/question/add', editor)
    const { data } = await request
    if (data.code !== 200) throw new Error(data.message)
    editorVisible.value = false
    source.value = 'mine'
    page.value = 1
    await load()
    ElMessage.success(editingId.value ? '题目已更新' : '题目已创建')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '保存失败')
  } finally { saving.value = false }
}

async function removeQuestion(item) {
  try { await ElMessageBox.confirm('删除后无法恢复，确定删除这道题吗？', '删除题目', { type: 'warning' }) } catch { return }
  try {
    const { data } = await api.delete(`/question/${item.id}`)
    if (data.code !== 200) throw new Error(data.message)
    if (questions.value.length === 1 && page.value > 1) page.value--
    await load()
    ElMessage.success('题目已删除')
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '删除失败') }
}

function handleCommand(command, item) { command === 'edit' ? openEdit(item) : removeQuestion(item) }

async function generateQuestions() {
  if (!aiForm.category.trim()) return ElMessage.warning('请输入技术方向')
  generating.value = true
  try {
    const { data } = await api.post('/question/ai-generate', aiForm)
    if (data.code !== 200) throw new Error(data.message)
    aiVisible.value = false
    source.value = 'mine'
    page.value = 1
    await load()
    ElMessage.success(`已生成 ${data.data.length} 道题目`)
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '生成失败，请检查大模型连接') }
  finally { generating.value = false }
}

onMounted(load)
</script>

<style scoped>
.practice-page { max-width: 1120px; margin: 0 auto; color: var(--text-primary, #172033); }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 28px; }
.eyebrow { color: #1677ff; font-size: 13px; font-weight: 700; }
.page-header h1 { margin: 5px 0 7px; font-size: 30px; line-height: 1.2; letter-spacing: 0; }
.page-header p { margin: 0; color: var(--text-secondary, #697386); font-size: 14px; }
.header-actions, .filters { display: flex; align-items: center; gap: 10px; }
.filter-band { padding: 18px 0; border-top: 1px solid var(--border-color, #e6e9ef); border-bottom: 1px solid var(--border-color, #e6e9ef); }
.filter-band, .filters { display: flex; justify-content: space-between; gap: 14px; }
.filters :deep(.el-input) { width: 250px; }
.filters :deep(.el-select) { width: 140px; }
.result-bar { display: flex; justify-content: space-between; padding: 18px 2px 12px; color: var(--text-secondary, #697386); font-size: 13px; }
.question-area { min-height: 260px; }
.question-list { border-top: 1px solid var(--border-color, #e6e9ef); }
.question-card { display: grid; grid-template-columns: 54px minmax(0, 1fr); padding: 25px 4px; border-bottom: 1px solid var(--border-color, #e6e9ef); scroll-margin-top: 24px; }
.question-card:target, .question-card:hover { background: var(--surface-subtle, #fafbfc); }
.question-index { color: #9aa3b2; font: 600 13px/24px ui-monospace, SFMono-Regular, Menlo, monospace; }
.meta-row { display: flex; align-items: center; min-height: 28px; gap: 9px; }
.category { color: #1677ff; font-size: 13px; font-weight: 700; }
.source-label { color: var(--text-secondary, #697386); font-size: 12px; }
.item-menu { margin-left: auto; }
.question-content h2 { max-width: 850px; margin: 12px 0 15px; font-size: 17px; line-height: 1.7; letter-spacing: 0; }
.answer-toggle { display: inline-flex; align-items: center; gap: 7px; padding: 0; border: 0; background: transparent; color: #1677ff; cursor: pointer; font: inherit; font-size: 13px; }
.answer-panel { margin-top: 19px; padding: 20px 22px; border-left: 3px solid #1677ff; background: var(--surface-subtle, #f7f9fc); }
.answer-title { margin-bottom: 12px; color: var(--text-secondary, #697386); font-size: 12px; font-weight: 700; }
.markdown-body { color: var(--text-primary, #263044); font-size: 14px; line-height: 1.85; overflow-wrap: anywhere; }
.markdown-body :deep(p:first-child), .markdown-body :deep(ul:first-child), .markdown-body :deep(ol:first-child) { margin-top: 0; }
.markdown-body :deep(p:last-child), .markdown-body :deep(ul:last-child), .markdown-body :deep(ol:last-child) { margin-bottom: 0; }
.markdown-body :deep(code) { padding: 2px 5px; border-radius: 4px; background: var(--code-bg, #e9edf4); }
.pagination-wrap { display: flex; justify-content: center; padding: 24px 0 8px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.field-hint { margin-left: 12px; color: var(--text-secondary, #7b8494); font-size: 12px; }

@media (max-width: 900px) {
  .filter-band { align-items: flex-start; flex-direction: column; }
  .filters { width: 100%; flex-wrap: wrap; justify-content: flex-start; }
  .filters :deep(.el-input) { flex: 1; min-width: 220px; }
}
@media (max-width: 600px) {
  .practice-page { padding-bottom: 8px; }
  .page-header { align-items: stretch; flex-direction: column; margin-bottom: 20px; }
  .page-header h1 { font-size: 26px; }
  .header-actions { display: grid; grid-template-columns: 1fr 1fr; }
  .filter-band :deep(.el-segmented) { width: 100%; }
  .filters { display: grid; grid-template-columns: 1fr 1fr; }
  .filters :deep(.el-input) { width: 100%; min-width: 0; grid-column: 1 / -1; }
  .filters :deep(.el-select), .filters > .el-button { width: 100%; }
  .filters > .el-button { grid-column: 1 / -1; }
  .question-card { grid-template-columns: 34px minmax(0, 1fr); padding: 21px 0; }
  .question-content h2 { font-size: 16px; }
  .answer-panel { margin-left: -34px; padding: 17px 16px; }
  .form-grid { grid-template-columns: 1fr; gap: 0; }
  .field-hint { display: block; margin: 8px 0 0; }
  :deep(.el-dialog) { width: calc(100% - 24px) !important; }
}
</style>
