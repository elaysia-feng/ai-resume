<template>
  <article class="chat-message" :class="`chat-message--${message.role}`">
    <!-- 系统消息：居中弱提示 -->
    <p v-if="message.role === 'system'" class="chat-system-note">{{ message.content }}</p>

    <!-- 工具消息：独立的工具卡片 -->
    <template v-else-if="message.role === 'tool'">
      <div v-for="tool in message.tools" :key="tool.id" class="chat-tool-card">
        <button
          class="chat-tool-head"
          type="button"
          :aria-expanded="isExpanded(tool.id)"
          @click="toggleTool(tool.id)"
        >
          <AppIcon :name="isExpanded(tool.id) ? 'caret-down' : 'caret-right'" :size="14" class="chat-tool-caret" />
          <AppIcon name="terminal-window" :size="14" class="chat-tool-kind" />
          <span class="chat-tool-name">{{ tool.name || 'tool' }}</span>
          <span class="ui-badge" :class="statusBadgeClass(tool.status)">{{ statusText(tool.status) }}</span>
        </button>
        <div v-if="isExpanded(tool.id)" class="chat-tool-body">
          <pre v-if="tool.input" class="chat-tool-input">{{ formatInput(tool.input) }}</pre>
          <p v-if="tool.summary" class="chat-tool-summary">{{ tool.summary }}</p>
        </div>
      </div>
    </template>

    <!-- 用户 / 助手消息 -->
    <template v-else>
      <div v-if="message.role === 'assistant'" class="chat-message-head">
        <span class="chat-role-label">
          <AppIcon name="sparkle" :size="14" />
          AI 助手
        </span>
        <span v-if="message.streaming" class="ui-badge ui-badge--accent">
          <AppIcon name="spinner-gap" :size="12" spin />
          生成中
        </span>
      </div>

      <div class="chat-bubble" :class="message.role === 'user' ? 'chat-bubble--user' : 'chat-bubble--assistant'">
        <p v-if="message.streaming && !message.content" class="chat-typing">
          <AppIcon name="spinner-gap" :size="14" spin />
          正在思考…
        </p>
        <RichTextContent v-else-if="message.role === 'user'" class="chat-user-text" :html="message.content" />
        <div v-else class="chat-md" v-html="assistantHtml"></div>
      </div>

      <p v-if="message.streaming && message.content" class="chat-streaming-hint">
        <AppIcon name="spinner-gap" :size="12" spin />
        正在生成…
      </p>

      <div v-for="tool in message.tools" :key="tool.id" class="chat-tool-card">
        <button
          class="chat-tool-head"
          type="button"
          :aria-expanded="isExpanded(tool.id)"
          @click="toggleTool(tool.id)"
        >
          <AppIcon :name="isExpanded(tool.id) ? 'caret-down' : 'caret-right'" :size="14" class="chat-tool-caret" />
          <AppIcon name="terminal-window" :size="14" class="chat-tool-kind" />
          <span class="chat-tool-name">{{ tool.name || 'tool' }}</span>
          <span class="ui-badge" :class="statusBadgeClass(tool.status)">{{ statusText(tool.status) }}</span>
        </button>
        <div v-if="isExpanded(tool.id)" class="chat-tool-body">
          <pre v-if="tool.input" class="chat-tool-input">{{ formatInput(tool.input) }}</pre>
          <p v-if="tool.summary" class="chat-tool-summary">{{ tool.summary }}</p>
        </div>
      </div>
    </template>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue';
import RichTextContent from '../common/RichTextContent.vue';
import AppIcon from '../common/AppIcon.vue';

const props = defineProps({
  message: { type: Object, required: true },
});

// 展开的工具卡片 ID 集合
const expandedTools = ref([]);

// 助手消息按 Markdown 渲染
const assistantHtml = computed(() =>
  props.message.role === 'assistant' ? markdownToHtml(props.message.content) : ''
);

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

