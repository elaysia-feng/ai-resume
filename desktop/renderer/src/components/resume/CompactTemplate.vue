<template>
  <article
    class="resume-template resume-template--compact"
    :class="{ 'resume-template--compact-single': !hasSidebarContent }"
  >
    <header class="compact-header">
      <div class="compact-header__identity">
        <p class="compact-header__eyebrow">校招档案 / ENTRY PROFILE</p>
        <h1 class="compact-header__name">{{ basicProfile.name }}</h1>
        <p v-if="basicProfile.title" class="compact-header__title">{{ basicProfile.title }}</p>
      </div>
      <div v-if="basicProfile.contacts.length" class="compact-header__contacts">
        <span v-for="item in basicProfile.contacts" :key="item.field">
          <strong>{{ item.label }}</strong>{{ item.value }}
        </span>
      </div>
    </header>

    <div v-if="jobIntentItems.length" class="compact-intent">
      <span v-for="item in jobIntentItems" :key="item.field">
        <strong>{{ item.label }}</strong>{{ item.value }}
      </span>
    </div>

    <div class="compact-body">
      <aside v-if="layout.asideSections.length" class="compact-sidebar">
        <ResumeSection
          v-for="section in layout.asideSections"
          :key="section.id"
          :section="section"
          :locale="locale"
          compact
        />
      </aside>

      <main class="compact-main">
        <ResumeSection
          v-for="section in orderedMainSections"
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
      lead: ['SUMMARY', 'SELF_EVALUATION'],
      aside: ['EDUCATION', 'SKILLS', 'CERTIFICATES', 'LAC_CERTIFICATES'],
      main: [
        'INTERNSHIP',
        'PROJECTS',
        'CAMPUS',
        'EXPERIENCE',
      ],
    },
    props.locale,
  )
})

const basicProfile = computed(() => buildBasicProfile(layout.value.basic, props.locale))
const jobIntentItems = computed(() => buildJobIntentItems(layout.value.jobIntent, props.locale))
const hasSidebarContent = computed(() => layout.value.asideSections.length > 0)
const orderedMainSections = computed(() => [
  ...layout.value.leadSections,
  ...layout.value.mainSections,
  ...layout.value.remainingSections,
])
</script>

<style scoped>
.resume-template--compact {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: #14283f;
  --resume-color-text: #293b4f;
  --resume-color-text-soft: #526477;
  --resume-color-text-faint: #728092;
  --resume-color-accent: #c35b35;
  --resume-color-accent-soft: #fbefe9;
  --resume-color-border: #d9dfe5;
  --resume-color-surface: #f4f6f8;
  --resume-color-surface-strong: #edf1f4;
  --resume-font-name: 27px;
  --resume-font-title: 10px;
  --resume-font-contact: 8.5px;
  --resume-font-pill: 8.5px;
  --resume-section-title-size: 10px;
  --resume-text-size: 9px;
  min-height: 1123px;
  padding: 37px 42px 42px;
}

.compact-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 0.92fr);
  align-items: flex-end;
  gap: 22px;
  padding: 14px 0 15px;
  border-top: 4px solid var(--resume-color-heading);
  border-bottom: 1px solid var(--resume-color-border);
}

.compact-header__identity {
  min-width: 0;
}

.compact-header__eyebrow {
  margin: 0 0 7px;
  color: var(--resume-color-accent);
  font-size: 7.5px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.compact-header__name {
  margin: 0;
  color: var(--resume-color-heading);
  font-size: var(--resume-font-name);
  font-weight: 760;
  letter-spacing: -0.025em;
  line-height: 1.1;
}

.compact-header__title {
  margin: 6px 0 0;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-title);
  font-weight: 600;
}

.compact-header__contacts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-content: end;
  gap: 8px 12px;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-contact);
  line-height: 1.4;
}

.compact-header__contacts > span {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 1px;
  padding-left: 8px;
  border-left: 1px solid var(--resume-color-border);
  overflow-wrap: anywhere;
}

.compact-header__contacts strong {
  color: var(--resume-color-text-faint);
  font-size: 7px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.compact-intent {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(115px, 1fr));
  gap: 7px 14px;
  margin-top: 9px;
  padding: 9px 0 10px;
  border-top: 1px solid var(--resume-color-border);
  border-bottom: 1px solid var(--resume-color-border);
  color: var(--resume-color-text);
  font-size: var(--resume-font-contact);
}

