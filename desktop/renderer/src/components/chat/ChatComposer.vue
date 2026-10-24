<template>
  <div class="chat-composer">
    <textarea
      ref="textareaRef"
      class="chat-composer-input"
      :value="modelValue"
      :disabled="disabled"
      rows="1"
      placeholder="输入你的问题，Enter 发送，Shift+Enter 换行"
      @input="handleInput"
      @keydown="handleKeydown"
    ></textarea>

    <div class="chat-composer-actions">
      <span class="chat-composer-tip">Enter 发送 / Shift+Enter 换行</span>
      <button v-if="streaming" class="chat-btn chat-btn-danger" @click="$emit('stop')">停止生成</button>
      <button v-else class="chat-btn chat-btn-primary" :disabled="!modelValue.trim()" @click="$emit('send')">
        发送
      </button>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue';

const props = defineProps({
  modelValue: { type: String, default: '' },
  // 流式输出中：禁用输入
  disabled: { type: Boolean, default: false },
  streaming: { type: Boolean, default: false },
});

const emit = defineEmits(['update:modelValue', 'send', 'stop']);

const textareaRef = ref(null);

function handleInput(event) {
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
  padding: 12px 24px 16px;
  background: var(--chat-surface);
  border-top: 1px solid var(--chat-border);
}

.chat-composer-input {
  display: block;
  width: 100%;
  min-height: 44px;
  max-height: 180px;
  padding: 10px 12px;
  font-family: inherit;
  font-size: 0.9375rem;
  line-height: 1.6;
  color: var(--chat-text);
  background: var(--chat-bg);
  border: 1px solid var(--chat-border-strong);
  border-radius: var(--chat-radius-md);
  resize: none;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.chat-composer-input:focus {
  border-color: var(--chat-accent);
  box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.12);
}

.chat-composer-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.chat-composer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.chat-composer-tip {
  font-size: 0.75rem;
  color: var(--chat-text-muted);
}
</style>