function statusBadgeClass(status) {
  if (status === 'success') {
    return 'ui-badge--success';
  }
  if (status === 'failed') {
    return 'ui-badge--danger';
  }
  return 'ui-badge--warning';
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

// ============================================================
// 极简 Markdown → HTML
// 做法：先把原文整体转义，再只拼装自己认识的标签，
// 所以模型输出里的 HTML 不会被当成结构执行，无需额外净化器。
// ============================================================

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// 行内标记：**粗体** *斜体* ~~删除线~~ `代码` [链接](url)
function renderInline(text) {
  const codes = [];
  let html = escapeHtml(text);

  // 行内代码先占位，避免里面的 * _ 被当成强调
  html = html.replace(/`([^`\n]+)`/g, (_, code) => {
    codes.push(code);
    return `\u0000${codes.length - 1}\u0000`;
  });

  html = html.replace(/\[([^\]\n]+)\]\(([^)\s]+)\)/g, (_, label, url) => {
    const safeUrl = /^https?:\/\//i.test(url) ? url : '#';
    return `<a href="${safeUrl}" target="_blank" rel="noopener noreferrer">${label}</a>`;
  });

  html = html.replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/(^|[^*\w])\*([^*\n]+)\*/g, '$1<em>$2</em>');
  html = html.replace(/~~([^~\n]+)~~/g, '<del>$1</del>');

  return html.replace(/\u0000(\d+)\u0000/g, (_, index) => `<code>${codes[Number(index)]}</code>`);
}

// 表格分隔行，例如 | --- | :---: |
function isTableSeparator(line, headerLine) {
  if (!line) {
    return false;
  }
  const trimmed = line.trim();
  if (!trimmed.includes('-') || !/^[\s|:-]+$/.test(trimmed)) {
    return false;
  }
  return headerLine.includes('|') ? trimmed.includes('|') : true;
}

function splitTableRow(line) {
  return line
    .replace(/^\s*\|/, '')
    .replace(/\|\s*$/, '')
    .split('|')
    .map((cell) => cell.trim());
}

function alignmentOf(cell) {
  const value = String(cell ?? '').trim();
  if (value.startsWith(':') && value.endsWith(':')) {
    return 'center';
  }
  if (value.endsWith(':')) {
    return 'right';
  }
  return 'left';
}

function renderTable(headers, aligns, rows) {
  const alignAttr = (index) => (aligns[index] && aligns[index] !== 'left' ? ` style="text-align:${aligns[index]}"` : '');
  const head = headers.map((cell, index) => `<th${alignAttr(index)}>${renderInline(cell)}</th>`).join('');
  const body = rows
    .map((row) => `<tr>${headers.map((_, index) => `<td${alignAttr(index)}>${renderInline(row[index] ?? '')}</td>`).join('')}</tr>`)
    .join('');
  return `<div class="chat-md-table-wrap"><table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table></div>`;
}

// 判断一行是否是新块的开头（段落遇到它就断开）
function isBlockStart(line, nextLine) {
  return (
    /^\s*```/.test(line) ||
    /^#{1,6}\s+/.test(line) ||
    /^\s*>\s?/.test(line) ||
    /^\s*(-{3,}|\*{3,}|_{3,})\s*$/.test(line) ||
    /^(\s*)([-*+]|\d+[.)])\s+/.test(line) ||
    (line.includes('|') && isTableSeparator(nextLine, line))
  );
}

function markdownToHtml(source) {
  const lines = String(source ?? '')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .split('\n');
  const blocks = [];
  let index = 0;

  while (index < lines.length) {
    const line = lines[index];

    // 围栏代码块
    const fence = /^\s*```\s*([\w+-]*)\s*$/.exec(line);
    if (fence) {
      const buffer = [];
      index += 1;
      while (index < lines.length && !/^\s*```\s*$/.test(lines[index])) {
        buffer.push(lines[index]);
        index += 1;
      }
      index += 1;
      const langClass = fence[1] ? ` class="language-${fence[1]}"` : '';
      blocks.push(`<pre><code${langClass}>${escapeHtml(buffer.join('\n'))}</code></pre>`);
      continue;
    }

    // 空行
    if (!line.trim()) {
      index += 1;
      continue;
    }

    // 分割线
    if (/^\s*(-{3,}|\*{3,}|_{3,})\s*$/.test(line)) {
      blocks.push('<hr />');
      index += 1;
      continue;
    }

    // 标题
    const heading = /^(#{1,6})\s+(.+)$/.exec(line);
    if (heading) {
      const level = heading[1].length;
      blocks.push(`<h${level}>${renderInline(heading[2].trim())}</h${level}>`);
      index += 1;
      continue;
    }

    // 表格：当前行含竖线，下一行是分隔行
    if (line.includes('|') && isTableSeparator(lines[index + 1], line)) {
      const headers = splitTableRow(line);
      const aligns = splitTableRow(lines[index + 1]).map(alignmentOf);
      index += 2;
      const rows = [];
      while (index < lines.length && lines[index].includes('|') && lines[index].trim()) {
        rows.push(splitTableRow(lines[index]));
        index += 1;
      }
      blocks.push(renderTable(headers, aligns, rows));
      continue;
    }

    // 引用
    if (/^\s*>\s?/.test(line)) {
      const buffer = [];
      while (index < lines.length && /^\s*>\s?/.test(lines[index])) {
        buffer.push(lines[index].replace(/^\s*>\s?/, ''));
        index += 1;
      }
      blocks.push(`<blockquote>${renderInline(buffer.join(' '))}</blockquote>`);
      continue;
    }

    // 有序 / 无序列表
    if (/^(\s*)([-*+]|\d+[.)])\s+/.test(line)) {
      const ordered = /^\s*\d/.test(line);
      const items = [];
      while (index < lines.length) {
        const item = /^(\s*)([-*+]|\d+[.)])\s+(.*)$/.exec(lines[index]);
        if (!item) {
          break;
        }
        items.push(item[3]);
        index += 1;
      }
      const tag = ordered ? 'ol' : 'ul';
      blocks.push(`<${tag}>${items.map((item) => `<li>${renderInline(item)}</li>`).join('')}</${tag}>`);
      continue;
    }

    // 段落：连续普通行合并成一段
    const paragraph = [];
    while (index < lines.length && lines[index].trim() && !isBlockStart(lines[index], lines[index + 1])) {
      paragraph.push(lines[index].trim());
      index += 1;
    }
    if (paragraph.length) {
      blocks.push(`<p>${renderInline(paragraph.join('\n')).replace(/\n/g, '<br />')}</p>`);
    } else {
      index += 1;
    }
  }

  return blocks.join('');
}
</script>

