/** 从 5×5 网格字形生成桌面端 SVG 与多尺寸 Windows ICO。 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const outputDir = path.join(root, 'build');
const sizes = [256, 128, 64, 48, 32, 24, 16];
const background = '#0057ff';
const foreground = '#ffffff';
const filledCells = new Set([2, 6, 8, 10, 14, 15, 16, 17, 18, 19, 20, 24]);
const gridStart = 84;
const cellSize = 58;
const gridGap = 14;

function buildSvg() {
  const cells = [...filledCells].map((index) => {
    const row = Math.floor(index / 5);
    const column = index % 5;
    const x = gridStart + column * (cellSize + gridGap);
    const y = gridStart + row * (cellSize + gridGap);
    return `  <rect x="${x}" y="${y}" width="${cellSize}" height="${cellSize}" rx="2" fill="${foreground}" />`;
  });

  return [
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512" role="img" aria-label="AI Resume Forge">',
    `  <rect x="16" y="16" width="480" height="480" rx="56" fill="${background}" />`,
    ...cells,
    '</svg>',
    '',
  ].join('\n');
}

function hexToRgb(hex) {
  const value = Number.parseInt(hex.slice(1), 16);
  return [(value >> 16) & 0xff, (value >> 8) & 0xff, value & 0xff];
}

function isInsideRoundedBackground(x, y, scale) {
  const left = 16 * scale;
  const top = 16 * scale;
  const right = 496 * scale;
  const bottom = 496 * scale;
  const radius = 56 * scale;
  if (x < left || x >= right || y < top || y >= bottom) {
    return false;
  }

  const centerX = Math.max(left + radius, Math.min(x, right - radius));
  const centerY = Math.max(top + radius, Math.min(y, bottom - radius));
  return (x - centerX) ** 2 + (y - centerY) ** 2 <= radius ** 2;
}

function getPixel(x, y, size) {
  const scale = size / 512;
  const backgroundRgb = hexToRgb(background);
  const foregroundRgb = hexToRgb(foreground);
  if (!isInsideRoundedBackground(x + 0.5, y + 0.5, scale)) {
    return [...backgroundRgb, 0];
  }

  const iconX = (x + 0.5) / scale;
  const iconY = (y + 0.5) / scale;
  for (const index of filledCells) {
    const row = Math.floor(index / 5);
    const column = index % 5;
    const left = gridStart + column * (cellSize + gridGap);
    const top = gridStart + row * (cellSize + gridGap);
    if (iconX >= left && iconX < left + cellSize && iconY >= top && iconY < top + cellSize) {
      return [...foregroundRgb, 255];
    }
  }

  return [...backgroundRgb, 255];
}

function buildIconFrame(size) {
  const xorBitmap = Buffer.alloc(size * size * 4);
  const maskRowBytes = Math.ceil(size / 32) * 4;
  const andMask = Buffer.alloc(maskRowBytes * size);
  let offset = 0;

  for (let y = size - 1; y >= 0; y -= 1) {
    for (let x = 0; x < size; x += 1) {
      const [red, green, blue, alpha] = getPixel(x, y, size);
      xorBitmap[offset] = blue;
      xorBitmap[offset + 1] = green;
      xorBitmap[offset + 2] = red;
      xorBitmap[offset + 3] = alpha;
      offset += 4;
      if (alpha === 0) {
        const maskOffset = (size - 1 - y) * maskRowBytes + Math.floor(x / 8);
        andMask[maskOffset] |= 1 << (7 - (x % 8));
      }
    }
  }

  const dibHeader = Buffer.alloc(40);
  dibHeader.writeUInt32LE(40, 0);
  dibHeader.writeInt32LE(size, 4);
  dibHeader.writeInt32LE(size * 2, 8);
  dibHeader.writeUInt16LE(1, 12);
  dibHeader.writeUInt16LE(32, 14);
  dibHeader.writeUInt32LE(0, 16);
  dibHeader.writeUInt32LE(xorBitmap.length + andMask.length, 20);
  return Buffer.concat([dibHeader, xorBitmap, andMask]);
}

function buildIco() {
  const frames = sizes.map((size) => ({ size, data: buildIconFrame(size) }));
  const header = Buffer.alloc(6);
  header.writeUInt16LE(0, 0);
  header.writeUInt16LE(1, 2);
  header.writeUInt16LE(frames.length, 4);

  let offset = header.length + frames.length * 16;
  const entries = frames.map(({ size, data }) => {
    const entry = Buffer.alloc(16);
    entry.writeUInt8(size === 256 ? 0 : size, 0);
    entry.writeUInt8(size === 256 ? 0 : size, 1);
    entry.writeUInt8(0, 2);
    entry.writeUInt8(0, 3);
    entry.writeUInt16LE(1, 4);
    entry.writeUInt16LE(32, 6);
    entry.writeUInt32LE(data.length, 8);
    entry.writeUInt32LE(offset, 12);
    offset += data.length;
    return entry;
  });

  return Buffer.concat([header, ...entries, ...frames.map((frame) => frame.data)]);
}

fs.mkdirSync(outputDir, { recursive: true });
fs.writeFileSync(path.join(outputDir, 'icon.svg'), buildSvg(), 'utf8');
fs.writeFileSync(path.join(outputDir, 'icon.ico'), buildIco());
console.log(`已生成 icon.svg 与 ${sizes.length} 尺寸的 icon.ico`);
