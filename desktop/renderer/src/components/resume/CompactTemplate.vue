<template>
  <article
    class="resume-template resume-template--compact"
    :class="{ 'resume-template--compact-single': !hasSidebarContent }"
  >
    <aside v-if="hasSidebarContent" class="compact-sidebar">
      <section class="compact-profile">
        <div v-if="basicProfile.avatar" class="compact-profile__avatar-wrap">
          <img :src="basicProfile.avatar" alt="avatar" class="resume-basic-header__avatar compact-profile__avatar" />
        </div>

        <div class="compact-profile__identity">
          <h1 class="compact-profile__name">{{ basicProfile.name }}</h1>
          <p v-if="basicProfile.title" class="compact-profile__title">{{ basicProfile.title }}</p>
        </div>
      </section>

      <section v-if="jobIntentItems.length" class="compact-sidebar__panel">
        <h2 class="compact-sidebar__panel-title">{{ locale === 'en' ? 'Target' : '求职意向' }}</h2>
        <div class="compact-intent-list">
          <div v-for="item in jobIntentItems" :key="item.field" class="compact-intent-item">
            <span class="compact-intent-item__label">{{ item.label }}</span>
            <span class="compact-intent-item__value">{{ item.value }}</span>
          </div>
        </div>
      </section>

      <section v-if="basicProfile.contacts.length" class="compact-sidebar__panel">
        <h2 class="compact-sidebar__panel-title">{{ locale === 'en' ? 'Contact' : '联系方式' }}</h2>
        <div class="compact-contact-list">
          <div v-for="item in basicProfile.contacts" :key="item.field" class="compact-contact-item">
            <span class="compact-contact-item__label">{{ item.label }}</span>
            <span class="compact-contact-item__value">{{ item.value }}</span>
          </div>
        </div>
      </section>

      <div class="compact-sidebar__sections">
        <ResumeSection
          v-for="section in layout.asideSections"
          :key="section.id"
          :section="section"
          :locale="locale"
          compact
        />
      </div>
    </aside>

    <main class="compact-main">
      <header v-if="!hasSidebarContent" class="compact-hero">
        <div class="compact-hero__intro">
          <h1 class="compact-hero__name">{{ basicProfile.name }}</h1>
          <p v-if="basicProfile.title" class="compact-hero__title">{{ basicProfile.title }}</p>
          <div v-if="!hasSidebarContent && basicProfile.contacts.length" class="resume-basic-header__contacts compact-hero__contacts">
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

        <div v-if="jobIntentItems.length" class="resume-meta-pills compact-hero__pills">
          <span v-for="item in jobIntentItems" :key="item.field" class="resume-meta-pill">
            <span class="resume-meta-pill__label">{{ item.label }}</span>
            <span>{{ item.value }}</span>
          </span>
        </div>
      </header>

      <div class="compact-main__sections" :class="{ 'compact-main__sections--with-header': !hasSidebarContent }">
        <ResumeSection
          v-for="section in orderedSections"
          :key="section.id"
          :section="section"
          :locale="locale"
        />
      </div>
    </main>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import ResumeSection from './ResumeSection.vue'
import { buildBasicProfile, buildJobIntentItems, buildTemplateLayout, formatNaturalText } from '../../utils/resumeTemplate.js'

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
      aside: ['SKILLS', 'CERTIFICATES', 'LAC_CERTIFICATES', 'SELF_EVALUATION'],
      main: [
        'SUMMARY',
        'EXPERIENCE',
        'PROJECTS',
        'INTERNSHIP',
        'EDUCATION',
        'CAMPUS',
      ],
    },
    props.locale,
  )
})

const basicProfile = computed(() => buildBasicProfile(layout.value.basic, props.locale))
const jobIntentItems = computed(() => buildJobIntentItems(layout.value.jobIntent, props.locale))
const rawBasicContent = computed(() => layout.value.basic?.content || {})
const hasSidebarContent = computed(() => {
  return Boolean(
    basicProfile.value.avatar
    || basicProfile.value.contacts.length
    || jobIntentItems.value.length
    || layout.value.asideSections.length
    || formatNaturalText(rawBasicContent.value.name)
    || formatNaturalText(rawBasicContent.value.title),
  )
})
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
   紧凑型：窄左栏 + 更小字号 + 更紧间距，一页塞下更多信息
   左栏配色跟随编辑器的「左栏配色」设置。
   ======================================== */
.resume-template--compact {
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

  --resume-font-name: 19px;
  --resume-font-title: 10px;
  --resume-font-contact: 8.5px;
  --resume-font-pill: 8.5px;
  --resume-section-title-size: 10.5px;

  display: grid;
  grid-template-columns: 186px minmax(0, 1fr);
  min-height: 1123px;
}

.resume-template--compact-single {
  grid-template-columns: minmax(0, 1fr);
}

/* ---------- 左栏 ---------- */
.compact-sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 24px 14px 26px;
  background: var(--resume-sidebar-bg, var(--resume-color-surface));
  border-right: 1px solid var(--resume-sidebar-border, var(--resume-color-border));
}

