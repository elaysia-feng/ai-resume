<template>
  <div class="chat-message" :class="`chat-message-${message.role}`">
    <!-- 系统消息：居中弱提示 -->
    <p v-if="message.role === 'system'" class="chat-system-note">{{ message.content }}</p>

    <!-- 工具消息：独立的工具卡片 -->
    <template v-else-if="message.role === 'tool'">
      <div v-for="tool in message.tools" :key="tool.id" class="chat-tool-card">
        <button class="chat-tool-head" @click="toggleTool(tool.id)">
          <span class="chat-tool-caret">{{ isExpanded(tool.id) ? '▾' : '▸' }}</span>
          <span class="chat-tool-name">{{ tool.name || 'tool' }}</span>
          <span class="chat-tool-status" :class="`chat-tool-status-${tool.status}`">{{ statusText(tool.status) }}</span>
        </button>
        <div v-if="isExpanded(tool.id)" class="chat-tool-body">
          <pre v-if="tool.input" class="chat-tool-input">{{ formatInput(tool.input) }}</pre>
          <p v-if="tool.summary" class="chat-tool-summary">{{ tool.summary }}</p>
        </div>
      </div>
    </template>

    <!-- 用户 / 助手消息 -->
    <template v-else>
      <div class="chat-bubble" :class="message.role === 'user' ? 'chat-bubble-user' : 'chat-bubble-assistant'">
        <p v-if="message.streaming && !message.content" class="chat-typing">
          <span class="chat-typing-dot"></span>
          <span class="chat-typing-dot"></span>
          <span class="chat-typing-dot"></span>
          思考中…
        </p>
        <p v-else class="chat-bubble-text">{{ message.content }}</p>
      </div>

      <div v-for="tool in message.tools" :key="tool.id" class="chat-tool-card">
        <button class="chat-tool-head" @click="toggleTool(tool.id)">
          <span class="chat-tool-caret">{{ isExpanded(tool.id) ? '▾' : '▸' }}</span>
          <span class="chat-tool-name">{{ tool.name || 'tool' }}</span>
          <span class="chat-tool-status" :class="`chat-tool-status-${tool.status}`">{{ statusText(tool.status) }}</span>
        </button>
        <div v-if="isExpanded(tool.id)" class="chat-tool-body">
          <pre v-if="tool.input" class="chat-tool-input">{{ formatInput(tool.input) }}</pre>
          <p v-if="tool.summary" class="chat-tool-summary">{{ tool.summary }}</p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const props = defineProps({
  message: { type: Object, required: true },
});

// 展开的工具卡片 ID 集合
const expandedTools = ref([]);

function toggleTool(toolId) {
  const index = expandedTools.value.indexOf(toolId);
  if (index === -1) {
    expandedTools.value = [...expandedTools.value, toolId];
  } else {
    expandedTools.value = expandedTools.value.filter((item) => item !== toolId);
  }
}

function isExpanded(toolId) {
  return expandedTools.value.includes(toolId);
}

function statusText(status) {
  if (status === 'success') {
    return '成功';
  }
  if (status === 'failed') {
    return '失败';
  }
  return '执行中';
}

function formatInput(input) {
  if (typeof input === 'string') {
    return input;
  }
  try {
    return JSON.stringify(input, null, 2);
  } catch {
    return String(input);
  }
}
</script>

<style scoped>
.chat-message {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 18px;
}

.chat-message-user {
  align-items: flex-end;
}

.chat-message-assistant,
.chat-message-tool {
  align-items: flex-start;
}

.chat-bubble {
  max-width: 76%;
  padding: 10px 14px;
  border-radius: var(--chat-radius-lg);
  line-height: 1.7;
  word-break: break-word;
}

.chat-bubble-user {
  color: var(--chat-text);
  background: var(--chat-user-bubble);
  border-bottom-right-radius: var(--chat-radius-sm);
}

.chat-bubble-assistant {
  max-width: 100%;
  color: var(--chat-text);
  background: var(--chat-assistant-bubble);
  border: 1px solid var(--chat-border);
  border-bottom-left-radius: var(--chat-radius-sm);
}

.chat-bubble-text {
  margin: 0;
  white-space: pre-wrap;
}

.chat-system-note {
  align-self: center;
  margin: 0;
  font-size: 0.8125rem;
  color: var(--chat-text-muted);
}

/* 思考中动画 */
.chat-typing {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.875rem;
  color: var(--chat-text-muted);
}

.chat-typing-dot {
  width: 6px;
  height: 6px;
  background: var(--chat-text-muted);
  border-radius: 50%;
  animation: chat-typing-blink 1.2s infinite ease-in-out;
}

.chat-typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.chat-typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes chat-typing-blink {
  0%, 80%, 100% { opacity: 0.25; }
  40% { opacity: 1; }
}

/* 工具调用卡片 */
.chat-tool-card {
  max-width: 100%;
  overflow: hidden;
  background: var(--chat-surface);
  border: 1px solid var(--chat-border);
  border-radius: var(--chat-radius-sm);
}

.chat-tool-head {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 6px 10px;
  font-size: 0.8125rem;
  color: var(--chat-text);
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
}

.chat-tool-caret {
  color: var(--chat-text-muted);
}

.chat-tool-name {
  font-family: 'Consolas', 'Monaco', monospace;
  color: var(--chat-accent);
}

.chat-tool-status {
  margin-left: auto;
  padding: 1px 8px;
  font-size: 0.75rem;
  border-radius: 999px;
}

.chat-tool-status-running {
  color: #92400e;
  background: #fef3c7;
}

.chat-tool-status-success {
  color: #166534;
  background: #dcfce7;
}

.chat-tool-status-failed {
  color: #991b1b;
  background: #fee2e2;
}

.chat-tool-body {
  padding: 0 10px 8px 10px;
  border-top: 1px dashed var(--chat-border);
}

.chat-tool-input {
  margin: 8px 0 4px;
  padding: 6px 8px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.75rem;
  color: var(--chat-text-secondary, #4b5563);
  background: var(--chat-bg);
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}

.chat-tool-summary {
  margin: 4px 0 0;
  font-size: 0.8125rem;
  color: var(--chat-text-muted);
  word-break: break-word;
}
</style>
