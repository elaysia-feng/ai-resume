<template>
  <aside class="chat-sidebar">
    <div class="chat-sidebar-top">
      <button class="chat-btn chat-btn-primary chat-btn-block" :disabled="loading" @click="$emit('create')">
        ＋ 新建会话
      </button>
    </div>

    <div class="chat-session-list">
      <p v-if="loading" class="chat-sidebar-hint">加载中…</p>
      <p v-else-if="sessions.length === 0" class="chat-sidebar-hint">还没有会话，发条消息即可新建</p>

      <button
        v-for="session in sessions"
        :key="session.id"
        class="chat-session-item"
        :class="{ active: session.id === currentSessionId }"
        @click="$emit('select', session.id)"
      >
        <span class="chat-session-title">{{ session.title || `会话 #${session.id}` }}</span>
        <span class="chat-session-meta">{{ formatMeta(session) }}</span>
        <span
          class="chat-session-delete"
          role="button"
          title="删除会话"
          @click.stop="$emit('remove', session.id)"
        >✕</span>
      </button>
    </div>

    <div class="chat-sidebar-bottom">
      <p class="chat-workspace-label">工作区目录</p>
      <p class="chat-workspace-path" :title="workspacePath || '未设置'">{{ workspacePath || '未设置' }}</p>

      <button v-if="canChooseDirectory" class="chat-btn chat-btn-block" @click="$emit('choose-workspace')">
        选择目录
      </button>

      <template v-else>
        <p class="chat-workspace-hint">浏览器环境无法打开系统目录选择框，可手动填写绝对路径</p>
        <div class="chat-workspace-input">
          <input
            v-model="manualPath"
            type="text"
            placeholder="例如 D:\projects\my-resume"
            @keyup.enter="submitManualPath"
          />
          <button class="chat-btn" :disabled="!manualPath.trim()" @click="submitManualPath">保存</button>
        </div>
      </template>
    </div>
  </aside>
</template>

<script setup>
import { ref } from 'vue';

defineProps({
  sessions: { type: Array, default: () => [] },
  currentSessionId: { type: [Number, String], default: null },
  workspacePath: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  // Electron 环境才有原生目录选择框
  canChooseDirectory: { type: Boolean, default: false },
});

const emit = defineEmits(['select', 'create', 'remove', 'choose-workspace', 'confirm-workspace']);

const manualPath = ref('');

function submitManualPath() {
  const path = manualPath.value.trim();
  if (!path) {
    return;
  }
  emit('confirm-workspace', path);
  manualPath.value = '';
}

// 会话副信息：消息条数 + 最后消息时间
function formatMeta(session) {
  const count = Number(session?.messageCount ?? 0);
  return `${count} 条消息 · ${formatTime(session?.lastMessageAt)}`;
}

function formatTime(value) {
  if (!value) {
    return '暂无消息';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '暂无消息';
  }
  const pad = (num) => String(num).padStart(2, '0');
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
</script>

<style scoped>
.chat-sidebar {
  display: flex;
  flex-direction: column;
  width: 264px;
  min-width: 264px;
  height: 100%;
  background: var(--chat-sidebar-bg);
  border-right: 1px solid var(--chat-border);
}

.chat-sidebar-top {
  padding: 12px;
  border-bottom: 1px solid var(--chat-border);
}

.chat-session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.chat-sidebar-hint {
  margin: 8px 4px;
  font-size: 0.8125rem;
  color: var(--chat-text-muted);
}

.chat-session-item {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  margin-bottom: 4px;
  padding: 8px 28px 8px 10px;
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--chat-radius-sm);
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.chat-session-item:hover {
  background: #e9ecef;
}

.chat-session-item.active {
  background: var(--chat-surface);
  border-color: var(--chat-border-strong);
}

.chat-session-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--chat-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-session-meta {
  font-size: 0.75rem;
  color: var(--chat-text-muted);
}

.chat-session-delete {
  position: absolute;
  top: 8px;
  right: 8px;
  display: none;
  width: 18px;
  height: 18px;
  font-size: 0.75rem;
  line-height: 18px;
  text-align: center;
  color: var(--chat-text-muted);
  border-radius: 4px;
}

.chat-session-item:hover .chat-session-delete {
  display: block;
}

.chat-session-delete:hover {
  color: #ffffff;
  background: var(--chat-danger);
}

.chat-sidebar-bottom {
  padding: 12px;
  border-top: 1px solid var(--chat-border);
}

.chat-workspace-label {
  margin-bottom: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--chat-text-muted);
}

.chat-workspace-path {
  margin-bottom: 8px;
  font-size: 0.8125rem;
  color: var(--chat-text);
  word-break: break-all;
}

.chat-workspace-hint {
  margin-bottom: 8px;
  font-size: 0.75rem;
  line-height: 1.5;
  color: var(--chat-text-muted);
}

.chat-workspace-input {
  display: flex;
  gap: 6px;
}

.chat-workspace-input input {
  flex: 1;
  min-width: 0;
  padding: 6px 8px;
  font-size: 0.8125rem;
  color: var(--chat-text);
  background: var(--chat-surface);
  border: 1px solid var(--chat-border-strong);
  border-radius: var(--chat-radius-sm);
}

@media (max-width: 768px) {
  .chat-sidebar {
    width: 200px;
    min-width: 200px;
  }
}
</style>
