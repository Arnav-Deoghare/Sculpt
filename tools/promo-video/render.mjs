// Renders the promo video and still images from scene.js using a local copy of Chrome.
//
//   npm install
//   node render.mjs stills    <outDir> [t1 t2 ...]   portrait still frames, handy while editing
//   node render.mjs poster    <outFile> [t]          one portrait frame as a JPEG
//   node render.mjs video     <outFile> [fps]        the portrait 720x1280 video (default 30 fps)
//   node render.mjs stills169 <outDir> [t1 t2 ...]   landscape still frames
//   node render.mjs poster169 <outFile> [t]          one landscape frame as a JPEG
//   node render.mjs video169  <outFile> [fps]        the landscape 1920x1080 video (default 60 fps)
//
// Set CHROME_PATH if Chrome is not in its usual place.

import { createServer } from 'node:http';
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { extname, join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import puppeteer from 'puppeteer-core';

const here = dirname(fileURLToPath(import.meta.url));
const CHROME =
  process.env.CHROME_PATH ||
  {
    darwin: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    win32: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    linux: '/usr/bin/google-chrome'
  }[process.platform];

const types = { '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml' };

function serve() {
  const server = createServer(async (req, res) => {
    try {
      const path = new URL(req.url, 'http://x').pathname.replace(/^\/$/, '/index.html');
      const body = await readFile(join(here, path));
      res.writeHead(200, { 'content-type': types[extname(path)] || 'application/octet-stream' });
      res.end(body);
    } catch {
      res.writeHead(404);
      res.end();
    }
  });
  return new Promise((resolve) => server.listen(0, '127.0.0.1', () => resolve(server)));
}

async function withPage(width, height, fn) {
  const server = await serve();
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: true,
    args: ['--no-sandbox', '--autoplay-policy=no-user-gesture-required', '--disable-gpu-vsync']
  });
  try {
    const page = await browser.newPage();
    await page.setViewport({ width, height, deviceScaleFactor: 1 });
    page.on('pageerror', (e) => console.error('page error:', e.message));
    page.on('console', (m) => m.type() === 'error' && console.error('console:', m.text()));
    return await fn(page, `http://127.0.0.1:${server.address().port}`);
  } finally {
    await browser.close();
    server.close();
  }
}

async function still(page, base, t, file, opts = {}) {
  await page.goto(`${base}/index.html?t=${t}`);
  await page.waitForFunction('window.__ready === true');
  await page.screenshot({ path: file, ...opts });
}

const [mode, out, ...rest] = process.argv.slice(2);

if (mode === 'stills') {
  await mkdir(out, { recursive: true });
  const times = rest.length ? rest.map(Number) : [1, 2.5, 7, 8.6, 11.3, 12.4, 15, 18.3, 20, 23, 25.5, 27];
  await withPage(1080, 1920, async (page, base) => {
    for (const t of times) await still(page, base, t, join(out, `t${String(t).replace('.', '_')}.png`), { type: 'png' });
  });
  console.log(`wrote ${times.length} stills to ${out}`);
} else if (mode === 'poster') {
  const t = Number(rest[0] ?? 8.8);
  await withPage(1080, 1920, (page, base) => still(page, base, t, out, { type: 'jpeg', quality: 86 }));
  console.log(`wrote ${out}`);
} else if (mode === 'stills169') {
  await mkdir(out, { recursive: true });
  const times = rest.length ? rest.map(Number) : [0.6, 1.6, 3, 4.8, 6.6, 8, 9.0, 11, 12.8, 14.4, 15.8, 19];
  await withPage(1920, 1080, async (page, base) => {
    for (const t of times) {
      await page.goto(`${base}/index169.html?t=${t}`);
      await page.waitForFunction('window.__ready === true');
      await page.screenshot({ path: join(out, `f${String(t).replace('.', '_')}.png`), type: 'png' });
    }
  });
  console.log(`wrote ${times.length} stills to ${out}`);
} else if (mode === 'poster169') {
  const t = Number(rest[0] ?? 5.5);
  await withPage(1920, 1080, async (page, base) => {
    await page.goto(`${base}/index169.html?t=${t}`);
    await page.waitForFunction('window.__ready === true');
    await page.screenshot({ path: out, type: 'jpeg', quality: 88 });
  });
  console.log(`wrote ${out}`);
} else if (mode === 'video' || mode === 'video169') {
  const landscape = mode === 'video169';
  const fps = Number(rest[0] ?? (landscape ? 60 : 30));
  const opts = landscape
    ? { fps, bitrate: 6_000_000 }
    : { fps, bitrate: 1_800_000, outWidth: 720, outHeight: 1280 };
  await withPage(landscape ? 1920 : 1080, landscape ? 1080 : 1920, async (page, base) => {
    page.setDefaultTimeout(0);
    await page.goto(`${base}/${landscape ? 'index169.html' : 'index.html'}`);
    await page.evaluate((o) => { window.encode(o).catch((e) => { window.__error = String(e); }); }, opts);
    const started = Date.now();
    let last = 0;
    for (;;) {
      await new Promise((r) => setTimeout(r, 3000));
      const s = await page.evaluate(() => ({ done: window.__done === true, error: window.__error, n: window.__progress ?? 0 }));
      if (s.error) throw new Error(s.error);
      if (s.done) break;
      if (s.n - last >= 120) { last = s.n; console.log(`  ${s.n} frames, ${Math.round((Date.now() - started) / 1000)}s`); }
    }
    const size = await page.evaluate(() => window.__video.length);
    const parts = [];
    for (let offset = 0; offset < size; offset += 1_000_000) {
      parts.push(
        await page.evaluate((o) => {
          const a = window.__video.subarray(o, o + 1_000_000);
          let s = '';
          for (let i = 0; i < a.length; i += 0x8000) s += String.fromCharCode.apply(null, a.subarray(i, i + 0x8000));
          return btoa(s);
        }, offset)
      );
    }
    await writeFile(out, Buffer.concat(parts.map((c) => Buffer.from(c, 'base64'))));
    console.log(`wrote ${out} (${(size / 1e6).toFixed(1)} MB, ${fps} fps)`);
  });
} else {
  console.error('usage: node render.mjs stills|video|poster|og <out> [args]');
  process.exit(1);
}
