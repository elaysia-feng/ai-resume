export function parseJsonContent(contentJson, fallback = {}) {
  if (!contentJson) {
    return fallback;
  }
  if (typeof contentJson === 'string') {
    try {
      return JSON.parse(contentJson);
    } catch {
      return fallback;
    }
  }
  if (typeof contentJson === 'object') {
    return contentJson;
  }
  return fallback;
}

export function formatDateTime(value, fallback = '-') {
  if (!value) {
    return fallback;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return fallback;
  }
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function getSectionPlainText(section) {
  const content = parseJsonContent(section?.contentJson, {});
  const schemaType = section?.schemaType;

  if (schemaType === 'TAGS') {
    return Array.isArray(content.items) ? content.items.join('，') : '';
  }

  if (schemaType === 'LIST') {
    if (!Array.isArray(content.items)) {
      return '';
    }
    return content.items
      .map((item) => (typeof item === 'string' ? item : item?.text || ''))
      .filter(Boolean)
      .join('\n');
  }

  if (typeof content.text === 'string') {
    return content.text;
  }

  return Object.values(content)
    .filter((value) => typeof value === 'string' && value.trim())
    .join('\n');
}

export function buildResumePlainText(sections = []) {
  return sections
    .map((section) => {
      const plainText = getSectionPlainText(section);
      if (!plainText) {
        return '';
      }
      return `${section.sectionTitle}：\n${plainText}`;
    })
    .filter(Boolean)
    .join('\n\n');
}
