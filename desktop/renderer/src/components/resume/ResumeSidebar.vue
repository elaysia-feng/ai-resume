<template>
  <nav class="resume-rail" aria-label="简历结构导航">
    <header class="resume-rail__head">
      <span class="resume-rail__title">简历结构</span>
      <button
        type="button"
        class="resume-rail__collapse"
        :aria-expanded="groupsOpen.sections"
        title="折叠 / 展开区块列表"
        @click="groupsOpen.sections = !groupsOpen.sections"
      >
        <AppIcon :name="groupsOpen.sections ? 'caret-down' : 'caret-right'" :size="16" />
      </button>
    </header>

    <div class="resume-rail__scroll ui-scroll">
      <section class="resume-rail__group">
        <span class="resume-rail__group-title">区块</span>

        <ul v-show="groupsOpen.sections" class="resume-rail__list">
          <li
            v-for="section in sections"
            :key="section.id"
            class="resume-rail__item"
            :class="{
              'is-active': isActiveSection(section),
              'is-drag-over': dragOverId === section.id,
              'is-hidden': section.visible === false,
            }"
            draggable="true"
            @dragstart="onDragStart(section.id, $event)"
            @dragover.prevent="onDragOver(section.id)"
            @dragleave="dragOverId = null"
            @drop.prevent="onDrop(section.id)"
            @dragend="onDragEnd"
          >
            <span class="resume-rail__handle" title="拖拽调整顺序" aria-hidden="true">
              <AppIcon name="dots-three-vertical" :size="14" />
            </span>

            <button
              type="button"
              class="resume-rail__link"
              :aria-current="isActiveSection(section) ? 'true' : undefined"
              @click="emit('select', section.id)"
            >
              <span class="resume-rail__name">{{ section.sectionTitle || section.sectionCode }}</span>
              <span class="resume-rail__meta" :class="{ 'is-empty': !summary(section).filled }">
                {{ summary(section).text }}
              </span>
            </button>
          </li>
        </ul>

        <div v-if="!sections.length" class="ui-empty resume-rail__empty">
          <div class="ui-empty__title">还没有简历区块</div>
          <div class="ui-empty__hint">添加基本信息、教育、实习等区块后，这里会显示顺序与完成度。</div>
        </div>
      </section>

      <section class="resume-rail__group">
        <span class="resume-rail__group-title">工具</span>
        <ul class="resume-rail__list">
          <li v-for="btn in buttons" :key="btn.event">
            <button
              type="button"
              class="resume-rail__tool"
              :class="{ 'is-active': activeBtn === btn.event }"
              @click="handleClick(btn.event)"
            >
              <span class="resume-rail__tool-icon" aria-hidden="true">
                <AppIcon :name="btn.icon" :size="16" />
              </span>
              <span class="resume-rail__tool-label">{{ btn.label }}</span>
            </button>
          </li>
        </ul>
      </section>
    </div>

    <footer class="resume-rail__foot">
      <button type="button" class="ui-btn ui-btn--sm ui-btn--outline ui-btn--block" @click="handleClick('add-block')">
        添加区块
      </button>
    </footer>
  </nav>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import AppIcon from '../common/AppIcon.vue'

const props = defineProps({
  activeBtn: {
    type: String,
    default: null,
  },
  // 简历区块列表（可选）：传入后展示顺序、完成度并支持拖拽排序
  sections: {
    type: Array,
    default: () => [],
  },
  // 当前选中的区块 id（可选）
  activeSectionId: {
    type: [String, Number],
    default: null,
  },
})

const emit = defineEmits(['template', 'style', 'settings', 'add-block', 'preview', 'select', 'reorder'])

const groupsOpen = reactive({ sections: true })
const draggedId = ref(null)
const dragOverId = ref(null)

const buttons = [
  {
    event: 'template',
    label: '模板',
    icon: 'layout',
  },
  {
    event: 'style',
    label: '样式',
    icon: 'palette',
  },
  {
    event: 'preview',
    label: '预览',
    icon: 'eye',
  },
  {
    event: 'settings',
    label: '设置',
    icon: 'gear',
  },
]

const sections = computed(() => (Array.isArray(props.sections) ? props.sections : []))

function handleClick(event) {
  emit(event)
}

function isActiveSection(section) {
  if (props.activeSectionId === null || props.activeSectionId === undefined) {
    return false
  }
  return String(props.activeSectionId) === String(section.id)
}

function countText(value) {
  if (value === null || value === undefined) return 0
  if (typeof value === 'string') {
    return value.replace(/<[^>]*>/g, '').trim().length
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value).length
  }
  if (typeof value === 'object') {
    return JSON.stringify(value).length
  }
  return 0
}

