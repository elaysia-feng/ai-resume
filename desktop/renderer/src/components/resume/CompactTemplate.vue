<template>
  <article
    class="resume-template resume-template--compact"
    :class="{ 'resume-template--compact-single': !hasSidebarContent }"
  >
    <aside v-if="hasSidebarContent" class="compact-sidebar">
      <section class="compact-profile">
        <div v-if="basicProfile.avatar" class="compact-profile__avatar-wrap">
          <img :src="basicProfile.avatar" alt="avatar" class="resume-basic-header__avatar compact-profile__avatar" />
        </div>

        <div class="compact-profile__identity">
          <h1 class="compact-profile__name">{{ basicProfile.name }}</h1>
          <p v-if="basicProfile.title" class="compact-profile__title">{{ basicProfile.title }}</p>
        </div>
      </section>

      <section v-if="jobIntentItems.length" class="compact-sidebar__panel">
        <h2 class="compact-sidebar__panel-title">{{ locale === 'en' ? 'Target' : '求职意向' }}</h2>
        <div class="compact-intent-list">
          <div v-for="item in jobIntentItems" :key="item.field" class="compact-intent-item">
            <span class="compact-intent-item__label">{{ item.label }}</span>
            <span class="compact-intent-item__value">{{ item.value }}</span>
          </div>
        </div>
      </section>

      <section v-if="basicProfile.contacts.length" class="compact-sidebar__panel">
        <h2 class="compact-sidebar__panel-title">{{ locale === 'en' ? 'Contact' : '联系方式' }}</h2>
        <div class="compact-contact-list">
          <div v-for="item in basicProfile.contacts" :key="item.field" class="compact-contact-item">
            <span class="compact-contact-item__label">{{ item.label }}</span>
            <span class="compact-contact-item__value">{{ item.value }}</span>
          </div>
        </div>
      </section>

      <div class="compact-sidebar__sections">
        <ResumeSection
          v-for="section in layout.asideSections"
          :key="section.id"
          :section="section"
          :locale="locale"
          compact
        />
      </div>
    </aside>

    <main class="compact-main">
      <header v-if="!hasSidebarContent" class="compact-hero">
        <div class="compact-hero__intro">
          <h1 class="compact-hero__name">{{ basicProfile.name }}</h1>
          <p v-if="basicProfile.title" class="compact-hero__title">{{ basicProfile.title }}</p>
          <div v-if="!hasSidebarContent && basicProfile.contacts.length" class="resume-basic-header__contacts compact-hero__contacts">
            <span
              v-for="item in basicProfile.contacts"
              :key="item.field"
              class="resume-basic-header__contact"
            >
              <span class="resume-basic-header__contact-label">{{ item.label }}</span>
              <span>{{ item.value }}</span>
            </span>
          </div>
        </div>

        <div v-if="jobIntentItems.length" class="resume-meta-pills compact-hero__pills">
          <span v-for="item in jobIntentItems" :key="item.field" class="resume-meta-pill">
            <span class="resume-meta-pill__label">{{ item.label }}</span>
            <span>{{ item.value }}</span>
          </span>
        </div>
      </header>

      <div class="compact-main__sections" :class="{ 'compact-main__sections--with-header': !hasSidebarContent }">
        <ResumeSection
          v-for="section in orderedSections"
          :key="section.id"
          :section="section"
          :locale="locale"
        />
      </div>
    </main>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import ResumeSection from './ResumeSection.vue'
import { buildBasicProfile, buildJobIntentItems, buildTemplateLayout, formatNaturalText } from '../../utils/resumeTemplate.js'

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
const rawBasicContent = computed(() => layout.value.basic?.content || {})
const hasSidebarContent = computed(() => {
  return Boolean(
    basicProfile.value.avatar
    || basicProfile.value.contacts.length
    || jobIntentItems.value.length
    || layout.value.asideSections.length
    || formatNaturalText(rawBasicContent.value.name)
    || formatNaturalText(rawBasicContent.value.title),
  )
})
const orderedSections = computed(() => {
  return [
    ...layout.value.leadSections,
    ...layout.value.mainSections,
    ...layout.value.remainingSections,
  ]
})
</script>

<style scoped>
.resume-template--compact {
  --resume-color-accent: #6d28d9;
  --resume-color-accent-soft: rgba(109, 40, 217, 0.12);
  --resume-color-border: #ddd6fe;
  --resume-color-surface: #faf5ff;
  --resume-color-surface-strong: #f3e8ff;
  --resume-color-heading: #3b0764;
  --resume-color-text: #1e1b4b;
  --resume-color-text-soft: #5b21b6;
  --resume-color-text-faint: #7c3aed;
  display: grid;
  --resume-font-name: 20px;
  --resume-font-title: 10px;
  --resume-font-contact: 8.5px;
  --resume-font-pill: 8.5px;
  grid-template-columns: 190px minmax(0, 1fr);
  min-height: 1123px;
}

