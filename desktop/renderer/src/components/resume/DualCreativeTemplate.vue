<template>
  <article
    class="resume-template resume-template--dual-creative"
    :class="{ 'resume-template--dual-creative-single': !hasSidebarContent }"
  >
    <header class="dual-creative-hero">
      <div class="dual-creative-hero__copy">
        <p class="dual-creative-hero__eyebrow">
          {{ locale === 'en' ? 'PROFILE / CAPABILITY INDEX' : '职业定位 / 能力索引' }}
        </p>
        <h1 class="dual-creative-hero__name">{{ basicProfile.name }}</h1>
        <p v-if="basicProfile.title" class="dual-creative-hero__title">{{ basicProfile.title }}</p>
      </div>

      <div v-if="basicProfile.contacts.length" class="dual-creative-hero__contacts">
        <span
          v-for="item in basicProfile.contacts"
          :key="item.field"
          class="dual-creative-hero__contact"
        >
          <span class="dual-creative-hero__contact-label">{{ item.label }}</span>
          <span>{{ item.value }}</span>
        </span>
      </div>

      <div v-if="basicProfile.avatar" class="dual-creative-hero__portrait">
        <img :src="basicProfile.avatar" alt="简历头像" class="dual-creative-hero__avatar" />
      </div>
      <div v-else-if="profileInitial" class="dual-creative-hero__monogram" aria-hidden="true">
        {{ profileInitial }}
      </div>
    </header>

    <div v-if="jobIntentItems.length" class="dual-creative-pills">
      <span v-for="item in jobIntentItems" :key="item.field" class="resume-meta-pill dual-creative-pill">
        <span class="resume-meta-pill__label">{{ item.label }}</span>
        <span>{{ item.value }}</span>
      </span>
    </div>

    <div class="dual-creative-body">
      <aside v-if="hasSidebarContent" class="dual-creative-sidebar">
        <p class="dual-creative-sidebar__label">{{ locale === 'en' ? 'INDEX' : '能力与背景' }}</p>
        <ResumeSection
          v-for="section in layout.asideSections"
          :key="section.id"
          :section="section"
          :locale="locale"
          compact
        />
      </aside>

      <main class="dual-creative-main">
        <ResumeSection
          v-for="section in orderedSections"
          :key="section.id"
          :section="section"
          :locale="locale"
        />
      </main>
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
      aside: ['SKILLS', 'EDUCATION', 'CERTIFICATES', 'LAC_CERTIFICATES', 'SELF_EVALUATION'],
      main: [
        'PROJECTS',
        'EXPERIENCE',
        'INTERNSHIP',
        'CAMPUS',
      ],
    },
    props.locale,
  )
})

const basicProfile = computed(() => buildBasicProfile(layout.value.basic, props.locale))
const jobIntentItems = computed(() => buildJobIntentItems(layout.value.jobIntent, props.locale))
const profileInitial = computed(() => String(basicProfile.value.name || '').trim().charAt(0))
const hasSidebarContent = computed(() => layout.value.asideSections.length > 0)
const orderedSections = computed(() => {
  return [
    ...layout.value.leadSections,
    ...layout.value.mainSections,
    ...layout.value.remainingSections,
  ]
})
</script>

<style scoped>
/* 复合能力双栏：窄栏索引能力，宽栏展开项目与经历。 */
.resume-template--dual-creative {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: #202d30;
  --resume-color-text: #39494c;
  --resume-color-text-soft: #617175;
  --resume-color-text-faint: #829093;
  --resume-color-accent: #0a6269;
  --resume-color-accent-soft: #e5f0ee;
  --resume-color-border: #d7e0de;
  --resume-color-surface: #f5f8f7;
  --resume-color-surface-strong: #e5f0ee;

  --resume-font-name: 27px;
  --resume-font-title: 12.5px;
  --resume-font-contact: 10px;
  --resume-font-pill: 9.5px;
  --resume-section-title-size: 11px;

  min-height: 1123px;
}

/* ---------- 抬头：定位、联系方式与姓名标记 ---------- */
.dual-creative-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 280px) 64px;
  align-items: end;
  gap: 20px;
  padding: 28px 36px 18px;
  border-bottom: 1px solid var(--resume-color-border);
}

.dual-creative-hero__copy {
  min-width: 0;
}

.dual-creative-hero__eyebrow {
  margin: 0 0 9px;
  color: var(--resume-color-accent);
  font-size: 7px;
  font-weight: 800;
  letter-spacing: 0.13em;
  text-transform: uppercase;
}

.dual-creative-hero__name {
  margin: 0;
  color: var(--resume-color-heading);
  font-family: var(--resume-font-display);
  font-size: 30px;
  font-weight: 650;
  line-height: 1.05;
  letter-spacing: -0.02em;
}

.dual-creative-hero__title {
  margin: 6px 0 0;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-title);
  font-weight: 600;
}

.dual-creative-hero__contacts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 12px;
  max-width: 280px;
}

.dual-creative-hero__contact {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: baseline;
  gap: 5px;
  min-width: 0;
  padding: 4px 0;
  border-bottom: 1px solid var(--resume-color-border);
  font-size: var(--resume-font-contact);
  color: var(--resume-color-text);
}

.dual-creative-hero__contact-label {
  color: var(--resume-color-text-faint);
  font-weight: 600;
}

.dual-creative-hero__portrait,
.dual-creative-hero__monogram {
  display: grid;
  width: 64px;
  height: 76px;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--resume-color-accent);
  background: var(--resume-color-accent);
}

