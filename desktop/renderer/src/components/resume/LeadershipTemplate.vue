<template>
  <article class="resume-template resume-template--leadership">
    <header
      class="leadership-masthead"
      :class="{ 'leadership-masthead--has-contacts': basicProfile.contacts.length }"
    >
      <div class="leadership-masthead__identity">
        <p class="leadership-masthead__eyebrow">管理履历 / LEADERSHIP PROFILE</p>
        <h1 class="leadership-masthead__name">{{ basicProfile.name }}</h1>
        <p v-if="basicProfile.title" class="leadership-masthead__title">{{ basicProfile.title }}</p>
      </div>

      <div v-if="basicProfile.contacts.length" class="leadership-masthead__contacts">
        <span v-for="item in basicProfile.contacts" :key="item.field">
          <strong>{{ item.label }}</strong>
          {{ item.value }}
        </span>
      </div>
    </header>

    <div v-if="jobIntentItems.length" class="leadership-target">
      <span class="leadership-target__label">目标方向</span>
      <span v-for="item in jobIntentItems" :key="item.field" class="leadership-target__item">
        <strong>{{ item.label }}</strong>{{ item.value }}
      </span>
    </div>

    <section v-if="layout.leadSections.length" class="leadership-thesis">
      <p class="leadership-thesis__label">管理判断<br /><span>IMPACT STATEMENT</span></p>
      <div class="leadership-thesis__body">
        <ResumeSection
          v-for="section in layout.leadSections"
          :key="section.id"
          :section="section"
          :locale="locale"
        />
      </div>
    </section>

    <main class="leadership-main">
      <ResumeSection
        v-for="section in contentSections"
        :key="section.id"
        :section="section"
        :locale="locale"
        class="leadership-main-section"
        :class="`leadership-main-section--${String(section.sectionCode || 'custom').toLowerCase()}`"
      />
    </main>
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
      lead: ['SUMMARY'],
      main: [
        'EXPERIENCE',
        'PROJECTS',
        'INTERNSHIP',
        'SKILLS',
        'EDUCATION',
        'CERTIFICATES',
        'LAC_CERTIFICATES',
        'CAMPUS',
        'SELF_EVALUATION',
      ],
    },
    props.locale,
  )
})

const basicProfile = computed(() => buildBasicProfile(layout.value.basic, props.locale))
const jobIntentItems = computed(() => buildJobIntentItems(layout.value.jobIntent, props.locale))
const contentSections = computed(() => [
  ...layout.value.mainSections,
  ...layout.value.remainingSections,
])
</script>

<style scoped>
.resume-template--leadership {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: #1f2d3c;
  --resume-color-text: #344353;
  --resume-color-text-soft: #61707d;
  --resume-color-text-faint: #7d8993;
  --resume-color-accent: #a45437;
  --resume-color-accent-soft: #f5eee8;
  --resume-color-border: #dce1e5;
  --resume-color-surface: #f7f8f9;
  --resume-color-surface-strong: #f1f3f5;
  --resume-font-name: 30px;
  --resume-font-title: 12px;
  --resume-font-contact: 9px;
  --resume-font-pill: 9px;
  --resume-section-title-size: 10px;
  --resume-text-size: 9px;
  min-height: 1123px;
  padding: 44px 52px 50px;
}

.leadership-masthead {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: end;
  gap: 20px;
  padding: 16px 0 17px;
  border-top: 4px solid var(--resume-color-heading);
  border-bottom: 1px solid var(--resume-color-border);
}

.leadership-masthead--has-contacts {
  grid-template-columns: minmax(0, 1fr) minmax(0, 260px);
}

.leadership-masthead__eyebrow {
  margin: 0 0 8px;
  color: var(--resume-color-accent);
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.leadership-masthead__name {
  margin: 0;
  color: var(--resume-color-heading);
  font-family: var(--resume-font-display);
  font-size: var(--resume-font-name);
  font-weight: 650;
  letter-spacing: -0.025em;
  line-height: 1.05;
}

.leadership-masthead__title {
  margin: 7px 0 0;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-title);
  font-weight: 600;
}

.leadership-masthead__contacts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px 12px;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-contact);
}

.leadership-masthead__contacts > span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.leadership-masthead__contacts strong {
  display: block;
  color: var(--resume-color-text-faint);
  font-size: 8px;
  font-weight: 700;
}

.leadership-target {
  display: grid;
  grid-template-columns: 105px minmax(0, 1fr);
  align-items: start;
  gap: 10px 15px;
  padding: 12px 0 13px;
  border-bottom: 1px solid var(--resume-color-border);
}

.leadership-target__label {
  color: var(--resume-color-accent);
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.leadership-target__item {
  color: var(--resume-color-text);
  font-size: var(--resume-font-contact);
  line-height: 1.55;
}

.leadership-target__item strong {
  margin-right: 6px;
  color: var(--resume-color-text-faint);
  font-size: 7px;
  font-weight: 650;
}

.leadership-thesis {
  display: grid;
  grid-template-columns: 105px minmax(0, 1fr);
  gap: 15px;
  margin: 18px 0 20px;
  padding: 13px 15px 14px 0;
  border-top: 2px solid var(--resume-color-accent);
  border-bottom: 1px solid var(--resume-color-border);
  background: var(--resume-color-surface);
}

.leadership-thesis__label {
  margin: 1px 0 0;
  color: var(--resume-color-accent);
  font-size: 9px;
  font-weight: 800;
  line-height: 1.5;
}

.leadership-thesis__label span {
  color: var(--resume-color-text-faint);
  font-size: 6px;
  letter-spacing: 0.12em;
}

.leadership-thesis__body :deep(.resume-section-block__head) {
  display: none;
}

.leadership-thesis__body :deep(.resume-rich-text) {
  color: var(--resume-color-heading);
  font-family: var(--resume-font-display);
  font-size: 11px;
  line-height: 1.6;
}

.leadership-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.leadership-main :deep(.resume-section-block__head) {
  margin-bottom: 7px;
  padding-bottom: 5px;
  border-bottom: 1px solid var(--resume-color-border);
}

.leadership-main :deep(.resume-section-block__title) {
  color: var(--resume-color-heading);
  font-size: var(--resume-section-title-size);
  font-weight: 800;
  letter-spacing: 0.07em;
}

.leadership-main-section--experience :deep(.resume-entry) {
  margin-bottom: 8px;
  padding: 0 0 9px;
  border: 0;
  border-bottom: 1px solid var(--resume-color-border);
}

.leadership-main-section--experience :deep(.resume-entry__title) {
  color: var(--resume-color-heading);
  font-weight: 750;
}

.leadership-main-section--experience :deep(.resume-entry__subtitle) {
  color: var(--resume-color-accent);
  font-weight: 650;
}

.leadership-main-section--experience :deep(.resume-entry__date) {
  color: var(--resume-color-text-soft);
  font-size: 8px;
  font-variant-numeric: tabular-nums;
}

.leadership-main-section--projects :deep(.resume-entry-list) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
}

.leadership-main-section--projects :deep(.resume-entry) {
  padding: 0 0 9px;
  border: 0;
  border-bottom: 1px solid var(--resume-color-border);
}

.leadership-main-section--projects :deep(.resume-entry__title) {
  font-family: var(--resume-font-display);
  color: var(--resume-color-heading);
  font-weight: 700;
}

.leadership-main-section--skills :deep(.resume-chip) {
  border: 1px solid var(--resume-color-border);
  border-radius: 2px;
  background: var(--resume-color-surface);
  color: var(--resume-color-text);
}

@media print {
  .resume-template--leadership,
  .leadership-thesis {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
