<template>
  <article class="resume-template resume-template--classic">
    <header class="resume-basic-header classic-header">
      <div class="resume-basic-header__main">
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

      </div>

      <div v-if="jobIntentItems.length" class="resume-meta-pills classic-header__pills">
        <span v-for="item in jobIntentItems" :key="item.field" class="resume-meta-pill">
          <span class="resume-meta-pill__label">{{ item.label }}</span>
          <span>{{ item.value }}</span>
        </span>
      </div>
    </header>

    <div class="resume-divider classic-header__divider"></div>

    <div class="classic-body">
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
      lead: ['SUMMARY'],
      main: [
        'EXPERIENCE',
        'PROJECTS',
        'INTERNSHIP',
        'EDUCATION',
        'SKILLS',
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
   清晰投递版：单栏、无头像、按 ATS 扫读顺序排列经历
   纸面配色固定浅色（只用 --ui-* 中性 / 品牌色阶推导），
   屏幕预览、打印、导出 PDF 观感一致。
   ======================================== */
.resume-template--classic {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: #172633;
  --resume-color-text: #354450;
  --resume-color-text-soft: #5d6b75;
  --resume-color-text-faint: #73818b;
  --resume-color-accent: #1d4d70;
  --resume-color-accent-soft: #eef4f7;
  --resume-color-border: #d7e0e5;
  --resume-color-surface: #f7f9fa;
  --resume-color-surface-strong: #f2f6f8;

  --resume-font-name: 29px;
  --resume-font-title: 12px;
  --resume-font-contact: 10px;
  --resume-font-pill: 10px;
  --resume-section-title-size: 12px;

  padding: 48px 56px 52px;
}

/* ---------- 抬头 ---------- */
.classic-header {
  gap: 13px;
}

.classic-header .resume-basic-header__identity {
  padding: 0;
  border: 0;
}

.classic-header .resume-basic-header__name {
  font-size: var(--resume-font-name);
  letter-spacing: -0.025em;
  line-height: 1.05;
}

.classic-header .resume-basic-header__title {
  margin-top: 7px;
  color: var(--resume-color-accent);
  font-size: 12px;
  font-weight: 700;
}

.classic-header .resume-basic-header__contacts {
  gap: 4px 17px;
  margin-top: 13px;
}

.classic-header .resume-basic-header__contact {
  font-size: var(--resume-font-contact);
}

.classic-header .resume-basic-header__contact-label {
  color: var(--resume-color-text-faint);
  font-weight: 600;
}

.classic-header__pills {
  gap: 8px 12px;
  padding-top: 8px;
  border-top: 1px solid var(--resume-color-border);
}

.classic-header__pills .resume-meta-pill {
  padding: 0 12px 0 0;
  border-right: 1px solid var(--resume-color-border);
  border-radius: 0;
  background: transparent;
  color: var(--resume-color-text);
  font-size: 9px;
  font-weight: 600;
}

.classic-header__pills .resume-meta-pill:last-child {
  padding-right: 0;
  border-right: 0;
}

.classic-header__pills .resume-meta-pill__label {
  color: var(--resume-color-text-faint);
  font-weight: 500;
}

/* 抬头以单一实线收束，维持机器和人工都容易识别的阅读顺序。 */
.classic-header__divider {
  height: 2px;
  margin-top: 17px;
  background: var(--resume-color-accent);
}

/* ---------- 正文 ---------- */
.classic-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 18px;
}

/* 模块标题：克制的通栏细线，标题本身不上色 */
.resume-template--classic :deep(.resume-section-block__head) {
  padding-bottom: 5px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--classic :deep(.resume-section-block__title) {
  color: var(--resume-color-accent);
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 0.12em;
}

/* 记录条目以紧凑的横向节奏分隔，避免装饰卡片影响 ATS 解析。 */
.resume-template--classic :deep(.resume-entry) {
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--classic :deep(.resume-entry:last-child) {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.resume-template--classic :deep(.resume-entry__title) {
  color: var(--resume-color-heading);
  font-size: 11px;
  letter-spacing: 0;
}

.resume-template--classic :deep(.resume-entry__subtitle) {
  margin-top: 2px;
  color: var(--resume-color-text-soft);
}

.resume-template--classic :deep(.resume-entry__date) {
  padding-top: 1px;
  color: var(--resume-color-accent);
  font-size: 8px;
  letter-spacing: 0.04em;
}

.resume-template--classic :deep(.resume-chip) {
  border: 1px solid var(--resume-color-border);
  border-radius: 2px;
  background: var(--ui-neutral-0);
  color: var(--resume-color-text);
}

/* 打印时保留强调色块，避免只剩颜色区分的关键信息丢失 */
@media print {
  .resume-template--classic {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