.compact-profile {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.compact-profile__avatar-wrap {
  display: flex;
  justify-content: flex-start;
}

.compact-profile__avatar {
  width: 52px;
  height: 52px;
  border-radius: var(--ui-radius-sm);
  border: 1px solid var(--resume-sidebar-panel-border, var(--resume-color-border));
}

.compact-profile__identity {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.compact-profile__name {
  margin: 0;
  color: var(--resume-sidebar-heading, var(--resume-color-heading));
  font-size: 18px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: 0.01em;
}

/* 姓名下方一条短强调线 */
.compact-profile__name::after {
  content: '';
  display: block;
  width: 26px;
  height: 2px;
  margin-top: 6px;
  background: var(--resume-sidebar-accent, var(--resume-color-accent));
}

.compact-profile__title {
  margin: 0;
  color: var(--resume-sidebar-text-soft, var(--resume-color-text-soft));
  font-size: 10px;
  font-weight: 600;
  line-height: 1.4;
}

.compact-sidebar__panel {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 9px 10px;
  border-radius: var(--ui-radius-md);
  background: var(--resume-sidebar-panel-bg, var(--resume-color-page));
  border: 1px solid var(--resume-sidebar-panel-border, var(--resume-color-border));
}

.compact-sidebar__panel-title {
  margin: 0;
  color: var(--resume-sidebar-heading, var(--resume-color-heading));
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.compact-intent-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.compact-intent-item {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.compact-intent-item__label {
  color: var(--resume-sidebar-text-faint, var(--resume-color-text-faint));
  font-size: 8px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.compact-intent-item__value {
  color: var(--resume-sidebar-text, var(--resume-color-text));
  font-size: 9px;
  font-weight: 700;
  line-height: 1.4;
}

.compact-contact-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.compact-contact-item {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.compact-contact-item__label {
  color: var(--resume-sidebar-text-faint, var(--resume-color-text-faint));
  font-size: 8px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.compact-contact-item__value {
  color: var(--resume-sidebar-text, var(--resume-color-text));
  font-size: 9px;
  line-height: 1.45;
  word-break: break-word;
}

.compact-sidebar__sections {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* ---------- 右栏 ---------- */
.compact-main {
  padding: 24px 22px 28px;
}

.compact-hero {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--resume-color-border);
}

.compact-hero__name {
  margin: 0;
  color: var(--resume-color-heading);
  font-size: 19px;
  font-weight: 800;
  line-height: 1.1;
}

.compact-hero__title {
  margin: 6px 0 0;
  color: var(--resume-color-text-soft);
  font-size: 11px;
  font-weight: 600;
}

.compact-hero__contacts {
  margin-top: 12px;
}

.compact-main__sections {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.compact-main__sections--with-header {
  margin-top: 12px;
}

/* 模块标题：标题文字下方一条短强调线 */
.resume-template--compact :deep(.resume-section-block__head) {
  padding-bottom: 5px;
}

.resume-template--compact :deep(.resume-section-block__title) {
  padding-bottom: 3px;
  border-bottom: 2px solid var(--resume-color-accent);
  letter-spacing: 0.06em;
}

/* 条目只用细线分隔，不加卡片，保证密度 */
.resume-template--compact :deep(.resume-entry) {
  padding-bottom: 8px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--compact :deep(.resume-entry:last-child) {
  padding-bottom: 0;
  border-bottom: none;
}

.resume-template--compact :deep(.resume-entry__title) {
  letter-spacing: 0;
}

.resume-template--compact :deep(.resume-chip) {
  padding: 2px 6px;
}

/* 左栏内的文字改用左栏配色变量 */
.compact-sidebar :deep(.resume-section-block__head) {
  border-bottom-color: var(--resume-sidebar-divider, var(--resume-color-border));
}

.compact-sidebar :deep(.resume-entry),
.compact-sidebar :deep(.resume-kv) {
  background: var(--resume-sidebar-panel-bg, transparent);
  border-color: var(--resume-sidebar-panel-border, transparent);
}

.compact-sidebar :deep(.resume-section-block__title),
.compact-sidebar :deep(.resume-entry__title) {
  color: var(--resume-sidebar-heading, var(--resume-color-heading));
}

.compact-sidebar :deep(.resume-entry__subtitle),
.compact-sidebar :deep(.resume-entry__meta),
.compact-sidebar :deep(.resume-entry__date),
.compact-sidebar :deep(.resume-kv__label),
.compact-sidebar :deep(.resume-chip__meta) {
  color: var(--resume-sidebar-text-faint, var(--resume-color-text-faint));
}

.compact-sidebar :deep(.resume-rich-text),
.compact-sidebar :deep(.resume-entry__description),
.compact-sidebar :deep(.resume-entry__bullets),
.compact-sidebar :deep(.resume-kv__value),
.compact-sidebar :deep(.resume-bullet-list) {
  color: var(--resume-sidebar-text, var(--resume-color-text));
}

.compact-sidebar :deep(.resume-chip) {
  background: var(--resume-sidebar-chip-bg, var(--resume-color-surface-strong));
  color: var(--resume-sidebar-accent, var(--resume-color-accent));
}

/* 打印时保留左栏底色 */
@media print {
  .resume-template--compact {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
