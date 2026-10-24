<template>
  <div ref="listRef" class="chat-message-list ui-scroll">
    <div class="chat-stream">
      <p v-if="loading" class="chat-state-hint">消息加载中…</p>

      <div v-else-if="messages.length === 0" class="ui-empty chat-empty">
        <h2 class="ui-empty__title">开始和 AI 聊聊你的简历</h2>
        <p class="ui-empty__hint">
          我可以帮你梳理项目经历、优化表达、准备面试题；设置了工作区目录后，还能一起分析本地文件。
        </p>
        <div class="chat-examples">
          <button
            v-for="example in examples"
            :key="example"
            class="chat-example"
            type="button"
            @click="$emit('use-example', example)"
          >
            {{ example }}
          </button>
        </div>
      </div>

      <template v-else>
        <ChatMessageItem v-for="message in messages" :key="message.id" :message="message" />
      </template>
    </div>

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
  min-height: 0;
  overflow-y: auto;
  padding: var(--ui-space-5) 0 var(--ui-space-4);
}

/* 消息正文控制在可读行长内，1440px 以上不再拉满整屏 */
.chat-stream {
  max-width: 820px;
  margin: 0 auto;
  padding: 0 var(--ui-space-6);
}

.chat-state-hint {
  font-size: var(--ui-text-sm);
  color: var(--chat-text-muted);
}

.chat-empty {
  margin-top: var(--ui-space-8);
}

.chat-examples {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--ui-space-2);
  max-width: 640px;
}

.chat-example {
  padding: var(--ui-space-2) var(--ui-space-3);
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
  background: var(--chat-surface);
  border: 1px solid var(--chat-border);
  border-radius: var(--chat-radius-md);
  cursor: pointer;
  text-align: left;
  transition: color var(--ui-duration-fast) var(--ui-ease-out),
    border-color var(--ui-duration-fast) var(--ui-ease-out),
    background var(--ui-duration-fast) var(--ui-ease-out);
}

.chat-example:hover {
  color: var(--ui-fg-accent);
  border-color: var(--ui-accent-border);
  background: var(--ui-accent-soft);
}

.chat-example:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

@media (max-width: 768px) {
  .chat-stream {
    padding: 0 var(--ui-space-4);
  }
}
</style>
