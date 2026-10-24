<template>
  <div class="chat-composer">
    <div class="chat-composer-inner">
      <div class="chat-composer-field">
        <textarea
          ref="textareaRef"
          class="chat-composer-input"
          :value="modelValue"
          :disabled="disabled"
          rows="1"
          placeholder="描述你的需求，例如：帮我把这段项目经历改得更专业"
          @input="handleInput"
          @keydown="handleKeydown"
        ></textarea>

        <div class="chat-composer-toolbar">
          <div class="chat-composer-tools">
            <button
              v-if="canPickFiles"
              class="ui-btn ui-btn--ghost ui-btn--sm"
              type="button"
              :disabled="disabled"
              @click="pickAttachments"
            >
              <AppIcon name="paperclip" :size="16" />
              附件
            </button>
            <span class="chat-composer-hint">Enter 发送 · Shift+Enter 换行</span>
          </div>

          <div class="chat-composer-actions">
            <button
              v-if="streaming"
              class="ui-btn ui-btn--danger ui-btn--sm"
              type="button"
              @click="$emit('stop')"
            >
              <AppIcon name="x-circle" :size="16" />
              停止生成
            </button>
            <button
              v-else
              class="ui-btn ui-btn--primary ui-btn--sm"
              type="button"
              :disabled="!modelValue.trim()"
              @click="$emit('send')"
            >
              <AppIcon name="paper-plane-tilt" :size="16" />
              发送
            </button>
          </div>
        </div>
      </div>

      <p v-if="notice" class="chat-composer-notice">{{ notice }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import AppIcon from '../common/AppIcon.vue';

const props = defineProps({
  modelValue: { type: String, default: '' },
  // 流式输出中：禁用输入
  disabled: { type: Boolean, default: false },
  streaming: { type: Boolean, default: false },
});

const emit = defineEmits(['update:modelValue', 'send', 'stop']);

const textareaRef = ref(null);
const notice = ref('');

// Electron 环境才有原生文件选择框
const canPickFiles = computed(
  () => typeof window !== 'undefined' && typeof window.desktop?.pickFiles === 'function'
);

// 发送后父组件会清空文本，这里同步把输入框收回一行
watch(
  () => props.modelValue,
  () => {
    resizeTextarea();
  }
);

function handleInput(event) {
  notice.value = '';
  emit('update:modelValue', event.target.value);
  resizeTextarea();
}

// Enter 发送，Shift+Enter 换行；中文输入法组词过程中不触发发送
function handleKeydown(event) {
  if (event.key !== 'Enter' || event.shiftKey) {
    return;
  }
  if (event.isComposing || event.keyCode === 229) {
    return;
  }
  event.preventDefault();
  if (!props.disabled && props.modelValue.trim()) {
    emit('send');
  }
}

// 选中的文件以路径形式写进输入框，作为本次提问的上下文
async function pickAttachments() {
  notice.value = '';
  try {
    const result = await window.desktop.pickFiles();
    const paths = (result?.paths ?? []).filter(Boolean);
    if (!paths.length) {
      return;
    }
    const context = paths.map((item) => `附件：${item}`).join('\n');
    const current = props.modelValue.trim();
    emit('update:modelValue', current ? `${current}\n${context}` : context);
    await nextTick();
    resizeTextarea();
  } catch (error) {
    notice.value = `选择文件失败：${error?.message || '未知错误'}`;
  }
}

function resizeTextarea() {
  const el = textareaRef.value;
  if (!el) {
    return;
  }
  el.style.height = 'auto';
  el.style.height = `${Math.min(el.scrollHeight, 180)}px`;
}

// 输入框获得焦点后保持光标可见
function focus() {
  nextTick(() => textareaRef.value?.focus());
}

defineExpose({ focus });
</script>

<style scoped>
.chat-composer {
  flex: none;
  padding: var(--ui-space-3) 0 var(--ui-space-4);
  background: var(--ui-bg-surface);
  background-image: var(--ui-surface-wash);
  border-top: 1px solid var(--ui-border-hairline);
}

/* 与消息流同一条可读行长 */
.chat-composer-inner {
  max-width: 820px;
  margin: 0 auto;
  padding: 0 var(--ui-space-6);
}

/* 输入框做成一块抬升面：底色比画布更亮一层 + 分层阴影 */
.chat-composer-field {
  background: var(--ui-bg-surface);
  border: 1px solid var(--ui-border-strong);
  border-radius: var(--ui-radius-lg);
  box-shadow: var(--ui-shadow-sm), var(--ui-highlight-top);
  transition: border-color var(--ui-duration-fast) var(--ui-ease-out),
    box-shadow var(--ui-duration-fast) var(--ui-ease-out);
}

.chat-composer-field:focus-within {
  border-color: var(--ui-border-focus);
  box-shadow: var(--ui-ring), var(--ui-shadow-sm);
}

.chat-composer-input {
  display: block;
  width: 100%;
  min-height: 44px;
  max-height: 180px;
  padding: var(--ui-space-3);
  font-family: inherit;
  font-size: var(--ui-text-base);
  line-height: 1.6;
  color: var(--ui-fg-primary);
  background: transparent;
  border: none;
  resize: none;
  outline: none;
}

.chat-composer-input::placeholder {
  color: var(--ui-fg-muted);
}

.chat-composer-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.chat-composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-3);
  padding: var(--ui-space-2) var(--ui-space-2) var(--ui-space-2) var(--ui-space-3);
}

.chat-composer-tools {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  min-width: 0;
}

/* 快捷键提示刻意弱化，不与主按钮抢注意力 */
.chat-composer-hint {
  min-width: 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-composer-actions {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  flex: none;
}

.chat-composer-notice {
  margin: var(--ui-space-2) 0 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-danger);
}

@media (max-width: 768px) {
  .chat-composer-inner {
    padding: 0 var(--ui-space-4);
  }
}
</style>
