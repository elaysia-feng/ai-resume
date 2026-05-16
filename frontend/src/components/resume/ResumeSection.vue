<template>
  <section class="resume-section-block" :class="sectionClasses">
    <header v-if="showTitle" class="resume-section-block__head">
      <h2 class="resume-section-block__title">{{ sectionData.displayTitle }}</h2>
    </header>

    <div class="resume-section-block__body">
      <div v-if="isJobIntentSection" class="resume-kv-grid">
        <div v-for="item in jobIntentItems" :key="item.field" class="resume-kv">
          <span class="resume-kv__label">{{ item.label }}</span>
          <span class="resume-kv__value">{{ item.value }}</span>
        </div>
      </div>

      <RichTextContent
        v-else-if="isTextSection"
        :html="sectionData.content.text"
        class="resume-rich-text"
      />

      <div v-else-if="isSkillsSection" class="resume-chip-list">
        <span
          v-for="(item, index) in normalizedItems"
          :key="index"
          class="resume-chip"
        >
          <span>{{ item.name || item }}</span>
          <span v-if="item.proficiency" class="resume-chip__meta">{{ item.proficiency }}</span>
        </span>
      </div>

      <div v-else-if="isDetailSection" class="resume-entry-list">
        <article
          v-for="(item, index) in normalizedItems"
          :key="index"
          class="resume-entry"
        >
          <div class="resume-entry__header">
            <div class="resume-entry__main">
              <h3 class="resume-entry__title">{{ detailTitle(item) }}</h3>
              <p v-if="detailSubtitle(item)" class="resume-entry__subtitle">{{ detailSubtitle(item) }}</p>
            </div>
            <span v-if="detailDate(item)" class="resume-entry__date">{{ detailDate(item) }}</span>
          </div>

          <p v-if="detailMeta(item)" class="resume-entry__meta">{{ detailMeta(item) }}</p>

          <RichTextContent
            v-if="item.description"
            :html="item.description"
            class="resume-entry__description"
          />

          <ul v-if="item.bullets?.length" class="resume-entry__bullets">
            <li v-for="(bullet, bulletIndex) in item.bullets" :key="bulletIndex">{{ bullet }}</li>
          </ul>

          <div v-if="item.techStackList?.length" class="resume-chip-list resume-chip-list--compact">
            <span v-for="(tech, techIndex) in item.techStackList" :key="techIndex" class="resume-chip">
              {{ tech }}
            </span>
          </div>
        </article>
      </div>

      <div v-else-if="isEducationSection" class="resume-entry-list">
        <article
          v-for="(item, index) in normalizedItems"
          :key="index"
          class="resume-entry"
        >
          <div class="resume-entry__header">
            <div class="resume-entry__main">
              <h3 class="resume-entry__title">{{ item.school }}</h3>
              <p v-if="educationSubtitle(item)" class="resume-entry__subtitle">{{ educationSubtitle(item) }}</p>
            </div>
            <span v-if="item.date" class="resume-entry__date">{{ item.date }}</span>
          </div>

          <p v-if="educationMeta(item)" class="resume-entry__meta">{{ educationMeta(item) }}</p>

          <RichTextContent
            v-if="item.description"
            :html="item.description"
            class="resume-entry__description"
          />
        </article>
      </div>

      <div v-else-if="isCertificateSection" class="resume-entry-list">
        <article
          v-for="(item, index) in normalizedItems"
          :key="index"
          class="resume-entry resume-entry--certificate"
        >
          <div class="resume-entry__header">
            <div class="resume-entry__main">
              <h3 class="resume-entry__title">{{ item.name || item.text || item }}</h3>
              <p v-if="certificateSubtitle(item)" class="resume-entry__subtitle">{{ certificateSubtitle(item) }}</p>
            </div>
            <span v-if="item.date" class="resume-entry__date">{{ item.date }}</span>
          </div>

          <RichTextContent
            v-if="item.description"
            :html="item.description"
            class="resume-entry__description"
          />
        </article>
      </div>

      <ul v-else-if="isListSection" class="resume-bullet-list">
        <li v-for="(item, index) in normalizedItems" :key="index">
          {{ listItemText(item) }}
        </li>
      </ul>

      <div v-else-if="isTagSection" class="resume-chip-list">
        <span v-for="(item, index) in normalizedItems" :key="index" class="resume-chip">
          {{ typeof item === 'string' ? item : item.name || item.text }}
        </span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import RichTextContent from '../common/RichTextContent.vue'
