<template>
  <main class="practice-page">
    <header class="practice-hero">
      <div class="practice-hero-copy">
        <span class="eyebrow"><i></i>KNOWLEDGE LAB / 练习中心</span>
        <h1>把“知道”，练成“答得出来”。</h1>
        <p>按知识点拆分准备，用一次次有反馈的回答，让面试表达真正变得稳定。</p>
        <div class="hero-actions"><el-button type="primary" size="large" :icon="VideoPlay" :loading="practiceLoading" :disabled="!questions.length" @click="startPractice('mixed')">开始专注练习</el-button><button type="button" class="hero-link" @click="openCreate">新建私有题目<el-icon><Plus /></el-icon></button><button type="button" class="hero-link" @click="aiVisible = true">让 AI 帮我出题<el-icon><ArrowRight /></el-icon></button></div>
      </div>
      <div class="mastery-visual" aria-label="当前题库掌握度">
        <div class="mastery-orbit orbit-one"></div><div class="mastery-orbit orbit-two"></div><div class="mastery-core"><strong>{{ masteryRate }}<small>%</small></strong><span>当前掌握率</span></div>
        <span class="orbit-label label-top">已掌握 {{ progressStats.mastered }}</span><span class="orbit-label label-bottom">待复习 {{ progressStats.review }}</span>
      </div>
    </header>

    <section class="practice-cockpit">
      <article class="focus-card">
        <div class="focus-card-copy"><span class="section-kicker">NEXT BEST PRACTICE</span><h2>{{ focusTitle }}</h2><p>{{ focusDescription }}</p></div>
        <div class="focus-actions"><el-button v-if="progressStats.review" plain :icon="RefreshLeft" :loading="practiceLoading" :disabled="!questions.length" @click="startPractice('review')">优先练待复习</el-button><el-button type="primary" :icon="VideoPlay" :loading="practiceLoading" :disabled="!questions.length" @click="startPractice('mixed')">开始随机练习</el-button></div>
      </article>
      <div class="cockpit-stat teal"><span class="stat-icon"><el-icon><EditPen /></el-icon></span><strong>{{ progressStats.practiced }}</strong><small>已练习题目</small><em>持续回答，数据才会变准</em></div>
      <div class="cockpit-stat green"><span class="stat-icon"><el-icon><CircleCheck /></el-icon></span><strong>{{ progressStats.mastered }}</strong><small>稳定掌握</small><em>{{ masteryRate >= 70 ? '基础状态不错' : '再完成几轮训练' }}</em></div>
      <div class="cockpit-stat amber"><span class="stat-icon"><el-icon><RefreshLeft /></el-icon></span><strong>{{ progressStats.review }}</strong><small>待复习</small><em>下一轮优先处理</em></div>
    </section>

    <section class="filter-panel">
      <div class="filter-head"><div><span class="section-kicker">QUESTION LIBRARY</span><h2>选择今天要练的范围</h2></div><button v-if="activeFilters" type="button" class="clear-filter" @click="clearFilters">清除全部筛选<el-icon><Close /></el-icon></button></div>
      <div class="filter-row">
        <el-segmented v-model="source" :options="sourceOptions" @change="resetAndLoad" />
        <div class="filters">
          <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索题目或参考答案" @clear="resetAndLoad" @keydown.enter="resetAndLoad" />
          <el-select v-model="category" clearable placeholder="全部分类" @change="resetAndLoad"><el-option v-for="item in categories" :key="item" :label="item" :value="item" /></el-select>
          <el-select v-model="difficulty" clearable placeholder="全部难度" @change="resetAndLoad"><el-option v-for="item in difficulties" :key="item" :label="item" :value="item" /></el-select>
          <el-button :icon="Refresh" :disabled="!questions.length" @click="randomPractice">定位随机题</el-button>
        </div>
      </div>
      <div class="category-quick"><span>快速进入：</span><button v-for="item in quickCategories" :key="item" type="button" :class="{ active: category === item }" @click="selectCategory(item)">{{ item }}</button></div>
    </section>

    <section class="practice-layout">
      <div class="question-column">
        <div class="result-bar"><div><span class="section-kicker">YOUR QUEUE</span><strong>{{ activeFilters ? '筛选结果' : '全部可练题目' }}</strong><small>共 {{ total }} 道</small></div><span v-if="activeFilters" class="active-filter-note">已应用筛选条件</span></div>
        <div v-loading="loading" class="question-area">
          <el-empty v-if="!loading && !questions.length" description="没有匹配的题目"><el-button type="primary" @click="clearFilters">清空筛选</el-button></el-empty>
          <section v-else class="question-list">
            <article v-for="(item, index) in questions" :id="'question-' + item.id" :key="item.id" class="question-card">
              <div class="question-index">{{ String((page - 1) * pageSize + index + 1).padStart(2, '0') }}</div>
              <div class="question-content">
                <div class="meta-row"><span class="category">{{ item.category }}</span><el-tag :type="difficultyType(item.difficulty)" size="small" effect="plain">{{ item.difficulty }}</el-tag><span class="source-label">{{ item.userId ? '我的题目' : '公共题' }}</span><span v-if="progressMap[item.id]" :class="['progress-label', progressMap[item.id].status]">{{ progressMap[item.id].status === 'mastered' ? '已掌握' : '待复习' }}</span><el-dropdown v-if="item.userId" trigger="click" class="item-menu" @command="handleCommand($event, item)"><el-button text circle :icon="MoreFilled" aria-label="题目操作" /><template #dropdown><el-dropdown-menu><el-dropdown-item command="edit" :icon="Edit">编辑</el-dropdown-item><el-dropdown-item command="delete" :icon="Delete" divided>删除</el-dropdown-item></el-dropdown-menu></template></el-dropdown></div>
                <h2>{{ item.question }}</h2>
                <div class="question-footer"><button class="answer-toggle" type="button" @click="toggleAnswer(item.id)"><el-icon><View v-if="!expanded.has(item.id)" /><Hide v-else /></el-icon>{{ expanded.has(item.id) ? '收起参考答案' : '查看参考答案' }}</button><button class="single-practice" type="button" @click="startSinglePractice(item)">单题练习<el-icon><ArrowRight /></el-icon></button></div>
                <div v-if="expanded.has(item.id)" class="answer-panel"><div class="answer-title"><span>REFERENCE ANSWER</span><small>先回答，再对照</small></div><div class="markdown-body" v-html="renderMarkdown(item.answer || '暂无参考答案')"></div></div>
              </div>
            </article>
          </section>
        </div>
        <footer v-if="total > pageSize" class="pagination-wrap"><el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" background layout="prev, pager, next" @current-change="load" /></footer>
      </div>

      <aside class="practice-aside">
        <article class="aside-card category-card"><div class="aside-heading"><div><span class="section-kicker">TOPICS</span><h2>按方向练习</h2></div><el-icon><Collection /></el-icon></div><button v-for="item in categories" :key="item" type="button" :class="{ active: category === item }" @click="selectCategory(item)"><span>{{ item }}</span><el-icon><ArrowRight /></el-icon></button></article>
        <article class="aside-card method-card"><div class="aside-heading"><div><span class="section-kicker">METHOD</span><h2>一轮练习怎么做</h2></div><el-icon><Aim /></el-icon></div><ol><li><b>01</b><span><strong>先组织回答</strong><small>不要立刻打开参考答案</small></span></li><li><b>02</b><span><strong>再对照结构</strong><small>看自己漏掉了哪些关键点</small></span></li><li><b>03</b><span><strong>最后标记状态</strong><small>让下一轮优先补薄弱项</small></span></li></ol></article>
        <article class="aside-card ai-card"><span class="ai-mark"><el-icon><MagicStick /></el-icon></span><div><strong>没有合适的题目？</strong><p>告诉 AI 你的目标方向，生成一组更贴近当前面试的题目。</p><button type="button" @click="aiVisible = true">创建专属题组<el-icon><ArrowRight /></el-icon></button></div></article>
      </aside>
    </section>

    <ActionConfirmDialog v-model="confirmVisible" title="删除这道题吗？" description="删除后无法恢复，这道题及其练习进度都会被移除。" kicker="DELETE QUESTION / 删除题目" confirm-text="删除题目" loading-text="正在删除" tone="danger" :icon="Delete" :confirm-icon="Delete" :loading="confirmLoading" @confirm="confirmRemoveQuestion" />

    <el-dialog v-model="editorVisible" :title="editingId ? '编辑我的题目' : '新建私有题目'" width="620px" destroy-on-close><el-form label-position="top"><div class="form-grid"><el-form-item label="技术分类"><el-input v-model="editor.category" maxlength="50" placeholder="例如：Java 并发" /></el-form-item><el-form-item label="难度"><el-segmented v-model="editor.difficulty" :options="difficulties" /></el-form-item></div><el-form-item label="题目"><el-input v-model="editor.question" type="textarea" :rows="4" maxlength="2000" show-word-limit /></el-form-item><el-form-item label="参考答案"><el-input v-model="editor.answer" type="textarea" :rows="8" maxlength="10000" show-word-limit placeholder="支持 Markdown" /></el-form-item></el-form><template #footer><el-button @click="editorVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveQuestion">保存题目</el-button></template></el-dialog>

    <el-dialog v-model="aiVisible" title="AI 生成练习题" width="520px" destroy-on-close><el-form label-position="top"><el-form-item label="技术方向"><el-input v-model="aiForm.category" maxlength="50" placeholder="例如：Spring Boot、系统设计、React" /></el-form-item><el-form-item label="难度"><el-segmented v-model="aiForm.difficulty" :options="difficulties" /></el-form-item><el-form-item label="生成数量"><el-input-number v-model="aiForm.count" :min="1" :max="5" /><span class="field-hint">一次最多生成 5 道，生成后仅自己可见</span></el-form-item></el-form><template #footer><el-button @click="aiVisible = false">取消</el-button><el-button type="primary" :loading="generating" :icon="MagicStick" @click="generateQuestions">开始生成</el-button></template></el-dialog>

    <el-drawer v-model="practiceVisible" class="practice-drawer" size="min(700px, 100%)" direction="rtl" destroy-on-close>
      <template #header><div class="practice-title"><span>{{ practiceMode === 'review' ? '待复习练习' : practiceMode === 'single' ? '单题练习' : '专注练习' }}</span><small>{{ practiceProgress }}</small></div></template>
      <div v-if="currentPractice" class="focus-practice"><div class="focus-meta"><span>{{ currentPractice.category }}</span><el-tag :type="difficultyType(currentPractice.difficulty)" effect="plain">{{ currentPractice.difficulty }}</el-tag><small>已练 {{ progressMap[currentPractice.id]?.practiceCount || 0 }} 次</small></div><h2>{{ currentPractice.question }}</h2><div v-if="!practiceAnswerVisible" class="thinking-panel"><el-icon><EditPen /></el-icon><strong>先回答，再看答案</strong><p>在心里或纸上组织一次完整回答，训练回忆和表达，而不是只做阅读。</p><el-button type="primary" plain @click="practiceAnswerVisible = true">查看参考答案</el-button></div><div v-else class="focus-answer"><div><span>REFERENCE ANSWER</span><small>对照自己的思路，找出缺口</small></div><article v-html="renderMarkdown(currentPractice.answer || '暂无参考答案')"></article></div><div v-if="practiceAnswerVisible" class="practice-actions"><el-button :icon="RefreshLeft" @click="ratePractice('review')">稍后复习</el-button><el-button type="primary" :icon="CircleCheck" @click="ratePractice('mastered')">已经掌握</el-button></div></div>
      <el-result v-else icon="success" title="本轮练习完成" :sub-title="'已完成 ' + practiceQueue.length + ' 道题，可继续浏览题库或再来一轮。'"><template #extra><el-button type="primary" @click="startPractice(practiceMode === 'review' ? 'review' : 'mixed')">再练一轮</el-button></template></el-result>
    </el-drawer>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Aim, ArrowRight, CircleCheck, Close, Collection, Delete, Edit, EditPen, Hide, MagicStick, MoreFilled, Plus, Refresh, RefreshLeft, Search, VideoPlay, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api, { getErrorMessage } from '../api'