<style scoped>
.chat-message {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
  margin-bottom: var(--ui-space-6);
}

.chat-message--user {
  align-items: flex-end;
}

.chat-message--assistant,
.chat-message--tool {
  align-items: flex-start;
}

.chat-message-head {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
}

.chat-role-label {
  display: inline-flex;
  align-items: center;
  gap: var(--ui-space-1);
  font-size: var(--ui-text-xs);
  font-weight: 600;
  color: var(--ui-fg-muted);
}

/* 用户消息用浅色气泡；助手消息不用气泡，直接落在画布上像文档流 */
.chat-bubble {
  max-width: 100%;
  color: var(--ui-fg-primary);
}

.chat-bubble--user {
  max-width: 78%;
  padding: var(--ui-space-3) var(--ui-space-4);
  background: var(--ui-accent-soft);
  border: 1px solid var(--ui-accent-border);
  border-radius: var(--ui-radius-lg) var(--ui-radius-lg) var(--ui-radius-sm) var(--ui-radius-lg);
  line-height: var(--ui-line-normal);
}

.chat-bubble--assistant {
  padding: 0;
  background: transparent;
}

.chat-user-text {
  font-size: var(--ui-text-base);
}

.chat-system-note {
  align-self: center;
  margin: 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

/* 流式状态：思考中 / 正在生成 */
.chat-typing {
  display: inline-flex;
  align-items: center;
  gap: var(--ui-space-2);
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-muted);
}

.chat-streaming-hint {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  margin: 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

/* 工具调用卡片 */
.chat-tool-card {
  width: 100%;
  overflow: hidden;
  background: var(--ui-bg-surface);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  box-shadow: var(--ui-shadow-xs);
}

.chat-tool-head {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  width: 100%;
  padding: var(--ui-space-2) var(--ui-space-3);
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-primary);
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background var(--ui-duration-fast) var(--ui-ease-out);
}

.chat-tool-head:hover {
  background: var(--ui-bg-hover);
}

.chat-tool-head:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.chat-tool-head .ui-badge {
  margin-left: auto;
}

.chat-tool-caret,
.chat-tool-kind {
  color: var(--ui-fg-muted);
}

.chat-tool-name {
  font-family: var(--ui-font-mono);
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-accent);
}

.chat-tool-body {
  padding: var(--ui-space-2) var(--ui-space-3);
  border-top: 1px solid var(--ui-border-hairline);
  background: var(--ui-bg-sunken);
}

.chat-tool-input {
  margin: 0 0 var(--ui-space-2);
  padding: var(--ui-space-2);
  font-family: var(--ui-font-mono);
  font-size: var(--ui-text-xs);
  line-height: 1.6;
  color: var(--ui-fg-secondary);
  background: var(--ui-bg-surface);
  border: 1px solid var(--ui-border-hairline);
  border-radius: var(--ui-radius-xs);
  white-space: pre-wrap;
  word-break: break-all;
}

.chat-tool-summary {
  margin: 0;
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  word-break: break-word;
}
</style>
