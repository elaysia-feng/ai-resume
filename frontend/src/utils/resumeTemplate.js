import { normalizeSectionContentForView } from './resumeSectionNormalizer.js'
import { stripRichText } from './richText.js'
import { resolveApiAssetUrl } from '../api/request.js'

const BUILTIN_SECTION_TITLES = {
  SUMMARY: { zh: '个人简介', en: 'Summary' },
  SELF_EVALUATION: { zh: '自我评价', en: 'Self Evaluation' },
  JOB_INTENT: { zh: '求职意向', en: 'Job Target' },
  EXPERIENCE: { zh: '工作经历', en: 'Work Experience' },
  PROJECTS: { zh: '项目经历', en: 'Projects' },
  INTERNSHIP: { zh: '实习经历', en: 'Internship' },
  EDUCATION: { zh: '教育背景', en: 'Education' },
  SKILLS: { zh: '专业技能', en: 'Skills' },
  CERTIFICATES: { zh: '荣誉证书', en: 'Honor & Awards' },
  LAC_CERTIFICATES: { zh: '技能证书', en: 'Certifications' },
  CAMPUS: { zh: '校园经历', en: 'Campus Experience' },
}

const CONTACT_LABELS = {
  phone: { zh: '电话', en: 'Phone' },
  email: { zh: '邮箱', en: 'Email' },
  location: { zh: '城市', en: 'Location' },
  wechat: { zh: '微信', en: 'WeChat' },
  github: { zh: 'GitHub', en: 'GitHub' },
  website: { zh: '网站', en: 'Website' },
}

const JOB_INTENT_LABELS = {
  desiredPosition: { zh: '期望职位', en: 'Position' },
  desiredCity: { zh: '期望城市', en: 'City' },
  salaryRange: { zh: '薪资范围', en: 'Salary' },
  employmentType: { zh: '工作性质', en: 'Type' },
  jobStatus: { zh: '求职状态', en: 'Status' },
}

const HEADER_CODES = new Set(['BASIC', 'JOB_INTENT'])
const NATURAL_CASE_EXCEPTIONS = new Set(['HTTP', 'HTTPS', 'HTML', 'JSON', 'REST', 'CRUD', 'JVM'])

function resolveLocaleText(record, locale = 'zh') {
  if (!record) {
    return ''
  }
  return locale === 'en' ? record.en : record.zh
}

function hasTextContent(value) {
  if (typeof value !== 'string') {
    return false
  }
  return stripRichText(value).trim().length > 0
}

function hasRenderableValue(value) {
  if (Array.isArray(value)) {
    return value.some((item) => hasRenderableValue(item))
  }

  if (typeof value === 'string') {
    return hasTextContent(value)
  }

  if (typeof value === 'boolean') {
    return value
  }

  if (!value || typeof value !== 'object') {
    return false
  }

  return Object.values(value).some((item) => hasRenderableValue(item))
}

function getSectionKey(section) {
  return section.id || `${section.sectionCode}-${section.__index ?? 0}`
}

export function formatNaturalText(value) {
  if (typeof value !== 'string') {
    return ''
  }

  const trimmedValue = value.trim()
  if (!trimmedValue) {
    return ''
  }

  if (/[a-z]/.test(trimmedValue)) {
    return trimmedValue
  }

  return trimmedValue.replace(/[A-Z]{4,}/g, (segment) => {
    if (NATURAL_CASE_EXCEPTIONS.has(segment)) {
      return segment
    }
    return segment.charAt(0) + segment.slice(1).toLowerCase()
  })
}

export function resolveSectionTitle(section, locale = 'zh') {
  if (!section) {
    return ''
  }

  if (section.sectionType === 'CUSTOM') {
    return section.sectionTitle || resolveLocaleText({ zh: '自定义模块', en: 'Custom Section' }, locale)
  }

  if (locale === 'en') {
    return resolveLocaleText(BUILTIN_SECTION_TITLES[section.sectionCode], locale) || section.sectionTitle || section.sectionCode
  }

  return section.sectionTitle || resolveLocaleText(BUILTIN_SECTION_TITLES[section.sectionCode], locale) || section.sectionCode
}

export function normalizeTemplateSection(section, locale = 'zh') {
  const content = normalizeSectionContentForView(section?.sectionCode, section?.contentJson || {})

  return {
    ...section,
    content,
    displayTitle: resolveSectionTitle(section, locale),
  }
}

export function hasRenderableSectionContent(section) {
  const normalized = normalizeTemplateSection(section)
  const { content, schemaType } = normalized

  if (normalized.sectionCode === 'BASIC' || normalized.sectionCode === 'JOB_INTENT') {
    return hasRenderableValue(content)
  }

  if (schemaType === 'TEXT') {
    return hasRenderableValue(content?.text)
  }

  if (schemaType === 'LIST' || schemaType === 'TAGS') {
    return hasRenderableValue(content?.items)
  }

  return hasRenderableValue(content)
}

export function buildTemplateLayout(sections = [], layout = {}, locale = 'zh') {
  const sourceSections = Array.isArray(sections) ? sections : []

  const normalizedSections = sourceSections
    .filter(Boolean)
    .map((section, index) => ({
      __index: index,
      ...normalizeTemplateSection(section, locale),
    }))
    .filter((section) => hasRenderableSectionContent(section))

  const basic = normalizedSections.find((section) => section.sectionCode === 'BASIC') || null
  const jobIntent = normalizedSections.find((section) => section.sectionCode === 'JOB_INTENT') || null
  const bodySections = normalizedSections.filter((section) => !HEADER_CODES.has(section.sectionCode))
  const usedKeys = new Set()

  function pickByCodes(codes = []) {
    return codes.flatMap((code) => {
      return bodySections
        .filter((section) => section.sectionCode === code && !usedKeys.has(getSectionKey(section)))
        .map((section) => {
          usedKeys.add(getSectionKey(section))
          return section
        })
    })
  }

  const leadSections = pickByCodes(layout.lead)
  const asideSections = pickByCodes(layout.aside)
  const mainSections = pickByCodes(layout.main)
  const remainingSections = bodySections
    .filter((section) => !usedKeys.has(getSectionKey(section)))
    .sort((left, right) => left.__index - right.__index)

  return {
    basic,
    jobIntent,
    leadSections,
    asideSections,
    mainSections,
    remainingSections,
  }
}

export function buildBasicProfile(section, locale = 'zh') {
  const content = section?.content || normalizeSectionContentForView('BASIC', section?.contentJson || {})
  const contacts = Object.entries(CONTACT_LABELS)
    .map(([field, label]) => ({
      field,
      label: resolveLocaleText(label, locale),
      value: typeof content[field] === 'string' ? content[field].trim() : '',
    }))
    .filter((item) => item.value)

  return {
    name: content.name?.trim() || (locale === 'en' ? 'Candidate Name' : '候选人'),
    title: formatNaturalText(content.title) || '',
    avatar: resolveApiAssetUrl(content.avatar?.trim() || ''),
    contacts,
    inlineContacts: contacts.map((item) => item.value),
  }
}

export function buildJobIntentItems(section, locale = 'zh') {
  const content = section?.content || normalizeSectionContentForView('JOB_INTENT', section?.contentJson || {})

  return Object.entries(JOB_INTENT_LABELS)
    .map(([field, label]) => ({
      field,
      label: resolveLocaleText(label, locale),
      value: typeof content[field] === 'string' ? content[field].trim() : '',
    }))
    .filter((item) => item.value)
}