import { buildJobIntentItems, formatNaturalText, normalizeTemplateSection } from '../../utils/resumeTemplate.js'

const props = defineProps({
  section: {
    type: Object,
    required: true,
  },
  locale: {
    type: String,
    default: 'zh',
  },
  compact: {
    type: Boolean,
    default: false,
  },
  showTitle: {
    type: Boolean,
    default: true,
  },
})

const sectionData = computed(() => normalizeTemplateSection(props.section, props.locale))
const code = computed(() => sectionData.value.sectionCode)
const schemaType = computed(() => sectionData.value.schemaType)
const normalizedItems = computed(() => {
  return Array.isArray(sectionData.value.content?.items) ? sectionData.value.content.items : []
})
const isJobIntentSection = computed(() => code.value === 'JOB_INTENT')
const isTextSection = computed(() => {
  return code.value === 'SUMMARY'
    || code.value === 'SELF_EVALUATION'
    || schemaType.value === 'TEXT'
})
const isSkillsSection = computed(() => code.value === 'SKILLS')
const isDetailSection = computed(() => {
  return ['EXPERIENCE', 'PROJECTS', 'INTERNSHIP', 'CAMPUS'].includes(code.value)
})
const isEducationSection = computed(() => code.value === 'EDUCATION')
const isCertificateSection = computed(() => ['CERTIFICATES', 'LAC_CERTIFICATES'].includes(code.value))
const isListSection = computed(() => {
  return schemaType.value === 'LIST'
    && !isDetailSection.value
    && !isEducationSection.value
    && !isCertificateSection.value
})
const isTagSection = computed(() => schemaType.value === 'TAGS' && !isSkillsSection.value)
const jobIntentItems = computed(() => buildJobIntentItems(sectionData.value, props.locale))
const sectionClasses = computed(() => ({
  'resume-section-block--compact': props.compact,
}))

function detailTitle(item) {
  const rawTitle = (() => {
    if (code.value === 'PROJECTS') {
      return item.name || '-'
    }
    if (code.value === 'CAMPUS') {
      return item.role || item.title || item.position || item.organization || item.company || '-'
    }
    return item.position || item.title || '-'
  })()

  return formatNaturalText(rawTitle)
}

function detailSubtitle(item) {
  const rawSubtitle = (() => {
    if (code.value === 'PROJECTS') {
      return item.role || ''
    }
    if (code.value === 'CAMPUS') {
      return item.organization || item.company || ''
    }
    return item.company || ''
  })()

  return formatNaturalText(rawSubtitle)
}

function detailMeta(item) {
  if (code.value === 'PROJECTS') {
    return ''
  }
  return ''
}

function detailDate(item) {
  return item.date || ''
}

function educationSubtitle(item) {
  return [item.degree, item.major].filter(Boolean).map(formatNaturalText).join(' · ')
}

function educationMeta(item) {
  return item.gpa ? `GPA ${item.gpa}` : ''
}

function certificateSubtitle(item) {
  return [item.level, item.issuer].filter(Boolean).map(formatNaturalText).join(' · ')
}

function listItemText(item) {
  const rawText = typeof item === 'string'
    ? item
    : item.text || item.name || ''

  return formatNaturalText(rawText)
}
</script>

<style scoped>
.resume-section-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.resume-section-block__head {
  display: flex;
  align-items: center;
}

