<template>
  <article
    class="resume-template resume-template--dual-creative"
    :class="{ 'resume-template--dual-creative-single': !hasSidebarContent }"
  >
    <header class="dual-creative-hero">
      <div class="dual-creative-hero__copy">
        <h1 class="dual-creative-hero__name">{{ basicProfile.name }}</h1>
        <p v-if="basicProfile.title" class="dual-creative-hero__title">{{ basicProfile.title }}</p>

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
      </div>

      <img
        v-if="basicProfile.avatar"
        :src="basicProfile.avatar"
        alt="avatar"
        class="resume-basic-header__avatar dual-creative-hero__avatar"
      />
    </header>

    <div v-if="jobIntentItems.length" class="dual-creative-pills">
      <span v-for="item in jobIntentItems" :key="item.field" class="resume-meta-pill dual-creative-pill">
        <span class="resume-meta-pill__label">{{ item.label }}</span>
        <span>{{ item.value }}</span>
      </span>
    </div>

    <div class="dual-creative-body">
      <aside v-if="hasSidebarContent" class="dual-creative-sidebar">
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
.resume-template--dual-creative {
  --resume-color-accent: #92400e;
  --resume-color-accent-soft: rgba(146, 64, 14, 0.12);
  --resume-color-border: #e8d5b7;
  --resume-color-surface: #fffdf8;
  --resume-color-surface-strong: #fef3c7;
  --resume-color-heading: #451a03;
  --resume-color-text: #451a03;
  --resume-color-text-soft: #78350f;
  --resume-color-text-faint: #a16207;
  min-height: 1123px;
  background: linear-gradient(180deg, #fffbeb 0%, #fef3c7 100%);
}

.dual-creative-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  padding: 36px 44px 28px;
  background: linear-gradient(135deg, #78350f 0%, #92400e 50%, #b45309 100%);
  color: #fefce8;
}

.dual-creative-hero__copy {
  min-width: 0;
  flex: 1;
}

.dual-creative-hero__name {
  margin: 0;
  font-size: 32px;
  font-weight: 800;
  line-height: 1.06;
  letter-spacing: 0.02em;
}

.dual-creative-hero__title {
  margin: 8px 0 0;
  color: rgba(254, 252, 232, 0.82);
  font-size: 14px;
  font-weight: 600;
}

.dual-creative-hero__contacts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 14px;
}

.dual-creative-hero__contact {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: rgba(254, 252, 232, 0.86);
}

.dual-creative-hero__contact-label {
  color: rgba(254, 252, 232, 0.6);
  font-weight: 700;
}

.dual-creative-hero__avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.2);
}

.dual-creative-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 14px 44px;
  background: rgba(146, 64, 14, 0.06);
  border-bottom: 1px solid var(--resume-color-border);
}

.dual-creative-pill {
  background: rgba(146, 64, 14, 0.1);
  color: var(--resume-color-heading);
}

.dual-creative-body {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 24px;
  padding: 28px 32px 34px;
}

.resume-template--dual-creative-single .dual-creative-body {
  grid-template-columns: 1fr;
}

.dual-creative-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dual-creative-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.resume-template--dual-creative :deep(.resume-section-block) {
  padding: 14px 16px;
  border: 1px solid var(--resume-color-border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.75);
}

.resume-template--dual-creative :deep(.resume-section-block__head) {
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(146, 64, 14, 0.16);
}

.resume-template--dual-creative :deep(.resume-section-block--compact) {
  background: rgba(255, 251, 235, 0.88);
}

.resume-template--dual-creative :deep(.resume-entry) {
  padding-bottom: 10px;
  border-bottom: 1px dashed rgba(146, 64, 14, 0.2);
}

.resume-template--dual-creative :deep(.resume-entry:last-child) {
  padding-bottom: 0;
  border-bottom: none;
}
</style>
