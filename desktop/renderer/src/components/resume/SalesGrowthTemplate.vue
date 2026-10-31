<template>
  <article
    class="resume-template resume-template--sales-growth"
    :class="{ 'resume-template--sales-growth-single': !layout.asideSections.length }"
  >
    <header
      class="sales-header"
      :class="{ 'sales-header--has-contacts': basicProfile.contacts.length }"
    >
      <div class="sales-header__identity">
        <p class="sales-header__eyebrow">
          {{ locale === 'en' ? 'COMMERCIAL GROWTH PROFILE' : '商务增长履历' }}
        </p>
        <h1 class="sales-header__name">{{ basicProfile.name }}</h1>
        <p v-if="basicProfile.title" class="sales-header__title">{{ basicProfile.title }}</p>
      </div>

      <div v-if="basicProfile.contacts.length" class="sales-header__contacts">
        <span v-for="item in basicProfile.contacts" :key="item.field" class="sales-contact">
          <strong>{{ item.label }}</strong>
          <span>{{ item.value }}</span>
        </span>
      </div>
    </header>

    <section v-if="jobIntentItems.length" class="sales-target">
      <h2 class="sales-target__label">
        {{ locale === 'en' ? 'ROLE TARGET' : '求职方向' }}
      </h2>
      <div class="sales-target__fields">
        <div v-for="item in jobIntentItems" :key="item.field" class="sales-target__field">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
    </section>

    <section v-if="layout.leadSections.length" class="sales-profile">
      <p class="sales-profile__label">
        {{ locale === 'en' ? 'COMMERCIAL POSITIONING' : '商业定位' }}
      </p>
      <ResumeSection
        v-for="section in layout.leadSections"
        :key="section.id"
        :section="section"
        :locale="locale"
        class="sales-profile__section"
      />
    </section>

    <div class="sales-body">
      <main class="sales-main">
        <ResumeSection
          v-for="section in orderedMainSections"
          :key="section.id"
          :section="section"
          :locale="locale"
          class="sales-main__section"
          :class="`sales-main__section--${String(section.sectionCode || 'custom').toLowerCase()}`"
        />
      </main>

      <aside v-if="layout.asideSections.length" class="sales-rail">
        <p class="sales-rail__label">
          {{ locale === 'en' ? 'COMMERCIAL TOOLKIT' : '商务工具箱' }}
        </p>
        <ResumeSection
          v-for="section in layout.asideSections"
          :key="section.id"
          :section="section"
          :locale="locale"
          compact
          class="sales-rail__section"
        />
      </aside>
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
      aside: ['SKILLS', 'CERTIFICATES', 'LAC_CERTIFICATES'],
      main: [
        'EXPERIENCE',
        'PROJECTS',
        'INTERNSHIP',
        'CAMPUS',
        'EDUCATION',
      ],
    },
    props.locale,
  )
})

const basicProfile = computed(() => buildBasicProfile(layout.value.basic, props.locale))
const jobIntentItems = computed(() => buildJobIntentItems(layout.value.jobIntent, props.locale))
const orderedMainSections = computed(() => [
  ...layout.value.mainSections,
  ...layout.value.remainingSections,
])
</script>

<style scoped>
.resume-template--sales-growth {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: #193249;
  --resume-color-text: #304659;
  --resume-color-text-soft: #5b7080;
  --resume-color-text-faint: #7a8c97;
  --resume-color-accent: #b35f43;
  --resume-color-accent-soft: #f5eee9;
  --resume-color-border: #d9e1e4;
  --resume-color-surface: #f5f7f7;
  --resume-color-surface-strong: #edf1f2;
  --resume-font-name: 27px;
  --resume-font-title: 10.5px;
  --resume-font-contact: 8.5px;
  --resume-font-pill: 8.5px;
  --resume-section-title-size: 10px;
  --resume-text-size: 9px;
  min-height: 1123px;
  padding: 38px 48px 46px;
}

.sales-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: end;
  gap: 18px 24px;
  padding: 12px 0 14px;
  border-top: 4px solid var(--resume-color-heading);
  border-bottom: 1px solid var(--resume-color-border);
}

.sales-header--has-contacts {
  grid-template-columns: minmax(0, 1fr) minmax(230px, 0.9fr);
}

.sales-header__identity {
  min-width: 0;
}

.sales-header__eyebrow,
.sales-profile__label,
.sales-target__label,
.sales-rail__label {
  margin: 0;
  color: var(--resume-color-accent);
  font-size: 8.5px;
  font-weight: 750;
  letter-spacing: 0.15em;
}

.sales-header__name {
  margin: 7px 0 0;
  color: var(--resume-color-heading);
  font-family: var(--resume-font-display);
  font-size: var(--resume-font-name);
  font-weight: 750;
  letter-spacing: -0.02em;
  line-height: 1.1;
}

.sales-header__title {
  margin: 5px 0 0;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-title);
  font-weight: 600;
}

.sales-header__contacts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px 12px;
}

.sales-contact {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 1px;
  padding-left: 8px;
  border-left: 1px solid var(--resume-color-border);
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-contact);
  overflow-wrap: anywhere;
}

.sales-contact strong {
  color: var(--resume-color-text-faint);
  font-size: 8px;
  font-weight: 650;
}

.sales-target {
  display: grid;
  grid-template-columns: 94px minmax(0, 1fr);
  align-items: start;
  gap: 12px;
  margin-top: 11px;
  padding: 9px 0 10px;
  border-bottom: 1px solid var(--resume-color-border);
}

.sales-target__label {
  padding-top: 2px;
  color: var(--resume-color-text-faint);
  font-size: 7px;
}

