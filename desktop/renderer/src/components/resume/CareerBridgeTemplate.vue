<template>
  <article class="resume-template resume-template--career-bridge">
    <header class="career-header">
      <div class="career-header__masthead">
        <p class="career-header__label">
          {{ locale === 'en' ? 'CAREER BRIDGE' : '职业迁移履历' }}
        </p>
        <span class="career-header__edition">{{ locale === 'en' ? 'PROFILE' : '个人档案' }}</span>
      </div>

      <h1 class="career-header__name">{{ basicProfile.name }}</h1>

      <div v-if="basicProfile.contacts.length" class="career-header__contacts">
        <span v-for="item in basicProfile.contacts" :key="item.field" class="career-contact">
          <strong>{{ item.label }}</strong>
          <span>{{ item.value }}</span>
        </span>
      </div>
    </header>

    <section
      v-if="basicProfile.title || targetPosition"
      class="career-route"
      :aria-label="locale === 'en' ? 'Career direction' : '职业方向'"
    >
      <div class="career-route__line">
        <div v-if="basicProfile.title" class="career-route__step">
          <span class="career-route__label">{{ locale === 'en' ? 'CURRENT DIRECTION' : '当前方向' }}</span>
          <strong>{{ basicProfile.title }}</strong>
        </div>

        <span
          v-if="basicProfile.title && targetPosition"
          class="career-route__arrow"
          aria-hidden="true"
        >→</span>

        <div v-if="targetPosition" class="career-route__step career-route__step--target">
          <span class="career-route__label">{{ locale === 'en' ? 'TARGET ROLE' : '目标职位' }}</span>
          <strong>{{ targetPosition.value }}</strong>
        </div>
      </div>

      <div v-if="routeDetails.length" class="career-route__details">
        <span v-for="item in routeDetails" :key="item.field" class="career-route__detail">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </span>
      </div>
    </section>

    <main class="career-ledger">
      <ResumeSection
        v-for="section in orderedSections"
        :key="section.id"
        :section="section"
        :locale="locale"
      />
    </main>
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
        'SKILLS',
        'EXPERIENCE',
        'INTERNSHIP',
        'PROJECTS',
        'CAMPUS',
        'EDUCATION',
        'CERTIFICATES',
        'LAC_CERTIFICATES',
      ],
    },
    props.locale,
  )
})

const basicProfile = computed(() => buildBasicProfile(layout.value.basic, props.locale))
const jobIntentItems = computed(() => buildJobIntentItems(layout.value.jobIntent, props.locale))
const targetPosition = computed(() => {
  return jobIntentItems.value.find((item) => item.field === 'desiredPosition') || null
})
const routeDetails = computed(() => {
  return jobIntentItems.value.filter((item) => item.field !== 'desiredPosition')
})
const orderedSections = computed(() => [
  ...layout.value.leadSections,
  ...layout.value.mainSections,
  ...layout.value.remainingSections,
])
</script>

<style scoped>
.resume-template--career-bridge {
  --resume-color-page: var(--ui-neutral-0);
  --resume-color-heading: #17324a;
  --resume-color-text: #30465a;
  --resume-color-text-soft: #566d7f;
  --resume-color-text-faint: #788995;
  --resume-color-accent: #28766e;
  --resume-color-accent-soft: #eaf3f1;
  --resume-color-border: #d8e2e3;
  --resume-color-surface: #f4f8f7;
  --resume-color-surface-strong: #eaf3f1;
  --resume-font-name: 29px;
  --resume-font-title: 11px;
  --resume-font-contact: 8.5px;
  --resume-font-pill: 8.5px;
  --resume-section-title-size: 10.5px;
  --resume-text-size: 9px;
  min-height: 1123px;
  padding: 40px 50px 48px;
}

.career-header {
  padding-top: 12px;
  border-top: 3px solid var(--resume-color-heading);
  border-bottom: 1px solid var(--resume-color-border);
}

