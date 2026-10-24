<template>
  <aside class="chat-sidebar" :class="{ 'is-collapsed': collapsed }">
    <div class="chat-sidebar-head">
      <template v-if="!collapsed">
        <span class="chat-sidebar-title">
          <AppIcon name="chat-circle" :size="16" />
          会话
        </span>
        <span class="ui-badge">{{ sessions.length }}</span>
      </template>
      <button
        class="ui-icon-btn chat-sidebar-toggle"
        type="button"
        :title="collapsed ? '展开会话列表' : '收起会话列表'"
        :aria-expanded="!collapsed"
        @click="toggleCollapsed"
      >
        <AppIcon :name="collapsed ? 'caret-right' : 'caret-left'" :size="16" />
      </button>
    </div>

    <!-- 收起态：只留窄轨道 -->
    <div v-if="collapsed" class="chat-sidebar-rail">
      <button
        class="ui-icon-btn ui-icon-btn--outline chat-sidebar-rail-btn"
        type="button"
        title="新建会话"
        :disabled="loading"
        @click="$emit('create')"
      >
        <AppIcon name="plus" :size="18" />
      </button>
    </div>

    <template v-else>
      <div class="chat-sidebar-top">
        <button
          class="ui-btn ui-btn--primary ui-btn--block"
          type="button"
          :disabled="loading"
          @click="$emit('create')"
        >
          <AppIcon name="plus" :size="16" />
          新建会话
        </button>
      </div>

      <div class="chat-session-list ui-scroll">
        <!-- 加载态：骨架屏占位，不用转圈 -->
        <div v-if="loading" class="chat-session-skeletons">
          <div v-for="index in 4" :key="index" class="ui-skeleton chat-session-skeleton"></div>
        </div>

        <div v-else-if="sessions.length === 0" class="ui-empty chat-sidebar-empty">
          <span class="ui-empty__icon">
            <AppIcon name="chat-circle-dots" :size="24" />
          </span>
          <p class="ui-empty__title">还没有会话</p>
          <p class="ui-empty__hint">新建一个会话，或直接输入第一条消息。</p>
        </div>

        <template v-else>
          <div
            v-for="session in sessions"
            :key="session.id"
            class="chat-session-item"
            :class="{ 'is-active': session.id === currentSessionId }"
          >
            <!-- 删除前二次确认 -->
            <template v-if="pendingDelete === session.id">
              <span class="chat-session-confirm-text">删除该会话？</span>
              <button class="ui-btn ui-btn--danger ui-btn--sm" type="button" @click="confirmRemove(session.id)">
                删除
              </button>
              <button class="ui-btn ui-btn--ghost ui-btn--sm" type="button" @click="cancelRemove">取消</button>
            </template>

            <template v-else>
              <button class="chat-session-open" type="button" @click="$emit('select', session.id)">
                <span class="chat-session-title">{{ session.title || `会话 #${session.id}` }}</span>
                <span class="chat-session-meta">
                  <span class="ui-badge">{{ messageCount(session) }} 条</span>
                  <span class="chat-session-time">{{ formatTime(session?.lastMessageAt) }}</span>
                  <span v-if="session.id === currentSessionId" class="ui-badge ui-badge--accent">当前</span>
                </span>
              </button>
              <button
                class="ui-icon-btn chat-session-delete"
                type="button"
                title="删除会话"
                @click="askRemove(session.id)"
              >
                <AppIcon name="x" :size="14" />
              </button>
            </template>
          </div>
        </template>
      </div>

      <div class="chat-sidebar-bottom">
        <p class="chat-workspace-label">
          <AppIcon name="hard-drives" :size="14" />
          工作区目录
        </p>
        <p class="chat-workspace-path" :title="workspacePath || '未设置'">{{ workspacePath || '未设置' }}</p>

        <button
          v-if="canChooseDirectory"
          class="ui-btn ui-btn--outline ui-btn--block ui-btn--sm"
          type="button"
          @click="$emit('choose-workspace')"
        >
          选择目录
        </button>

        <template v-else>
          <p class="chat-workspace-hint">浏览器环境无法打开系统目录选择框，可手动填写绝对路径</p>
          <div class="chat-workspace-input">
            <input
              v-model="manualPath"
              class="ui-input"
              type="text"
              placeholder="例如 D:\projects\my-resume"
              @keyup.enter="submitManualPath"
            />
            <button class="ui-btn ui-btn--sm" type="button" :disabled="!manualPath.trim()" @click="submitManualPath">
              保存
            </button>
          </div>
        </template>
      </div>
    </template>
  </aside>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue';