.resume-template--compact-single {
  grid-template-columns: 1fr;
}

.compact-sidebar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 22px 14px 24px;
  background: var(--resume-sidebar-bg);
  border-right: 1px solid var(--resume-sidebar-border);
}

.compact-profile {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.compact-profile__avatar-wrap {
  display: flex;
  justify-content: flex-start;
}

.compact-profile__avatar {
  width: 60px;
  height: 60px;
  border-radius: 10px;
}

.compact-profile__identity {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.compact-profile__name {
  margin: 0;
  color: var(--resume-sidebar-heading);
  font-size: 19px;
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: 0.01em;
}

.compact-profile__title {
  margin: 0;
  color: var(--resume-sidebar-text-soft);
  font-size: 10px;
  font-weight: 600;
  line-height: 1.4;
}

.compact-sidebar__panel {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 9px;
  border-radius: 10px;
  background: var(--resume-sidebar-panel-bg);
  border: 1px solid var(--resume-sidebar-panel-border);
}

.compact-sidebar__panel-title {
  margin: 0;
  color: var(--resume-sidebar-heading);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.compact-intent-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.compact-intent-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.compact-intent-item__label {
  color: var(--resume-sidebar-text-faint);
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.compact-intent-item__value {
  color: var(--resume-sidebar-accent);
  font-size: 9px;
  font-weight: 700;
  line-height: 1.4;
}

.compact-contact-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.compact-contact-item {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.compact-contact-item__label {
  color: var(--resume-sidebar-text-faint);
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.compact-contact-item__value {
  color: var(--resume-sidebar-text);
  font-size: 9px;
  line-height: 1.5;
}

.compact-sidebar__sections {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.compact-main {
  padding: 24px 22px 28px;
}

.compact-hero {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--resume-color-border);
}

.compact-hero__name {
  margin: 0;
  color: var(--resume-color-heading);
  font-size: 20px;
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: 0.02em;
}

.compact-hero__title {
  margin: 6px 0 0;
  color: var(--resume-color-text-soft);
  font-size: 11px;
  font-weight: 600;
}

.compact-hero__contacts {
  margin-top: 12px;
}

.compact-main__sections {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.compact-main__sections--with-header {
  margin-top: 12px;
}

.resume-template--compact :deep(.resume-section-block__head) {
  padding-bottom: 6px;
  border-bottom: 1px solid var(--resume-color-border);
}

.compact-sidebar :deep(.resume-section-block__head) {
  border-bottom-color: var(--resume-sidebar-divider);
}

.compact-sidebar :deep(.resume-entry),
.compact-sidebar :deep(.resume-kv) {
  background: var(--resume-sidebar-panel-bg);
  border-color: var(--resume-sidebar-panel-border);
}

.compact-sidebar :deep(.resume-section-block__title),
.compact-sidebar :deep(.resume-entry__title) {
  color: var(--resume-sidebar-heading);
}

.compact-sidebar :deep(.resume-entry__subtitle),
.compact-sidebar :deep(.resume-entry__meta),
.compact-sidebar :deep(.resume-entry__date),
.compact-sidebar :deep(.resume-kv__label),
.compact-sidebar :deep(.resume-chip__meta) {
  color: var(--resume-sidebar-text-faint);
}

.compact-sidebar :deep(.resume-rich-text),
.compact-sidebar :deep(.resume-entry__description),
.compact-sidebar :deep(.resume-entry__bullets),
.compact-sidebar :deep(.resume-kv__value),
.compact-sidebar :deep(.resume-bullet-list) {
  color: var(--resume-sidebar-text);
}

.compact-sidebar :deep(.resume-chip) {
  background: var(--resume-sidebar-chip-bg);
  color: var(--resume-sidebar-accent);
}

.resume-template--compact :deep(.resume-entry) {
  padding: 6px 8px;
  border-radius: 6px;
  background: var(--resume-color-surface);
  border: 1px solid rgba(109, 40, 217, 0.06);
}

.resume-template--compact :deep(.resume-entry__title) {
  letter-spacing: 0;
}

.resume-template--compact :deep(.resume-section-block--compact .resume-entry) {
  padding: 5px 7px;
  border-radius: 6px;
}

.resume-template--compact :deep(.resume-section-block--compact .resume-section-block__head) {
  padding-bottom: 5px;
}
</style>
