import { resolveApiAssetUrl } from '../api/request.js'

const EXPORT_SCALE = 2
const PDF_SECTION_BREAK_SEARCH_RATIO = 0.92

function waitForNextFrame() {
  return new Promise((resolve) => {
    requestAnimationFrame(() => resolve())
  })
}

function waitForImageLoad(image) {
  if (!image) {
    return Promise.resolve()
  }

  if (image.complete) {
    return Promise.resolve()
  }

  return new Promise((resolve) => {
    const cleanup = () => {
      image.removeEventListener('load', onDone)
      image.removeEventListener('error', onDone)
    }

    const onDone = () => {
      cleanup()
      resolve()
    }

    image.addEventListener('load', onDone, { once: true })
    image.addEventListener('error', onDone, { once: true })
  })
}

function blobToDataUrl(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => reject(reader.error || new Error('图片读取失败'))
    reader.readAsDataURL(blob)
  })
}

async function toExportSafeImageSrc(src) {
  const resolvedSrc = resolveApiAssetUrl(src)
  if (!resolvedSrc || resolvedSrc.startsWith('data:') || resolvedSrc.startsWith('blob:')) {
    return resolvedSrc
  }

  const response = await fetch(resolvedSrc, {
    method: 'GET',
    mode: 'cors',
    credentials: 'omit',
  })
  if (!response.ok) {
    throw new Error(`图片下载失败: ${response.status}`)
  }

  return blobToDataUrl(await response.blob())
}

async function prepareExportElement(element) {
  const rect = element.getBoundingClientRect()
  const mountNode = document.createElement('div')
  const clonedElement = element.cloneNode(true)

  mountNode.setAttribute('aria-hidden', 'true')
  Object.assign(mountNode.style, {
    position: 'fixed',
    left: '-100000px',
    top: '0',
    width: `${Math.ceil(rect.width)}px`,
    opacity: '0',
    pointerEvents: 'none',
    zIndex: '-1',
    overflow: 'hidden',
    background: '#ffffff',
  })

  clonedElement.style.width = `${Math.ceil(rect.width)}px`
  mountNode.appendChild(clonedElement)
  document.body.appendChild(mountNode)

  const images = Array.from(clonedElement.querySelectorAll('img'))

  await Promise.all(images.map(async (image) => {
    const originalSrc = image.currentSrc || image.getAttribute('src') || ''
    if (!originalSrc) {
      return
    }

    const resolvedSrc = resolveApiAssetUrl(originalSrc)
    if (resolvedSrc && resolvedSrc !== originalSrc) {
      image.src = resolvedSrc
    }
    image.crossOrigin = 'anonymous'

    try {
      const safeSrc = await toExportSafeImageSrc(resolvedSrc)
      if (safeSrc) {
        image.src = safeSrc
      }
    } catch (error) {
      console.warn('导出时处理图片失败，请检查图片地址是否可访问（本地文件需带 token）', resolvedSrc, error)
    }

    await waitForImageLoad(image)
  }))

  if (document.fonts?.ready) {
    await document.fonts.ready
  }
  await waitForNextFrame()

  return {
    element: clonedElement,
    dispose() {
      mountNode.remove()
    },
  }
}

async function renderElementToCanvas(element) {
  // 1. 同步准备离屏简历和画布渲染器。
  const [{ default: html2canvas }, prepared] = await Promise.all([
    import('html2canvas'),
    prepareExportElement(element),
  ])

  try {
    // 2. 记录章节边界并按原始宽度渲染，供后续分页使用。
    const elementTop = prepared.element.getBoundingClientRect().top
    const sectionBreakOffsets = Array.from(prepared.element.querySelectorAll('.resume-section-block'))
      .map((section) => Math.round((section.getBoundingClientRect().top - elementTop) * EXPORT_SCALE))
      .filter((offset) => offset > 0)

    const canvas = await html2canvas(prepared.element, {
      scale: EXPORT_SCALE,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false,
    })

    // 3. 返回画布和章节位置；finally 会清理克隆的离屏节点。
    return { canvas, sectionBreakOffsets }
  } finally {
    prepared.dispose()
  }
}

function findPdfPageEnd(startOffset, maxEndOffset, sectionBreakOffsets) {
  // 页面尾部 8% 内若有下一节起点，就在该处换页，避免章节标题贴在页尾。
  const searchStartOffset = startOffset + Math.floor((maxEndOffset - startOffset) * PDF_SECTION_BREAK_SEARCH_RATIO)
  let pageEndOffset = maxEndOffset

  for (const sectionBreakOffset of sectionBreakOffsets) {
    if (sectionBreakOffset > startOffset
        && sectionBreakOffset >= searchStartOffset
        && sectionBreakOffset <= maxEndOffset) {
      pageEndOffset = sectionBreakOffset
    }
  }

  return pageEndOffset
}

export async function exportToPDF(element, filename = 'resume') {
  if (!element) {
    throw new Error('导出元素不存在')
  }

  // 1. 加载 PDF 渲染器并捕获页面内容。
  const [{ default: jsPDF }, rendered] = await Promise.all([
    import('jspdf'),
    renderElementToCanvas(element),
  ])
  const { canvas, sectionBreakOffsets } = rendered

  const pdf = new jsPDF({
    orientation: 'portrait',
    unit: 'mm',
    format: 'a4',
  })

  // 2. 根据 A4 宽度计算单页画布高度，保持文字尺寸不变。
  const pdfWidth = pdf.internal.pageSize.getWidth()
  const pdfHeight = pdf.internal.pageSize.getHeight()
  const scaleToPageWidth = pdfWidth / canvas.width
  const pageCanvasHeight = Math.max(1, Math.floor(pdfHeight / scaleToPageWidth))
  const pageCanvas = document.createElement('canvas')
  pageCanvas.width = canvas.width
  const pageContext = pageCanvas.getContext('2d')
  if (!pageContext) {
    throw new Error('无法创建 PDF 页面画布')
  }

  let sourceTop = 0
  let pageIndex = 0
  // 3. 按原始宽度裁成 A4 页面，避免长简历整体缩小到一页。
  while (sourceTop < canvas.height) {
    const maxEnd = Math.min(sourceTop + pageCanvasHeight, canvas.height)
    const sourceBottom = findPdfPageEnd(sourceTop, maxEnd, sectionBreakOffsets)
    const sliceHeight = sourceBottom - sourceTop
    pageCanvas.height = sliceHeight
    pageContext.clearRect(0, 0, pageCanvas.width, sliceHeight)
    pageContext.drawImage(canvas, 0, sourceTop, canvas.width, sliceHeight, 0, 0, canvas.width, sliceHeight)

    if (pageIndex > 0) {
      pdf.addPage()
    }

    pdf.addImage(
      pageCanvas.toDataURL('image/png'),
      'PNG',
      0,
      0,
      pdfWidth,
      sliceHeight * scaleToPageWidth,
    )
    sourceTop = sourceBottom
    pageIndex += 1
  }

  // 4. 保存已分页的 PDF。
  pdf.save(`${filename}.pdf`)
}

export async function exportToImage(element, filename = 'resume') {
  if (!element) throw new Error('导出元素不存在')

  const canvas = await renderElementToCanvas(element)

  const link = document.createElement('a')
  link.download = `${filename}.png`
  link.href = canvas.toDataURL('image/png')
  link.click()
}