function summary(section) {
  const content = section?.contentJson || {}

  if (Array.isArray(content.items)) {
    const chars = content.items.reduce((sum, item) => sum + countText(item), 0)
    return {
      filled: content.items.length > 0,
      text: content.items.length ? `${content.items.length} 条 · ${chars} 字` : '暂无条目',
    }
  }

  if (typeof content.text === 'string') {
    const chars = countText(content.text)
    return { filled: chars > 0, text: chars ? `${chars} 字` : '未填写' }
  }

  const chars = countText(content)
  return { filled: chars > 0, text: chars ? `${chars} 字` : '未填写' }
}

function onDragStart(id, event) {
  draggedId.value = id
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', String(id))
}

function onDragOver(id) {
  if (draggedId.value && draggedId.value !== id) {
    dragOverId.value = id
  }
}

function onDrop(targetId) {
  if (!draggedId.value || draggedId.value === targetId) return
  emit('reorder', { fromId: draggedId.value, toId: targetId })
  dragOverId.value = null
}

function onDragEnd() {
  draggedId.value = null
  dragOverId.value = null
}
</script>

<style scoped>
.resume-rail {
  display: flex;
  flex-direction: column;
  width: 220px;
  min-height: 100%;
  border-right: 1px solid var(--ui-border);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-primary);
}

.resume-rail__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
  padding: var(--ui-space-3) var(--ui-space-4);
  border-bottom: 1px solid var(--ui-border);
}

.resume-rail__title {
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.resume-rail__collapse {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: var(--ui-radius-sm);
  background: transparent;
  color: var(--ui-fg-secondary);
  cursor: pointer;
  transition: background var(--ui-duration-fast) var(--ui-ease-out);
}

.resume-rail__collapse:hover {
  background: var(--ui-bg-hover);
  color: var(--ui-fg-primary);
}

.resume-rail__collapse:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.resume-rail__scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: var(--ui-space-3) var(--ui-space-2) var(--ui-space-4);
}

.resume-rail__group + .resume-rail__group {
  margin-top: var(--ui-space-5);
}

.resume-rail__group-title {
  display: block;
  padding: 0 var(--ui-space-2) var(--ui-space-2);
  font-size: var(--ui-text-xs);
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ui-fg-muted);
}

.resume-rail__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.resume-rail__item {
  display: flex;
  align-items: center;
  gap: var(--ui-space-1);
  border-radius: var(--ui-radius-md);
  transition: background var(--ui-duration-fast) var(--ui-ease-out);
}

.resume-rail__item:hover {
  background: var(--ui-bg-hover);
}

.resume-rail__item.is-drag-over {
  box-shadow: inset 0 2px 0 0 var(--ui-accent);
}

.resume-rail__item.is-hidden .resume-rail__name {
  color: var(--ui-fg-muted);
  text-decoration: line-through;
}

.resume-rail__handle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  flex-shrink: 0;
  color: var(--ui-fg-muted);
  cursor: grab;
  opacity: 0;
  transition: opacity var(--ui-duration-fast) var(--ui-ease-out);
}

.resume-rail__item:hover .resume-rail__handle {
  opacity: 1;
}

.resume-rail__link {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 1px;
  padding: var(--ui-space-2);
  border: 0;
  border-radius: var(--ui-radius-md);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.resume-rail__link:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.resume-rail__item.is-active {
  background: var(--ui-bg-selected);
}

.resume-rail__name {
  font-size: var(--ui-text-sm);
  font-weight: 500;
  color: var(--ui-fg-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.resume-rail__item.is-active .resume-rail__name {
  color: var(--ui-fg-accent);
  font-weight: 600;
}

.resume-rail__meta {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
  font-variant-numeric: tabular-nums;
}

.resume-rail__meta.is-empty {
  color: var(--ui-warning);
}

.resume-rail__empty {
  padding: var(--ui-space-6) var(--ui-space-3);
}

.resume-rail__empty :deep(.ui-empty__title) {
  font-size: var(--ui-text-sm);
}

.resume-rail__empty :deep(.ui-empty__hint) {
  font-size: var(--ui-text-xs);
}

/* ---------- 工具按钮 ---------- */
.resume-rail__tool {
  display: flex;
  align-items: center;
  gap: var(--ui-space-2);
  width: 100%;
  padding: var(--ui-space-2);
  border: 0;
  border-radius: var(--ui-radius-md);
  background: transparent;
  color: var(--ui-fg-secondary);
  font-size: var(--ui-text-sm);
  font-weight: 500;
  cursor: pointer;
  text-align: left;
  transition: background var(--ui-duration-fast) var(--ui-ease-out),
    color var(--ui-duration-fast) var(--ui-ease-out);
}

.resume-rail__tool:hover {
  background: var(--ui-bg-hover);
  color: var(--ui-fg-primary);
}

.resume-rail__tool:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.resume-rail__tool.is-active {
  background: var(--ui-bg-selected);
  color: var(--ui-fg-accent);
}

.resume-rail__tool-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  flex-shrink: 0;
}

/* ---------- 底部 ---------- */
.resume-rail__foot {
  padding: var(--ui-space-3) var(--ui-space-4);
  border-top: 1px solid var(--ui-border);
}
</style>
