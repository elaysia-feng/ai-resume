<template>
  <article class="resume-template resume-template--academic">
    <header class="resume-basic-header academic-header">
      <img
        v-if="basicProfile.avatar"
        :src="basicProfile.avatar"
        alt="avatar"
        class="resume-basic-header__avatar academic-header__avatar"
      />

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
      <ResumeSection
        v-for="section in orderedSections"
        :key="section.id"
        :section="section"
        :locale="locale"
      />
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
      main: [
        'EDUCATION',
        'EXPERIENCE',
        'PROJECTS',
        'INTERNSHIP',
        'SKILLS',
        'CERTIFICATES',
        'LAC_CERTIFICATES',
        'CAMPUS',
      ],
    },
    props.locale,
  )
})

const basicProfile = computed(() => buildBasicProfile(layout.value.basic, props.locale))
const jobIntentItems = computed(() => buildJobIntentItems(layout.value.jobIntent, props.locale))
const orderedSections = computed(() => {
  return [
    ...layout.value.leadSections,
    ...layout.value.mainSections,
    ...layout.value.remainingSections,
  ]
})
</script>

<style scoped>
/* ========================================
   学术科研版：居中抬头 + 通栏细线 + 衬线模块标题
   近乎单色的严谨版面，教育背景前置。
   ======================================== */
.resume-template--academic {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: var(--ui-neutral-900);
  --resume-color-text: var(--ui-neutral-700);
  --resume-color-text-soft: var(--ui-neutral-600);
  --resume-color-text-faint: var(--ui-neutral-500);
  --resume-color-accent: var(--ui-brand-700);
  --resume-color-accent-soft: var(--ui-brand-50);
  --resume-color-border: var(--ui-neutral-200);
  --resume-color-surface: var(--ui-neutral-50);
  --resume-color-surface-strong: var(--ui-neutral-100);

  --resume-font-name: 24px;
  --resume-font-title: 12px;
  --resume-font-contact: 9.5px;
  --resume-font-pill: 9.5px;
  --resume-section-title-size: 11.5px;

  padding: 50px 56px 56px;
}

/* ---------- 抬头：整体居中 ---------- */
.academic-header {
  align-items: center;
  gap: var(--ui-space-3);
  text-align: center;
}

.academic-header .resume-basic-header__identity {
  width: 100%;
}

.academic-header .resume-basic-header__name {
  font-size: var(--resume-font-name);
  letter-spacing: 0.08em;
}

.academic-header .resume-basic-header__title {
  margin-top: 8px;
  letter-spacing: 0.06em;
}

.academic-header__avatar {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  border: 1px solid var(--resume-color-border);
}

/* 联系方式排成居中一行，用中点分隔 */
.academic-header .resume-basic-header__contacts {
  justify-content: center;
  gap: 2px var(--ui-space-3);
  margin-top: var(--ui-space-4);
}

.academic-header .resume-basic-header__contact {
  font-size: var(--resume-font-contact);
}

.academic-header .resume-basic-header__contact:not(:last-child)::after {
  content: '·';
  margin-left: var(--ui-space-3);
  color: var(--resume-color-text-faint);
}

.academic-header .resume-basic-header__contact-label {
  color: var(--resume-color-text-faint);
  font-weight: 600;
}

.academic-header__pills {
  justify-content: center;
}

/* 抬头下方：短促的强调刻度 + 通栏细线 */
.academic-rule {
  position: relative;
  height: 1px;
  margin-top: var(--ui-space-6);
  background: var(--resume-color-border);
}

.academic-rule__tick {
  position: absolute;
  top: -1px;
  left: 50%;
  width: 72px;
  height: 3px;
  transform: translateX(-50%);
  background: var(--resume-color-accent);
}

/* ---------- 正文 ---------- */
.academic-body {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-5);
  margin-top: var(--ui-space-6);
}

/* 模块标题：衬线体 + 通栏细线，左端叠一小段强调色 */
.resume-template--academic :deep(.resume-section-block__head) {
  position: relative;
  padding-bottom: 7px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--academic :deep(.resume-section-block__head)::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 52px;
  height: 2px;
  background: var(--resume-color-accent);
}

.resume-template--academic :deep(.resume-section-block__title) {
  font-family: var(--resume-font-display);
  color: var(--resume-color-heading);
  letter-spacing: 0.12em;
}

/* 条目之间只用细线，不加卡片 */
.resume-template--academic :deep(.resume-entry) {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--academic :deep(.resume-entry:last-child) {
  padding-bottom: 0;
  border-bottom: none;
}

.resume-template--academic :deep(.resume-entry__title) {
  font-family: var(--resume-font-display);
  letter-spacing: 0.01em;
}

.resume-template--academic :deep(.resume-entry__date) {
  padding-top: 2px;
  letter-spacing: 0.02em;
}

.resume-template--academic :deep(.resume-chip) {
  border: 1px solid var(--resume-color-border);
  font-weight: 600;
}

/* 打印时保留强调刻度 */
@media print {
  .resume-template--academic {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
