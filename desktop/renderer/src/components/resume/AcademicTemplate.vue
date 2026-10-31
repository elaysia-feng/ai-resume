<template>
  <article class="resume-template resume-template--academic">
    <header
      class="resume-basic-header academic-header"
      :class="{ 'academic-header--has-intent': jobIntentItems.length }"
    >
      <div class="resume-basic-header__identity">
        <h1 class="resume-basic-header__name">{{ basicProfile.name }}</h1>
        <p v-if="basicProfile.title" class="resume-basic-header__title">{{ basicProfile.title }}</p>

        <div v-if="basicProfile.contacts.length" class="resume-basic-header__contacts">
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

      <div v-if="jobIntentItems.length" class="resume-meta-pills academic-header__pills">
        <span v-for="item in jobIntentItems" :key="item.field" class="resume-meta-pill">
          <span class="resume-meta-pill__label">{{ item.label }}</span>
          <span>{{ item.value }}</span>
        </span>
      </div>
    </header>

    <div class="academic-rule" aria-hidden="true">
      <span class="academic-rule__tick"></span>
    </div>

    <div class="academic-body">
      <div v-if="layout.leadSections.length" class="academic-record">
        <span class="academic-record__caption">
          {{ locale === 'en' ? 'ACADEMIC RECORD' : '学术履历' }}
        </span>
        <ResumeSection
          v-for="section in layout.leadSections"
          :key="section.id"
          :section="section"
          :locale="locale"
          class="academic-credentials"
        />
      </div>

      <div class="academic-body__main">
        <ResumeSection
          v-for="section in orderedMainSections"
          :key="section.id"
          :section="section"
          :locale="locale"
          class="academic-main-section"
          :class="`academic-main-section--${String(section.sectionCode || 'custom').toLowerCase()}`"
        />
      </div>
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
      lead: ['EDUCATION'],
      main: [
        'PROJECTS',
        'EXPERIENCE',
        'INTERNSHIP',
        'CERTIFICATES',
        'LAC_CERTIFICATES',
        'CAMPUS',
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
const orderedMainSections = computed(() => {
  return [
    ...layout.value.mainSections,
    ...layout.value.remainingSections,
  ]
})
</script>

<style scoped>
/* ========================================
   科研学术版：出版物式抬头、教育档案和编号章节。
   ======================================== */
.resume-template--academic {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: #1c2d25;
  --resume-color-text: #35443c;
  --resume-color-text-soft: #5a685f;
  --resume-color-text-faint: #7c877f;
  --resume-color-accent: #526b58;
  --resume-color-accent-soft: #edf2ed;
  --resume-color-border: #d9dfd9;
  --resume-color-surface: #f6f8f5;
  --resume-color-surface-strong: #edf2ed;

  --resume-font-name: 31px;
  --resume-font-title: 11px;
  --resume-font-contact: 9px;
  --resume-font-pill: 8.5px;
  --resume-section-title-size: 12px;

  padding: 42px 50px 48px;
}

/* ---------- 抬头：左侧姓名，右侧研究方向 ---------- */
.academic-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: end;
  gap: 18px 24px;
  padding: 14px 0 17px;
  border-top: 2px solid var(--resume-color-heading);
  border-bottom: 1px solid var(--resume-color-border);
  text-align: left;
}

.academic-header--has-intent {
  grid-template-columns: minmax(0, 1fr) minmax(180px, 235px);
}

.academic-header::before {
  grid-column: 1 / -1;
  color: var(--resume-color-accent);
  content: 'ACADEMIC PROFILE';
  font-size: 7.5px;
  font-weight: 750;
  letter-spacing: 0.2em;
}

.academic-header .resume-basic-header__identity {
  width: auto;
}

.academic-header .resume-basic-header__name {
  font-size: var(--resume-font-name);
  letter-spacing: 0.005em;
  line-height: 1.05;
}

.academic-header .resume-basic-header__title {
  margin-top: 7px;
  color: var(--resume-color-accent);
  font-family: var(--resume-font-display);
  font-style: italic;
  letter-spacing: 0.015em;
}

/* 联系方式作为姓名下方的刊头信息，保留字段标签便于快速查找。 */
.academic-header .resume-basic-header__contacts {
  justify-content: flex-start;
  gap: 4px 12px;
  margin-top: 12px;
}

.academic-header .resume-basic-header__contact {
  font-size: var(--resume-font-contact);
}

.academic-header .resume-basic-header__contact-label {
  color: var(--resume-color-text-faint);
  font-size: 8px;
  font-weight: 650;
}

.academic-header__pills {
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
}

