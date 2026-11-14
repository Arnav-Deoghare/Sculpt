// The landscape (16:9) promo video, made for X and YouTube. It reuses the app screens drawn in
// scene.js, shows them on a phone, and puts big headlines next to it. draw(ctx, t) paints the
// frame at time t seconds, and is a pure function of t so frames can be rendered with motion blur.

import {
  C, clamp, lerp, easeInOut, seg, text, rrect, pill, tilePath, grainPattern,
  cameraScreen, compareScreen
} from './scene.js';

export const W = 1920;
export const H = 1080;
export const DURATION = 20;

// ---------- motion ----------

const easeOutExpo = (p) => (p >= 1 ? 1 : 1 - Math.pow(2, -10 * clamp(p)));
const easeInCubic = (p) => clamp(p) ** 3;
const springOut = (p) => (p >= 1 ? 1 : p <= 0 ? 0 : 1 - Math.exp(-6.5 * p) * Math.cos(10.5 * p));
const easeInOutCubic = (p) => easeInOut(clamp(p));

// ---------- offscreen canvases for the phone screen ----------

const SW = 1080;
const SH = 1920;
const layers = {};
function layer(name) {
  if (!layers[name]) {
    const c = document.createElement('canvas');
    c.width = SW;
    c.height = SH;
    layers[name] = { canvas: c, ctx: c.getContext('2d') };
  }
  const l = layers[name];
  l.ctx.setTransform(1, 0, 0, 1, 0, 0);
  l.ctx.globalAlpha = 1;
  l.ctx.clearRect(0, 0, SW, SH);
  return l;
}

// ---------- background ----------

function background(ctx, t, dim) {
  const s = 480;
  const drift = (t * 34) % (2 * s);
  ctx.fillStyle = C.SEAM;
  ctx.fillRect(0, 0, W, H);
  for (let r = -2; r <= 4; r++) {
    for (let c = -2; c <= 6; c++) {
      const checker = (r + c) % 2 === 0;
      const color = checker ? (((r % 2) + 2) % 2 === 0 ? C.M1 : C.K1) : C.K2;
      tilePath(ctx, c * s - drift, r * s - drift * 0.45, s, 3, s * 0.08);
      ctx.fillStyle = color;
      ctx.fill();
      ctx.lineWidth = 4;
      ctx.strokeStyle = C.SEAM;
      ctx.lineJoin = 'round';
      ctx.stroke();
    }
  }
  ctx.fillStyle = grainPattern(ctx);
  ctx.fillRect(0, 0, W, H);

  const g = ctx.createLinearGradient(0, 0, W * 0.75, 0);
  g.addColorStop(0, `rgba(8,6,6,${0.86 * dim})`);
  g.addColorStop(1, `rgba(8,6,6,${0.42 * dim})`);
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, W, H);
}

// ---------- the phone ----------

const PHONE_F = 0.5; // screen scale
const PW = SW * PHONE_F;
const PH = SH * PHONE_F;