.dual-creative-hero__avatar {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dual-creative-hero__monogram {
  color: var(--ui-neutral-0);
  font-family: var(--resume-font-display);
  font-size: 30px;
  font-weight: 650;
  line-height: 1;
}

/* ---------- 求职意向 ---------- */
.dual-creative-pills {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 12px 18px;
  padding: 11px 36px 13px;
  border-bottom: 1px solid var(--resume-color-border);
}

.dual-creative-pill {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 5px 0 0;
  border: 0;
  border-top: 1px solid var(--resume-color-accent);
  border-radius: 0;
  background: transparent;
  color: var(--resume-color-text);
}

/* ---------- 正文：背景索引栏与项目叙事栏 ---------- */
.dual-creative-body {
  display: grid;
  grid-template-columns: minmax(0, 0.31fr) minmax(0, 0.69fr);
  align-items: stretch;
  padding: 0 36px 28px;
}

.resume-template--dual-creative-single .dual-creative-body {
  grid-template-columns: minmax(0, 1fr);
}

.resume-template--dual-creative-single .dual-creative-body {
  grid-template-columns: minmax(0, 1fr);
}

.resume-template--dual-creative-single .dual-creative-main {
  padding-left: 0;
}

.dual-creative-sidebar {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 17px 15px 20px 0;
  border-right: 1px solid var(--resume-color-border);
  background: var(--resume-sidebar-chip-bg, var(--resume-color-accent-soft));
}

.dual-creative-sidebar__label {
  margin: 0;
  color: var(--resume-sidebar-accent, var(--resume-color-accent));
  font-size: 7px;
  font-weight: 800;
  letter-spacing: 0.13em;
  text-transform: uppercase;
}

.dual-creative-main {
  counter-reset: dual-entry;
  display: flex;
  flex-direction: column;
  gap: 15px;
  min-width: 0;
  padding: 17px 0 0 18px;
}

.dual-creative-main :deep(.resume-entry) {
  counter-increment: dual-entry;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  column-gap: 11px;
  padding: 0 0 12px;
  border: 0;
  border-bottom: 1px solid var(--resume-color-border);
}

.dual-creative-main :deep(.resume-entry__title::before) {
  margin-right: 6px;
  color: var(--resume-color-text-faint);
  content: 'ROLE';
  font-size: 6px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.dual-creative-main :deep(.resume-entry::before) {
  grid-column: 1;
  grid-row: 1 / span 6;
  padding-top: 1px;
  border-right: 1px solid var(--resume-color-accent);
  color: var(--resume-color-accent);
  content: counter(dual-entry, decimal-leading-zero);
  font-family: var(--resume-font-display);
  font-size: 9px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.dual-creative-main :deep(.resume-entry__header),
.dual-creative-main :deep(.resume-entry__meta),
.dual-creative-main :deep(.resume-entry__description),
.dual-creative-main :deep(.resume-entry__bullets),
.dual-creative-main :deep(.resume-entry > .resume-chip-list) {
  grid-column: 2;
}

/* 模块标题：标题前一个强调色小方块 */
.resume-template--dual-creative :deep(.resume-section-block__title) {
  display: flex;
  align-items: center;
  gap: 7px;
  letter-spacing: 0.08em;
}

.resume-template--dual-creative :deep(.resume-section-block__title)::before {
  content: '';
  flex-shrink: 0;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--resume-color-accent);
}

.resume-template--dual-creative :deep(.resume-section-block__head) {
  padding-bottom: 5px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--dual-creative :deep(.resume-entry) {
  padding-bottom: 12px;
}

.resume-template--dual-creative :deep(.resume-entry:last-child) {
  padding-bottom: 0;
  border-bottom: none;
}

/* 左栏内的文字跟随「左栏配色」设置 */
.dual-creative-sidebar :deep(.resume-section-block__head) {
  border-bottom-color: var(--resume-sidebar-divider, var(--resume-color-border));
}

.dual-creative-sidebar :deep(.resume-section-block__title)::before {
  background: var(--resume-sidebar-accent, var(--resume-color-accent));
}

.dual-creative-sidebar :deep(.resume-section-block__title),
.dual-creative-sidebar :deep(.resume-entry__title) {
  color: var(--resume-sidebar-heading, var(--resume-color-heading));
}

.dual-creative-sidebar :deep(.resume-entry),
.dual-creative-sidebar :deep(.resume-kv) {
  background: var(--resume-sidebar-panel-bg, transparent);
  border-color: var(--resume-sidebar-panel-border, transparent);
}

.dual-creative-sidebar :deep(.resume-entry__subtitle),
.dual-creative-sidebar :deep(.resume-entry__meta),
.dual-creative-sidebar :deep(.resume-entry__date),
.dual-creative-sidebar :deep(.resume-kv__label),
.dual-creative-sidebar :deep(.resume-chip__meta) {
  color: var(--resume-sidebar-text-faint, var(--resume-color-text-faint));
}

.dual-creative-sidebar :deep(.resume-rich-text),
.dual-creative-sidebar :deep(.resume-entry__description),
.dual-creative-sidebar :deep(.resume-entry__bullets),
.dual-creative-sidebar :deep(.resume-kv__value),
.dual-creative-sidebar :deep(.resume-bullet-list) {
  color: var(--resume-sidebar-text, var(--resume-color-text));
}

.dual-creative-sidebar :deep(.resume-chip) {
  background: var(--resume-sidebar-chip-bg, var(--resume-color-surface-strong));
  color: var(--resume-sidebar-accent, var(--resume-color-accent));
}

/* 打印时保留抬头色带 */
@media print {
  .resume-template--dual-creative {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .dual-creative-sidebar,
  .dual-creative-hero__monogram {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