.academic-header__pills :deep(.resume-meta-pill) {
  max-width: 100%;
  padding: 3px 7px;
  border: 1px solid var(--resume-color-border);
  border-radius: 2px;
  background: transparent;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-pill);
}

.academic-header__pills :deep(.resume-meta-pill__label) {
  color: var(--resume-color-text-faint);
}

/* ---------- 刊头分隔：短刻度从页边开始，而非居中装饰 ---------- */
.academic-rule {
  position: relative;
  height: 1px;
  margin-top: 0;
  background: var(--resume-color-border);
}

.academic-rule__tick {
  position: absolute;
  top: -1px;
  left: 0;
  width: 64px;
  height: 3px;
  background: var(--resume-color-accent);
}

/* ---------- 教育档案：学历与专业形成清晰横向层级 ---------- */
.academic-body {
  display: flex;
  flex-direction: column;
  gap: 17px;
  margin-top: 17px;
}

.academic-record {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr);
  align-items: start;
  gap: 20px;
  padding: 14px 0 15px;
  border-bottom: 1px solid var(--resume-color-border);
}

.academic-record__caption {
  padding-top: 3px;
  color: var(--resume-color-accent);
  font-family: var(--resume-font-display);
  font-size: 9px;
  font-style: italic;
  letter-spacing: 0.08em;
}

.academic-credentials :deep(.resume-section-block__head) {
  margin-bottom: 7px;
  padding: 0;
  border: 0;
}

.academic-credentials :deep(.resume-section-block__title) {
  font-size: 12px;
  letter-spacing: 0.04em;
}

.academic-credentials :deep(.resume-entry-list) {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 10px 16px;
}

.academic-credentials :deep(.resume-entry) {
  gap: 3px;
  padding: 0;
  border: 0;
}

.academic-credentials :deep(.resume-entry__title) {
  font-family: var(--resume-font-display);
  font-size: 11px;
}

.academic-credentials :deep(.resume-entry__subtitle) {
  color: var(--resume-color-text-soft);
  font-size: 8.5px;
}

.academic-credentials :deep(.resume-entry__date),
.academic-credentials :deep(.resume-entry__meta) {
  font-size: 8px;
}

/* ---------- 研究与经历：期刊式章节序号 + 悬挂条目 ---------- */
.academic-body__main {
  display: flex;
  flex-direction: column;
  gap: 16px;
  counter-reset: academic-section;
}

.academic-body__main :deep(.resume-section-block) {
  counter-increment: academic-section;
}

.academic-body__main :deep(.resume-section-block__head) {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  align-items: baseline;
  gap: 8px;
  margin-bottom: 7px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--resume-color-border);
}

.academic-body__main :deep(.resume-section-block__head)::before {
  content: counter(academic-section, decimal-leading-zero);
  color: var(--resume-color-accent);
  font-size: 8px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.04em;
}

.academic-body__main :deep(.resume-section-block__title) {
  font-family: var(--resume-font-display);
  color: var(--resume-color-heading);
  font-size: var(--resume-section-title-size);
  letter-spacing: 0.06em;
}

.academic-body__main :deep(.resume-entry) {
  padding: 0 0 10px 12px;
  border-bottom: 1px solid var(--resume-color-border);
}

.academic-body__main :deep(.resume-entry:last-child) {
  padding-bottom: 0;
  border-bottom: none;
}

.academic-body__main :deep(.resume-entry__title) {
  font-family: var(--resume-font-display);
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.005em;
}

.academic-body__main :deep(.resume-entry__subtitle) {
  font-family: var(--resume-font-display);
  font-style: italic;
}

.academic-body__main :deep(.resume-entry__date) {
  padding-top: 1px;
  color: var(--resume-color-accent);
  font-size: 8px;
  letter-spacing: 0.02em;
}

.academic-body__main :deep(.resume-entry__description),
.academic-body__main :deep(.resume-entry__bullets),
.academic-body__main :deep(.resume-rich-text) {
  line-height: 1.55;
}

.academic-body__main :deep(.academic-main-section--projects .resume-entry) {
  padding-top: 1px;
  padding-bottom: 12px;
}

.academic-body__main :deep(.academic-main-section--projects .resume-entry__title) {
  font-size: 11.5px;
}

.academic-body__main :deep(.resume-chip) {
  padding: 2px 7px;
  border: 1px solid var(--resume-color-border);
  border-radius: 2px;
  background: transparent;
  color: var(--resume-color-text-soft);
  font-weight: 550;
}

@media print {
  .resume-template--academic {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}

@media (max-width: 720px) {
  .academic-header {
    grid-template-columns: minmax(0, 1fr);
  }

  .academic-header__pills {
    align-items: flex-start;
  }

  .academic-record {
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
  }
}
</style>
