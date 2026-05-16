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
.resume-template--classic {
  --resume-color-accent: #1d4f91;
  --resume-color-accent-soft: rgba(29, 79, 145, 0.1);
  --resume-color-border: #d8e1ec;
  --resume-color-surface: #f8fbff;
  --resume-color-surface-strong: #eef4fb;
  --resume-color-heading: #102a43;
  --resume-color-text: #243b53;
  --resume-color-text-soft: #486581;
  --resume-color-text-faint: #7b8794;
  padding: 54px 56px 58px;
}

.classic-header__avatar {
  width: 104px;
  height: 104px;
  border-radius: 16px;
}

.classic-header__pills {
  padding-top: 2px;
}

.classic-header__divider {
  margin-top: 24px;
  background: linear-gradient(90deg, var(--resume-color-accent), rgba(29, 79, 145, 0.15));
}

.classic-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-top: 24px;
}

.resume-template--classic :deep(.resume-section-block__head) {
  padding-bottom: 8px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--classic :deep(.resume-entry) {
  padding-bottom: 14px;
  border-bottom: 1px dashed rgba(123, 135, 148, 0.28);
}

.resume-template--classic :deep(.resume-entry:last-child) {
  padding-bottom: 0;
  border-bottom: none;
}

.resume-template--classic :deep(.resume-entry__date) {
  padding-top: 2px;
}
</style>