.resume-section-block__title {
  margin: 0;
  color: var(--resume-color-heading);
  font-size: var(--resume-section-title-size, 10px);
  font-weight: 800;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.resume-section-block__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.resume-rich-text {
  color: var(--resume-color-text);
  font-size: var(--resume-text-size, 9px);
  line-height: 1.45;
}

.resume-rich-text :deep(*) {
  font-size: inherit !important;
  line-height: inherit !important;
}

.resume-entry-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resume-entry {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.resume-entry__header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
}

.resume-entry__main {
  min-width: 0;
}

.resume-entry__title {
  margin: 0;
  color: var(--resume-color-heading);
  font-size: var(--resume-title-size, 11px);
  font-weight: 700;
  line-height: 1.3;
}

.resume-entry__subtitle,
.resume-entry__meta {
  margin: 3px 0 0;
  color: var(--resume-color-text-soft);
  font-size: var(--resume-meta-size, 9px);
  line-height: 1.45;
}

.resume-entry__date {
  color: var(--resume-color-text-faint);
  font-size: var(--resume-date-size, 9px);
  font-weight: 600;
  white-space: nowrap;
}

.resume-entry__description {
  color: var(--resume-color-text);
  font-size: var(--resume-text-size, 9px);
  line-height: 1.45;
}

.resume-entry__description :deep(*) {
  font-size: inherit !important;
  line-height: inherit !important;
}

.resume-entry__bullets {
  margin: 0;
  padding-left: 15px;
  color: var(--resume-color-text);
  font-size: var(--resume-text-size, 9px);
  line-height: 1.45;
}

.resume-entry__bullets li + li {
  margin-top: 4px;
}

.resume-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.resume-chip-list--compact {
  gap: 6px;
}

.resume-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  border-radius: 999px;
  background: var(--resume-color-surface-strong);
  color: var(--resume-color-accent);
  font-size: var(--resume-chip-size, 9px);
  font-weight: 700;
  line-height: 1.2;
}

.resume-chip__meta {
  color: var(--resume-color-text-faint);
  font-weight: 600;
}

.resume-kv-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 14px;
}

.resume-kv {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 7px 9px;
  border: 1px solid var(--resume-color-border);
  border-radius: 12px;
  background: var(--resume-color-surface);
}

.resume-kv__label {
  color: var(--resume-color-text-faint);
  font-size: var(--resume-meta-size, 9px);
  font-weight: 700;
}

.resume-kv__value {
  color: var(--resume-color-text);
  font-size: var(--resume-text-size, 9px);
  font-weight: 600;
}

.resume-bullet-list {
  margin: 0;
  padding-left: 15px;
  color: var(--resume-color-text);
  font-size: var(--resume-text-size, 9px);
  line-height: 1.45;
}

.resume-bullet-list li + li {
  margin-top: 4px;
}

.resume-section-block--compact .resume-section-block__body {
  gap: 8px;
}

.resume-section-block--compact .resume-section-block__title {
  font-size: calc(var(--resume-section-title-size, 10px) - 1px);
  letter-spacing: 0.08em;
}

.resume-section-block--compact .resume-entry-list {
  gap: 7px;
}

.resume-section-block--compact .resume-entry__title {
  font-size: calc(var(--resume-title-size, 11px) - 1px);
}

.resume-section-block--compact .resume-entry__subtitle,
.resume-section-block--compact .resume-entry__meta,
.resume-section-block--compact .resume-entry__description,
.resume-section-block--compact .resume-bullet-list,
.resume-section-block--compact .resume-rich-text {
  font-size: calc(var(--resume-text-size, 9px) - 1px);
}

.resume-section-block--compact .resume-chip {
  padding: 2px 7px;
  font-size: calc(var(--resume-chip-size, 9px) - 0.5px);
}

@media (max-width: 720px) {
  .resume-kv-grid {
    grid-template-columns: 1fr;
  }

  .resume-entry__header {
    grid-template-columns: 1fr;
  }

  .resume-entry__date {
    white-space: normal;
  }
}
</style>