import AppIcon from '../common/AppIcon.vue';

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
const collapsed = ref(false);
// 等待确认删除的会话 ID
const pendingDelete = ref(null);

// 窄屏（窗口宽度小于该值）默认收起会话列表
const NARROW_WIDTH = 1080;

onMounted(() => {
  collapsed.value = window.innerWidth < NARROW_WIDTH;
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
});

function handleResize() {
  if (window.innerWidth < NARROW_WIDTH) {
    collapsed.value = true;
  }
}

function toggleCollapsed() {
  collapsed.value = !collapsed.value;
}

function askRemove(sessionId) {
  pendingDelete.value = sessionId;
}

function cancelRemove() {
  pendingDelete.value = null;
}

function confirmRemove(sessionId) {
  pendingDelete.value = null;
  emit('remove', sessionId);
}

function submitManualPath() {
  const path = manualPath.value.trim();
  if (!path) {
    return;
  }
  emit('confirm-workspace', path);
  manualPath.value = '';
}

// 会话副信息：消息条数
function messageCount(session) {
  return Number(session?.messageCount ?? 0);
}

// 最后一条消息时间
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
  position: relative;
  display: flex;
  flex-direction: column;
  flex: none;
  width: 260px;
  min-width: 260px;
  height: 100%;
  background: var(--ui-bg-surface);
  border-right: 1px solid var(--ui-border-hairline);
  transition: width var(--ui-duration-normal) var(--ui-ease-out),
    min-width var(--ui-duration-normal) var(--ui-ease-out);
}

.chat-sidebar.is-collapsed {
  width: 52px;
  min-width: 52px;
}

.chat-sidebar-head {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  flex: none;
  height: var(--ui-header-height);
  padding: 0 var(--ui-space-2) 0 var(--ui-space-4);
  border-bottom: 1px solid var(--ui-border-hairline);
}

.chat-sidebar.is-collapsed .chat-sidebar-head {
  justify-content: center;
  padding: 0;
}

.chat-sidebar-title {
  display: inline-flex;
  align-items: center;
  gap: var(--ui-space-2);
  font-size: var(--ui-text-sm);
  font-weight: 600;
  letter-spacing: var(--ui-tracking-snug);
  color: var(--ui-fg-primary);
}

.chat-sidebar-toggle {
  width: 26px;
  height: 26px;
  margin-left: auto;
}

.chat-sidebar.is-collapsed .chat-sidebar-toggle {
  margin-left: 0;
}

/* 收起态窄轨道 */
.chat-sidebar-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ui-space-2);
  padding: var(--ui-space-3) 0;
}

.chat-sidebar-rail-btn {
  width: 32px;
  height: 32px;
}

.chat-sidebar-rail-btn:hover:not(:disabled) {
  color: var(--ui-fg-accent);
  background: var(--ui-accent-soft);
  border-color: var(--ui-accent-border);
}

.chat-sidebar-top {
  flex: none;
  padding: var(--ui-space-3) var(--ui-space-3) var(--ui-space-2);
}

.chat-session-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 var(--ui-space-2) var(--ui-space-3);
}

/* 会话列表加载态 */
.chat-session-skeletons {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  padding: var(--ui-space-1);
}

