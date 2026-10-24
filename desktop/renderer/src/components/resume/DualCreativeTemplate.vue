<template>
  <article
    class="resume-template resume-template--dual-creative"
    :class="{ 'resume-template--dual-creative-single': !hasSidebarContent }"
  >
    <header class="dual-creative-hero">
      <div class="dual-creative-hero__copy">
        <h1 class="dual-creative-hero__name">{{ basicProfile.name }}</h1>
        <p v-if="basicProfile.title" class="dual-creative-hero__title">{{ basicProfile.title }}</p>
      </div>

      <div v-if="basicProfile.contacts.length" class="dual-creative-hero__contacts">
        <span
          v-for="item in basicProfile.contacts"
          :key="item.field"
          class="dual-creative-hero__contact"
        >
          <span class="dual-creative-hero__contact-label">{{ item.label }}</span>
          <span>{{ item.value }}</span>
        </span>
      </div>

      <img
        v-if="basicProfile.avatar"
        :src="basicProfile.avatar"
        alt="avatar"
        class="resume-basic-header__avatar dual-creative-hero__avatar"
      />
    </header>

    <div v-if="jobIntentItems.length" class="dual-creative-pills">
      <span v-for="item in jobIntentItems" :key="item.field" class="resume-meta-pill dual-creative-pill">
        <span class="resume-meta-pill__label">{{ item.label }}</span>
        <span>{{ item.value }}</span>
      </span>
    </div>

    <div class="dual-creative-body">
      <aside v-if="hasSidebarContent" class="dual-creative-sidebar">
        <ResumeSection
          v-for="section in layout.asideSections"
          :key="section.id"
          :section="section"
          :locale="locale"
          compact
        />
      </aside>

      <main class="dual-creative-main">
        <ResumeSection
          v-for="section in orderedSections"
          :key="section.id"
          :section="section"
          :locale="locale"
        />
      </main>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import ResumeSection from './ResumeSection.vue'
import { buildBasicProfile, buildJobIntentItems, buildTemplateLayout } from '../../utils/resumeTemplate.js'

const props = defineProps({
  sections: {
    type: Array,
    required: true,
  },
  locale: {
    type: String,
    default: 'zh',
  },
})

const layout = computed(() => {
  return buildTemplateLayout(
    props.sections,
    {
      aside: ['SKILLS', 'CERTIFICATES', 'LAC_CERTIFICATES', 'SELF_EVALUATION'],
      main: [
        'SUMMARY',
        'EXPERIENCE',
        'PROJECTS',
        'INTERNSHIP',
        'EDUCATION',
        'CAMPUS',
      ],
    },
    props.locale,
  )
})

const basicProfile = computed(() => buildBasicProfile(layout.value.basic, props.locale))
const jobIntentItems = computed(() => buildJobIntentItems(layout.value.jobIntent, props.locale))
const hasSidebarContent = computed(() => layout.value.asideSections.length > 0)
const orderedSections = computed(() => {
  return [
    ...layout.value.leadSections,
    ...layout.value.mainSections,
    ...layout.value.remainingSections,
  ]
})
</script>

<style scoped>
/* ========================================
   双栏创意版：等宽左右分栏 + 中缝分隔线 + 横向抬头带
   左栏放技能证书，右栏放经历主线。
   ======================================== */
.resume-template--dual-creative {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: var(--ui-neutral-900);
  --resume-color-text: var(--ui-neutral-700);
  --resume-color-text-soft: var(--ui-neutral-600);
  --resume-color-text-faint: var(--ui-neutral-500);
  --resume-color-accent: var(--ui-brand-700);
  --resume-color-accent-soft: var(--ui-brand-50);
  --resume-color-border: var(--ui-neutral-200);
  --resume-color-surface: var(--ui-neutral-50);
  --resume-color-surface-strong: var(--ui-brand-50);

  --resume-font-name: 27px;
  --resume-font-title: 12.5px;
  --resume-font-contact: 10px;
  --resume-font-pill: 9.5px;
  --resume-section-title-size: 11px;

  min-height: 1123px;
}

/* ---------- 抬头：横向色带 ---------- */
.dual-creative-hero {
  display: flex;
  align-items: center;
  gap: var(--ui-space-6);
  padding: 26px 36px 22px;
  background: var(--resume-color-accent-soft);
  border-bottom: 4px solid var(--resume-color-accent);
}

