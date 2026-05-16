<template>
  <article
    class="resume-template resume-template--creative"
    :class="{ 'resume-template--creative-single': !hasSidebarContent }"
  >
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
        alt="avatar"
        class="resume-basic-header__avatar creative-hero__avatar"
      />
    </header>

    <div class="creative-body">
      <aside v-if="hasSidebarContent" class="creative-sidebar">
        <ResumeSection
          v-for="section in layout.asideSections"
          :key="section.id"
          :section="section"
          :locale="locale"
          compact
        />
      </aside>

      <main class="creative-main">
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
      aside: ['SELF_EVALUATION', 'SKILLS', 'CERTIFICATES', 'LAC_CERTIFICATES'],
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
.resume-template--creative {
  --resume-color-accent: #b45309;
  --resume-color-accent-soft: rgba(180, 83, 9, 0.12);
  --resume-color-border: #eadfcd;
  --resume-color-surface: #fffaf3;
  --resume-color-surface-strong: #f9edd9;
  --resume-color-heading: #35210f;
  --resume-color-text: #453427;
  --resume-color-text-soft: #6b4e35;
  --resume-color-text-faint: #8a715b;
  min-height: 1123px;
  background: linear-gradient(180deg, #fffdf8 0%, #fffaf3 100%);
}

.creative-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  padding: 42px 48px 34px;
  background: linear-gradient(135deg, #1f2937 0%, #24364a 100%);
  color: #f8fafc;
}

.creative-hero__copy {
  min-width: 0;
  flex: 1;
}

.creative-hero__eyebrow {
  margin: 0 0 10px;
  color: rgba(248, 250, 252, 0.72);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

.creative-hero__name {
  margin: 0;
  font-size: 34px;
  font-weight: 800;
  line-height: 1.06;
  letter-spacing: 0.02em;
}

.creative-hero__title {
  margin: 10px 0 0;
  color: rgba(248, 250, 252, 0.82);
  font-size: 15px;
  font-weight: 600;
}

.creative-hero__contacts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 16px;
}

.creative-hero__contact {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(248, 250, 252, 0.86);
}

.creative-hero__contact-label {
  color: rgba(248, 250, 252, 0.6);
  font-weight: 700;
}

.creative-hero__pills :deep(.resume-meta-pill) {
  background: rgba(255, 255, 255, 0.12);
  color: #f8fafc;
}

.creative-hero__avatar {
  width: 104px;
  height: 104px;
  border-radius: 22px;
  border-color: rgba(255, 255, 255, 0.18);
}

.creative-body {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 26px;
  padding: 30px 32px 36px;
}

.resume-template--creative-single .creative-body {
  grid-template-columns: 1fr;
}

.creative-sidebar {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.creative-main {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.resume-template--creative :deep(.resume-section-block) {
  padding: 16px 18px;
  border: 1px solid var(--resume-color-border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--resume-shadow-card);
}

.resume-template--creative :deep(.resume-section-block__head) {
  padding-bottom: 7px;
  border-bottom: 1px solid rgba(180, 83, 9, 0.16);
}

.resume-template--creative :deep(.resume-section-block--compact) {
  background: rgba(255, 253, 248, 0.92);
}

.resume-template--creative :deep(.resume-entry) {
  padding-bottom: 12px;
  border-bottom: 1px dashed rgba(138, 113, 91, 0.24);
}

.resume-template--creative :deep(.resume-entry:last-child) {
  padding-bottom: 0;
  border-bottom: none;
}
</style>