.chat-session-skeleton {
  height: 46px;
  border-radius: var(--ui-radius-md);
}

.chat-sidebar-empty {
  gap: var(--ui-space-1);
  padding: var(--ui-space-5) var(--ui-space-2);
}

.chat-sidebar-empty :deep(.ui-empty__icon) {
  width: 40px;
  height: 40px;
  border-radius: var(--ui-radius-md);
}

.chat-sidebar-empty :deep(.ui-empty__title) {
  font-size: var(--ui-text-sm);
}

.chat-sidebar-empty :deep(.ui-empty__hint) {
  font-size: var(--ui-text-xs);
  line-height: var(--ui-line-normal);
}

/* 会话条目：选中态用底色 + 左侧竖条两重强调 */
.chat-session-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: var(--ui-space-1);
  margin-bottom: 2px;
  padding: var(--ui-space-2);
  border: 1px solid transparent;
  border-radius: var(--ui-radius-md);
  transition: background var(--ui-duration-fast) var(--ui-ease-out),
    border-color var(--ui-duration-fast) var(--ui-ease-out);
}

.chat-session-item:hover {
  background: var(--ui-bg-hover);
}

.chat-session-item.is-active {
  background: var(--ui-bg-selected);
  border-color: var(--ui-accent-border);
}

.chat-session-item.is-active::before {
  content: '';
  position: absolute;
  left: -1px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  border-radius: var(--ui-radius-full);
  background: var(--ui-accent);
}

.chat-session-open {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-1);
  flex: 1;
  min-width: 0;
  padding: 0;
  text-align: left;
  background: transparent;
  border: none;
  cursor: pointer;
}

.chat-session-open:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
  border-radius: var(--ui-radius-sm);
}

.chat-session-title {
  font-size: var(--ui-text-sm);
  font-weight: 500;
  color: var(--ui-fg-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-session-meta {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  min-width: 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.chat-session-time {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-session-delete {
  width: 24px;
  height: 24px;
  opacity: 0;
  transition: opacity var(--ui-duration-fast) var(--ui-ease-out),
    color var(--ui-duration-fast) var(--ui-ease-out),
    background var(--ui-duration-fast) var(--ui-ease-out);
}

.chat-session-item:hover .chat-session-delete,
.chat-session-delete:focus-visible {
  opacity: 1;
}

.chat-session-delete:hover {
  color: var(--ui-fg-inverse);
  background: var(--ui-danger);
}

.chat-session-confirm-text {
  flex: 1;
  align-self: center;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-primary);
}

.chat-sidebar-bottom {
  flex: none;
  padding: var(--ui-space-3);
  border-top: 1px solid var(--ui-border-hairline);
}

.chat-workspace-label {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  margin: 0 0 var(--ui-space-1);
  font-size: var(--ui-text-xs);
  font-weight: 600;
  color: var(--ui-fg-muted);
}

.chat-workspace-path {
  max-height: 3.4em;
  margin: 0 0 var(--ui-space-2);
  overflow-y: auto;
  font-size: var(--ui-text-xs);
  line-height: 1.7;
  color: var(--ui-fg-primary);
  word-break: break-all;
}

.chat-workspace-hint {
  margin: 0 0 var(--ui-space-2);
  font-size: var(--ui-text-xs);
  line-height: var(--ui-line-normal);
  color: var(--ui-fg-muted);
}

.chat-workspace-input {
  display: flex;
  gap: var(--ui-space-2);
}

.chat-workspace-input .ui-input {
  flex: 1;
  min-width: 0;
  padding: var(--ui-space-1) var(--ui-space-2);
  font-size: var(--ui-text-xs);
}

@media (max-width: 900px) {
  .chat-sidebar {
    width: 220px;
    min-width: 220px;
  }

  .chat-sidebar.is-collapsed {
    width: 48px;
    min-width: 48px;
  }
}
</style>