.sales-target__fields {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(105px, 1fr));
  gap: 7px 12px;
}

.sales-target__field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.sales-target__field > span {
  color: var(--resume-color-text-faint);
  font-size: 7px;
}

.sales-target__field > strong {
  color: var(--resume-color-heading);
  font-size: 8.5px;
  font-weight: 700;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.sales-profile {
  margin-top: 14px;
  padding: 10px 13px 11px;
  border-top: 1px solid var(--resume-color-accent);
  border-bottom: 1px solid var(--resume-color-border);
  background: var(--resume-color-surface);
}

.sales-profile__label {
  margin-bottom: 5px;
}

.sales-profile__section :deep(.resume-section-block__head) {
  margin-bottom: 4px;
}

.sales-profile__section :deep(.resume-section-block__title) {
  color: var(--resume-color-heading);
  font-size: 9.5px;
}

.sales-profile__section :deep(.resume-section-block__body) {
  gap: 0;
}

.sales-profile__section :deep(.resume-rich-text) {
  color: var(--resume-color-text);
  line-height: 1.5;
}

.sales-body {
  display: grid;
  grid-template-columns: minmax(0, 0.69fr) minmax(0, 0.31fr);
  align-items: start;
  gap: 20px;
  margin-top: 16px;
}

.resume-template--sales-growth-single .sales-body {
  grid-template-columns: minmax(0, 1fr);
}

.sales-main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 13px;
  counter-reset: sales-section;
}

.sales-main :deep(.resume-section-block) {
  counter-increment: sales-section;
}

.sales-main :deep(.resume-section-block__head) {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  align-items: baseline;
  gap: 7px;
  margin-bottom: 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--resume-color-border);
}

.sales-main :deep(.resume-section-block__head)::before {
  content: counter(sales-section, decimal-leading-zero);
  color: var(--resume-color-accent);
  font-size: 7.5px;
  font-variant-numeric: tabular-nums;
}

.sales-main :deep(.resume-section-block__title) {
  color: var(--resume-color-heading);
  font-size: var(--resume-section-title-size);
  font-weight: 750;
  letter-spacing: 0.035em;
}

.sales-main :deep(.resume-entry) {
  padding: 0 0 9px 10px;
  border: 0;
  border-bottom: 1px solid var(--resume-color-border);
  border-radius: 0;
  background: transparent;
}

.sales-main :deep(.resume-entry__header) {
  align-items: baseline;
}

.sales-main :deep(.resume-entry__title) {
  color: var(--resume-color-heading);
  font-size: 10px;
  font-weight: 700;
}

.sales-main :deep(.resume-entry__subtitle) {
  color: var(--resume-color-accent);
  font-size: 8.5px;
  font-weight: 650;
}

.sales-main :deep(.resume-entry__date) {
  color: var(--resume-color-text-faint);
  font-size: 8px;
  font-variant-numeric: tabular-nums;
}

.sales-main :deep(.resume-entry__bullets li::marker),
.sales-main :deep(.resume-bullet-list li::marker) {
  color: var(--resume-color-accent);
}

.sales-main :deep(.resume-entry__description),
.sales-main :deep(.resume-entry__bullets),
.sales-main :deep(.resume-rich-text) {
  line-height: 1.48;
}

.sales-main :deep(.sales-main__section--projects .resume-entry) {
  padding-bottom: 11px;
}

.sales-rail {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 12px;
  padding: 12px 12px 14px;
  border-left: 1px solid var(--resume-sidebar-border, var(--resume-color-border));
  border-top: 2px solid var(--resume-sidebar-accent, var(--resume-color-heading));
  background: var(--resume-sidebar-bg, var(--resume-color-surface));
}

.sales-rail__label {
  color: var(--resume-sidebar-accent, var(--resume-color-accent));
  font-size: 7px;
}

.sales-rail__section :deep(.resume-section-block__head) {
  margin-bottom: 5px;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--resume-sidebar-divider, var(--resume-color-border));
}

.sales-rail__section :deep(.resume-section-block__title) {
  color: var(--resume-sidebar-heading, var(--resume-color-heading));
  font-size: 9px;
  letter-spacing: 0.03em;
}

.sales-rail__section :deep(.resume-entry) {
  padding: 0 0 6px;
  border: 0;
  border-bottom: 1px solid var(--resume-color-border);
  background: transparent;
}

.sales-rail__section :deep(.resume-entry__title),
.sales-rail__section :deep(.resume-kv__value) {
  color: var(--resume-sidebar-text, var(--resume-color-text));
  font-size: 8.5px;
}

.sales-rail__section :deep(.resume-entry__subtitle),
.sales-rail__section :deep(.resume-entry__meta),
.sales-rail__section :deep(.resume-entry__date),
.sales-rail__section :deep(.resume-kv__label),
.sales-rail__section :deep(.resume-chip__meta) {
  color: var(--resume-sidebar-text-faint, var(--resume-color-text-faint));
}

.sales-rail__section :deep(.resume-rich-text),
.sales-rail__section :deep(.resume-entry__description),
.sales-rail__section :deep(.resume-entry__bullets),
.sales-rail__section :deep(.resume-bullet-list) {
  color: var(--resume-sidebar-text, var(--resume-color-text));
}

.sales-rail__section :deep(.resume-chip) {
  padding: 2px 5px;
  border: 1px solid var(--resume-sidebar-panel-border, var(--resume-color-border));
  border-radius: 2px;
  background: var(--resume-sidebar-chip-bg, var(--ui-neutral-0));
  color: var(--resume-sidebar-accent, var(--resume-color-accent));
  font-size: 8px;
}

@media print {
  .resume-template--sales-growth {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