.career-header__masthead {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.career-header__label,
.career-header__edition {
  margin: 0;
  color: var(--resume-color-accent);
  font-size: 7.5px;
  font-weight: 750;
  letter-spacing: 0.16em;
}

.career-header__edition {
  color: var(--resume-color-text-faint);
}

.career-header__name {
  margin: 9px 0 0;
  color: var(--resume-color-heading);
  font-family: var(--resume-font-display);
  font-size: var(--resume-font-name);
  font-weight: 750;
  letter-spacing: -0.02em;
  line-height: 1.1;
}

.career-header__contacts {
  display: flex;
  flex-wrap: wrap;
  gap: 7px 18px;
  margin: 11px 0 13px;
}

.career-contact {
  display: inline-flex;
  min-width: 0;
  align-items: baseline;
  gap: 5px;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-font-contact);
  overflow-wrap: anywhere;
}

.career-contact strong {
  color: var(--resume-color-text-faint);
  font-size: 7.5px;
  font-weight: 650;
}

.career-route {
  margin-top: 16px;
  padding: 12px 14px 11px;
  border-top: 1px solid var(--resume-color-border);
  border-bottom: 1px solid var(--resume-color-border);
  background: var(--resume-color-surface);
}

.career-route__line {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.career-route__step {
  display: flex;
  min-width: 0;
  flex: 1 1 220px;
  flex-direction: column;
  gap: 4px;
}

.career-route__label {
  color: var(--resume-color-text-faint);
  font-size: 7px;
  font-weight: 750;
  letter-spacing: 0.14em;
}

.career-route__step strong {
  color: var(--resume-color-heading);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.career-route__step--target strong {
  color: var(--resume-color-accent);
}

.career-route__arrow {
  flex: 0 0 auto;
  color: var(--resume-color-accent);
  font-size: 19px;
  line-height: 1;
}

.career-route__details {
  display: flex;
  flex-wrap: wrap;
  gap: 5px 16px;
  margin-top: 9px;
  padding-top: 8px;
  border-top: 1px solid var(--resume-color-border);
}

.career-route__detail {
  display: inline-flex;
  gap: 5px;
  color: var(--resume-color-text-soft);
  font-size: 8px;
}

.career-route__detail > span {
  color: var(--resume-color-text-faint);
}

.career-route__detail strong {
  color: var(--resume-color-text);
  font-weight: 650;
}

.career-ledger {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 18px;
  counter-reset: career-section;
}

.career-ledger :deep(.resume-section-block) {
  counter-increment: career-section;
}

.career-ledger :deep(.resume-section-block__head) {
  display: grid;
  grid-template-columns: 25px minmax(0, 1fr);
  align-items: baseline;
  gap: 8px;
  margin-bottom: 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--resume-color-border);
}

.career-ledger :deep(.resume-section-block__head)::before {
  content: counter(career-section, decimal-leading-zero);
  color: var(--resume-color-accent);
  font-size: 8px;
  font-weight: 750;
  font-variant-numeric: tabular-nums;
}

.career-ledger :deep(.resume-section-block__title) {
  color: var(--resume-color-heading);
  font-size: var(--resume-section-title-size);
  font-weight: 750;
  letter-spacing: 0.035em;
}

.career-ledger :deep(.resume-entry) {
  padding: 0 0 9px 11px;
  border: 0;
  border-bottom: 1px solid var(--resume-color-border);
  border-radius: 0;
  background: transparent;
}

.career-ledger :deep(.resume-entry__header) {
  align-items: baseline;
}

.career-ledger :deep(.resume-entry__title) {
  color: var(--resume-color-heading);
  font-size: 10.5px;
  font-weight: 700;
}

.career-ledger :deep(.resume-entry__date) {
  color: var(--resume-color-accent);
  font-size: 8.5px;
  font-variant-numeric: tabular-nums;
}

.career-ledger :deep(.resume-entry__description),
.career-ledger :deep(.resume-entry__bullets),
.career-ledger :deep(.resume-rich-text) {
  line-height: 1.52;
}

.career-ledger :deep(.resume-chip) {
  padding: 2px 6px;
  border: 1px solid var(--resume-color-border);
  border-radius: 2px;
  background: var(--ui-neutral-0);
  color: var(--resume-color-text-soft);
}

@media print {
  .resume-template--career-bridge {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
