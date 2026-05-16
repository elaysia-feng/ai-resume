import { resolveApiAssetUrl } from '../api/request.js'

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
      console.warn('导出时处理图片失败，请检查 OSS 图片是否开启 CORS', resolvedSrc, error)
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
  const [{ default: html2canvas }, prepared] = await Promise.all([
    import('html2canvas'),
    prepareExportElement(element),
  ])

  try {
    return await html2canvas(prepared.element, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false,
    })
  } finally {
    prepared.dispose()
  }
}

export async function exportToPDF(element, filename = 'resume') {
  if (!element) throw new Error('导出元素不存在')

  const [{ default: jsPDF }, canvas] = await Promise.all([
    import('jspdf'),
    renderElementToCanvas(element),
  ])

  const imgData = canvas.toDataURL('image/png')
  const pdf = new jsPDF({
    orientation: 'portrait',
    unit: 'mm',
    format: 'a4',
  })

  const pdfWidth = pdf.internal.pageSize.getWidth()
  const pdfHeight = pdf.internal.pageSize.getHeight()
  const canvasWidth = canvas.width
  const canvasHeight = canvas.height
  const ratio = Math.min(pdfWidth / canvasWidth, pdfHeight / canvasHeight)
  const imgWidth = canvasWidth * ratio
  const imgHeight = canvasHeight * ratio
  const x = (pdfWidth - imgWidth) / 2
  const y = 0

  pdf.addImage(imgData, 'PNG', x, y, imgWidth, imgHeight)
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