.dual-creative-hero__copy {
  min-width: 0;
  flex: 1;
}

.dual-creative-hero__name {
  margin: 0;
  color: var(--resume-color-heading);
  font-family: var(--resume-font-display);
  font-size: var(--resume-font-name);
  font-weight: 700;
  line-height: 1.1;
}

.dual-creative-hero__title {
  margin: 6px 0 0;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-title);
  font-weight: 600;
}

.dual-creative-hero__contacts {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 4px var(--ui-space-4);
  max-width: 300px;
}

.dual-creative-hero__contact {
  display: inline-flex;
  align-items: baseline;
  gap: 5px;
  font-size: var(--resume-font-contact);
  color: var(--resume-color-text);
}

.dual-creative-hero__contact-label {
  color: var(--resume-color-text-faint);
  font-weight: 600;
}

.dual-creative-hero__avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 1px solid var(--resume-color-border);
  background: var(--resume-color-page);
}

/* ---------- 求职意向 ---------- */
.dual-creative-pills {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ui-space-2);
  padding: 10px 36px;
  border-bottom: 1px solid var(--resume-color-border);
}

.dual-creative-pill {
  background: var(--resume-color-accent-soft);
  color: var(--resume-color-accent);
  border: 1px solid var(--resume-color-border);
}

/* ---------- 正文：等宽双栏 ---------- */
.dual-creative-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--ui-space-6);
  padding: 22px 36px 30px;
}

.resume-template--dual-creative-single .dual-creative-body {
  grid-template-columns: minmax(0, 1fr);
}

.dual-creative-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-right: var(--ui-space-5);
  border-right: 1px solid var(--resume-color-border);
  background: var(--resume-sidebar-bg, transparent);
}

.dual-creative-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 模块标题：标题前一个强调色小方块 */
.resume-template--dual-creative :deep(.resume-section-block__title) {
  display: flex;
  align-items: center;
  gap: 7px;
  letter-spacing: 0.08em;
}

.resume-template--dual-creative :deep(.resume-section-block__title)::before {
  content: '';
  flex-shrink: 0;
  width: 6px;
  height: 6px;
  border-radius: 1px;
  background: var(--resume-color-accent);
}

.resume-template--dual-creative :deep(.resume-section-block__head) {
  padding-bottom: 6px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--dual-creative :deep(.resume-entry) {
  padding-bottom: 10px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--dual-creative :deep(.resume-entry:last-child) {
  padding-bottom: 0;
  border-bottom: none;
}

/* 左栏内的文字跟随「左栏配色」设置 */
.dual-creative-sidebar :deep(.resume-section-block__head) {
  border-bottom-color: var(--resume-sidebar-divider, var(--resume-color-border));
}

.dual-creative-sidebar :deep(.resume-section-block__title)::before {
  background: var(--resume-sidebar-accent, var(--resume-color-accent));
}

.dual-creative-sidebar :deep(.resume-section-block__title),
.dual-creative-sidebar :deep(.resume-entry__title) {
  color: var(--resume-sidebar-heading, var(--resume-color-heading));
}

.dual-creative-sidebar :deep(.resume-entry),
.dual-creative-sidebar :deep(.resume-kv) {
  background: var(--resume-sidebar-panel-bg, transparent);
  border-color: var(--resume-sidebar-panel-border, transparent);
}

.dual-creative-sidebar :deep(.resume-entry__subtitle),
.dual-creative-sidebar :deep(.resume-entry__meta),
.dual-creative-sidebar :deep(.resume-entry__date),
.dual-creative-sidebar :deep(.resume-kv__label),
.dual-creative-sidebar :deep(.resume-chip__meta) {
  color: var(--resume-sidebar-text-faint, var(--resume-color-text-faint));
}

.dual-creative-sidebar :deep(.resume-rich-text),
.dual-creative-sidebar :deep(.resume-entry__description),
.dual-creative-sidebar :deep(.resume-entry__bullets),
.dual-creative-sidebar :deep(.resume-kv__value),
.dual-creative-sidebar :deep(.resume-bullet-list) {
  color: var(--resume-sidebar-text, var(--resume-color-text));
}

.dual-creative-sidebar :deep(.resume-chip) {
  background: var(--resume-sidebar-chip-bg, var(--resume-color-surface-strong));
  color: var(--resume-sidebar-accent, var(--resume-color-accent));
}

/* 打印时保留抬头色带 */
@media print {
  .resume-template--dual-creative {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
