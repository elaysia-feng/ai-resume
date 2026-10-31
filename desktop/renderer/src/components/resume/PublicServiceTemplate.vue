<template>
  <article class="resume-template resume-template--public-service">
    <header class="public-service__header">
      <div class="public-service__header-row">
        <div class="public-service__identity">
          <p class="public-service__document-label">
            {{ locale === 'en' ? 'PROFESSIONAL RECORD' : '资历与任职履历' }}
          </p>
          <h1 class="public-service__name">{{ basicProfile.name }}</h1>
          <p v-if="basicProfile.title" class="public-service__title">{{ basicProfile.title }}</p>
        </div>
      </div>

      <div v-if="basicProfile.contacts.length" class="public-service__contacts">
        <span v-for="item in basicProfile.contacts" :key="item.field" class="public-service__contact">
          <span class="public-service__contact-label">{{ item.label }}</span>
          <span>{{ item.value }}</span>
        </span>
      </div>

      <div v-if="targetItems.length" class="public-service__intent">
        <span v-for="item in targetItems" :key="item.field" class="public-service__intent-item">
          <span class="public-service__intent-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </span>
      </div>
    </header>

    <section v-if="qualificationSections.length" class="public-service__qualifications">
      <ResumeSection
        v-for="section in qualificationSections"
        :key="section.id"
        :section="section"
        :locale="locale"
        class="public-service__qualification"
      />
    </section>

    <main class="public-service__history">
      <ResumeSection
        v-for="section in remainingSections"
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

const QUALIFICATION_CODES = new Set(['EDUCATION', 'CERTIFICATES', 'LAC_CERTIFICATES'])
const INTENT_FIELDS = new Set(['desiredPosition', 'desiredCity'])

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
      main: [
        'EDUCATION',
        'CERTIFICATES',
        'LAC_CERTIFICATES',
        'EXPERIENCE',
        'INTERNSHIP',
        'CAMPUS',
        'PROJECTS',
        'SKILLS',
        'SUMMARY',
        'SELF_EVALUATION',
      ],
    },
    props.locale,
  )
})

const basicProfile = computed(() => buildBasicProfile(layout.value.basic, props.locale))
const jobIntentItems = computed(() => buildJobIntentItems(layout.value.jobIntent, props.locale))
const targetItems = computed(() => {
  return jobIntentItems.value.filter((item) => INTENT_FIELDS.has(item.field))
})
const orderedSections = computed(() => [
  ...layout.value.mainSections,
  ...layout.value.remainingSections,
])
const qualificationSections = computed(() => {
  return orderedSections.value.filter((section) => QUALIFICATION_CODES.has(section.sectionCode))
})
const remainingSections = computed(() => {
  return orderedSections.value.filter((section) => !QUALIFICATION_CODES.has(section.sectionCode))
})
</script>

<style scoped>
/* 规范履历以资格与完整经历为索引，采用正式单栏顺序，不增加未经提供的个人字段。 */
.resume-template--public-service {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: #1d2b39;
  --resume-color-text: #35434f;
  --resume-color-text-soft: #5d6b76;
  --resume-color-text-faint: #73808a;
  --resume-color-accent: #3c5a70;
  --resume-color-accent-soft: #eef2f5;
  --resume-color-border: #d8dfe4;
  --resume-color-surface: #f7f9fa;
  --resume-color-surface-strong: #edf2f5;

  --resume-font-name: 27px;
  --resume-font-title: 11px;
  --resume-font-contact: 9px;
  --resume-font-pill: 9px;
  --resume-section-title-size: 10px;

  min-height: 1123px;
  padding: 46px 56px 50px;
}

.public-service__header {
  padding-bottom: 15px;
  border-bottom: 2px solid var(--resume-color-accent);
}

.public-service__header-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.public-service__identity {
  min-width: 0;
}

