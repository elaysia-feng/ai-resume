<template>
  <article class="resume-template resume-template--academic">
    <header class="resume-basic-header academic-header">
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
          class="resume-basic-header__avatar academic-header__avatar"
        />
      </div>

      <div v-if="jobIntentItems.length" class="resume-meta-pills academic-header__pills">
        <span v-for="item in jobIntentItems" :key="item.field" class="resume-meta-pill">
          <span class="resume-meta-pill__label">{{ item.label }}</span>
          <span>{{ item.value }}</span>
        </span>
      </div>
    </header>

    <div class="academic-divider academic-header__divider"></div>

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
.resume-template--academic {
  --resume-color-accent: #1e3a5f;
  --resume-color-accent-soft: rgba(30, 58, 95, 0.1);
  --resume-color-border: #c8d6e5;
  --resume-color-surface: #f7f9fc;
  --resume-color-surface-strong: #edf2f7;
  --resume-color-heading: #0f2744;
  --resume-color-text: #1a365d;
  --resume-color-text-soft: #2d4a7a;
  --resume-color-text-faint: #5a7ba8;
  padding: 48px 52px 54px;
}

.academic-header__avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  border: 3px solid var(--resume-color-accent);
}

.academic-header__pills {
  padding-top: 2px;
}

.academic-header__divider {
  margin-top: 20px;
  height: 3px;
  background: linear-gradient(90deg, var(--resume-color-accent), var(--resume-color-accent-soft), transparent);
  border-radius: 2px;
}

.academic-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 22px;
}

.resume-template--academic :deep(.resume-section-block__head) {
  padding-bottom: 8px;
  border-bottom: 2px solid var(--resume-color-accent);
}

.resume-template--academic :deep(.resume-section-block__title) {
  color: var(--resume-color-accent);
  font-weight: 800;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.resume-template--academic :deep(.resume-entry) {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--resume-color-border);
}

.resume-template--academic :deep(.resume-entry:last-child) {
  padding-bottom: 0;
  border-bottom: none;
}

.resume-template--academic :deep(.resume-entry__title) {
  color: var(--resume-color-heading);
  font-weight: 700;
}

.resume-template--academic :deep(.resume-entry__date) {
  padding-top: 2px;
  color: var(--resume-color-text-faint);
}
</style>