function phone(ctx, t, content) {
  const enter = seg(t, 0.15, 1.15);
  const exit = easeInCubic(seg(t, 14.0, 14.8));
  if (enter <= 0 || exit >= 1) return;

  const se = springOut(enter);
  const cx = 1400 + exit * 900;
  const cy = 540 + (1 - se) * 900 + Math.sin(t * 1.3) * 6;
  const rot = (-10 * (1 - se) + Math.sin(t * 0.8) * 0.5 + exit * 8) * (Math.PI / 180);
  const scale = lerp(0.9, 1, se) * (1 + 0.012 * seg(t, 1.2, 20));

  ctx.save();
  ctx.globalAlpha = 1 - exit;
  ctx.translate(cx, cy);
  ctx.rotate(rot);
  ctx.scale(scale, scale);

  // soft maroon glow and a drop shadow
  const glow = ctx.createRadialGradient(0, 0, 120, 0, 0, 640);
  glow.addColorStop(0, 'rgba(194,58,82,0.30)');
  glow.addColorStop(1, 'rgba(194,58,82,0)');
  ctx.fillStyle = glow;
  ctx.fillRect(-700, -700, 1400, 1400);

  ctx.shadowColor = 'rgba(0,0,0,0.6)';
  ctx.shadowBlur = 70;
  ctx.shadowOffsetY = 30;
  rrect(ctx, -PW / 2 - 14, -PH / 2 - 14, PW + 28, PH + 28, 70);
  ctx.fillStyle = '#050404';
  ctx.fill();
  ctx.shadowColor = 'transparent';
  ctx.lineWidth = 3;
  ctx.strokeStyle = '#332a2a';
  ctx.stroke();

  ctx.save();
  rrect(ctx, -PW / 2, -PH / 2, PW, PH, 56);
  ctx.clip();
  ctx.drawImage(content, -PW / 2, -PH / 2, PW, PH);
  ctx.restore();
  ctx.fillStyle = '#050404';
  ctx.beginPath();
  ctx.arc(0, -PH / 2 + 30, 11, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();
}

// What the phone shows: the camera, pushed aside by the compare screen.
function phoneScreen(t) {
  const cam = (t - 2.0) * 1.4;
  const cmp = (t - 8.9) * 1.5;
  const push = easeInOutCubic(seg(t, 8.55, 9.25));

  const out = layer('out');
  if (push <= 0) {
    cameraScreen(out.ctx, clamp(cam, 0, 10.5));
    return out.canvas;
  }
  const a = layer('a');
  cameraScreen(a.ctx, clamp(cam, 0, 10.5));
  if (push >= 1) {
    compareScreen(out.ctx, clamp(cmp, 0, 8));
    return out.canvas;
  }
  const b = layer('b');
  compareScreen(b.ctx, clamp(cmp, 0, 8));
  out.ctx.drawImage(a.canvas, -push * SW, 0);
  out.ctx.drawImage(b.canvas, (1 - push) * SW, 0);
  return out.canvas;
}

// ---------- text ----------

// One line of text that rises out of a mask, and lifts away again when its block ends.
function line(ctx, str, x, y, size, color, from, to, { delay = 0, weight = 800, align = 'left' } = {}) {
  const p = easeOutExpo(seg(ctx.__t, from + delay, from + delay + 0.7));
  const q = easeInCubic(seg(ctx.__t, to - 0.35, to));
  if (p <= 0 || q >= 1) return;
  ctx.save();
  ctx.beginPath();
  ctx.rect(0, y - size * 0.8, W, size * 1.6);
  ctx.clip();
  text(ctx, str, x, y + (1 - p) * size * 1.15 - q * size * 0.9, size, color, {
    weight, align, alpha: (1 - q) * clamp(p * 2.4)
  });
  ctx.restore();
}

function chip(ctx, str, x, y, from, to) {
  const p = easeOutExpo(seg(ctx.__t, from, from + 0.5));
  const q = easeInCubic(seg(ctx.__t, to - 0.3, to));
  if (p <= 0 || q >= 1) return;
  ctx.save();
  ctx.font = `800 34px system-ui, -apple-system, sans-serif`;
  const w = ctx.measureText(str).width + 56;
  ctx.globalAlpha = (1 - q) * p;
  ctx.translate((1 - p) * -40, 0);
  rrect(ctx, x, y - 32, w, 64, 32);
  ctx.fillStyle = C.ACC;
  ctx.fill();
  ctx.restore();
  ctx.save();
  ctx.translate((1 - p) * -40, 0);
  text(ctx, str, x + w / 2, y + 1, 34, '#fff', { weight: 800, alpha: (1 - q) * p, spacing: 3 });
  ctx.restore();
}

function block(ctx, from, to, step, lines, sub) {
  const t = ctx.__t;
  if (t < from || t > to) return;
  const x = 130;
  if (step) chip(ctx, step, x, 270, from, to);
  lines.forEach((l, i) => line(ctx, l.text, x, 430 + i * 150, 138, l.color ?? '#ffffff', from, to, { delay: 0.1 + i * 0.1 }));
  sub.forEach((s, i) => line(ctx, s, x, 430 + lines.length * 150 + 30 + i * 62, 46, C.MU, from, to, { delay: 0.45 + i * 0.08, weight: 600 }));
}

// ---------- scenes without the phone ----------

function icon(ctx, kind, cx, cy, a) {
  ctx.save();
  ctx.globalAlpha = a;
  ctx.strokeStyle = C.TX;
  ctx.fillStyle = C.TX;
  ctx.lineWidth = 9;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  if (kind === 'camera') {
    rrect(ctx, cx - 52, cy - 34, 104, 72, 16);
    ctx.stroke();
    ctx.beginPath(); ctx.arc(cx, cy + 2, 19, 0, Math.PI * 2); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(cx - 22, cy - 34); ctx.lineTo(cx - 14, cy - 50); ctx.lineTo(cx + 14, cy - 50); ctx.lineTo(cx + 22, cy - 34); ctx.stroke();
  } else if (kind === 'wifi') {
    for (const r of [24, 48, 72]) {
      ctx.beginPath(); ctx.arc(cx, cy + 36, r, -Math.PI * 0.8, -Math.PI * 0.2); ctx.stroke();
    }
    ctx.beginPath(); ctx.arc(cx, cy + 36, 5, 0, Math.PI * 2); ctx.fill();
    ctx.strokeStyle = '#ff5b6e';
    ctx.lineWidth = 11;
    ctx.beginPath(); ctx.moveTo(cx - 62, cy - 44); ctx.lineTo(cx + 62, cy + 52); ctx.stroke();
  } else {
    rrect(ctx, cx - 34, cy - 56, 68, 112, 16);
    ctx.stroke();
    ctx.beginPath(); ctx.arc(cx, cy + 4, 12, 0, Math.PI * 2); ctx.stroke();
  }
  ctx.restore();
}

function privacy(ctx, t) {
  const from = 14.0, to = 17.6;
  if (t < from - 0.3 || t > to) return;
  const exit = easeInCubic(seg(t, to - 0.35, to));
  ctx.save();
  ctx.globalAlpha = 1 - exit;
  const a = easeOutExpo(seg(t, from + 0.5, from + 1.1));
  text(ctx, 'PRIVATE BY DESIGN', 960, 200, 38, C.ACC, { weight: 800, spacing: 7, alpha: a });
  const rows = [
    ['camera', 'The camera is the only permission.'],
    ['wifi', 'No internet. No accounts. No ads.'],
    ['phone', 'Your photos never leave your phone.']
  ];
  rows.forEach(([kind, label], i) => {
    const p = easeOutExpo(seg(t, from + 0.7 + i * 0.4, from + 1.4 + i * 0.4));
    const y = 400 + i * 190;
    ctx.save();
    ctx.translate((1 - p) * -120, 0);
    rrect(ctx, 260, y - 78, 1400, 156, 36);
    ctx.globalAlpha = (1 - exit) * p * 0.78;
    ctx.fillStyle = '#0c0a0a';
    ctx.fill();
    ctx.restore();
    ctx.save();
    ctx.translate((1 - p) * -120, 0);
    icon(ctx, kind, 380, y, p * (1 - exit));
    text(ctx, label, 500, y + 2, 60, i === 2 ? '#ffffff' : '#e3d8d1', { weight: i === 2 ? 800 : 600, align: 'left', alpha: p * (1 - exit) });
    ctx.restore();
  });
  ctx.restore();
}

function ending(ctx, t) {
  const from = 17.4;
  if (t < from) return;
  const p = seg(t, from, from + 0.9);
  const a = easeOutExpo(p);
  const sc = lerp(0.7, 1, springOut(p));
  ctx.save();
  ctx.globalAlpha = a;
  ctx.translate(960, 330);
  ctx.scale(sc, sc);
  const s = 98, ox = -1.5 * s, oy = -1.5 * s;
  rrect(ctx, ox - 24, oy - 24, 3 * s + 48, 3 * s + 48, 62);
  ctx.fillStyle = C.SEAM;
  ctx.fill();
  ctx.save();
  rrect(ctx, ox - 24, oy - 24, 3 * s + 48, 3 * s + 48, 62);
  ctx.clip();
  for (let r = 0; r < 3; r++) for (let c = 0; c < 3; c++) {
    tilePath(ctx, ox + c * s, oy + r * s, s, 2, 7);
    ctx.fillStyle = (r + c) % 2 === 0 ? '#6B1426' : '#1E1B1B';
    ctx.fill();
    ctx.lineWidth = 3; ctx.strokeStyle = C.SEAM; ctx.stroke();
  }
  ctx.restore();
  ctx.restore();

  const l = (str, y, size, color, d, weight) => {
    const q = easeOutExpo(seg(t, from + d, from + d + 0.6));
    ctx.save();
    ctx.beginPath(); ctx.rect(0, y - size * 0.8, W, size * 1.6); ctx.clip();
    text(ctx, str, 960, y + (1 - q) * size, size, color, { weight, alpha: q, spacing: size > 150 ? -3 : 0 });
    ctx.restore();
  };
  l('Sculpt', 610, 190, C.TX, 0.15, 800);
  l('Free and open source', 740, 58, C.TX, 0.35, 600);
  l('Android 8.0+  ·  Apache-2.0', 810, 38, C.MU, 0.45, 500);
  const pa = easeOutExpo(seg(t, from + 0.7, from + 1.3));
  ctx.save();
  ctx.translate(0, (1 - pa) * 30);
  pill(ctx, 'github.com/Arnav-Deoghare/Sculpt', 960, 930, 50, { bg: C.ACC, alpha: pa, padX: 80, weight: 700 });
  ctx.restore();
}

// ---------- the timeline ----------

export function draw(ctx, t) {
  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.globalAlpha = 1;
  ctx.__t = t;

  const full = t >= 13.8; // the phone is gone, so the floor shows through
  background(ctx, t, full ? lerp(0.9, 1.15, seg(t, 14, 15)) : 1);

  phone(ctx, t, phoneScreen(t));

  // left-hand headlines
  block(ctx, 0.25, 2.5, null, [{ text: 'Same pose.' }, { text: 'Every time.', color: '#e2566d' }], ['Private progress photos for Android.']);
  block(ctx, 2.5, 6.5, 'STEP 1', [{ text: 'Line up with' }, { text: 'the guide.' }], ['A faint outline for every pose,', 'so every photo matches.']);
  block(ctx, 6.5, 8.9, 'STEP 2', [{ text: 'One tap.' }, { text: 'Every pose.' }], ['Watch your day fill up.']);
  block(ctx, 8.9, 13.9, 'STEP 3', [{ text: 'Compare any' }, { text: 'two dates.' }], ['Side by side, or faded together.']);

  privacy(ctx, t);
  ending(ctx, t);
}