import { renderMarkdown } from '../utils/markdown'
import ActionConfirmDialog from '../components/ActionConfirmDialog.vue'

const categories = ['Java', 'Spring', '数据库', 'Redis', '计算机网络', '操作系统', '设计模式', '微服务', '前端', '算法']
const difficulties = ['简单', '中等', '困难']
const quickCategories = ['Java', 'Spring', '数据库', 'Redis', '前端']
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
const confirmVisible = ref(false)
const confirmLoading = ref(false)
const confirmTarget = ref(null)
const progressMap = reactive({})
const progressStats = reactive({ practiced: 0, mastered: 0, review: 0 })
const practiceVisible = ref(false)
const practiceLoading = ref(false)
const practiceQueue = ref([])
const practiceIndex = ref(0)
const practiceAnswerVisible = ref(false)
const practiceMode = ref('mixed')
const editor = reactive({ category: '', difficulty: '中等', question: '', answer: '' })
const aiForm = reactive({ category: 'Java', difficulty: '中等', count: 3 })
const activeFilters = computed(() => keyword.value || category.value || difficulty.value || source.value !== 'all')
const masteryRate = computed(() => progressStats.practiced ? Math.round(progressStats.mastered / progressStats.practiced * 100) : 0)
const currentPractice = computed(() => practiceQueue.value[practiceIndex.value] || null)
const practiceProgress = computed(() => currentPractice.value ? (practiceIndex.value + 1) + ' / ' + practiceQueue.value.length : '本轮完成')
const focusTitle = computed(() => progressStats.review ? '先把待复习题变成稳定得分点' : questions.value.length ? '从当前题目开始建立第一轮节奏' : '先选择一个知识方向')
const focusDescription = computed(() => progressStats.review ? '优先处理待复习题，比盲目扩展新题更容易形成短期提升。' : questions.value.length ? '每轮随机抽取最多 5 道题，回答后再查看参考答案并标记状态。' : '选择分类或清除筛选，让题库为你准备下一轮练习。')

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/question/list', { params: { keyword: keyword.value || undefined, category: category.value || undefined, difficulty: difficulty.value || undefined, source: source.value, page: page.value, size: pageSize } })
    if (data.code === 200) {
      questions.value = data.data.records || []
      total.value = data.data.total || 0
      expanded.value = new Set()
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '题目加载失败'))
  } finally {
    loading.value = false
  }
}

