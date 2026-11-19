// The tests of png.mjs: node --test scripts/store-screenshots/creative-assets/png.test.mjs
import assert from "node:assert/strict";
import { deflateSync, inflateSync } from "node:zlib";
import { test } from "node:test";
import { describePng, withoutAlpha } from "./png.mjs";

const SIGNATURE = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

function chunk(type, data) {
  const length = Buffer.alloc(4);
  length.writeUInt32BE(data.length);
  // The reader under test doesn't check a chunk's CRC, so a stand-in will do.
  return Buffer.concat([length, Buffer.from(type, "latin1"), data, Buffer.alloc(4)]);
}

function rgbaPng(width, rows) {
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width, 0);
  header.writeUInt32BE(rows.length, 4);
  header[8] = 8;
  header[9] = 6;
  const raw = Buffer.concat(rows.map(({ filter, bytes }) => Buffer.from([filter, ...bytes])));
  return Buffer.concat([SIGNATURE, chunk("IHDR", header), chunk("IDAT", deflateSync(raw)), chunk("IEND", Buffer.alloc(0))]);
}

// The picture's pixels as RGB, read back through the filters withoutAlpha wrote.
function pixelsOf(png) {
  const { width, height } = describePng(png);
  let offset = 8;
  let data = Buffer.alloc(0);
  while (offset < png.length) {
    const length = png.readUInt32BE(offset);
    if (png.toString("latin1", offset + 4, offset + 8) === "IDAT") data = png.subarray(offset + 8, offset + 8 + length);
    offset += 12 + length;
  }
  const raw = inflateSync(data);
  const stride = width * 3;
  const pixels = [];
  let previous = Buffer.alloc(stride);
  for (let y = 0; y < height; y++) {
    const start = y * (stride + 1);
    const row = Buffer.from(raw.subarray(start + 1, start + 1 + stride));
    for (let i = 0; i < stride; i++) {
      const a = i >= 3 ? row[i - 3] : 0;
      const b = previous[i];
      const c = i >= 3 ? previous[i - 3] : 0;
      const p = a + b - c;
      const [pa, pb, pc] = [Math.abs(p - a), Math.abs(p - b), Math.abs(p - c)];
      const paeth = pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
      row[i] = (row[i] + [0, a, b, 0, paeth][raw[start]]) & 0xff;
    }
    pixels.push([...row]);
    previous = row;
  }
  return pixels;
}

test("drops the alpha channel and keeps every color, whatever filter the rows came in", () => {
  const png = rgbaPng(2, [
    { filter: 0, bytes: [10, 20, 30, 255, 40, 50, 60, 255] },
    { filter: 2, bytes: [1, 2, 3, 0, 4, 5, 6, 0] },
    { filter: 1, bytes: [7, 8, 9, 255, 1, 1, 1, 0] },
  ]);

  const flat = withoutAlpha(png);

  assert.deepEqual(describePng(flat), { width: 2, height: 3, depth: 8, colorType: 2 });
  assert.deepEqual(pixelsOf(flat), [
    [10, 20, 30, 40, 50, 60],
    [11, 22, 33, 44, 55, 66],
    [7, 8, 9, 8, 9, 10],
  ]);
});

test("refuses a picture with a pixel that isn't opaque", () => {
  const png = rgbaPng(1, [{ filter: 0, bytes: [1, 2, 3, 128] }]);

  assert.throws(() => withoutAlpha(png), /not opaque/);
});

test("hands back a picture that has no alpha channel as it is", () => {
  const png = withoutAlpha(rgbaPng(1, [{ filter: 0, bytes: [1, 2, 3, 255] }]));

  assert.equal(withoutAlpha(png), png);
});
