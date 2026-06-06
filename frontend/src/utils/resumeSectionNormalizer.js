import { sanitizeRichTextHtml } from './richText.js'

const BASIC_DEFAULTS = {
  name: '',
  title: '',
  email: '',
  phone: '',
  location: '',
  wechat: '',
  github: '',
  website: '',
  avatar: '',
}

const JOB_INTENT_DEFAULTS = {
  desiredPosition: '',
  desiredCity: '',
  salaryRange: '',
  employmentType: '',
  jobStatus: '',
}

function asObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
}

function asText(value) {
  return typeof value === 'string' ? value.trim() : ''
}

function hasAnyValue(value) {
  if (!value || typeof value !== 'object') {
    return false
  }
  return Object.values(value).some((item) => {
    if (typeof item === 'boolean') {
      return item
    }
    return typeof item === 'string' ? item.trim() : Boolean(item)
  })
}

export function buildDateRange(startDate, endDate, current = false) {
  const start = asText(startDate)
  const end = current ? '至今' : asText(endDate)

  if (start && end) {
    return `${start} - ${end}`
  }
  if (start) {
    return start
  }
  return end
}

function splitTechStack(value) {
  if (Array.isArray(value)) {
    return value.map((item) => asText(item)).filter(Boolean)
  }
  if (typeof value !== 'string') {
    return []
  }
  return value
    .split(/[,\n，、]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function splitLines(text) {
  return asText(text)
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
}

function parseLegacyItem(sectionCode, item) {
  const rawText = typeof item === 'string' ? item : item?.text
  const lines = splitLines(rawText)
  if (!lines.length) {
    return {}
  }

  const [header, ...rest] = lines
  const headerParts = header.split('·').map((item) => item.trim()).filter(Boolean)
  const bulletLines = rest
    .filter((line) => /^[•·\-]/.test(line))
    .map((line) => line.replace(/^[•·\-]\s*/, '').trim())
    .filter(Boolean)
  const plainLines = rest.filter((line) => !/^[•·\-]/.test(line))
  const parsed = {}

  if (sectionCode === 'EDUCATION') {
    parsed.school = headerParts[0] || ''
    parsed.major = headerParts[1] || ''
    parsed.degree = headerParts[2] || ''
    parsed.date = headerParts.slice(3).join(' · ')
  }

  if (sectionCode === 'EXPERIENCE' || sectionCode === 'INTERNSHIP') {
    parsed.company = headerParts[0] || ''
    parsed.position = headerParts[1] || ''
    parsed.date = headerParts.slice(2).join(' · ')
  }

  if (sectionCode === 'PROJECTS') {
    parsed.name = headerParts[0] || ''
    if (headerParts.length === 2) {
      parsed.date = headerParts[1]
    } else {
      parsed.role = headerParts[1] || ''
      parsed.date = headerParts.slice(2).join(' · ')
    }
  }

  if (sectionCode === 'CAMPUS') {
    parsed.organization = headerParts[0] || ''
    parsed.role = headerParts[1] || ''
    parsed.date = headerParts.slice(2).join(' · ')
  }

  if (sectionCode === 'CERTIFICATES' || sectionCode === 'LAC_CERTIFICATES') {
    parsed.name = lines[0]
  }

  if (bulletLines.length) {
    parsed.bullets = bulletLines
  }

  if (plainLines.length) {
    parsed.description = plainLines.join('\n')
  }

  return parsed
}

function splitDateRange(dateRange) {
  if (typeof dateRange !== 'string' || !dateRange.trim()) {
    return { startDate: '', endDate: '' }
  }
  const parts = dateRange.split(/\s*[-–—]\s*/)
  if (parts.length >= 2) {
    return { startDate: parts[0].trim(), endDate: parts.slice(1).join(' - ').trim() }
  }
  return { startDate: parts[0].trim(), endDate: '' }
}

export function normalizeStructuredItem(sectionCode, item) {
  const source = typeof item === 'string' ? { text: item } : asObject(item)
  const normalized = { ...parseLegacyItem(sectionCode, source), ...source }

  // Backend stores dateRange; split into startDate/endDate for editor compatibility
  if (!normalized.startDate && !normalized.endDate && normalized.dateRange) {
    const { startDate, endDate } = splitDateRange(normalized.dateRange)
    normalized.startDate = startDate
    normalized.endDate = endDate
  }

  if (!normalized.startDate && !normalized.endDate && normalized.date) {
    const { startDate, endDate } = splitDateRange(normalized.date)
    normalized.startDate = startDate
    normalized.endDate = endDate
  }

  if (!normalized.date) {
    normalized.date = buildDateRange(normalized.startDate, normalized.endDate, normalized.current)
  }

  if (sectionCode === 'PROJECTS') {
    normalized.techStackList = splitTechStack(normalized.techStack)
  }

  // Backend stores highlights as string[]; map to description for editor
  if ((sectionCode === 'EXPERIENCE' || sectionCode === 'INTERNSHIP') && !normalized.description && Array.isArray(normalized.highlights)) {
    normalized.description = normalized.highlights.join('\n')
  }

  return normalized
}

export function normalizeSectionContentForView(sectionCode, contentJson) {
  const source = asObject(contentJson)

  if (sectionCode === 'BASIC') {
    return { ...BASIC_DEFAULTS, ...source }
  }

  if (sectionCode === 'JOB_INTENT') {
    const nestedText = source.text && typeof source.text === 'object' ? source.text : {}
    const mapped = {
      ...JOB_INTENT_DEFAULTS,
      ...source,
      ...nestedText,
      desiredPosition: source.desiredPosition || source.targetPosition || '',
      desiredCity: source.desiredCity || source.city || '',
    }
    return mapped
  }

  if (sectionCode === 'SUMMARY') {
    return { ...source, text: source.text || source.summary || '' }
  }

  if (sectionCode === 'SELF_EVALUATION') {
    return { ...source, text: source.text || source.evaluation || '' }
  }

  if (sectionCode === 'SKILLS') {
    // Backend stores { skills: string[] }, frontend uses { items: [{name, proficiency}] }
    const skillItems = source.items || (Array.isArray(source.skills) ? source.skills.map((s) => ({ name: s })) : [])
    return {
      ...source,
      items: skillItems.map((item) => ({
        name: asText(typeof item === 'string' ? item : item?.name || item?.text),
        proficiency: asText(typeof item === 'object' ? item?.proficiency : ''),
      })).filter((item) => item.name),
    }
  }

  if (Array.isArray(source.items)) {
    return {
      ...source,
      items: source.items.map((item) => normalizeStructuredItem(sectionCode, item)),
    }
  }

  return source
}

export function normalizeSectionContentForEditor(sectionCode, schemaType, contentJson) {
  const source = normalizeSectionContentForView(sectionCode, contentJson)

  if (sectionCode === 'BASIC') {
    return source
  }

  if (sectionCode === 'JOB_INTENT') {
    return source
  }

  if (sectionCode === 'EDUCATION') {
    return (source.items || []).map((item) => ({
      school: asText(item.school),
      degree: asText(item.degree),
      major: asText(item.major),
      gpa: asText(item.gpa),
      startDate: asText(item.startDate),
      endDate: asText(item.endDate),
      description: asText(item.description),
    }))
  }

  if (sectionCode === 'EXPERIENCE') {
    return (source.items || []).map((item) => ({
      company: asText(item.company),
      position: asText(item.position),
      startDate: asText(item.startDate),
      endDate: asText(item.endDate),
      current: Boolean(item.current),
      description: asText(item.description),
    }))
  }

  if (sectionCode === 'PROJECTS') {
    return (source.items || []).map((item) => ({
      name: asText(item.name),
      role: asText(item.role),
      startDate: asText(item.startDate),
      endDate: asText(item.endDate),
      description: asText(item.description),
      techStack: Array.isArray(item.techStackList) ? item.techStackList.join(', ') : asText(item.techStack),
    }))
  }

  if (sectionCode === 'INTERNSHIP') {
    return (source.items || []).map((item) => ({
      company: asText(item.company),
      position: asText(item.position),
      startDate: asText(item.startDate),
      endDate: asText(item.endDate),
      description: asText(item.description),
    }))
  }

  if (sectionCode === 'CAMPUS') {
    return (source.items || []).map((item) => ({
      organization: asText(item.organization),
      role: asText(item.role),
      startDate: asText(item.startDate),
      endDate: asText(item.endDate),
      description: asText(item.description),
    }))
  }

  if (sectionCode === 'CERTIFICATES' || sectionCode === 'LAC_CERTIFICATES') {
    return (source.items || []).map((item) => ({
      name: asText(item.name),
      level: asText(item.level),
      date: asText(item.date),
      issuer: asText(item.issuer),
      description: asText(item.description),
    }))
  }

  if (sectionCode === 'SKILLS') {
    return (source.items || []).map((item) => ({
      name: asText(item.name || item),
      proficiency: asText(item.proficiency),
    }))
  }

  if (schemaType === 'LIST' || schemaType === 'TAGS') {
    return source.items ?? []
  }

  if (schemaType === 'TEXT') {
    return typeof source.text === 'string' ? source.text : ''
  }

  return source
}

export function sanitizeSectionContentForSave(sectionCode, contentJson) {
  const source = normalizeSectionContentForView(sectionCode, contentJson)

  if (sectionCode === 'BASIC') {
    return {
      name: asText(source.name),
      title: asText(source.title),
      email: asText(source.email),
      phone: asText(source.phone),
      location: asText(source.location),
      wechat: asText(source.wechat),
      github: asText(source.github),
      website: asText(source.website),
      avatar: asText(source.avatar),
    }
  }

  if (sectionCode === 'JOB_INTENT') {
    return {
      targetPosition: asText(source.desiredPosition),
      city: asText(source.desiredCity),
      salaryRange: asText(source.salaryRange),
      jobStatus: asText(source.jobStatus),
    }
  }

  if (sectionCode === 'SUMMARY') {
    return {
      summary: asText(typeof source.text === 'string' ? source.text : source.summary),
    }
  }

  if (sectionCode === 'SELF_EVALUATION') {
    return {
      evaluation: asText(typeof source.text === 'string' ? source.text : source.evaluation),
    }
  }

  if (sectionCode === 'EDUCATION') {
    return {
      items: (source.items || [])
        .map((item) => ({
          school: asText(item.school),
          degree: asText(item.degree),
          major: asText(item.major),
          dateRange: buildDateRange(item.startDate, item.endDate, item.current),
        }))
        .filter((item) => item.school || item.degree || item.major),
    }
  }

  if (sectionCode === 'EXPERIENCE') {
    return {
      items: (source.items || [])
        .map((item) => ({
          company: asText(item.company),
          position: asText(item.position),
          dateRange: buildDateRange(item.startDate, item.endDate, item.current),
          highlights: splitLines(item.description),
        }))
        .filter((item) => item.company || item.position),
    }
  }

  if (sectionCode === 'PROJECTS') {
    return {
      items: (source.items || [])
        .map((item) => ({
          name: asText(item.name),
          role: asText(item.role),
          dateRange: buildDateRange(item.startDate, item.endDate, item.current),
          description: sanitizeRichTextHtml(item.description),
          techStack: Array.isArray(item.techStackList) ? item.techStackList : splitTechStack(item.techStack),
        }))
        .filter((item) => item.name),
    }
  }

  if (sectionCode === 'INTERNSHIP') {
    return {
      items: (source.items || [])
        .map((item) => ({
          company: asText(item.company),
          position: asText(item.position),
          dateRange: buildDateRange(item.startDate, item.endDate, item.current),
          highlights: splitLines(item.description),
        }))
        .filter((item) => item.company || item.position),
    }
  }

  if (sectionCode === 'CAMPUS') {
    return {
      items: (source.items || [])
        .map((item) => ({
          organization: asText(item.organization),
          role: asText(item.role),
          dateRange: buildDateRange(item.startDate, item.endDate, item.current),
          description: sanitizeRichTextHtml(item.description),
        }))
        .filter((item) => item.organization || item.role),
    }
  }

  if (sectionCode === 'CERTIFICATES') {
    return {
      items: (source.items || [])
        .map((item) => ({
          name: asText(item.name),
          issuer: asText(item.issuer),
          level: asText(item.level),
          date: asText(item.date),
        }))
        .filter((item) => item.name),
    }
  }

  if (sectionCode === 'LAC_CERTIFICATES') {
    return {
      items: (source.items || [])
        .map((item) => ({
          name: asText(item.name),
          score: asText(item.score),
          date: asText(item.date),
        }))
        .filter((item) => item.name),
    }
  }

  if (sectionCode === 'SKILLS') {
    return {
      skills: (source.items || [])
        .map((item) => asText(typeof item === 'string' ? item : item.name || item))
        .filter(Boolean),
    }
  }

  return source
}
