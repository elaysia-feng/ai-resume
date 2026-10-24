<template>
  <article
    class="resume-template resume-template--creative"
    :class="{ 'resume-template--creative-single': !hasSidebarContent }"
  >
    <header class="creative-hero">
      <div class="creative-hero__copy">
        <p class="creative-hero__eyebrow">{{ locale === 'en' ? 'Professional Resume' : '职业简历' }}</p>
        <h1 class="creative-hero__name">{{ basicProfile.name }}</h1>
        <p v-if="basicProfile.title" class="creative-hero__title">{{ basicProfile.title }}</p>

        <div v-if="basicProfile.contacts.length" class="creative-hero__contacts">
          <span
            v-for="item in basicProfile.contacts"
            :key="item.field"
            class="creative-hero__contact"
          >
            <span class="creative-hero__contact-label">{{ item.label }}</span>
            <span>{{ item.value }}</span>
          </span>
        </div>

        <div v-if="jobIntentItems.length" class="resume-meta-pills creative-hero__pills">
          <span v-for="item in jobIntentItems" :key="item.field" class="resume-meta-pill">
            <span class="resume-meta-pill__label">{{ item.label }}</span>
            <span>{{ item.value }}</span>
          </span>
        </div>
      </div>

      <img
        v-if="basicProfile.avatar"
        :src="basicProfile.avatar"
        alt="avatar"
        class="resume-basic-header__avatar creative-hero__avatar"
      />
    </header>

    <div class="creative-body">
      <aside v-if="hasSidebarContent" class="creative-sidebar">
        <ResumeSection
          v-for="section in layout.asideSections"
          :key="section.id"
          :section="section"
          :locale="locale"
          compact
        />
      </aside>

      <main class="creative-main">
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
      aside: ['SELF_EVALUATION', 'SKILLS', 'CERTIFICATES', 'LAC_CERTIFICATES'],
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
   创意风：强调色块抬头 + 竖向分割线 + 模块卡片
   强调色只做「色块」，正文仍是深色文字，
   打印时即使底色丢失也不会影响可读性。
   ======================================== */
.resume-template--creative {
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

  --resume-font-name: 28px;
  --resume-font-title: 13px;
  --resume-font-contact: 10.5px;
  --resume-font-pill: 10px;
  --resume-section-title-size: 11px;

  min-height: 1123px;
}

/* ---------- 抬头：整块强调色带 ---------- */
.creative-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--ui-space-6);
  padding: 32px 38px 28px;
  background: var(--resume-color-accent-soft);
  border-left: 8px solid var(--resume-color-accent);
  border-bottom: 1px solid var(--resume-color-border);
}

.creative-hero__copy {
  min-width: 0;
  flex: 1;
}

.creative-hero__eyebrow {
  margin: 0 0 10px;
  color: var(--resume-color-accent);
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.2em;
}

.creative-hero__name {
  margin: 0;
  color: var(--resume-color-heading);
  font-family: var(--resume-font-display);
  font-size: var(--resume-font-name);
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: 0.01em;
}

.creative-hero__title {
  margin: 8px 0 0;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-title);
  font-weight: 600;
}

.creative-hero__contacts {
  display: flex;
  flex-wrap: wrap;
  gap: 4px var(--ui-space-4);
  margin-top: 14px;
}

.creative-hero__contact {
  display: inline-flex;
  align-items: baseline;
  gap: 5px;
  font-size: var(--resume-font-contact);
  color: var(--resume-color-text);
}

.creative-hero__contact-label {
  color: var(--resume-color-text-faint);
  font-weight: 600;
}

.creative-hero__pills {
  margin-top: 14px;
}

.creative-hero__pills :deep(.resume-meta-pill) {
  background: var(--resume-color-page);
  color: var(--resume-color-accent);
  border: 1px solid var(--resume-color-border);
}

.creative-hero__avatar {
  width: 84px;
  height: 84px;
  border-radius: var(--ui-radius-lg);
  border: 1px solid var(--resume-color-border);
  background: var(--resume-color-page);
}

/* ---------- 正文：左栏 + 强调色分割线 + 右栏 ---------- */
.creative-body {
  display: grid;
  grid-template-columns: 196px minmax(0, 1fr);
  gap: var(--ui-space-6);
  padding: 26px 32px 32px;
}

.resume-template--creative-single .creative-body {
  grid-template-columns: minmax(0, 1fr);
}

.creative-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-right: var(--ui-space-5);
  border-right: 3px solid var(--resume-color-accent);
  background: var(--resume-sidebar-bg, transparent);
}

.creative-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 模块做成独立卡片，标题是强调色块 */
.resume-template--creative :deep(.resume-section-block) {
  padding: 13px 15px;
  border: 1px solid var(--resume-color-border);
  border-radius: var(--ui-radius-lg);
  background: var(--resume-color-page);
}

.resume-template--creative :deep(.resume-section-block__head) {
  padding-bottom: 7px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--creative :deep(.resume-section-block__title) {
  padding: 2px 8px;
  border-radius: var(--ui-radius-xs);
  background: var(--resume-color-accent-soft);
  color: var(--resume-color-accent);
  letter-spacing: 0.08em;
}

.resume-template--creative :deep(.resume-entry) {
  padding-bottom: 10px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--creative :deep(.resume-entry:last-child) {
  padding-bottom: 0;
  border-bottom: none;
}

/* 左栏内的文字跟随「左栏配色」设置 */
.creative-sidebar :deep(.resume-section-block__head) {
  border-bottom-color: var(--resume-sidebar-divider, var(--resume-color-border));
}

.creative-sidebar :deep(.resume-section-block__title) {
  background: var(--resume-sidebar-chip-bg, var(--resume-color-accent-soft));
  color: var(--resume-sidebar-accent, var(--resume-color-accent));
}

.creative-sidebar :deep(.resume-entry),
.creative-sidebar :deep(.resume-kv) {
  background: var(--resume-sidebar-panel-bg, transparent);
  border-color: var(--resume-sidebar-panel-border, transparent);
}

.creative-sidebar :deep(.resume-section-block__title),
.creative-sidebar :deep(.resume-entry__title) {
  color: var(--resume-sidebar-accent, var(--resume-color-accent));
}

.creative-sidebar :deep(.resume-entry__subtitle),
.creative-sidebar :deep(.resume-entry__meta),
.creative-sidebar :deep(.resume-entry__date),
.creative-sidebar :deep(.resume-kv__label),
.creative-sidebar :deep(.resume-chip__meta) {
  color: var(--resume-sidebar-text-faint, var(--resume-color-text-faint));
}

.creative-sidebar :deep(.resume-rich-text),
.creative-sidebar :deep(.resume-entry__description),
.creative-sidebar :deep(.resume-entry__bullets),
.creative-sidebar :deep(.resume-kv__value),
.creative-sidebar :deep(.resume-bullet-list) {
  color: var(--resume-sidebar-text, var(--resume-color-text));
}

.creative-sidebar :deep(.resume-chip) {
  background: var(--resume-sidebar-chip-bg, var(--resume-color-surface-strong));
  color: var(--resume-sidebar-accent, var(--resume-color-accent));
}

/* 打印时保留强调色块 */
@media print {
  .resume-template--creative {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
