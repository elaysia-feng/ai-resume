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

export function normalizeStructuredItem(sectionCode, item) {
  const source = typeof item === 'string' ? { text: item } : asObject(item)
  const normalized = { ...parseLegacyItem(sectionCode, source), ...source }

  if (!normalized.date) {
    normalized.date = buildDateRange(normalized.startDate, normalized.endDate, normalized.current)
  }

  if (sectionCode === 'PROJECTS') {
    normalized.techStackList = splitTechStack(normalized.techStack)
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
    return { ...JOB_INTENT_DEFAULTS, ...source, ...nestedText }
  }

  if (sectionCode === 'SKILLS') {
    return {
      ...source,
      items: (source.items || []).map((item) => ({
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
      desiredPosition: asText(source.desiredPosition),
      desiredCity: asText(source.desiredCity),
      salaryRange: asText(source.salaryRange),
      employmentType: asText(source.employmentType),
      jobStatus: asText(source.jobStatus),
    }
  }

  if (sectionCode === 'EDUCATION') {
    return {
      items: (source.items || [])
        .map((item) => ({
          school: asText(item.school),
          degree: asText(item.degree),
          major: asText(item.major),
          gpa: asText(item.gpa),
          startDate: asText(item.startDate),
          endDate: asText(item.endDate),
          description: sanitizeRichTextHtml(item.description),
        }))
        .filter(hasAnyValue),
    }
  }

  if (sectionCode === 'EXPERIENCE') {
    return {
      items: (source.items || [])
        .map((item) => ({
          company: asText(item.company),
          position: asText(item.position),
          startDate: asText(item.startDate),
          endDate: asText(item.endDate),
          current: Boolean(item.current),
          description: sanitizeRichTextHtml(item.description),
        }))
        .filter(hasAnyValue),
    }
  }

  if (sectionCode === 'PROJECTS') {
    return {
      items: (source.items || [])
        .map((item) => ({
          name: asText(item.name),
          role: asText(item.role),
          startDate: asText(item.startDate),
          endDate: asText(item.endDate),
          description: sanitizeRichTextHtml(item.description),
          techStack: Array.isArray(item.techStackList) ? item.techStackList.join(', ') : asText(item.techStack),
        }))
        .filter(hasAnyValue),
    }
  }

  if (sectionCode === 'INTERNSHIP') {
    return {
      items: (source.items || [])
        .map((item) => ({
          company: asText(item.company),
          position: asText(item.position),
          startDate: asText(item.startDate),
          endDate: asText(item.endDate),
          description: sanitizeRichTextHtml(item.description),
        }))
        .filter(hasAnyValue),
    }
  }

  if (sectionCode === 'CAMPUS') {
    return {
      items: (source.items || [])
        .map((item) => ({
          organization: asText(item.organization),
          role: asText(item.role),
          startDate: asText(item.startDate),
          endDate: asText(item.endDate),
          description: sanitizeRichTextHtml(item.description),
        }))
        .filter(hasAnyValue),
    }
  }

  if (sectionCode === 'CERTIFICATES' || sectionCode === 'LAC_CERTIFICATES') {
    return {
      items: (source.items || [])
        .map((item) => ({
          name: asText(item.name),
          level: asText(item.level),
          date: asText(item.date),
          issuer: asText(item.issuer),
          description: sanitizeRichTextHtml(item.description),
        }))
        .filter(hasAnyValue),
    }
  }

  if (sectionCode === 'SKILLS') {
    return {
      items: (source.items || [])
        .map((item) => ({
          name: asText(item.name || item),
          proficiency: asText(item.proficiency),
        }))
        .filter((item) => item.name),
    }
  }

  if (typeof source.text === 'string') {
    return { text: sanitizeRichTextHtml(source.text) }
  }

  return source
}