function resetAndLoad() { page.value = 1; load() }
function clearFilters() { keyword.value = ''; category.value = ''; difficulty.value = ''; source.value = 'all'; resetAndLoad() }
function selectCategory(value) { category.value = category.value === value ? '' : value; page.value = 1; load() }
function difficultyType(value) { return value === '简单' ? 'success' : value === '中等' ? 'warning' : 'danger' }
function toggleAnswer(id) { const next = new Set(expanded.value); next.has(id) ? next.delete(id) : next.add(id); expanded.value = next }

function randomPractice() {
  const item = questions.value[Math.floor(Math.random() * questions.value.length)]
  if (!item) return
  expanded.value = new Set()
  document.getElementById('question-' + item.id)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

async function loadPracticePool() {
  const { data } = await api.get('/question/list', { params: { keyword: keyword.value || undefined, category: category.value || undefined, difficulty: difficulty.value || undefined, source: source.value, page: 1, size: 100 } })
  if (data.code !== 200) throw new Error(data.message || '练习题目加载失败')
  return data.data.records || []
}

async function loadProgress() {
  try {
    const { data } = await api.get('/question/progress')
    if (data.code === 200) {
      Object.keys(progressMap).forEach(key => delete progressMap[key])
      ;(data.data.records || []).forEach(item => { progressMap[item.questionId] = item })
      Object.assign(progressStats, data.data.stats || { practiced: 0, mastered: 0, review: 0 })
    }
  } catch { /* 页面仍可浏览题库 */ }
}

async function startPractice(mode = 'mixed') {
  if (practiceLoading.value) return
  practiceLoading.value = true
  try {
    const allPool = await loadPracticePool()
    if (mode === 'review') await loadProgress()
    let pool = mode === 'review' ? allPool.filter(item => progressMap[item.id]?.status === 'review') : allPool
    if (mode === 'review' && !pool.length) {
      ElMessage.info('当前筛选范围没有待复习题，已切换为随机练习')
      mode = 'mixed'
      pool = allPool
    }
    if (!pool.length) return ElMessage.warning('当前没有可练习的题目')
    practiceMode.value = mode
    practiceQueue.value = [...pool].sort(() => Math.random() - .5).slice(0, Math.min(5, pool.length))
    practiceIndex.value = 0
    practiceAnswerVisible.value = false
    practiceVisible.value = true
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '练习题目加载失败'))
  } finally {
    practiceLoading.value = false
  }
}

