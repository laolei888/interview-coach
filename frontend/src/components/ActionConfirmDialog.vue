<template>
  <el-dialog
    v-model="visible"
    class="action-confirm-dialog"
    width="min(480px, calc(100% - 28px))"
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="!loading"
    align-center
    modal-class="action-confirm-overlay"
  >
    <div class="action-confirm-card">
      <div :class="['action-confirm-icon', `tone-${tone}`]"><el-icon><component :is="icon" /></el-icon></div>
      <span :class="['action-confirm-kicker', `tone-${tone}`]">{{ kicker }}</span>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div class="action-confirm-actions">
        <button type="button" class="action-confirm-cancel" :disabled="loading" @click="visible = false">{{ cancelText }}</button>
        <button type="button" :class="['action-confirm-submit', `tone-${tone}`]" :disabled="loading" @click="$emit('confirm')">
          <el-icon v-if="loading" class="is-loading"><Loading /></el-icon>
          <el-icon v-else><component :is="confirmIcon" /></el-icon>
          {{ loading ? loadingText : confirmText }}
        </button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { CircleClose, DocumentChecked, Loading } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '确认操作' },
  description: { type: String, default: '' },
  kicker: { type: String, default: 'CONFIRM ACTION / 确认操作' },
  confirmText: { type: String, default: '确认' },
  cancelText: { type: String, default: '取消' },
  loadingText: { type: String, default: '处理中' },
  loading: { type: Boolean, default: false },
  tone: { type: String, default: 'danger' },
  icon: { type: [Object, Function], default: CircleClose },
  confirmIcon: { type: [Object, Function], default: DocumentChecked }
})

const emit = defineEmits(['update:modelValue', 'confirm'])
const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})
</script>

<style scoped>
:global(.action-confirm-overlay) { background: rgba(15, 23, 42, .46) !important; backdrop-filter: blur(6px); }
:global(.action-confirm-dialog) { overflow: hidden; padding: 0 !important; border-radius: 22px !important; background: var(--bg-card, #fff) !important; box-shadow: 0 28px 90px rgba(15, 23, 42, .24) !important; }
:global(.action-confirm-dialog .el-dialog__header) { display: none; }
:global(.action-confirm-dialog .el-dialog__body) { padding: 0; }
.action-confirm-card { padding: 30px 32px 28px; color: var(--text); text-align: center; }
.action-confirm-icon { width: 54px; height: 54px; display: grid; place-items: center; margin: 0 auto 14px; border: 1px solid; border-radius: 17px; font-size: 25px; }
.action-confirm-icon.tone-danger { border-color: #ffd9d7; background: #fff1f0; color: #d9534f; }
.action-confirm-icon.tone-warning { border-color: #f4dfaa; background: #fff8e8; color: #b87813; }
.action-confirm-kicker { font: 800 9px ui-monospace, monospace; letter-spacing: .1em; }.action-confirm-kicker.tone-danger { color: #d9534f; }.action-confirm-kicker.tone-warning { color: #b87813; }
.action-confirm-card h2 { margin: 9px 0; color: var(--text); font-size: 21px; letter-spacing: -.035em; }.action-confirm-card > p { max-width: 360px; margin: 0 auto; color: var(--text-muted); font-size: 12px; line-height: 1.75; }
.action-confirm-actions { display: grid; grid-template-columns: 1fr 1.35fr; gap: 10px; margin-top: 25px; }.action-confirm-actions button { min-height: 44px; border-radius: 11px; font: inherit; font-size: 12px; font-weight: 700; cursor: pointer; transition: transform .2s, box-shadow .2s, background .2s; }.action-confirm-actions button:hover:not(:disabled) { transform: translateY(-1px); }.action-confirm-actions button:disabled { cursor: wait; opacity: .6; }
.action-confirm-cancel { border: 1px solid var(--border); background: var(--bg-card); color: var(--text-secondary); }.action-confirm-cancel:hover:not(:disabled) { background: var(--hover); }
.action-confirm-submit { display: inline-flex; align-items: center; justify-content: center; gap: 6px; color: #fff; }.action-confirm-submit.tone-danger { border: 1px solid #d9534f; background: #d9534f; box-shadow: 0 10px 22px rgba(217, 83, 79, .2); }.action-confirm-submit.tone-danger:hover:not(:disabled) { border-color: #c94743; background: #c94743; }.action-confirm-submit.tone-warning { border: 1px solid #b87813; background: #b87813; box-shadow: 0 10px 22px rgba(184, 120, 19, .18); }.action-confirm-submit.tone-warning:hover:not(:disabled) { border-color: #a56b10; background: #a56b10; }
@media (max-width: 600px) { .action-confirm-card { padding: 26px 20px 22px; }.action-confirm-card h2 { font-size: 19px; }.action-confirm-actions { grid-template-columns: 1fr; gap: 8px; margin-top: 21px; }.action-confirm-submit { order: -1; } }
</style>
