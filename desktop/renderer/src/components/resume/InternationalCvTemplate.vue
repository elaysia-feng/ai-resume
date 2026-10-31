<template>
  <article class="resume-template resume-template--international-cv">
    <div class="international-cv__top-rule" aria-hidden="true"></div>

    <header
      class="international-cv__header"
      :class="{ 'international-cv__header--has-contacts': contacts.length }"
    >
      <div class="international-cv__identity">
        <p class="international-cv__eyebrow">CV / PROFESSIONAL PROFILE</p>
        <h1 class="international-cv__name">{{ basicProfile.name }}</h1>
        <p v-if="basicProfile.title" class="international-cv__title">{{ basicProfile.title }}</p>
      </div>

      <div v-if="contacts.length" class="international-cv__contacts" aria-label="联系方式">
        <span v-for="item in contacts" :key="item.field" class="international-cv__contact">
          <span class="international-cv__contact-label">{{ item.label }}</span>
          <span class="international-cv__contact-value">{{ item.value }}</span>
        </span>
      </div>

      <div v-if="targetItems.length" class="international-cv__targets" aria-label="求职意向">
        <span v-for="item in targetItems" :key="item.field" class="international-cv__target">
          <span class="international-cv__target-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </span>
      </div>
    </header>

    <section v-if="layout.leadSections.length" class="international-cv__summary">
      <ResumeSection
        v-for="section in layout.leadSections"
        :key="section.id"
        :section="section"
        :locale="locale"
      />
    </section>

    <main class="international-cv__main">
      <ResumeSection
        v-for="section in contentSections"
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

const INTERNATIONAL_TARGET_FIELDS = new Set(['desiredPosition', 'desiredCity', 'employmentType'])

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
const contacts = computed(() => {
  return basicProfile.value.contacts.filter((item) => item.field !== 'wechat')
})
const jobIntentItems = computed(() => buildJobIntentItems(layout.value.jobIntent, props.locale))
const targetItems = computed(() => {
  return jobIntentItems.value.filter((item) => INTERNATIONAL_TARGET_FIELDS.has(item.field))
})
const contentSections = computed(() => [
  ...layout.value.mainSections,
  ...layout.value.remainingSections,
])
</script>

<style scoped>
/* 国际投递版以纯文本、英文标签和简历既有内容顺序呈现，避免照片与装饰影响 ATS 扫读。 */
.resume-template--international-cv {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: #172b3b;
  --resume-color-text: #354956;
  --resume-color-text-soft: #5d707d;
  --resume-color-text-faint: #71838f;
  --resume-color-accent: #17628f;
  --resume-color-accent-soft: #edf4f8;
  --resume-color-border: #d7e1e7;
  --resume-color-surface: #f7f9fa;
  --resume-color-surface-strong: #edf4f8;

  --resume-font-name: 29px;
  --resume-font-title: 12px;
  --resume-font-contact: 9px;
  --resume-font-pill: 9px;
  --resume-section-title-size: 10px;

  min-height: 1123px;
  padding: 0 56px 48px;
}

.international-cv__top-rule {
  height: 4px;
  margin: 0 -56px 27px;
  background: var(--resume-color-accent);
}

.international-cv__header {
  padding-bottom: 15px;
  border-bottom: 1px solid var(--resume-color-border);
}

.international-cv__header--has-contacts {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 0.82fr);
  align-items: end;
  gap: 16px 28px;
}

.international-cv__eyebrow {
  margin: 0 0 8px;
  color: var(--resume-color-accent);
  font-size: 8px;
  font-weight: 750;
  letter-spacing: 0.16em;
}

.international-cv__name {
  margin: 0;
  color: var(--resume-color-heading);
  font-family: var(--resume-font-display);
  font-size: var(--resume-font-name);
  font-weight: 720;
  letter-spacing: -0.025em;
  line-height: 1.08;
}

.international-cv__title {
  margin: 7px 0 0;
  color: var(--resume-color-accent);
  font-size: var(--resume-font-title);
  font-weight: 650;
  line-height: 1.4;
}

.international-cv__contacts {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  margin-top: 14px;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-contact);
}

.international-cv__header--has-contacts .international-cv__contacts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  justify-content: end;
  gap: 7px 12px;
  margin-top: 0;
  text-align: right;
}

.international-cv__header--has-contacts .international-cv__targets {
  grid-column: 1 / -1;
  width: 100%;
}

.international-cv__contact {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 3px 5px;
  min-width: 0;
  overflow-wrap: anywhere;
}

.international-cv__contact-label,
.international-cv__target-label {
  color: var(--resume-color-text-faint);
  font-size: 8px;
  font-weight: 650;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.international-cv__contact-value {
  overflow-wrap: anywhere;
}

.international-cv__targets {
  display: flex;
  flex-wrap: wrap;
  gap: 5px 16px;
  margin-top: 12px;
  padding-top: 9px;
  border-top: 1px solid var(--resume-color-border);
}

.international-cv__target {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 3px 5px;
  min-width: 0;
  color: var(--resume-color-text);
  font-size: 9px;
  overflow-wrap: anywhere;
}

.international-cv__target strong {
  color: var(--resume-color-heading);
  font-weight: 650;
}

.international-cv__summary {
  margin: 17px 0 19px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--resume-color-border);
}

.international-cv__summary :deep(.resume-section-block__head),
.international-cv__main :deep(.resume-section-block__head) {
  padding-bottom: 5px;
  border-bottom: 1px solid var(--resume-color-border);
}

.international-cv__summary :deep(.resume-section-block__title),
.international-cv__main :deep(.resume-section-block__title) {
  color: var(--resume-color-accent);
  font-size: var(--resume-section-title-size);
  font-weight: 750;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.international-cv__summary :deep(.resume-rich-text) {
  color: var(--resume-color-text);
  font-size: var(--resume-text-size, 10px);
  line-height: 1.65;
}

.international-cv__main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.international-cv__main :deep(.resume-entry) {
  margin-bottom: 7px;
  padding-bottom: 7px;
  border-bottom: 1px solid var(--resume-color-border);
  border-radius: 0;
  background: transparent;
}

.international-cv__main :deep(.resume-entry:last-child) {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: 0;
}

.international-cv__main :deep(.resume-entry__title) {
  color: var(--resume-color-heading);
  font-size: 10.5px;
  font-weight: 700;
}

.international-cv__main :deep(.resume-entry__subtitle),
.international-cv__main :deep(.resume-entry__meta) {
  color: var(--resume-color-text-soft);
}

.international-cv__main :deep(.resume-entry__date) {
  max-width: 112px;
  color: var(--resume-color-text-faint);
  font-size: 8px;
  line-height: 1.4;
  text-align: right;
  white-space: normal;
}

.international-cv__main :deep(.resume-entry__bullets),
.international-cv__main :deep(.resume-rich-text),
.international-cv__main :deep(.resume-bullet-list) {
  font-size: var(--resume-text-size, 10px);
  line-height: 1.55;
}

.international-cv__main :deep(.resume-chip) {
  border: 1px solid var(--resume-color-border);
  border-radius: 2px;
  background: var(--ui-neutral-0);
  color: var(--resume-color-text);
}

@media print {
  .resume-template--international-cv {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
