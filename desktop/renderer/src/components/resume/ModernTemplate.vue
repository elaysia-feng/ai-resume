<template>
  <article
    class="resume-template resume-template--modern"
    :class="{ 'resume-template--modern-single': !hasSidebarContent }"
  >
    <aside v-if="hasSidebarContent" class="modern-sidebar">
      <section class="modern-profile">
        <div v-if="basicProfile.avatar" class="modern-profile__avatar-wrap">
          <img :src="basicProfile.avatar" alt="avatar" class="resume-basic-header__avatar modern-profile__avatar" />
        </div>

        <div class="modern-profile__identity">
          <h1 class="modern-profile__name">{{ basicProfile.name }}</h1>
          <p v-if="basicProfile.title" class="modern-profile__title">{{ basicProfile.title }}</p>
        </div>
      </section>

      <section v-if="jobIntentItems.length" class="modern-sidebar__panel">
        <h2 class="modern-sidebar__panel-title">{{ locale === 'en' ? 'Target' : '求职意向' }}</h2>
        <div class="modern-intent-list">
          <div v-for="item in jobIntentItems" :key="item.field" class="modern-intent-item">
            <span class="modern-intent-item__label">{{ item.label }}</span>
            <span class="modern-intent-item__value">{{ item.value }}</span>
          </div>
        </div>
      </section>

      <section v-if="basicProfile.contacts.length" class="modern-sidebar__panel">
        <h2 class="modern-sidebar__panel-title">{{ locale === 'en' ? 'Contact' : '联系方式' }}</h2>
        <div class="modern-contact-list">
          <div v-for="item in basicProfile.contacts" :key="item.field" class="modern-contact-item">
            <span class="modern-contact-item__label">{{ item.label }}</span>
            <span class="modern-contact-item__value">{{ item.value }}</span>
          </div>
        </div>
      </section>

      <div class="modern-sidebar__sections">
        <ResumeSection
          v-for="section in layout.asideSections"
          :key="section.id"
          :section="section"
          :locale="locale"
          compact
        />
      </div>
    </aside>

    <main class="modern-main">
      <header v-if="!hasSidebarContent" class="modern-hero">
        <div class="modern-hero__intro">
          <h1 class="modern-hero__name">{{ basicProfile.name }}</h1>
          <p v-if="basicProfile.title" class="modern-hero__title">{{ basicProfile.title }}</p>
          <div v-if="!hasSidebarContent && basicProfile.contacts.length" class="resume-basic-header__contacts modern-hero__contacts">
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

        <div v-if="jobIntentItems.length" class="resume-meta-pills modern-hero__pills">
          <span v-for="item in jobIntentItems" :key="item.field" class="resume-meta-pill">
            <span class="resume-meta-pill__label">{{ item.label }}</span>
            <span>{{ item.value }}</span>
          </span>
        </div>
      </header>

      <div class="modern-main__sections" :class="{ 'modern-main__sections--with-header': !hasSidebarContent }">
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
      aside: ['SKILLS', 'CERTIFICATES', 'LAC_CERTIFICATES', 'CAMPUS'],
      main: [
        'SUMMARY',
        'SELF_EVALUATION',
        'EXPERIENCE',
        'PROJECTS',
        'INTERNSHIP',
        'EDUCATION',
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
   现代风：左侧信息栏 + 右侧主栏，留白多、层级清楚
   左栏配色跟随编辑器的「左栏配色」设置。
   ======================================== */
.resume-template--modern {
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

  --resume-font-name: 23px;
  --resume-font-title: 11px;
  --resume-font-contact: 9.5px;
  --resume-font-pill: 9.5px;
  --resume-section-title-size: 12px;

  display: grid;
  grid-template-columns: 218px minmax(0, 1fr);
  min-height: 1123px;
}

.resume-template--modern-single {
  grid-template-columns: minmax(0, 1fr);
}

/* ---------- 左栏 ---------- */
.modern-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 30px 20px 32px;
  background: var(--resume-sidebar-bg, var(--resume-color-surface));
  border-right: 1px solid var(--resume-sidebar-border, var(--resume-color-border));
}

