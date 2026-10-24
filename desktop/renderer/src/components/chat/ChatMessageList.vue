<template>
  <div ref="listRef" class="chat-message-list">
    <p v-if="loading" class="chat-state-hint">加载消息中…</p>

    <div v-else-if="messages.length === 0" class="chat-empty">
      <h2 class="chat-empty-title">开始和 AI 聊聊你的简历</h2>
      <p class="chat-empty-desc">
        我可以帮你梳理项目经历、优化表达、准备面试题；如果设置了工作区目录，还能一起分析本地文件。
      </p>
      <div class="chat-examples">
        <button
          v-for="example in examples"
          :key="example"
          class="chat-example"
          @click="$emit('use-example', example)"
        >
          {{ example }}
        </button>
      </div>
    </div>

    <template v-else>
      <ChatMessageItem v-for="message in messages" :key="message.id" :message="message" />
    </template>

    <p v-if="error" class="chat-error-banner">{{ error }}</p>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue';
import ChatMessageItem from './ChatMessageItem.vue';

const props = defineProps({
  messages: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
});

defineEmits(['use-example']);

const listRef = ref(null);

// 空态示例问题
const examples = [
  '帮我把这段项目经历改得更专业',
  '针对我的简历准备 5 道高频面试题',
  '这个工作区的目录结构是做什么的？',
];

// 用内容长度做签名，文本增量到达时也能触发滚动
const contentSignature = () =>
  props.messages
    .map((message) => `${message.id}:${(message.content || '').length}:${message.streaming ? 1 : 0}:${(message.tools || []).length}`)
    .join('|');

watch(contentSignature, async () => {
  await nextTick();
  scrollToBottom();
});

function scrollToBottom() {
  const el = listRef.value;
  if (!el) {
    return;
  }
  const streaming = props.messages.some((message) => message.streaming);
  el.scrollTo({ top: el.scrollHeight, behavior: streaming ? 'auto' : 'smooth' });
}
</script>

<style scoped>
.chat-message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.chat-state-hint {
  font-size: 0.875rem;
  color: var(--chat-text-muted);
}

.chat-empty {
  max-width: 560px;
  margin: 8vh auto 0;
  text-align: center;
}

.chat-empty-title {
  margin: 0 0 8px;
  font-size: 1.25rem;
  color: var(--chat-text);
}

.chat-empty-desc {
  margin: 0 0 20px;
  font-size: 0.9375rem;
  line-height: 1.7;
  color: var(--chat-text-muted);
}

.chat-examples {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-example {
  padding: 10px 14px;
  font-size: 0.875rem;
  color: var(--chat-text);
  background: var(--chat-surface);
  border: 1px solid var(--chat-border);
  border-radius: var(--chat-radius-md);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s ease, color 0.2s ease;
}

.chat-example:hover {
  color: var(--chat-accent);
  border-color: var(--chat-accent);
}
</style>