.compact-intent > span {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.compact-intent strong {
  color: var(--resume-color-text-faint);
  font-size: 7px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.compact-body {
  display: grid;
  grid-template-columns: minmax(0, 0.28fr) minmax(0, 0.72fr);
  align-items: start;
  gap: 20px;
  margin-top: 17px;
}

.resume-template--compact-single .compact-body {
  grid-template-columns: minmax(0, 1fr);
}

.compact-sidebar {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 14px;
  padding: 13px 12px;
  border-right: 1px solid var(--resume-sidebar-border, var(--resume-color-border));
  border-top: 2px solid var(--resume-sidebar-accent, var(--resume-color-accent));
  background: var(--resume-sidebar-bg, var(--resume-color-surface));
}

.compact-sidebar :deep(.resume-section-block__head) {
  margin-bottom: 5px;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--resume-sidebar-divider, var(--resume-color-border));
}

.compact-sidebar :deep(.resume-section-block__title) {
  color: var(--resume-sidebar-heading, var(--resume-color-heading));
  font-size: 9px;
  letter-spacing: 0.04em;
}

.compact-sidebar :deep(.resume-entry) {
  padding: 0 0 7px;
  border: 0;
  border-bottom: 1px solid var(--resume-color-border);
  border-radius: 0;
  background: var(--resume-sidebar-panel-bg, transparent);
}

.compact-sidebar :deep(.resume-entry__title) {
  color: var(--resume-sidebar-heading, var(--resume-color-heading));
}

.compact-sidebar :deep(.resume-entry__subtitle),
.compact-sidebar :deep(.resume-entry__meta),
.compact-sidebar :deep(.resume-entry__date),
.compact-sidebar :deep(.resume-kv__label),
.compact-sidebar :deep(.resume-chip__meta) {
  color: var(--resume-sidebar-text-faint, var(--resume-color-text-faint));
}

.compact-sidebar :deep(.resume-rich-text),
.compact-sidebar :deep(.resume-entry__description),
.compact-sidebar :deep(.resume-entry__bullets),
.compact-sidebar :deep(.resume-kv__value),
.compact-sidebar :deep(.resume-bullet-list) {
  color: var(--resume-sidebar-text, var(--resume-color-text));
}

.compact-sidebar :deep(.resume-chip) {
  padding: 2px 5px;
  border-radius: 2px;
  border: 1px solid var(--resume-sidebar-panel-border, #d7e1ea);
  background: var(--resume-sidebar-chip-bg, #ffffff);
  color: var(--resume-sidebar-accent, var(--resume-color-heading));
}

.compact-main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 14px;
  counter-reset: compact-section;
}

.compact-main :deep(.resume-section-block) {
  counter-increment: compact-section;
}

.compact-main :deep(.resume-section-block__head) {
  margin-bottom: 7px;
  padding-bottom: 5px;
  border-bottom: 1px solid var(--resume-color-border);
}

.compact-main :deep(.resume-section-block__title) {
  display: flex;
  align-items: baseline;
  gap: 8px;
  color: var(--resume-color-heading);
  font-size: var(--resume-section-title-size);
  font-weight: 750;
  letter-spacing: 0.025em;
}

.compact-main :deep(.resume-section-block__title)::before {
  content: counter(compact-section, decimal-leading-zero);
  color: var(--resume-color-accent);
  font-size: 8px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.04em;
}

.compact-main :deep(.resume-entry) {
  padding: 0 0 9px;
  border: 0;
  border-bottom: 1px solid var(--resume-color-border);
  border-radius: 0;
  background: transparent;
}

.compact-main :deep(.resume-entry__header) {
  align-items: baseline;
}

.compact-main :deep(.resume-entry__title) {
  color: var(--resume-color-heading);
  font-size: 10.5px;
  font-weight: 700;
}

.compact-main :deep(.resume-entry__date) {
  color: var(--resume-color-accent);
  font-size: 8.5px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.compact-main :deep(.resume-entry__description),
.compact-main :deep(.resume-entry__bullets) {
  line-height: 1.48;
}

.compact-main :deep(.resume-chip) {
  padding: 2px 6px;
  border: 1px solid var(--resume-color-border);
  border-radius: 2px;
  background: var(--ui-neutral-0);
  color: var(--resume-color-text-soft);
}

@media print {
  .resume-template--compact {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
