<template>
  <article class="resume-template resume-template--creative">
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
        alt="简历头像"
        class="resume-basic-header__avatar creative-hero__avatar"
      />
    </header>

    <section v-if="layout.leadSections.length" class="creative-featured">
      <ResumeSection
        v-for="section in layout.leadSections"
        :key="section.id"
        :section="section"
        :locale="locale"
      />
    </section>

    <main class="creative-main">
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
      lead: ['PROJECTS'],
      main: [
        'EXPERIENCE',
        'INTERNSHIP',
        'EDUCATION',
        'SKILLS',
        'CERTIFICATES',
        'LAC_CERTIFICATES',
        'CAMPUS',
        'SUMMARY',
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
.resume-template--creative {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: #292d2f;
  --resume-color-text: #3e4548;
  --resume-color-text-soft: #626b6f;
  --resume-color-text-faint: #858c8f;
  --resume-color-accent: #b44c2f;
  --resume-color-accent-soft: #f8eee8;
  --resume-color-border: #ded8d3;
  --resume-color-surface: #faf7f4;
  --resume-color-surface-strong: #f8eee8;
  --resume-font-name: 34px;
  --resume-font-title: 13px;
  --resume-font-contact: 9px;
  --resume-font-pill: 8px;
  --resume-section-title-size: 11px;
  --resume-text-size: 9px;
  min-height: 1123px;
  padding: 36px 48px 42px;
}

.creative-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 22px;
  padding: 0 0 16px;
  border-bottom: 1px solid var(--resume-color-border);
}

.creative-hero__copy {
  min-width: 0;
}

.creative-hero__eyebrow {
  margin: 0 0 10px;
  color: var(--resume-color-accent);
  font-size: 7px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.creative-hero__name {
  margin: 0;
  color: var(--resume-color-heading);
  font-size: var(--resume-font-name);
  font-family: var(--resume-font-display);
  font-weight: 650;
  line-height: 1.04;
  letter-spacing: -0.025em;
}

.creative-hero__title {
  margin: 7px 0 0;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-title);
  font-weight: 500;
}

.creative-hero__avatar {
  width: 76px;
  height: 88px;
  flex: 0 0 auto;
  border: 1px solid var(--resume-color-border);
  border-radius: 0;
  object-fit: cover;
}

.creative-hero__contacts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
  margin-top: 16px;
  border-top: 1px solid var(--resume-color-border);
}

.creative-hero__contact {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 5px;
  min-width: 0;
  padding: 5px 0 4px;
  border-bottom: 1px solid var(--resume-color-border);
  font-size: var(--resume-font-contact);
}

.creative-hero__contact-label {
  color: var(--resume-color-text-faint);
  font-weight: 700;
}

.creative-hero__pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  margin-top: 12px;
}

.creative-hero__pills :deep(.resume-meta-pill) {
  padding: 4px 0 3px;
  border: 0;
  border-top: 1px solid var(--resume-color-accent);
  border-radius: 0;
  background: transparent;
  color: var(--resume-color-text);
  font-size: var(--resume-font-pill);
}

.creative-featured {
  margin: 22px 0 25px;
  padding: 0;
}

.creative-featured :deep(.resume-section-block__head) {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 9px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--resume-color-heading);
}

.creative-featured :deep(.resume-section-block__head)::after {
  content: 'CASE STUDIES';
  color: var(--resume-color-text-faint);
  font-size: 7px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.creative-featured :deep(.resume-section-block__title) {
  color: var(--resume-color-heading);
  font-size: 13px;
  font-weight: 750;
  letter-spacing: 0.01em;
}

.creative-featured :deep(.resume-entry-list) {
  display: flex;
  flex-direction: column;
  gap: 0;
  counter-reset: creative-case;
}

.creative-featured :deep(.resume-entry) {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  column-gap: 10px;
  margin: 0;
  padding: 10px 0 12px;
  border: 0;
  border-bottom: 1px solid var(--resume-color-border);
  border-radius: 0;
  background: transparent;
  counter-increment: creative-case;
}

.creative-featured :deep(.resume-entry::before) {
  grid-column: 1;
  grid-row: 1 / span 6;
  padding-top: 1px;
  color: var(--resume-color-accent);
  content: counter(creative-case, decimal-leading-zero);
  font-family: var(--resume-font-display);
  font-size: 18px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.creative-featured :deep(.resume-entry:last-child) {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: 0;
}

.creative-featured :deep(.resume-entry__header),
.creative-featured :deep(.resume-entry__meta),
.creative-featured :deep(.resume-entry__description),
.creative-featured :deep(.resume-entry__bullets),
.creative-featured :deep(.resume-entry > .resume-chip-list) {
  grid-column: 2;
}

.creative-featured :deep(.resume-entry__title) {
  color: var(--resume-color-heading);
  font-size: 12px;
  font-family: var(--resume-font-display);
  font-weight: 700;
}

.creative-featured :deep(.resume-entry__title::before) {
  margin-right: 6px;
  color: var(--resume-color-accent);
  content: 'CASE';
  font-family: var(--resume-font-body);
  font-size: 6px;
  font-weight: 750;
  letter-spacing: 0.12em;
}

.creative-featured :deep(.resume-entry__subtitle) {
  color: var(--resume-color-accent);
  font-weight: 650;
}

.creative-main {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
  gap: 18px 26px;
}

.creative-main :deep(.resume-section-block:first-child) {
  grid-column: 1 / -1;
}

.creative-main :deep(.resume-section-block__head) {
  margin-bottom: 7px;
  padding-bottom: 5px;
  border-bottom: 1px solid var(--resume-color-border);
}

.creative-main :deep(.resume-section-block__title) {
  color: var(--resume-color-heading);
  font-size: var(--resume-section-title-size);
  font-weight: 750;
  letter-spacing: 0.04em;
}

.creative-main :deep(.resume-entry) {
  padding: 0 0 7px;
  border: 0;
  border-bottom: 1px solid var(--resume-color-border);
  border-radius: 0;
  background: transparent;
}

.creative-main :deep(.resume-entry__date) {
  color: var(--resume-color-accent);
  font-variant-numeric: tabular-nums;
}

@media print {
  .resume-template--creative,
  .creative-hero,
  .creative-featured {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
