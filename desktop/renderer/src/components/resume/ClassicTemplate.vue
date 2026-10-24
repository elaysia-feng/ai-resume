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

        <img
          v-if="basicProfile.avatar"
          :src="basicProfile.avatar"
          alt="avatar"
          class="resume-basic-header__avatar classic-header__avatar"
        />
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
      lead: ['SUMMARY', 'SELF_EVALUATION'],
      main: [
        'EXPERIENCE',
        'PROJECTS',
        'INTERNSHIP',
        'EDUCATION',
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
   经典稳重版：单栏通排 + 竖向强调条 + 细分隔线
   纸面配色固定浅色（只用 --ui-* 中性 / 品牌色阶推导），
   屏幕预览、打印、导出 PDF 观感一致。
   ======================================== */
.resume-template--classic {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: var(--ui-neutral-900);
  --resume-color-text: var(--ui-neutral-700);
  --resume-color-text-soft: var(--ui-neutral-600);
  --resume-color-text-faint: var(--ui-neutral-500);
  --resume-color-accent: var(--ui-brand-700);
  --resume-color-accent-soft: var(--ui-brand-50);
  --resume-color-border: var(--ui-neutral-200);
  --resume-color-surface: var(--ui-neutral-50);
  --resume-color-surface-strong: var(--ui-brand-50);

  --resume-font-name: 25px;
  --resume-font-title: 12px;
  --resume-font-contact: 10px;
  --resume-font-pill: 10px;
  --resume-section-title-size: 12px;

  padding: 54px 58px 60px;
}

/* ---------- 抬头 ---------- */
.classic-header .resume-basic-header__identity {
  padding-left: 14px;
  border-left: 3px solid var(--resume-color-accent);
}

.classic-header .resume-basic-header__name {
  font-size: var(--resume-font-name);
  letter-spacing: 0.02em;
}

.classic-header .resume-basic-header__title {
  margin-top: 6px;
  letter-spacing: 0.02em;
}

.classic-header .resume-basic-header__contacts {
  gap: 2px var(--ui-space-5);
  margin-top: var(--ui-space-3);
}

.classic-header .resume-basic-header__contact {
  font-size: var(--resume-font-contact);
}

.classic-header .resume-basic-header__contact-label {
  color: var(--resume-color-text-faint);
  font-weight: 600;
}

.classic-header__avatar {
  width: 88px;
  height: 88px;
  border-radius: var(--ui-radius-md);
  border: 1px solid var(--resume-color-border);
}

.classic-header__pills {
  padding-top: var(--ui-space-2);
}

/* 抬头与正文之间：左侧一段强调色，右侧渐隐到分隔线 */
.classic-header__divider {
  height: 2px;
  margin-top: var(--ui-space-6);
  background: linear-gradient(90deg, var(--resume-color-accent) 0%, var(--resume-color-border) 62%);
}

/* ---------- 正文 ---------- */
.classic-body {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-6);
  margin-top: var(--ui-space-6);
}

/* 模块标题：克制的通栏细线，标题本身不上色 */
.resume-template--classic :deep(.resume-section-block__head) {
  padding-bottom: 7px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--classic :deep(.resume-section-block__title) {
  color: var(--resume-color-heading);
  letter-spacing: 0.06em;
}

/* 经历条目之间用细线分隔，末条不加分隔线 */
.resume-template--classic :deep(.resume-entry) {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--classic :deep(.resume-entry:last-child) {
  padding-bottom: 0;
  border-bottom: none;
}

.resume-template--classic :deep(.resume-entry__title) {
  letter-spacing: 0.01em;
}

.resume-template--classic :deep(.resume-entry__date) {
  padding-top: 2px;
  letter-spacing: 0.02em;
}

.resume-template--classic :deep(.resume-chip) {
  border: 1px solid var(--resume-color-border);
}

/* 打印时保留强调色块，避免只剩颜色区分的关键信息丢失 */
@media print {
  .resume-template--classic {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