.modern-profile {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.modern-profile__avatar-wrap {
  display: flex;
  justify-content: flex-start;
}

.modern-profile__avatar {
  width: 64px;
  height: 64px;
  border-radius: var(--ui-radius-md);
  border: 1px solid var(--resume-sidebar-panel-border, var(--resume-color-border));
}

.modern-profile__identity {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modern-profile__name {
  margin: 0;
  color: var(--resume-sidebar-heading, var(--resume-color-heading));
  font-size: 21px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: 0.01em;
}

/* 姓名下方一条短强调线，代替彩色装饰 */
.modern-profile__name::after {
  content: '';
  display: block;
  width: 32px;
  height: 2px;
  margin-top: 8px;
  background: var(--resume-sidebar-accent, var(--resume-color-accent));
}

.modern-profile__title {
  margin: 0;
  color: var(--resume-sidebar-text-soft, var(--resume-color-text-soft));
  font-size: var(--resume-font-title);
  font-weight: 600;
  line-height: 1.5;
}

.modern-sidebar__panel {
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding: 12px 13px;
  border-radius: var(--ui-radius-lg);
  background: var(--resume-sidebar-panel-bg, var(--resume-color-page));
  border: 1px solid var(--resume-sidebar-panel-border, var(--resume-color-border));
}

.modern-sidebar__panel-title {
  margin: 0;
  color: var(--resume-sidebar-heading, var(--resume-color-heading));
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.modern-intent-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.modern-intent-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.modern-intent-item__label {
  color: var(--resume-sidebar-text-faint, var(--resume-color-text-faint));
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.modern-intent-item__value {
  color: var(--resume-sidebar-text, var(--resume-color-text));
  font-size: 10.5px;
  font-weight: 700;
  line-height: 1.5;
}

.modern-contact-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.modern-contact-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.modern-contact-item__label {
  color: var(--resume-sidebar-text-faint, var(--resume-color-text-faint));
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.modern-contact-item__value {
  color: var(--resume-sidebar-text, var(--resume-color-text));
  font-size: 10.5px;
  line-height: 1.55;
  word-break: break-word;
}

.modern-sidebar__sections {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---------- 右栏 ---------- */
.modern-main {
  padding: 32px 32px 36px;
}

.modern-hero {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--resume-color-border);
}

.modern-hero__name {
  margin: 0;
  color: var(--resume-color-heading);
  font-size: 23px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: 0.01em;
}

.modern-hero__title {
  margin: 8px 0 0;
  color: var(--resume-color-text-soft);
  font-size: 12px;
  font-weight: 600;
}

.modern-hero__contacts {
  margin-top: 14px;
}

.modern-main__sections {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modern-main__sections--with-header {
  margin-top: 16px;
}

/* 模块标题：通栏细线 + 标题左侧强调条 */
.resume-template--modern :deep(.resume-section-block__head) {
  padding-bottom: 8px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--modern :deep(.resume-section-block__title) {
  padding-left: 9px;
  border-left: 3px solid var(--resume-color-accent);
  letter-spacing: 0.06em;
}

/* 条目做成浅底卡片，和正文拉开层次 */
.resume-template--modern :deep(.resume-entry) {
  padding: 10px 12px;
  border-radius: var(--ui-radius-md);
  background: var(--resume-color-surface);
  border: 1px solid var(--resume-color-border);
}

.resume-template--modern :deep(.resume-entry__title) {
  letter-spacing: 0.01em;
}

.resume-template--modern :deep(.resume-section-block--compact .resume-entry) {
  padding: 9px 10px;
}

/* 左栏内的文字改用左栏配色变量，深色左栏也能读 */
.modern-sidebar :deep(.resume-section-block__head) {
  border-bottom-color: var(--resume-sidebar-divider, var(--resume-color-border));
}

.modern-sidebar :deep(.resume-entry),
.modern-sidebar :deep(.resume-kv) {
  background: var(--resume-sidebar-panel-bg, transparent);
  border-color: var(--resume-sidebar-panel-border, transparent);
}

.modern-sidebar :deep(.resume-section-block__title),
.modern-sidebar :deep(.resume-entry__title) {
  color: var(--resume-sidebar-heading, var(--resume-color-heading));
}

.modern-sidebar :deep(.resume-entry__subtitle),
.modern-sidebar :deep(.resume-entry__meta),
.modern-sidebar :deep(.resume-entry__date),
.modern-sidebar :deep(.resume-kv__label),
.modern-sidebar :deep(.resume-chip__meta) {
  color: var(--resume-sidebar-text-faint, var(--resume-color-text-faint));
}

.modern-sidebar :deep(.resume-rich-text),
.modern-sidebar :deep(.resume-entry__description),
.modern-sidebar :deep(.resume-entry__bullets),
.modern-sidebar :deep(.resume-kv__value),
.modern-sidebar :deep(.resume-bullet-list) {
  color: var(--resume-sidebar-text, var(--resume-color-text));
}

.modern-sidebar :deep(.resume-chip) {
  background: var(--resume-sidebar-chip-bg, var(--resume-color-surface-strong));
  color: var(--resume-sidebar-accent, var(--resume-color-accent));
}

/* 打印时保留左栏底色 */
@media print {
  .resume-template--modern {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
