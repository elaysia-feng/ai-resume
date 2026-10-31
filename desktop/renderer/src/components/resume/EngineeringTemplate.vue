<template>
  <article class="resume-template resume-template--engineering">
    <header
      class="engineering-header"
      :class="{ 'engineering-header--has-contacts': basicProfile.contacts.length }"
    >
      <div class="engineering-header__identity">
        <p class="engineering-header__eyebrow">
          {{ locale === 'en' ? 'ENGINEERING PRACTICE' : '工程实践 / 技术履历' }}
        </p>
        <h1 class="engineering-header__name">{{ basicProfile.name }}</h1>
        <p v-if="basicProfile.title" class="engineering-header__title">{{ basicProfile.title }}</p>
      </div>

      <div v-if="basicProfile.contacts.length" class="engineering-header__contacts">
        <span
          v-for="item in basicProfile.contacts"
          :key="item.field"
          class="engineering-header__contact"
        >
          <span class="engineering-header__contact-label">{{ item.label }}</span>
          <span class="engineering-header__contact-value">{{ item.value }}</span>
        </span>
      </div>
    </header>

    <div v-if="jobIntentItems.length" class="engineering-intent">
      <span v-for="item in jobIntentItems" :key="item.field" class="engineering-intent__item">
        <span class="engineering-intent__label">{{ item.label }}</span>
        <span class="engineering-intent__value">{{ item.value }}</span>
      </span>
    </div>

    <section v-if="layout.leadSections.length" class="engineering-summary">
      <ResumeSection
        v-for="section in layout.leadSections"
        :key="section.id"
        :section="section"
        :locale="locale"
      />
    </section>

    <section v-if="layout.asideSections.length" class="engineering-skills">
      <ResumeSection
        v-for="section in layout.asideSections"
        :key="section.id"
        :section="section"
        :locale="locale"
      />
    </section>

    <section v-if="projectSections.length" class="engineering-projects">
      <ResumeSection
        v-for="section in projectSections"
        :key="section.id"
        :section="section"
        :locale="locale"
      />
    </section>

    <main v-if="supportingSections.length" class="engineering-main">
      <ResumeSection
        v-for="section in supportingSections"
        :key="section.id"
        :section="section"
        :locale="locale"
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
      aside: ['SKILLS'],
      main: [
        'PROJECTS',
        'EXPERIENCE',
        'INTERNSHIP',
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
const orderedBodySections = computed(() => [
  ...layout.value.mainSections,
  ...layout.value.remainingSections,
])
const projectSections = computed(() => orderedBodySections.value.filter((section) => section.sectionCode === 'PROJECTS'))
const supportingSections = computed(() => orderedBodySections.value.filter((section) => section.sectionCode !== 'PROJECTS'))
</script>

<style scoped>
.resume-template--engineering {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: #182b3a;
  --resume-color-text: #354653;
  --resume-color-text-soft: #5d6d78;
  --resume-color-text-faint: #7a8992;
  --resume-color-accent: #155e83;
  --resume-color-accent-soft: #eaf2f6;
  --resume-color-border: #d7e1e6;
  --resume-color-surface: #f7f9fa;
  --resume-color-surface-strong: #eaf2f6;
  --resume-font-name: 30px;
  --resume-font-title: 12px;
  --resume-font-contact: 9px;
  --resume-font-pill: 8px;
  --resume-section-title-size: 11px;
  --resume-text-size: 9px;
  min-height: 1123px;
  padding: 40px 50px 46px;
}

.engineering-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: end;
  gap: 24px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--resume-color-heading);
}

.engineering-header--has-contacts {
  grid-template-columns: minmax(0, 1fr) minmax(190px, 250px);
}

.engineering-header__identity {
  min-width: 0;
}

.engineering-header__eyebrow {
  margin: 0 0 8px;
  color: var(--resume-color-accent);
  font-size: 7px;
  font-weight: 800;
  letter-spacing: 0.15em;
}

.engineering-header__name {
  margin: 0;
  color: var(--resume-color-heading);
  font-family: var(--resume-font-display);
  font-size: var(--resume-font-name);
  font-weight: 700;
  line-height: 1.08;
  letter-spacing: -0.02em;
}

.engineering-header__title {
  margin: 6px 0 0;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-title);
  font-weight: 600;
}

.engineering-header__contacts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 12px;
  min-width: 0;
}

.engineering-header__contact {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 5px;
  min-width: 0;
  padding: 4px 0;
  border-bottom: 1px solid var(--resume-color-border);
  font-size: var(--resume-font-contact);
}

.engineering-header__contact-label {
  color: var(--resume-color-text-faint);
  font-weight: 650;
}

.engineering-header__contact-value {
  min-width: 0;
  color: var(--resume-color-text);
}

.engineering-intent {
  display: flex;
  flex-wrap: wrap;
  gap: 7px 18px;
  padding: 9px 0 11px;
  border-bottom: 1px solid var(--resume-color-border);
}

.engineering-intent__item {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 5px;
  color: var(--resume-color-text);
  font-size: var(--resume-font-pill);
}

.engineering-intent__label {
  color: var(--resume-color-text-faint);
  font-weight: 650;
}