function startSinglePractice(item) {
  practiceMode.value = 'single'
  practiceQueue.value = [item]
  practiceIndex.value = 0
  practiceAnswerVisible.value = false
  practiceVisible.value = true
}

async function ratePractice(status) {
  try {
    const { data } = await api.post('/question/' + currentPractice.value.id + '/progress', { status })
    if (data.code !== 200) throw new Error(data.message)
    await loadProgress()
    practiceIndex.value += 1
    practiceAnswerVisible.value = false
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '进度保存失败'))
  }
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
    const request = editingId.value ? api.put('/question/' + editingId.value, editor) : api.post('/question/add', editor)
    const { data } = await request
    if (data.code !== 200) throw new Error(data.message)
    editorVisible.value = false
    source.value = 'mine'
    page.value = 1
    await load()
    ElMessage.success(editingId.value ? '题目已更新' : '题目已创建')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '保存失败'))
  } finally {
    saving.value = false
  }
}

function removeQuestion(item) {
  confirmTarget.value = item
  confirmVisible.value = true
}

async function confirmRemoveQuestion() {
  if (!confirmTarget.value) return
  confirmLoading.value = true
  try {
    const { data } = await api.delete('/question/' + confirmTarget.value.id)
    if (data.code !== 200) throw new Error(data.message)
    if (questions.value.length === 1 && page.value > 1) page.value--
    await load()
    ElMessage.success('题目已删除')
    confirmVisible.value = false
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '删除失败'))
  } finally {
    confirmLoading.value = false
  }
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
    ElMessage.success('已生成 ' + data.data.length + ' 道题目')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '生成失败，请检查大模型连接'))
  } finally {
    generating.value = false
  }
}

onMounted(() => { load(); loadProgress() })
</script>

