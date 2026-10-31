<template>
  <article class="resume-template resume-template--modern">
    <header class="modern-hero">
      <div class="modern-hero__top">
        <div class="modern-hero__identity">
          <p class="modern-hero__eyebrow">职业履历 / CAREER PROFILE</p>
          <h1 class="modern-hero__name">{{ basicProfile.name }}</h1>
          <p v-if="basicProfile.title" class="modern-hero__title">{{ basicProfile.title }}</p>
        </div>
        <img
          v-if="basicProfile.avatar"
          :src="basicProfile.avatar"
          alt=""
          class="resume-basic-header__avatar modern-hero__avatar"
        />
      </div>

      <div v-if="basicProfile.contacts.length" class="modern-hero__contacts">
        <span v-for="item in basicProfile.contacts" :key="item.field">
          <span>{{ item.label }}</span>
          {{ item.value }}
        </span>
      </div>

      <div v-if="jobIntentItems.length" class="modern-hero__intent">
        <span v-for="item in jobIntentItems" :key="item.field">
          {{ item.label }} <strong>{{ item.value }}</strong>
        </span>
      </div>
    </header>

    <section v-if="layout.leadSections.length" class="modern-impact" aria-label="核心优势">
      <ResumeSection
        v-for="section in layout.leadSections"
        :key="section.id"
        :section="section"
        :locale="locale"
      />
    </section>

    <main class="modern-main">
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
      main: [
        'EXPERIENCE',
        'PROJECTS',
        'INTERNSHIP',
        'EDUCATION',
        'CAMPUS',
        'SKILLS',
        'CERTIFICATES',
        'LAC_CERTIFICATES',
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
.resume-template--modern {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: #1c2d28;
  --resume-color-text: #394842;
  --resume-color-text-soft: #65736c;
  --resume-color-text-faint: #78857e;
  --resume-color-accent: #987238;
  --resume-color-accent-soft: #f4efe4;
  --resume-color-border: #ded8ca;
  --resume-color-surface: #faf8f2;
  --resume-color-surface-strong: #f4efe4;
  --resume-font-name: 28px;
  --resume-font-title: 12px;
  --resume-font-contact: 9px;
  --resume-font-pill: 9px;
  --resume-section-title-size: 11px;
  --resume-text-size: 9px;
  min-height: 1123px;
  padding: 0 46px 42px;
  background: var(--resume-color-page);
}

.modern-hero {
  margin: 0 -46px 18px;
  padding: 34px 46px 22px;
  background: #17322b;
  color: #f7f5ef;
}

.modern-hero__top {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.modern-hero__identity {
  min-width: 0;
}

.modern-hero__eyebrow {
  margin: 0 0 11px;
  color: #c6b17f;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.modern-hero__name {
  margin: 0;
  color: #fffdf8;
  font-size: 30px;
  font-weight: 750;
  letter-spacing: -0.02em;
  line-height: 1.1;
}

.modern-hero__title {
  margin: 7px 0 0;
  color: #e1e7df;
  font-size: 13px;
  font-weight: 550;
}

.modern-hero__avatar {
  width: 58px;
  height: 72px;
  flex: 0 0 auto;
  border: 1px solid #b7a474;
  border-radius: 0;
  object-fit: cover;
}

.modern-hero__contacts,
.modern-hero__intent {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 18px;
  margin-top: 18px;
  color: #e6e9df;
  font-size: var(--resume-font-contact);
}

.modern-hero__contacts > span > span {
  margin-right: 4px;
  color: #c6b17f;
}

.modern-hero__intent {
  padding-top: 11px;
  border-top: 1px solid #52675c;
  color: #d0d7ca;
}

.modern-hero__intent strong {
  color: #f4d78e;
  font-weight: 650;
}

.modern-impact {
  margin-bottom: 18px;
  display: grid;
  grid-template-columns: 106px minmax(0, 1fr);
  gap: 18px;
  padding: 13px 15px 14px;
  border-top: 2px solid var(--resume-color-accent);
  border-bottom: 1px solid var(--resume-color-border);
  background: var(--resume-color-accent-soft);
}

.modern-impact :deep(.resume-section-block__head) {
  grid-column: 1;
  margin: 0;
  border: 0;
  padding: 0;
  padding-top: 2px;
}

.modern-impact :deep(.resume-section-block__body) {
  grid-column: 2;
  min-width: 0;
}

.modern-impact :deep(.resume-section-block__title) {
  color: var(--resume-color-accent);
  font-size: 9px;
  font-weight: 750;
  letter-spacing: 0.1em;
}

.modern-impact :deep(.resume-rich-text) {
  color: var(--resume-color-heading);
  font-size: 10px;
  font-weight: 550;
  line-height: 1.62;
}

.modern-main {
  display: flex;
  flex-direction: column;
  gap: 17px;
}

.modern-main :deep(.resume-section-block) {
  display: grid;
  grid-template-columns: 106px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.modern-main :deep(.resume-section-block__head) {
  grid-column: 1;
  margin: 0;
  padding: 5px 0 0;
  border: 0;
}

.modern-main :deep(.resume-section-block__title) {
  color: var(--resume-color-accent);
  font-size: 9px;
  font-weight: 750;
  letter-spacing: 0.1em;
}

.modern-main :deep(.resume-section-block__body) {
  grid-column: 2;
  min-width: 0;
  padding-top: 4px;
  border-top: 1px solid var(--resume-color-border);
}

.modern-main :deep(.resume-entry) {
  position: relative;
  margin: 0 0 7px;
  padding: 0 0 7px 16px;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.modern-main :deep(.resume-entry:last-child) {
  margin-bottom: 0;
  padding-bottom: 0;
}

.modern-main :deep(.resume-entry::before) {
  position: absolute;
  top: 4px;
  bottom: -7px;
  left: 2px;
  width: 1px;
  background: #c8bd9f;
  content: '';
}

.modern-main :deep(.resume-entry::after) {
  position: absolute;
  top: 4px;
  left: -1px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--resume-color-accent);
  content: '';
}

.modern-main :deep(.resume-entry:last-child::before) {
  bottom: auto;
  height: 8px;
}

.modern-main :deep(.resume-entry__title) {
  color: var(--resume-color-heading);
  font-weight: 700;
}

.modern-main :deep(.resume-entry__date) {
  color: var(--resume-color-accent);
  font-weight: 650;
  font-variant-numeric: tabular-nums;
  font-size: 8px;
}

.modern-main :deep(.resume-entry__subtitle),
.modern-main :deep(.resume-entry__meta) {
  color: var(--resume-color-text-soft);
}

.modern-main :deep(.resume-chip) {
  border: 1px solid #e3dac4;
  border-radius: 2px;
  background: var(--resume-color-accent-soft);
  color: var(--resume-color-accent);
}

@media print {
  .resume-template--modern {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
