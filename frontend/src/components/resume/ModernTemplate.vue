<template>
  <article
    class="resume-template resume-template--modern"
    :class="{ 'resume-template--modern-single': !hasSidebarContent }"
  >
    <aside v-if="hasSidebarContent" class="modern-sidebar">
      <section class="modern-profile">
        <div v-if="basicProfile.avatar" class="modern-profile__avatar-wrap">
          <img :src="basicProfile.avatar" alt="avatar" class="resume-basic-header__avatar modern-profile__avatar" />
        </div>

        <div class="modern-profile__identity">
          <h1 class="modern-profile__name">{{ basicProfile.name }}</h1>
          <p v-if="basicProfile.title" class="modern-profile__title">{{ basicProfile.title }}</p>
        </div>
      </section>

      <section v-if="jobIntentItems.length" class="modern-sidebar__panel">
        <h2 class="modern-sidebar__panel-title">{{ locale === 'en' ? 'Target' : '求职意向' }}</h2>
        <div class="modern-intent-list">
          <div v-for="item in jobIntentItems" :key="item.field" class="modern-intent-item">
            <span class="modern-intent-item__label">{{ item.label }}</span>
            <span class="modern-intent-item__value">{{ item.value }}</span>
          </div>
        </div>
      </section>

      <section v-if="basicProfile.contacts.length" class="modern-sidebar__panel">
        <h2 class="modern-sidebar__panel-title">{{ locale === 'en' ? 'Contact' : '联系方式' }}</h2>
        <div class="modern-contact-list">
          <div v-for="item in basicProfile.contacts" :key="item.field" class="modern-contact-item">
            <span class="modern-contact-item__label">{{ item.label }}</span>
            <span class="modern-contact-item__value">{{ item.value }}</span>
          </div>
        </div>
      </section>

      <div class="modern-sidebar__sections">
        <ResumeSection
          v-for="section in layout.asideSections"
          :key="section.id"
          :section="section"
          :locale="locale"
          compact
        />
      </div>
    </aside>

    <main class="modern-main">
      <header v-if="!hasSidebarContent" class="modern-hero">
        <div class="modern-hero__intro">
          <h1 class="modern-hero__name">{{ basicProfile.name }}</h1>
          <p v-if="basicProfile.title" class="modern-hero__title">{{ basicProfile.title }}</p>
          <div v-if="!hasSidebarContent && basicProfile.contacts.length" class="resume-basic-header__contacts modern-hero__contacts">
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

        <div v-if="jobIntentItems.length" class="resume-meta-pills modern-hero__pills">
          <span v-for="item in jobIntentItems" :key="item.field" class="resume-meta-pill">
            <span class="resume-meta-pill__label">{{ item.label }}</span>
            <span>{{ item.value }}</span>
          </span>
        </div>
      </header>

      <div class="modern-main__sections" :class="{ 'modern-main__sections--with-header': !hasSidebarContent }">
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
      aside: ['SKILLS', 'CERTIFICATES', 'LAC_CERTIFICATES', 'CAMPUS'],
      main: [
        'SUMMARY',
        'SELF_EVALUATION',
        'EXPERIENCE',
        'PROJECTS',
        'INTERNSHIP',
        'EDUCATION',
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
.resume-template--modern {
  --resume-color-accent: #0f766e;
  --resume-color-accent-soft: rgba(15, 118, 110, 0.14);
  --resume-color-border: #d8e6e3;
  --resume-color-surface: #f5fbfa;
  --resume-color-surface-strong: #e8f6f3;
  --resume-color-heading: #134e4a;
  --resume-color-text: #1f2937;
  --resume-color-text-soft: #476a68;
  --resume-color-text-faint: #6b7280;
  display: grid;
  --resume-font-name: 24px;
  --resume-font-title: 11px;
  --resume-font-contact: 9.5px;
  --resume-font-pill: 9.5px;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: 1123px;
}

.resume-template--modern-single {
  grid-template-columns: 1fr;
}

.modern-sidebar {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 28px 18px 30px;
  background: var(--resume-sidebar-bg);
  border-right: 1px solid var(--resume-sidebar-border);
}

.modern-profile {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.modern-profile__avatar-wrap {
  display: flex;
  justify-content: flex-start;
}

.modern-profile__avatar {
  width: 70px;
  height: 70px;
  border-radius: 12px;
}

.modern-profile__identity {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modern-profile__name {
  margin: 0;
  color: var(--resume-sidebar-heading);
  font-size: 22px;
  font-weight: 800;
  line-height: 1.06;
  letter-spacing: 0.01em;
}

.modern-profile__title {
  margin: 0;
  color: var(--resume-sidebar-text-soft);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.5;
}

.modern-sidebar__panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 11px;
  border-radius: 14px;
  background: var(--resume-sidebar-panel-bg);
  border: 1px solid var(--resume-sidebar-panel-border);
  backdrop-filter: blur(4px);
}

.modern-sidebar__panel-title {
  margin: 0;
  color: var(--resume-sidebar-heading);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.modern-intent-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.modern-intent-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.modern-intent-item__label {
  color: var(--resume-sidebar-text-faint);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.modern-intent-item__value {
  color: var(--resume-sidebar-accent);
  font-size: 10px;
  font-weight: 700;
  line-height: 1.5;
}

.modern-contact-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modern-contact-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.modern-contact-item__label {
  color: var(--resume-sidebar-text-faint);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.modern-contact-item__value {
  color: var(--resume-sidebar-text);
  font-size: 11px;
  line-height: 1.6;
}

.modern-sidebar__sections {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modern-main {
  padding: 30px 28px 34px;
}

.modern-hero {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--resume-color-border);
}

.modern-hero__name {
  margin: 0;
  color: var(--resume-color-heading);
  font-size: 22px;
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: 0.02em;
}

.modern-hero__title {
  margin: 8px 0 0;
  color: var(--resume-color-text-soft);
  font-size: 12px;
  font-weight: 600;
}

.modern-hero__contacts {
  margin-top: 14px;
}

.modern-main__sections {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.modern-main__sections--with-header {
  margin-top: 14px;
}

.resume-template--modern :deep(.resume-section-block__head) {
  padding-bottom: 7px;
  border-bottom: 1px solid var(--resume-color-border);
}

.modern-sidebar :deep(.resume-section-block__head) {
  border-bottom-color: var(--resume-sidebar-divider);
}

.modern-sidebar :deep(.resume-entry),
.modern-sidebar :deep(.resume-kv) {
  background: var(--resume-sidebar-panel-bg);
  border-color: var(--resume-sidebar-panel-border);
}

.modern-sidebar :deep(.resume-section-block__title),
.modern-sidebar :deep(.resume-entry__title) {
  color: var(--resume-sidebar-heading);
}

.modern-sidebar :deep(.resume-entry__subtitle),
.modern-sidebar :deep(.resume-entry__meta),
.modern-sidebar :deep(.resume-entry__date),
.modern-sidebar :deep(.resume-kv__label),
.modern-sidebar :deep(.resume-chip__meta) {
  color: var(--resume-sidebar-text-faint);
}

.modern-sidebar :deep(.resume-rich-text),
.modern-sidebar :deep(.resume-entry__description),
.modern-sidebar :deep(.resume-entry__bullets),
.modern-sidebar :deep(.resume-kv__value),
.modern-sidebar :deep(.resume-bullet-list) {
  color: var(--resume-sidebar-text);
}

.modern-sidebar :deep(.resume-chip) {
  background: var(--resume-sidebar-chip-bg);
  color: var(--resume-sidebar-accent);
}

.resume-template--modern :deep(.resume-entry) {
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--resume-color-surface);
  border: 1px solid rgba(15, 118, 110, 0.08);
}

.resume-template--modern :deep(.resume-entry__title) {
  letter-spacing: 0;
}

.resume-template--modern :deep(.resume-section-block--compact .resume-entry) {
  padding: 7px 8px;
  border-radius: 8px;
}

.resume-template--modern :deep(.resume-section-block--compact .resume-section-block__head) {
  padding-bottom: 6px;
}
</style>
