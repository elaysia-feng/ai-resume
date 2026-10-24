<template>
  <div ref="listRef" class="chat-message-list ui-scroll">
    <div class="chat-stream">
      <!-- 加载态：骨架屏占位，不做整页遮罩 -->
      <div v-if="loading" class="chat-skeleton-list">
        <div v-for="index in 3" :key="index" class="chat-skeleton-item">
          <div class="ui-skeleton chat-skeleton-line chat-skeleton-line--label"></div>
          <div class="ui-skeleton chat-skeleton-line"></div>
          <div class="ui-skeleton chat-skeleton-line chat-skeleton-line--short"></div>
        </div>
      </div>

      <div v-else-if="messages.length === 0" class="ui-empty chat-empty">
        <span class="ui-empty__icon">
          <AppIcon name="chat-circle-dots" :size="24" />
        </span>
        <h2 class="ui-empty__title">开始和 AI 聊聊你的简历</h2>
        <p class="ui-empty__hint">
          我可以帮你梳理项目经历、优化表达、准备面试题；设置了工作区目录后，还能一起分析本地文件。
        </p>
        <div class="chat-examples">
          <button
            v-for="example in examples"
            :key="example"
            class="ui-btn ui-btn--outline ui-btn--sm chat-example"
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

    <p v-if="error" class="ui-callout ui-callout--danger chat-error-banner" role="alert">
      <AppIcon name="warning-circle" :size="16" />
      <span>{{ error }}</span>
    </p>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue';
import ChatMessageItem from './ChatMessageItem.vue';
import AppIcon from '../common/AppIcon.vue';

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

/* 加载态：几行骨架占位，形状接近真实消息 */
.chat-skeleton-list {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-6);
  padding-top: var(--ui-space-2);
}

.chat-skeleton-item {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.chat-skeleton-line {
  height: 12px;
  border-radius: var(--ui-radius-full);
}

.chat-skeleton-line--label {
  width: 88px;
  height: 10px;
}

.chat-skeleton-line--short {
  width: 62%;
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
  white-space: normal;
  text-align: left;
}

@media (max-width: 768px) {
  .chat-stream {
    padding: 0 var(--ui-space-4);
  }
}
</style>