.engineering-summary {
  margin-top: 17px;
  padding: 9px 12px 10px;
  border-top: 1px solid var(--resume-color-accent);
  border-bottom: 1px solid var(--resume-color-border);
  background: var(--resume-color-surface);
}

.engineering-summary :deep(.resume-section-block__head) {
  margin-bottom: 3px;
}

.engineering-summary :deep(.resume-section-block__title) {
  color: var(--resume-color-accent);
  font-size: 9px;
  letter-spacing: 0.08em;
}

.engineering-summary :deep(.resume-rich-text) {
  line-height: 1.5;
}

.engineering-skills {
  margin-top: 18px;
  padding: 10px 0 11px;
  border-top: 1px solid var(--resume-color-border);
  border-bottom: 1px solid var(--resume-color-border);
}

.engineering-skills :deep(.resume-section-block__head) {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.engineering-skills :deep(.resume-section-block__head)::after {
  content: 'TECHNICAL INDEX';
  color: var(--resume-color-text-faint);
  font-size: 7px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.engineering-skills :deep(.resume-section-block__title) {
  color: var(--resume-color-heading);
  font-size: 12px;
}

.engineering-skills :deep(.resume-chip-list) {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 4px 7px;
}

.engineering-skills :deep(.resume-chip) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px;
  min-width: 0;
  padding: 3px 6px;
  border: 1px solid var(--resume-color-border);
  border-radius: 0;
  background: var(--resume-color-surface);
  color: var(--resume-color-text);
  font-size: 8px;
  font-weight: 600;
}

.engineering-skills :deep(.resume-chip > span:first-child) {
  min-width: 0;
  overflow-wrap: anywhere;
}

.engineering-skills :deep(.resume-chip__meta) {
  color: var(--resume-color-accent);
  font-size: 7px;
}

.engineering-projects {
  margin-top: 19px;
  counter-reset: engineering-project;
}

.engineering-projects :deep(.resume-section-block__head) {
  margin-bottom: 7px;
  padding-bottom: 5px;
  border-bottom: 1px solid var(--resume-color-heading);
}

.engineering-projects :deep(.resume-section-block__title) {
  color: var(--resume-color-heading);
  font-size: 13px;
}

.engineering-projects :deep(.resume-entry) {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  column-gap: 9px;
  padding: 8px 0 10px;
  border: 0;
  border-bottom: 1px solid var(--resume-color-border);
  border-radius: 0;
  background: transparent;
  counter-increment: engineering-project;
}

.engineering-projects :deep(.resume-entry::before) {
  grid-column: 1;
  grid-row: 1 / span 6;
  padding-top: 1px;
  color: var(--resume-color-accent);
  content: counter(engineering-project, decimal-leading-zero);
  font-family: var(--ui-font-mono, var(--resume-font-body));
  font-size: 8px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.engineering-projects :deep(.resume-entry__header),
.engineering-projects :deep(.resume-entry__description),
.engineering-projects :deep(.resume-entry__bullets),
.engineering-projects :deep(.resume-entry > .resume-chip-list) {
  grid-column: 2;
}

.engineering-projects :deep(.resume-entry__header) {
  grid-row: 1;
}

.engineering-projects :deep(.resume-entry > .resume-chip-list) {
  grid-row: 2;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 3px 5px;
  margin-top: 2px;
}

.engineering-projects :deep(.resume-entry > .resume-chip-list)::before {
  content: 'STACK';
  margin-right: 3px;
  color: var(--resume-color-text-faint);
  font-size: 6px;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.engineering-projects :deep(.resume-entry > .resume-chip-list .resume-chip) {
  padding: 1px 5px;
  border: 1px solid var(--resume-color-border);
  border-radius: 0;
  background: transparent;
  color: var(--resume-color-accent);
  font-size: 7px;
  white-space: normal;
  overflow-wrap: anywhere;
}

.engineering-projects :deep(.resume-entry__description) {
  grid-row: 3;
  margin-top: 4px;
}

.engineering-projects :deep(.resume-entry__bullets) {
  grid-row: 4;
}

.engineering-projects :deep(.resume-entry__title) {
  color: var(--resume-color-heading);
  font-size: 12px;
}

.engineering-projects :deep(.resume-entry__subtitle),
.engineering-projects :deep(.resume-entry__date) {
  color: var(--resume-color-text-soft);
}

.engineering-projects :deep(.resume-entry__bullets li::marker) {
  color: var(--resume-color-accent);
}

.engineering-main {
  display: flex;
  flex-direction: column;
  gap: 13px;
  margin-top: 17px;
}

.engineering-main :deep(.resume-section-block__head) {
  margin-bottom: 5px;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--resume-color-border);
}

.engineering-main :deep(.resume-section-block__title) {
  color: var(--resume-color-heading);
  font-size: var(--resume-section-title-size);
}

.engineering-main :deep(.resume-entry) {
  padding: 0 0 7px;
  border: 0;
  border-bottom: 1px solid var(--resume-color-border);
  border-radius: 0;
  background: transparent;
}

@media print {
  .resume-template--engineering,
  .engineering-summary,
  .engineering-skills {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
