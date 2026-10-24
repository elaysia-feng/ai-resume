function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function isHtmlLike(value) {
  return typeof value === 'string' && /<\/?[a-z][\s\S]*>/i.test(value)
}

export function plainTextToRichHtml(value) {
  const text = String(value ?? '')
  if (!text.trim()) {
    return ''
  }

  return text
    .split('\n')
    .map((line) => {
      const trimmed = line.trim()
      if (!trimmed) {
        return ''  // 空行返回空字符串，不生成任何HTML
      }
      return `<p>${escapeHtml(trimmed)}</p>`
    })
    .filter(Boolean)
    .join('')
}

export function sanitizeRichTextHtml(value) {
  if (typeof value !== 'string') {
    return ''
  }

  const source = value.trim()
  if (!source) {
    return ''
  }

  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return isHtmlLike(source) ? source : plainTextToRichHtml(source)
  }

  // Parse HTML and extract only safe content
  const wrapper = document.createElement('div')
  wrapper.innerHTML = isHtmlLike(source) ? source : plainTextToRichHtml(source)

  function sanitizeNode(node) {
    if (node.nodeType === Node.TEXT_NODE) {
      return node.textContent || ''
    }

    if (node.nodeType !== Node.ELEMENT_NODE) {
      return ''
    }

    const tagName = node.tagName.toLowerCase()

    // Block tags - headings are flattened to paragraphs to keep resume typography stable
    if (['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'ul', 'ol', 'li'].includes(tagName)) {
      const children = Array.from(node.childNodes).map(sanitizeNode).join('')
      const trimmed = children.trim()
      if (!trimmed) return ''
      const safeTag = /^h[1-6]$/.test(tagName) ? 'p' : tagName
      return `<${safeTag}>${trimmed}</${safeTag}>`
    }

    // Inline tags
    if (['strong', 'b', 'em', 'i', 'u', 's', 'a', 'span'].includes(tagName)) {
      const children = Array.from(node.childNodes).map(sanitizeNode).join('')
      const tagMap = { b: 'strong', i: 'em' }
      const safeTag = tagMap[tagName] || tagName

      if (tagName === 'a') {
        const href = node.getAttribute('href') || ''
        const safeHref = href.match(/^https?:\/\//) ? href : '#'
        return `<a href="${safeHref}" target="_blank" rel="noopener noreferrer">${children}</a>`
      }

      if (tagName === 'span') {
        const style = node.getAttribute('style') || ''
        if (style) {
          const parts = []
          // 只保留 color、background-color、font-weight、font-style、text-decoration
          if (/color:\s*[^;]+/.test(style)) parts.push(style.match(/color:\s*[^;]+/)[0])
          if (/background-color:\s*[^;]+/.test(style)) parts.push(style.match(/background-color:\s*[^;]+/)[0])
          if (/font-weight:\s*[^;]+/.test(style)) parts.push(style.match(/font-weight:\s*[^;]+/)[0])
          if (/font-style:\s*[^;]+/.test(style)) parts.push(style.match(/font-style:\s*[^;]+/)[0])
          if (/text-decoration:\s*[^;]+/.test(style)) parts.push(style.match(/text-decoration:\s*[^;]+/)[0])
          if (parts.length > 0) {
            return `<span style="${parts.join('; ')}">${children}</span>`
          }
        }
        return children
      }

      return `<${safeTag}>${children}</${safeTag}>`
    }

    // Skip unknown tags but keep children
    return Array.from(node.childNodes).map(sanitizeNode).join('')
  }

  const sanitized = Array.from(wrapper.childNodes).map(sanitizeNode).filter(Boolean).join('')
  return sanitized || plainTextToRichHtml(source)
}

export function normalizeRichTextHtml(value) {
  if (typeof value !== 'string') {
    return ''
  }
  return sanitizeRichTextHtml(value)
}

export function stripRichText(value) {
  if (typeof value !== 'string') {
    return ''
  }

  if (!isHtmlLike(value)) {
    return value.trim()
  }

  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return value.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  }

  const wrapper = document.createElement('div')
  wrapper.innerHTML = sanitizeRichTextHtml(value)
  return (wrapper.textContent || '')
    .replace(/\u00a0/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}