.public-service__document-label {
  margin: 0 0 8px;
  color: var(--resume-color-accent);
  font-size: 8px;
  font-weight: 750;
  letter-spacing: 0.14em;
}

.public-service__name {
  margin: 0;
  color: var(--resume-color-heading);
  font-family: var(--resume-font-display);
  font-size: var(--resume-font-name);
  font-weight: 720;
  letter-spacing: 0.01em;
  line-height: 1.12;
}

.public-service__title {
  margin: 6px 0 0;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-title);
  font-weight: 600;
}

.public-service__contacts,
.public-service__intent {
  display: flex;
  flex-wrap: wrap;
  gap: 5px 16px;
}

.public-service__contacts {
  margin-top: 13px;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-contact);
}

.public-service__contact {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 3px 5px;
  min-width: 0;
  overflow-wrap: anywhere;
}

.public-service__contact-label,
.public-service__intent-label {
  color: var(--resume-color-text-faint);
  font-size: 8.5px;
  font-weight: 650;
}

.public-service__intent {
  margin-top: 11px;
  padding-top: 8px;
  border-top: 1px solid var(--resume-color-border);
}

.public-service__intent-item {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 3px 5px;
  min-width: 0;
  color: var(--resume-color-heading);
  font-size: 9px;
  overflow-wrap: anywhere;
}

.public-service__intent-item strong {
  font-weight: 650;
}

.public-service__qualifications {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 18px;
  padding-top: 12px;
  border-top: 1px solid var(--resume-color-border);
}

.public-service__history {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 17px;
}

.public-service__qualifications :deep(.resume-section-block__head),
.public-service__history :deep(.resume-section-block__head) {
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding-bottom: 5px;
  border: 0;
}

.public-service__qualifications :deep(.resume-section-block__head::after),
.public-service__history :deep(.resume-section-block__head::after) {
  height: 1px;
  background: var(--resume-color-border);
  content: '';
}

.public-service__qualifications :deep(.resume-section-block__title),
.public-service__history :deep(.resume-section-block__title) {
  color: var(--resume-color-accent);
  font-size: var(--resume-section-title-size);
  font-weight: 750;
  letter-spacing: 0.08em;
}

.public-service__qualifications :deep(.resume-entry),
.public-service__history :deep(.resume-entry) {
  margin-bottom: 7px;
  padding-bottom: 7px;
  border-bottom: 1px solid var(--resume-color-border);
  border-radius: 0;
  background: transparent;
}

.public-service__qualifications :deep(.resume-entry:last-child),
.public-service__history :deep(.resume-entry:last-child) {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: 0;
}

.public-service__qualifications :deep(.resume-entry__title),
.public-service__history :deep(.resume-entry__title) {
  color: var(--resume-color-heading);
  font-size: 10.5px;
}

.public-service__qualifications :deep(.resume-entry__date),
.public-service__history :deep(.resume-entry__date) {
  max-width: 110px;
  color: var(--resume-color-text-faint);
  font-size: 9px;
  line-height: 1.4;
  text-align: right;
  white-space: normal;
}

.public-service__qualifications :deep(.resume-entry__subtitle),
.public-service__qualifications :deep(.resume-entry__meta),
.public-service__history :deep(.resume-entry__subtitle),
.public-service__history :deep(.resume-entry__meta) {
  color: var(--resume-color-text-soft);
}

.public-service__qualifications :deep(.resume-entry__bullets),
.public-service__qualifications :deep(.resume-rich-text),
.public-service__history :deep(.resume-entry__bullets),
.public-service__history :deep(.resume-rich-text) {
  font-size: var(--resume-text-size, 10px);
  line-height: 1.55;
}

.public-service__qualifications :deep(.resume-chip),
.public-service__history :deep(.resume-chip) {
  border: 1px solid var(--resume-color-border);
  border-radius: 2px;
  background: var(--ui-neutral-0);
  color: var(--resume-color-text);
}

@media print {
  .resume-template--public-service {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