<style scoped>
.practice-page { max-width: 1200px; margin: 0 auto; padding-bottom: 24px; color: var(--text); }
.practice-hero { position: relative; display: flex; align-items: center; justify-content: space-between; gap: 35px; min-height: 270px; overflow: hidden; margin-bottom: 16px; padding: 34px 42px; border-radius: 12px; background: linear-gradient(125deg, #10201e 0%, #1c3732 56%, #245149 100%); color: #fff; box-shadow: 0 24px 55px rgba(23,32,42,.15); }.practice-hero::after { content: ''; position: absolute; width: 450px; height: 450px; right: -100px; top: -170px; border-radius: 50%; background: radial-gradient(circle, rgba(124,219,202,.27), transparent 64%); }.practice-hero-copy { position: relative; z-index: 1; max-width: 670px; }.eyebrow,.section-kicker { color: var(--blue); font: 800 10px ui-monospace, SFMono-Regular, Menlo, monospace; letter-spacing: .08em; }.eyebrow { display: flex; align-items: center; gap: 8px; color: #8ed3ca; }.eyebrow i { width: 7px; height: 7px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 4px rgba(240,139,115,.16); }.practice-hero h1 { margin: 12px 0 10px; font-size: clamp(29px, 4vw, 45px); letter-spacing: -.06em; line-height: 1.12; }.practice-hero p { max-width: 560px; color: rgba(255,255,255,.67); font-size: 13px; line-height: 1.75; }.hero-actions { display: flex; align-items: center; gap: 18px; margin-top: 24px; }.hero-actions .el-button { height: 42px; border: 0; background: #8ed3ca; color: #10201e; box-shadow: none; font-weight: 800; }.hero-link { display: flex; align-items: center; gap: 5px; border: 0; background: transparent; color: rgba(255,255,255,.72); cursor: pointer; font: inherit; font-size: 11px; }.hero-link:hover { color: #fff; }
.mastery-visual { position: relative; width: 226px; height: 190px; flex: 0 0 226px; z-index: 1; }.mastery-orbit { position: absolute; border: 1px solid rgba(142,211,202,.27); border-radius: 50%; transform: rotate(-22deg); }.orbit-one { inset: 16px 0 14px 0; }.orbit-two { inset: 0 35px 20px; transform: rotate(55deg); border-color: rgba(223,105,79,.31); }.mastery-core { position: absolute; inset: 48px 67px 55px; display: flex; flex-direction: column; align-items: center; justify-content: center; border: 1px solid rgba(142,211,202,.45); border-radius: 50%; background: rgba(13,35,31,.76); box-shadow: 0 0 35px rgba(142,211,202,.15); text-align: center; }.mastery-core strong,.mastery-core span { display: block; }.mastery-core strong { color: #a8e5db; font-size: 32px; line-height: 1; }.mastery-core strong small { font-size: 15px; }.mastery-core span { margin-top: 5px; color: rgba(255,255,255,.58); font-size: 9px; }.orbit-label { position: absolute; padding: 5px 8px; border: 1px solid rgba(255,255,255,.13); border-radius: 4px; background: rgba(255,255,255,.08); color: rgba(255,255,255,.75); font-size: 9px; }.label-top { right: 0; top: 22px; }.label-bottom { bottom: 10px; left: 2px; }
.practice-cockpit { display: grid; grid-template-columns: minmax(330px, 1.7fr) repeat(3, minmax(130px, 1fr)); gap: 10px; margin-bottom: 17px; }.focus-card,.cockpit-stat { min-height: 146px; border: 1px solid var(--border); border-radius: 9px; background: var(--bg-card); box-shadow: 0 14px 30px rgba(23,32,42,.045); }.focus-card { display: flex; flex-direction: column; justify-content: space-between; padding: 19px 22px; background: linear-gradient(125deg, var(--bg-card), color-mix(in srgb, var(--blue-light) 48%, var(--bg-card))); }.focus-card h2 { margin: 8px 0 5px; font-size: 17px; letter-spacing: -.025em; }.focus-card p { color: var(--text-secondary); font-size: 11px; line-height: 1.6; }.focus-actions { display: flex; align-items: center; gap: 8px; margin-top: 15px; }.focus-actions :deep(.el-button) { font-size: 10px; }.cockpit-stat { position: relative; display: flex; flex-direction: column; justify-content: center; padding: 18px; overflow: hidden; }.cockpit-stat::after { content: ''; position: absolute; right: -22px; bottom: -25px; width: 75px; height: 75px; border-radius: 50%; background: currentColor; opacity: .06; }.cockpit-stat.teal { color: var(--blue); }.cockpit-stat.green { color: var(--success); }.cockpit-stat.amber { color: #bc7d20; }.stat-icon { width: 29px; height: 29px; display: grid; place-items: center; margin-bottom: 11px; border-radius: 7px; background: color-mix(in srgb, currentColor 11%, var(--bg-card)); }.cockpit-stat strong,.cockpit-stat small,.cockpit-stat em { display: block; }.cockpit-stat strong { color: var(--text); font-size: 24px; line-height: 1; }.cockpit-stat small { margin-top: 6px; color: var(--text-secondary); font-size: 10px; }.cockpit-stat em { margin-top: 6px; color: var(--text-muted); font-size: 9px; font-style: normal; white-space: nowrap; }
.filter-panel { margin-bottom: 17px; padding: 17px 19px 13px; border: 1px solid var(--border); border-radius: 9px; background: var(--bg-card); box-shadow: 0 12px 28px rgba(23,32,42,.04); }.filter-head { display: flex; align-items: center; justify-content: space-between; gap: 15px; margin-bottom: 14px; }.filter-head h2 { margin-top: 6px; font-size: 15px; }.clear-filter { display: flex; align-items: center; gap: 4px; border: 0; background: transparent; color: var(--blue); cursor: pointer; font: inherit; font-size: 10px; }.filter-row { display: flex; align-items: center; justify-content: space-between; gap: 15px; }.filters { display: flex; align-items: center; gap: 8px; }.filters :deep(.el-input) { width: 250px; }.filters :deep(.el-select) { width: 130px; }.category-quick { display: flex; align-items: center; gap: 7px; margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--border); }.category-quick > span { color: var(--text-muted); font-size: 10px; }.category-quick button { padding: 4px 8px; border: 1px solid var(--border); border-radius: 4px; background: transparent; color: var(--text-secondary); cursor: pointer; font: inherit; font-size: 10px; }.category-quick button:hover,.category-quick button.active { border-color: color-mix(in srgb, var(--blue) 45%, var(--border)); background: var(--blue-light); color: var(--blue); }
.practice-layout { display: grid; grid-template-columns: minmax(0, 1fr) 252px; gap: 18px; align-items: start; }.question-column { min-width: 0; }.result-bar { display: flex; align-items: flex-end; justify-content: space-between; min-height: 53px; padding: 0 2px 12px; }.result-bar > div { display: flex; align-items: baseline; gap: 9px; }.result-bar strong { font-size: 14px; }.result-bar small { color: var(--text-muted); font-size: 10px; }.active-filter-note { color: var(--blue); font-size: 10px; }.question-area { min-height: 260px; }.question-list { display: grid; gap: 10px; }.question-card { position: relative; display: grid; grid-template-columns: 54px minmax(0, 1fr); padding: 20px 20px 18px 14px; border: 1px solid var(--border); border-radius: 8px; background: color-mix(in srgb, var(--bg-card) 93%, var(--blue-light)); scroll-margin-top: 24px; transition: border-color .18s, transform .18s, box-shadow .18s; }.question-card::before { content: ''; position: absolute; inset: 0 auto 0 0; width: 3px; border-radius: 8px 0 0 8px; background: color-mix(in srgb, var(--blue) 45%, transparent); }.question-card:target,.question-card:hover { border-color: color-mix(in srgb, var(--blue) 42%, var(--border)); transform: translateY(-2px); box-shadow: 0 12px 26px rgba(23,32,42,.06); }.question-index { width: 34px; height: 34px; display: grid; place-items: center; margin-top: 1px; border: 1px solid var(--border); border-radius: 7px; background: var(--hover); color: var(--blue); font: 700 12px ui-monospace, SFMono-Regular, Menlo, monospace; }.meta-row { display: flex; align-items: center; min-height: 28px; gap: 9px; }.category { color: var(--blue); font-size: 12px; font-weight: 700; }.source-label { color: var(--text-secondary); font-size: 10px; }.progress-label { padding: 3px 7px; border-radius: 4px; font-size: 9px; }.progress-label.mastered { background: #e8f6ee; color: #23845c; }.progress-label.review { background: #fff2e8; color: #c86636; }.item-menu { margin-left: auto; }.question-content h2 { max-width: 850px; margin: 12px 0 14px; font-size: 16px; line-height: 1.7; }.question-footer { display: flex; align-items: center; justify-content: space-between; gap: 10px; }.answer-toggle,.single-practice { display: inline-flex; align-items: center; gap: 6px; padding: 0; border: 0; background: transparent; cursor: pointer; font: inherit; font-size: 11px; }.answer-toggle { color: var(--blue); }.single-practice { color: var(--text-muted); }.single-practice:hover { color: var(--blue); }.answer-panel { margin-top: 17px; padding: 17px 19px; border: 1px solid var(--border); border-left: 3px solid var(--blue); border-radius: 0 7px 7px 0; background: var(--hover); }.answer-title { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 11px; }.answer-title span { color: var(--text-secondary); font: 800 9px ui-monospace,monospace; letter-spacing: .08em; }.answer-title small { color: var(--text-muted); font-size: 9px; }.markdown-body { color: var(--text); font-size: 13px; line-height: 1.85; overflow-wrap: anywhere; }.markdown-body :deep(p:first-child),.markdown-body :deep(ul:first-child),.markdown-body :deep(ol:first-child) { margin-top: 0; }.markdown-body :deep(p:last-child),.markdown-body :deep(ul:last-child),.markdown-body :deep(ol:last-child) { margin-bottom: 0; }.markdown-body :deep(code) { padding: 2px 5px; border-radius: 4px; background: var(--code-bg, #e9edf4); }.pagination-wrap { display: flex; justify-content: center; padding: 24px 0 8px; }
.practice-aside { display: grid; gap: 12px; }.aside-card { padding: 16px; border: 1px solid var(--border); border-radius: 9px; background: var(--bg-card); box-shadow: 0 12px 28px rgba(23,32,42,.04); }.aside-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; margin-bottom: 13px; }.aside-heading h2 { margin-top: 6px; font-size: 14px; }.aside-heading > .el-icon { color: var(--blue); font-size: 18px; }.category-card > button { width: 100%; display: flex; align-items: center; justify-content: space-between; padding: 8px 7px; border: 0; border-radius: 5px; background: transparent; color: var(--text-secondary); cursor: pointer; font: inherit; font-size: 11px; text-align: left; }.category-card > button:hover,.category-card > button.active { background: var(--blue-light); color: var(--blue); }.category-card > button .el-icon { color: var(--text-muted); font-size: 12px; }.category-card > button.active .el-icon { color: var(--blue); }.method-card { background: linear-gradient(145deg, var(--bg-card), color-mix(in srgb, var(--blue-light) 58%, var(--bg-card))); }.method-card ol { display: grid; gap: 13px; padding: 0; list-style: none; }.method-card li { display: flex; gap: 9px; }.method-card li > b { color: var(--blue); font: 800 9px ui-monospace,monospace; }.method-card li span { display: flex; flex-direction: column; gap: 3px; }.method-card strong { font-size: 11px; }.method-card small { color: var(--text-muted); font-size: 9px; line-height: 1.4; }.ai-card { display: flex; align-items: flex-start; gap: 10px; border-color: color-mix(in srgb, var(--accent) 25%, var(--border)); background: color-mix(in srgb, var(--accent) 6%, var(--bg-card)); }.ai-mark { width: 30px; height: 30px; display: grid; flex: 0 0 30px; place-items: center; border-radius: 7px; background: color-mix(in srgb, var(--accent) 13%, var(--bg-card)); color: var(--accent); }.ai-card strong,.ai-card p { display: block; }.ai-card strong { font-size: 11px; }.ai-card p { margin-top: 5px; color: var(--text-muted); font-size: 9px; line-height: 1.55; }.ai-card button { display: flex; align-items: center; gap: 4px; margin-top: 10px; border: 0; background: transparent; color: var(--accent); cursor: pointer; font: 700 10px inherit; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }.field-hint { margin-left: 12px; color: var(--text-secondary); font-size: 12px; }.practice-title span,.practice-title small { display: block; }.practice-title span { color: var(--text); font-size: 17px; font-weight: 700; }.practice-title small { margin-top: 3px; color: var(--text-muted); font-size: 11px; }.focus-meta { display: flex; align-items: center; gap: 9px; }.focus-meta > span { color: var(--blue); font-size: 12px; font-weight: 700; }.focus-meta small { margin-left: auto; color: var(--text-muted); font-size: 10px; }.focus-practice > h2 { margin: 24px 0; color: var(--text); font-size: 23px; line-height: 1.6; }.thinking-panel { min-height: 275px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 11px; border: 1px dashed var(--border); border-radius: 8px; background: var(--hover); text-align: center; }.thinking-panel > .el-icon { color: var(--blue); font-size: 30px; }.thinking-panel strong { font-size: 14px; }.thinking-panel p { color: var(--text-muted); font-size: 11px; }.focus-answer { padding: 20px 22px; border-left: 3px solid var(--blue); background: var(--hover); }.focus-answer > div { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; }.focus-answer span { color: var(--text-secondary); font: 800 9px ui-monospace,monospace; letter-spacing: .08em; }.focus-answer small { color: var(--text-muted); font-size: 9px; }.focus-answer article { margin-top: 12px; color: var(--text); font-size: 13px; line-height: 1.85; }.practice-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 22px; }
@media (max-width: 1020px) { .practice-layout { grid-template-columns: minmax(0, 1fr) 220px; }.practice-cockpit { grid-template-columns: minmax(300px, 1.5fr) repeat(3, 1fr); }.cockpit-stat { padding: 14px; } }
@media (max-width: 850px) { .practice-hero { align-items: flex-start; padding: 30px 230px 30px 30px; }.practice-hero-copy { max-width: 100%; z-index: 2; }.mastery-visual { position: absolute; right: 22px; bottom: -10px; opacity: .72; transform: scale(.8); transform-origin: bottom right; }.practice-cockpit { grid-template-columns: 1fr 1fr 1fr; }.focus-card { grid-column: 1 / -1; }.practice-layout { grid-template-columns: 1fr; }.practice-aside { grid-template-columns: repeat(2, 1fr); }.ai-card { grid-column: 1 / -1; }.filter-row { align-items: flex-start; flex-direction: column; }.filters { width: 100%; flex-wrap: wrap; }.filters :deep(.el-input) { flex: 1; min-width: 210px; } }
@media (max-width: 600px) { .practice-hero { min-height: 310px; padding: 25px 20px; }.practice-hero h1 { font-size: 30px; }.hero-actions { align-items: stretch; flex-direction: column; gap: 12px; }.hero-actions .el-button { width: max-content; }.mastery-visual { right: -8px; bottom: -17px; opacity: .3; transform: scale(.65); }.practice-cockpit { grid-template-columns: repeat(3, 1fr); }.focus-card { min-height: 175px; padding: 17px; }.focus-actions { align-items: stretch; flex-direction: column; }.focus-actions :deep(.el-button) { width: 100%; }.cockpit-stat { min-height: 115px; padding: 12px 10px; }.cockpit-stat strong { font-size: 21px; }.cockpit-stat em { overflow: hidden; text-overflow: ellipsis; }.filter-panel { padding: 14px 12px 11px; }.filter-head { align-items: flex-start; }.filter-row :deep(.el-segmented) { width: 100%; }.filters { display: grid; grid-template-columns: 1fr 1fr; }.filters :deep(.el-input) { width: 100%; min-width: 0; grid-column: 1 / -1; }.filters :deep(.el-select),.filters > .el-button { width: 100%; }.filters > .el-button { grid-column: 1 / -1; }.category-quick { overflow-x: auto; white-space: nowrap; }.question-card { grid-template-columns: 34px minmax(0, 1fr); padding: 18px 0; }.question-content h2 { font-size: 15px; }.question-footer { align-items: flex-start; flex-direction: column; gap: 9px; }.answer-panel { margin-left: -34px; padding: 16px; }.practice-aside { grid-template-columns: 1fr; }.form-grid { grid-template-columns: 1fr; gap: 0; }.field-hint { display: block; margin: 8px 0 0; }.focus-practice > h2 { font-size: 19px; }.practice-actions { display: grid; grid-template-columns: 1fr 1fr; }.thinking-panel { min-height: 230px; } }
/* ======== Question bank premium layer ======== */
.practice-hero { border-radius: 22px; background: linear-gradient(135deg, #0b211d 0%, #174239 57%, #2b7164 100%); box-shadow: 0 32px 82px rgba(13, 57, 47, .22); }
.practice-hero::before { content: ''; position: absolute; inset: 1px; border: 1px solid rgba(255, 255, 255, .12); border-radius: inherit; pointer-events: none; }
.practice-hero::after { width: 560px; height: 560px; right: -180px; top: -220px; }
.hero-actions .el-button { border-radius: 12px; box-shadow: 0 14px 27px rgba(107, 208, 192, .16); }
.practice-cockpit { gap: 14px; }
.focus-card, .cockpit-stat, .filter-panel, .question-card, .aside-card { border-radius: 16px; background: var(--surface-glass); box-shadow: 0 20px 50px rgba(16, 54, 47, .06); backdrop-filter: blur(16px); }
.focus-card { border-color: color-mix(in srgb, var(--blue) 16%, var(--border)); }
.cockpit-stat { border-color: color-mix(in srgb, currentColor 18%, var(--border)); }
.filter-panel { border-color: color-mix(in srgb, var(--blue) 13%, var(--border)); }
.question-list { gap: 14px; }
.question-card { padding: 22px 22px 20px 16px; transition: border-color .2s, transform .2s, box-shadow .2s; }
.question-card:hover, .question-card:target { transform: translateY(-5px); box-shadow: 0 24px 56px color-mix(in srgb, var(--blue) 11%, transparent); }
.question-index { border-radius: 10px; box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--blue) 12%, transparent); }
.answer-panel { border-radius: 0 12px 12px 0; background: color-mix(in srgb, var(--blue-light) 54%, var(--bg-card)); }
.practice-aside { gap: 14px; }
.aside-card { border-color: color-mix(in srgb, var(--blue) 13%, var(--border)); }
.category-card > button { border-radius: 9px; }
@media (max-width: 600px) {
  .practice-hero { border-radius: 18px; box-shadow: 0 24px 56px rgba(13, 57, 47, .2); }
  .focus-card, .cockpit-stat, .filter-panel, .question-card, .aside-card { border-radius: 13px; }
  .question-card { padding: 18px 15px 17px 12px; }
}
@media (max-width: 600px) {
  .mastery-visual { opacity: .14; transform: scale(.56); }
  .practice-hero-copy, .practice-hero .hero-actions { position: relative; z-index: 3; }
}
</style>
