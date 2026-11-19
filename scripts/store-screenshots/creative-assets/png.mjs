// The PNG work of the creative assets: App Store Connect refuses an image with an alpha channel, and Chrome always
// photographs a page as RGBA, opaque or not. No dependency: compose.mjs runs on what a Mac with Node has.
import { deflateSync, inflateSync } from "node:zlib";

const SIGNATURE = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
const COLOR_RGB = 2;
const COLOR_RGBA = 6;

const crcTable = Int32Array.from({ length: 256 }, (_, index) => {
  let value = index;
  for (let bit = 0; bit < 8; bit++) value = value & 1 ? 0xedb88320 ^ (value >>> 1) : value >>> 1;
  return value;
});

function crcOf(buffer) {
  let crc = -1;
  for (const byte of buffer) crc = crcTable[(crc ^ byte) & 0xff] ^ (crc >>> 8);
  return (crc ^ -1) >>> 0;
}

function chunkOf(type, data) {
  const typed = Buffer.concat([Buffer.from(type, "latin1"), data]);
  const length = Buffer.alloc(4);
  length.writeUInt32BE(data.length);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crcOf(typed));
  return Buffer.concat([length, typed, crc]);
}

function chunksOf(png) {
  if (!png.subarray(0, 8).equals(SIGNATURE)) throw new Error("Not a PNG");
  const chunks = [];
  for (let offset = 8; offset < png.length; ) {
    const length = png.readUInt32BE(offset);
    chunks.push({ type: png.toString("latin1", offset + 4, offset + 8), data: png.subarray(offset + 8, offset + 8 + length) });
    offset += 12 + length;
  }
  return chunks;
}

/** The size and color type a PNG's header states. */
export function describePng(png) {
  const header = chunksOf(png).find((chunk) => chunk.type === "IHDR").data;
  return { width: header.readUInt32BE(0), height: header.readUInt32BE(4), depth: header[8], colorType: header[9] };
}

function paeth(left, up, upLeft) {
  const estimate = left + up - upLeft;
  const toLeft = Math.abs(estimate - left);
  const toUp = Math.abs(estimate - up);
  const toUpLeft = Math.abs(estimate - upLeft);
  if (toLeft <= toUp && toLeft <= toUpLeft) return left;
  return toUp <= toUpLeft ? up : upLeft;
}

// Undoes one scanline's filter in place, `previous` being the row above, already undone.
function unfilter(filter, row, previous, bytesPerPixel) {
  for (let index = 0; index < row.length; index++) {
    const left = index >= bytesPerPixel ? row[index - bytesPerPixel] : 0;
    const up = previous[index];
    const upLeft = index >= bytesPerPixel ? previous[index - bytesPerPixel] : 0;
    const predicted = [0, left, up, (left + up) >> 1, paeth(left, up, upLeft)][filter];
    row[index] = (row[index] + predicted) & 0xff;
  }
}

// Writes the row with the filter that leaves the smallest residuals, the usual guess at what deflates best.
function filtered(row, previous, bytesPerPixel) {
  const candidates = [1, 2, 4].map((filter) => {
    const output = Buffer.alloc(row.length + 1);
    output[0] = filter;
    let cost = 0;
    for (let index = 0; index < row.length; index++) {
      const left = index >= bytesPerPixel ? row[index - bytesPerPixel] : 0;
      const up = previous[index];
      const upLeft = index >= bytesPerPixel ? previous[index - bytesPerPixel] : 0;
      const predicted = filter === 1 ? left : filter === 2 ? up : paeth(left, up, upLeft);
      const residual = (row[index] - predicted) & 0xff;
      output[index + 1] = residual;
      cost += residual < 128 ? residual : 256 - residual;
    }
    return { output, cost };
  });
  return candidates.reduce((best, candidate) => (candidate.cost < best.cost ? candidate : best)).output;
}

/** The picture's pixels, four bytes each, for an 8-bit RGB or RGBA PNG. */
export function readPixels(png) {
  const { width, height, depth, colorType } = describePng(png);
  if (depth !== 8 || (colorType !== COLOR_RGB && colorType !== COLOR_RGBA)) {
    throw new Error(`Unexpected PNG: color type ${colorType}, depth ${depth}`);
  }
  const channels = colorType === COLOR_RGBA ? 4 : 3;
  const raw = inflateSync(Buffer.concat(chunksOf(png).filter((chunk) => chunk.type === "IDAT").map((chunk) => chunk.data)));
  const stride = width * channels;
  const pixels = Buffer.alloc(width * height * 4, 0xff);
  let previous = Buffer.alloc(stride);
  for (let y = 0; y < height; y++) {
    const start = y * (stride + 1);
    const row = Buffer.from(raw.subarray(start + 1, start + 1 + stride));
    unfilter(raw[start], row, previous, channels);
    for (let x = 0; x < width; x++) row.copy(pixels, (y * width + x) * 4, x * channels, x * channels + channels);
    previous = row;
  }
  return { width, height, pixels };
}

/** The same picture without its alpha channel. Refuses a pixel that isn't opaque: dropping it would change the image. */
export function withoutAlpha(png) {
  const { width, height, depth, colorType } = describePng(png);
  if (colorType === COLOR_RGB) return png;
  if (colorType !== COLOR_RGBA || depth !== 8) throw new Error(`Unexpected PNG: color type ${colorType}, depth ${depth}`);
  const chunks = chunksOf(png);
  const raw = inflateSync(Buffer.concat(chunks.filter((chunk) => chunk.type === "IDAT").map((chunk) => chunk.data)));
  const stride = width * 4;
  let previous = Buffer.alloc(stride);
  let previousRgb = Buffer.alloc(width * 3);
  const rows = [];
  for (let y = 0; y < height; y++) {
    const start = y * (stride + 1);
    const row = Buffer.from(raw.subarray(start + 1, start + 1 + stride));
    unfilter(raw[start], row, previous, 4);
    const rgb = Buffer.alloc(width * 3);
    for (let x = 0; x < width; x++) {
      if (row[x * 4 + 3] !== 0xff) throw new Error(`The pixel at ${x},${y} is not opaque`);
      rgb[x * 3] = row[x * 4];
      rgb[x * 3 + 1] = row[x * 4 + 1];
      rgb[x * 3 + 2] = row[x * 4 + 2];
    }
    rows.push(filtered(rgb, previousRgb, 3));
    previous = row;
    previousRgb = rgb;
  }
  const header = Buffer.from(chunks.find((chunk) => chunk.type === "IHDR").data);
  header[9] = COLOR_RGB;
  return Buffer.concat([
    SIGNATURE,
    chunkOf("IHDR", header),
    chunkOf("IDAT", deflateSync(Buffer.concat(rows), { level: 9 })),
    chunkOf("IEND", Buffer.alloc(0)),
  ]);
}
