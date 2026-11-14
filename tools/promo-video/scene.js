// The Sculpt promo video, drawn with the canvas API. draw(ctx, t) paints the frame at time t
// (seconds). Everything here is vector art: no screenshots and no real photos are used.

export const W = 1080;
export const H = 1920;
export const DURATION = 28;

export const C = {
  M1: '#5C1121', M2: '#4A0D1A', K1: '#0F0E0E', K2: '#1A1717', SEAM: '#050404',
  ACC: '#C23A52', TX: '#EDE3DC', MU: '#A08E89', FIG: '#8DA2B6', FIG2: '#B7A58F'
};
export const FONT = 'system-ui, -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif';

export const clamp = (x, a = 0, b = 1) => Math.min(b, Math.max(a, x));
export const lerp = (a, b, t) => a + (b - a) * t;
export const easeOut = (t) => 1 - Math.pow(1 - t, 3);
export const easeInOut = (t) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
export const seg = (t, a, b) => clamp((t - a) / (b - a));

// ---------- drawing helpers ----------

export function text(ctx, str, x, y, size, color, o = {}) {
  ctx.save();
  ctx.globalAlpha *= o.alpha ?? 1;
  ctx.font = `${o.weight ?? 600} ${size}px ${FONT}`;
  ctx.textAlign = o.align ?? 'center';
  ctx.textBaseline = 'middle';
  if (o.spacing) ctx.letterSpacing = `${o.spacing}px`;
  if (o.shadow) {
    ctx.shadowColor = 'rgba(0,0,0,0.55)';
    ctx.shadowBlur = 18;
    ctx.shadowOffsetY = 4;
  }
  ctx.fillStyle = color;
  ctx.fillText(str, x, y);
  ctx.restore();
}

export function rrect(ctx, x, y, w, h, r) {
  ctx.beginPath();
  ctx.roundRect(x, y, w, h, r);
}

export function pill(ctx, str, cx, cy, size, o = {}) {
  ctx.save();
  ctx.font = `${o.weight ?? 600} ${size}px ${FONT}`;
  const w = ctx.measureText(str).width + (o.padX ?? 44);
  const h = size * 1.9;
  ctx.globalAlpha *= o.alpha ?? 1;
  rrect(ctx, cx - w / 2, cy - h / 2, w, h, h / 2);
  ctx.fillStyle = o.bg ?? 'rgba(0,0,0,0.62)';
  ctx.fill();
  if (o.border) {
    ctx.lineWidth = 3;
    ctx.strokeStyle = o.border;
    ctx.stroke();
  }
  ctx.restore();
  text(ctx, str, cx, cy + 1, size, o.color ?? '#fff', { weight: o.weight ?? 600, alpha: o.alpha });
  return w;
}

// ---------- the mat floor ----------

export function tilePath(ctx, x, y, s, n, d) {
  const u = s / (2 * n + 1);
  const dn = d + 1.4;
  ctx.beginPath();
  ctx.moveTo(x, y);
  for (let k = 0; k <= 2 * n; k++) {
    const a = x + k * u, b = a + u;
    if (k % 2) { ctx.lineTo(a, y + dn); ctx.lineTo(b, y + dn); ctx.lineTo(b, y); } else ctx.lineTo(b, y);
  }
  for (let k = 0; k <= 2 * n; k++) {
    const a = y + k * u, b = a + u;
    if (k % 2) { ctx.lineTo(x + s + d, a); ctx.lineTo(x + s + d, b); ctx.lineTo(x + s, b); } else ctx.lineTo(x + s, b);
  }
  for (let k = 0; k <= 2 * n; k++) {
    const a = x + s - k * u, b = a - u;
    if (k % 2) { ctx.lineTo(a, y + s + d); ctx.lineTo(b, y + s + d); ctx.lineTo(b, y + s); } else ctx.lineTo(b, y + s);
  }
  for (let k = 0; k <= 2 * n; k++) {
    const a = y + s - k * u, b = a - u;
    if (k % 2) { ctx.lineTo(x + dn, a); ctx.lineTo(x + dn, b); ctx.lineTo(x, b); } else ctx.lineTo(x, b);
  }
  ctx.closePath();
}

let grain = null;
export function grainPattern(ctx) {
  if (grain) return grain;
  const c = document.createElement('canvas');
  c.width = c.height = 128;
  const g = c.getContext('2d');
  const img = g.createImageData(128, 128);
  let seed = 4;
  const rnd = () => ((seed = (seed * 1664525 + 1013904223) >>> 0) / 4294967296);
  for (let i = 0; i < 128 * 128; i++) {
    const r = rnd();
    const o = i * 4;
    if (r < 0.08) { img.data.set([255, 255, 255, 14], o); } else if (r < 0.2) { img.data.set([0, 0, 0, 64], o); }
  }
  g.putImageData(img, 0, 0);
  grain = ctx.createPattern(c, 'repeat');
  return grain;
}

// Draws the interlocking floor. `build` (0..1) slides the tiles in; `maroonRows` colours the top rows.
export function floor(ctx, { maroonRows = 0, build = 1, dim = 0, w = W, h = H } = {}) {
  const s = w / 4;
  ctx.fillStyle = C.SEAM;
  ctx.fillRect(0, 0, w, h);
  const rows = Math.ceil(h / s);
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < 4; c++) {
      const delay = (r * 4 + c) * 0.018;
      const p = easeOut(clamp((build - delay) / 0.55));
      if (p <= 0) continue;
      const checker = (r + c) % 2 === 0;
      const color = r < maroonRows ? (checker ? C.M1 : C.K2) : checker ? C.K1 : C.K2;
      ctx.save();
      ctx.globalAlpha = p;
      ctx.translate(0, (1 - p) * -90);
      tilePath(ctx, c * s, r * s, s, 3, s * 0.08);
      ctx.fillStyle = color;
      ctx.fill();
      ctx.lineWidth = 3;
      ctx.strokeStyle = C.SEAM;
      ctx.lineJoin = 'round';
      ctx.stroke();
      ctx.restore();
    }
  }
  ctx.fillStyle = grainPattern(ctx);
  ctx.fillRect(0, 0, w, h);
  if (dim) {
    ctx.fillStyle = `rgba(0,0,0,${dim})`;
    ctx.fillRect(0, 0, w, h);
  }
}

// ---------- the body figure and guide (mirrors PoseGuide.kt) ----------

function limb(ctx, w, pts) {
  ctx.beginPath();
  ctx.moveTo(pts[0][0], pts[0][1]);
  for (let i = 1; i < pts.length; i++) ctx.lineTo(pts[i][0], pts[i][1]);
  ctx.lineWidth = w;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.stroke();
}

// A front-facing mannequin, `h` tall, head top at (cx, top). `shoulder` and `waist` slightly reshape it.
export function figure(ctx, cx, top, h, color, { shoulder = 1, waist = 1, alpha = 1 } = {}) {
  const X = (v) => cx + v * h;
  const Y = (v) => top + v * h;
  ctx.save();
  ctx.globalAlpha *= alpha;
  ctx.fillStyle = color;
  ctx.strokeStyle = color;
  ctx.beginPath();
  ctx.arc(X(0), Y(0.065), 0.062 * h, 0, Math.PI * 2);
  ctx.fill();
  limb(ctx, 0.05 * h, [[X(0), Y(0.11)], [X(0), Y(0.17)]]);
  const sw = 0.14 * shoulder, ww = 0.105 * waist;
  ctx.beginPath();
  ctx.moveTo(X(-sw), Y(0.17));
  ctx.lineTo(X(sw), Y(0.17));
  ctx.lineTo(X(ww), Y(0.34));
  ctx.lineTo(X(0.115), Y(0.5));
  ctx.lineTo(X(-0.115), Y(0.5));
  ctx.lineTo(X(-ww), Y(0.34));
  ctx.closePath();
  ctx.fill();
  for (const s of [-1, 1]) {
    limb(ctx, 0.1 * h, [[X(s * 0.06), Y(0.5)], [X(s * 0.075), Y(0.96)]]);
    limb(ctx, 0.035 * h, [[X(s * 0.075), Y(0.985)], [X(s * 0.135), Y(0.985)]]);
    limb(ctx, 0.055 * h, [[X(s * (sw + 0.005)), Y(0.19)], [X(s * 0.19 * shoulder), Y(0.35)], [X(s * 0.2 * shoulder), Y(0.5)]]);
  }
  ctx.restore();
}

// ---------- the camera screen ----------

const GUIDE = { top: 400, h: 1000 };

function cameraBackdrop(ctx) {
  const g = ctx.createLinearGradient(0, 0, 0, H);
  g.addColorStop(0, '#3a332e');
  g.addColorStop(0.7, '#201a17');
  g.addColorStop(1, '#120e0c');
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, W, H);
  // a door, a window and a floor, all abstract
  ctx.fillStyle = '#4a4038';
  rrect(ctx, 150, 360, 330, 1000, 6); ctx.fill();
  ctx.strokeStyle = '#2b241f'; ctx.lineWidth = 6;
  rrect(ctx, 190, 410, 250, 380, 4); ctx.stroke();
  rrect(ctx, 190, 850, 250, 450, 4); ctx.stroke();
  ctx.fillStyle = 'rgba(214,226,150,0.22)';
  ctx.fillRect(760, 300, 260, 780);
  ctx.fillStyle = '#17120f';
  ctx.beginPath();
  ctx.moveTo(0, 1360); ctx.lineTo(W, 1300); ctx.lineTo(W, H); ctx.lineTo(0, H); ctx.closePath();
  ctx.fill();
  ctx.fillStyle = 'rgba(0,0,0,0.28)';
  ctx.fillRect(0, 0, W, H);
}

function frameLines(ctx, alpha) {
  ctx.save();
  ctx.globalAlpha *= alpha;
  ctx.strokeStyle = 'rgba(255,255,255,0.55)';
  ctx.lineWidth = 3;
  ctx.setLineDash([22, 16]);
  for (const y of [GUIDE.top, GUIDE.top + GUIDE.h]) {
    ctx.beginPath(); ctx.moveTo(W * 0.2, y); ctx.lineTo(W * 0.8, y); ctx.stroke();
  }
  ctx.beginPath(); ctx.moveTo(W / 2, GUIDE.top); ctx.lineTo(W / 2, GUIDE.top + GUIDE.h); ctx.stroke();
  ctx.restore();
}

export function tabBar(ctx, active) {
  ctx.fillStyle = '#161313';
  ctx.fillRect(0, 1790, W, 130);
  const labels = ['Home', 'Camera', 'Gallery', 'Settings'];
  labels.forEach((l, i) => {
    text(ctx, l, 135 + i * 270, 1855, 42, i === active ? C.ACC : C.MU, { weight: i === active ? 800 : 500 });
  });
}

// One frame of the camera screen. tt is seconds since the camera scene started (0 to 10.5).
export function camera(ctx, tt) {
  cameraBackdrop(ctx);

  // guide fades in, then the person walks into it
  const guideIn = easeOut(seg(tt, 0.2, 1.0));
  ctx.save();
  ctx.globalAlpha = 0.22 * guideIn;
  figure(ctx, W / 2, GUIDE.top, GUIDE.h, '#ffffff');
  ctx.restore();
  frameLines(ctx, guideIn);

  const wp = seg(tt, 1.0, 3.8);
  const walk = wp >= 1 ? 1 : 1 - Math.exp(-5.5 * wp) * Math.cos(8.5 * wp);
  const aligned = tt >= 4.1;
  const scale = lerp(0.8, 1, walk);
  const h = GUIDE.h * scale;
  const cx = lerp(W / 2 + 230, W / 2, walk) + Math.sin(tt * 9) * 7 * (1 - Math.min(1, wp));
  const top = GUIDE.top + (GUIDE.h - h);
  const personIn = easeOut(seg(tt, 1.0, 1.6));
  figure(ctx, cx, top, h, C.FIG, { alpha: 0.95 * personIn });

  if (aligned) {
    const pulse = 0.5 + 0.5 * Math.sin((tt - 4.1) * 7);
    ctx.save();
    ctx.globalAlpha = 0.35 + 0.25 * pulse;
    ctx.strokeStyle = C.ACC;
    ctx.lineWidth = 10;
    ctx.setLineDash([]);
    figureOutline(ctx, W / 2, GUIDE.top, GUIDE.h);
    ctx.restore();
    pill(ctx, '✓  Aligned', W / 2, 360, 38, { bg: 'rgba(194,58,82,0.95)', alpha: easeOut(seg(tt, 4.1, 4.5)) });
  }

  // top controls
  pill(ctx, 'Ghost: On', 180, 150, 40);
  pill(ctx, 'Timer: Off  ↔', 540, 150, 40);
  ctx.fillStyle = 'rgba(0,0,0,0.62)';
  ctx.beginPath(); ctx.arc(930, 150, 52, 0, Math.PI * 2); ctx.fill();
  text(ctx, '⚡', 930, 152, 46, '#fff', { weight: 500, alpha: 0.5 });
  pill(ctx, 'Guide: Body', 190, 290, 40, { alpha: 1 });
  ctx.fillStyle = C.ACC; rrect(ctx, 380, 280, 250, 20, 10); ctx.fill();
  ctx.fillStyle = '#45405a'; rrect(ctx, 630, 280, 360, 20, 10); ctx.fill();
  ctx.fillStyle = C.ACC; rrect(ctx, 622, 262, 8, 56, 4); ctx.fill();

  // bottom stack
  const captured = tt >= 7.3;
  const count = captured ? 5 : 4;
  pill(ctx, `${count} of 5 today`, 540, 1440, 40);
  const segW = 150, gap = 14, x0 = 540 - (5 * segW + 4 * gap) / 2;
  for (let i = 0; i < 5; i++) {
    const on = i < 4 || tt >= 8.2;
    ctx.fillStyle = on ? '#fff' : 'rgba(255,255,255,0.45)';
    rrect(ctx, x0 + i * (segW + gap), 1496, segW, 10, 5);
    ctx.fill();
  }
  pill(ctx, `${captured ? '✓ ' : ''}Front  ↔`, 540, 1575, 44);

  // shutter
  const press = tt > 7.0 && tt < 7.3 ? 0.9 : 1;
  ctx.fillStyle = '#fff';
  ctx.beginPath(); ctx.arc(540, 1685, 62 * press, 0, Math.PI * 2); ctx.fill();
  ctx.lineWidth = 9; ctx.strokeStyle = 'rgba(255,255,255,0.95)';
  ctx.beginPath(); ctx.arc(540, 1685, 82, 0, Math.PI * 2); ctx.stroke();
  ctx.fillStyle = '#2d2a27'; rrect(ctx, 70, 1625, 120, 120, 18); ctx.fill();
  ctx.fillStyle = 'rgba(0,0,0,0.62)';
  ctx.beginPath(); ctx.arc(960, 1685, 58, 0, Math.PI * 2); ctx.fill();
  text(ctx, '↻', 960, 1690, 54, '#fff', { weight: 500 });

  tabBar(ctx, 1);
}

export function figureOutline(ctx, cx, top, h) {
  const X = (v) => cx + v * h, Y = (v) => top + v * h;
  ctx.beginPath();
  ctx.arc(X(0), Y(0.065), 0.062 * h, 0, Math.PI * 2);
  ctx.moveTo(X(-0.14), Y(0.17));
  ctx.lineTo(X(0.14), Y(0.17)); ctx.lineTo(X(0.105), Y(0.34)); ctx.lineTo(X(0.115), Y(0.5));
  ctx.lineTo(X(0.135), Y(0.985)); ctx.lineTo(X(-0.135), Y(0.985)); ctx.lineTo(X(-0.115), Y(0.5));
  ctx.lineTo(X(-0.105), Y(0.34)); ctx.closePath();
  ctx.stroke();
}

// ---------- scenes ----------

// A small label at the very top of the screen naming the step being shown.
function stepLabel(ctx, step, line, a) {
  pill(ctx, `${step}  \u00B7  ${line}`, 540, 72, 38, { bg: 'rgba(194,58,82,0.96)', alpha: a, padX: 60, weight: 700 });
}

function sceneTitle(ctx, t) {
  floor(ctx, { maroonRows: 2, build: t * 0.9 + 0.1 });
  const a = easeOut(seg(t, 1.0, 1.8));
  ctx.save();
  ctx.globalAlpha = a * 0.78;
  rrect(ctx, 100, 740, 880, 420, 36);
  ctx.fillStyle = '#0c0a0a';
  ctx.fill();
  ctx.restore();
  ctx.save();
  ctx.translate(540, 880);
  ctx.scale(lerp(0.92, 1, a), lerp(0.92, 1, a));
  text(ctx, 'Sculpt', 0, 0, 190, C.TX, { weight: 800, alpha: a, spacing: -2 });
  ctx.restore();
  text(ctx, 'Progress photos,', 540, 1020, 60, C.MU, { weight: 600, alpha: easeOut(seg(t, 1.7, 2.5)) });
  text(ctx, 'same pose every time.', 540, 1096, 60, C.TX, { weight: 700, alpha: easeOut(seg(t, 2.0, 2.8)) });
}

// The camera screen including the shutter flash and the shot flying into its progress segment.
export function cameraScreen(ctx, tt) {
  camera(ctx, tt);

  // capture: flash, then the frozen shot shrinks into its progress segment
  const flash = tt > 7.0 ? 1 - seg(tt, 7.0, 7.5) : 0;
  const fly = easeInOut(seg(tt, 7.3, 8.3));
  if (fly > 0 && fly < 1) {
    const shot = document.createElement('canvas');
    shot.width = W; shot.height = H;
    camera(shot.getContext('2d'), 7.0);
    const sc = lerp(1, 0.07, fly);
    ctx.save();
    ctx.translate(lerp(540, 868, fly), lerp(960, 1501, fly));
    ctx.scale(sc, sc);
    rrect(ctx, -540, -960, W, H, lerp(0, 900, fly));
    ctx.clip();
    ctx.globalAlpha = 1 - 0.5 * fly;
    ctx.drawImage(shot, -540, -960);
    ctx.restore();
  }
  if (flash > 0) {
    ctx.fillStyle = `rgba(255,255,255,${0.8 * flash})`;
    ctx.fillRect(0, 0, W, H);
  }
}

function sceneCamera(ctx, t) {
  const tt = t - 4;
  cameraScreen(ctx, tt);
  if (tt < 6.3) stepLabel(ctx, 'STEP 1', 'Line up with the guide', easeOut(seg(tt, 0.3, 1.0)) * (1 - seg(tt, 5.8, 6.3)));
  else stepLabel(ctx, 'STEP 2', 'One tap. Every pose.', easeOut(seg(tt, 6.3, 6.9)));
}

function pillRow(ctx, items, cy, size, selected) {
  ctx.save();
  ctx.font = `600 ${size}px ${FONT}`;
  const widths = items.map((s) => ctx.measureText(s).width + 56);
  ctx.restore();
  const total = widths.reduce((a, b) => a + b, 0) + 20 * (items.length - 1);
  let x = 540 - total / 2;
  items.forEach((s, i) => {
    const sel = selected === i;
    const h = size * 2;
    rrect(ctx, x, cy - h / 2, widths[i], h, h / 2);
    ctx.fillStyle = sel ? C.M1 : '#1f1b1b';
    ctx.fill();
    text(ctx, s, x + widths[i] / 2, cy + 1, size, sel ? C.TX : C.MU, { weight: sel ? 800 : 500 });
    x += widths[i] + 20;
  });
}

export function compareScreen(ctx, tt) {
  ctx.fillStyle = C.K1;
  ctx.fillRect(0, 0, W, H);
  text(ctx, 'Compare', 60, 175, 84, C.TX, { weight: 500, align: 'left' });
  text(ctx, 'Done', 1010, 185, 46, C.ACC, { weight: 600, align: 'right' });

  pillRow(ctx, ['Front · 3', 'Back · 2'], 330, 40, 0);
  const mode = easeInOut(seg(tt, 3.4, 3.9));
  pillRow(ctx, ['Side by side', 'Fade'], 450, 42, mode < 0.5 ? 0 : 1);

  const ax = 40, ay = 540, aw = 1000, ah = 910;
  const before = { shoulder: 1, waist: 1 };
  const after = { shoulder: 1.1, waist: 0.9 };
  const merge = easeInOut(seg(tt, 3.9, 4.8));
  const mix = easeInOut(seg(tt, 4.8, 7.0));

  if (merge < 1) {
    const gapW = lerp(12, 0, merge);
    const pw = (aw - 12) / 2;
    // left panel slides to the full width as the right one fades under it
    const lw = lerp(pw, aw, merge);
    panel(ctx, ax, ay, lw, ah, () => {
      figure(ctx, ax + lw / 2, ay + 90, ah - 190, C.FIG, before);
    }, 'Before');
    ctx.save();
    ctx.globalAlpha = 1 - merge;
    panel(ctx, ax + pw + gapW, ay, pw, ah, () => {
      figure(ctx, ax + pw + gapW + pw / 2, ay + 90, ah - 190, C.FIG2, after);
    }, 'After');
    ctx.restore();
  } else {
    panel(ctx, ax, ay, aw, ah, () => {
      figure(ctx, ax + aw / 2, ay + 90, ah - 190, C.FIG, { ...before, alpha: 1 });
      figure(ctx, ax + aw / 2, ay + 90, ah - 190, C.FIG2, { ...after, alpha: mix });
    }, null);
  }

  // fade slider
  const sl = easeOut(seg(tt, 4.4, 5.0));
  if (sl > 0) {
    ctx.save();
    ctx.globalAlpha = sl;
    text(ctx, 'Before', 80, 1500, 36, C.TX, { weight: 600, align: 'left' });
    text(ctx, 'After', 1000, 1500, 36, C.TX, { weight: 600, align: 'right' });
    const x1 = 230, x2 = 850;
    ctx.fillStyle = '#45405a'; rrect(ctx, x1, 1490, x2 - x1, 20, 10); ctx.fill();
    const px = lerp(x1, x2, mix);
    ctx.fillStyle = C.ACC; rrect(ctx, x1, 1490, px - x1, 20, 10); ctx.fill();
    ctx.fillStyle = C.ACC; rrect(ctx, px - 5, 1466, 10, 68, 5); ctx.fill();
    ctx.restore();
  }

  // date cards
  card(ctx, 40, 1580, 430, 'Before', '7 Aug 2026', 'Front');
  card(ctx, 610, 1580, 430, 'After', '7 Nov 2026', 'Front');
  text(ctx, '3 months', 540, 1665, 30, C.MU, { weight: 600 });
  tabBar(ctx, 2);

}

function sceneCompare(ctx, t) {
  const tt = t - 14;
  compareScreen(ctx, tt);
  stepLabel(ctx, 'STEP 3', 'Compare any two dates', easeOut(seg(tt, 0.3, 0.9)));
}

export function panel(ctx, x, y, w, h, content, tag) {
  ctx.save();
  rrect(ctx, x, y, w, h, 24);
  ctx.fillStyle = '#000';
  ctx.fill();
  ctx.clip();
  content();
  ctx.restore();
  if (tag) pill(ctx, tag, x + 80, y + 44, 30, { padX: 36 });
}

export function card(ctx, x, y, w, label, date, pose) {
  rrect(ctx, x, y, w, 170, 24);
  ctx.fillStyle = '#1b1818';
  ctx.fill();
  ctx.lineWidth = 2; ctx.strokeStyle = '#3a2f2f'; ctx.stroke();
  text(ctx, label, x + 30, y + 40, 30, C.ACC, { weight: 700, align: 'left' });
  text(ctx, date, x + 30, y + 92, 48, C.TX, { weight: 700, align: 'left' });
  text(ctx, pose, x + 30, y + 140, 30, C.MU, { weight: 500, align: 'left' });
}

function scenePrivacy(ctx, t) {
  const tt = t - 21;
  floor(ctx, { maroonRows: 0, build: 1, dim: 0.62 });
  const a = easeOut(seg(tt, 0.1, 0.8));
  text(ctx, 'PRIVATE BY DESIGN', 540, 200, 40, C.ACC, { weight: 800, spacing: 6, alpha: a });

  // phone with a camera, and a crossed-out wifi symbol
  const px = 540, py = 640;
  ctx.save();
  ctx.globalAlpha = a;
  ctx.lineWidth = 10; ctx.strokeStyle = C.TX;
  rrect(ctx, px - 170, py - 290, 340, 580, 52); ctx.stroke();
  ctx.fillStyle = C.TX; rrect(ctx, px - 40, py - 262, 80, 12, 6); ctx.fill();
  ctx.fillStyle = C.M1; ctx.beginPath(); ctx.arc(px, py + 10, 78, 0, Math.PI * 2); ctx.fill();
  ctx.lineWidth = 12; ctx.strokeStyle = C.TX; ctx.beginPath(); ctx.arc(px, py + 10, 78, 0, Math.PI * 2); ctx.stroke();
  ctx.fillStyle = C.TX; ctx.beginPath(); ctx.arc(px, py + 10, 30, 0, Math.PI * 2); ctx.fill();
  ctx.restore();

  const wf = easeOut(seg(tt, 0.9, 1.5));
  ctx.save();
  ctx.globalAlpha = wf;
  ctx.strokeStyle = C.MU; ctx.lineWidth = 14; ctx.lineCap = 'round';
  const wx = 860, wy = 560;
  for (const r of [50, 100, 150]) {
    ctx.beginPath(); ctx.arc(wx, wy + 70, r, -Math.PI * 0.8, -Math.PI * 0.2); ctx.stroke();
  }
  ctx.fillStyle = C.MU; ctx.beginPath(); ctx.arc(wx, wy + 70, 12, 0, Math.PI * 2); ctx.fill();
  const slash = easeOut(seg(tt, 1.4, 1.9));
  ctx.strokeStyle = '#ff5b6e'; ctx.lineWidth = 18;
  ctx.beginPath(); ctx.moveTo(wx - 110, wy - 30); ctx.lineTo(lerp(wx - 110, wx + 110, slash), lerp(wy - 30, wy + 190, slash)); ctx.stroke();
  ctx.restore();

  const lines = ['Only the camera permission.', 'No internet. No accounts. No ads.', 'Your photos never leave your phone.'];
  lines.forEach((l, i) => {
    const p = easeOut(seg(tt, 1.9 + i * 0.6, 2.5 + i * 0.6));
    ctx.save();
    ctx.translate(0, (1 - p) * 30);
    text(ctx, l, 540, 1140 + i * 130, 54, i === 2 ? C.TX : '#d5c9c2', { weight: i === 2 ? 800 : 600, alpha: p, shadow: true });
    ctx.restore();
  });
}

function sceneEnd(ctx, t) {
  const tt = t - 25;
  floor(ctx, { maroonRows: 3, build: 1, dim: 0.15 });
  const a = easeOut(seg(tt, 0.1, 0.9));
  // the app icon: a 3x3 grid of mat tiles
  ctx.save();
  ctx.globalAlpha = a;
  const s = 130, ox = 540 - 1.5 * s, oy = 460;
  rrect(ctx, ox - 30, oy - 30, 3 * s + 60, 3 * s + 60, 70);
  ctx.fillStyle = C.SEAM; ctx.fill();
  ctx.save(); rrect(ctx, ox - 30, oy - 30, 3 * s + 60, 3 * s + 60, 70); ctx.clip();
  for (let r = 0; r < 3; r++) for (let c = 0; c < 3; c++) {
    tilePath(ctx, ox + c * s, oy + r * s, s, 2, 8);
    ctx.fillStyle = (r + c) % 2 === 0 ? '#6B1426' : '#1E1B1B'; ctx.fill();
    ctx.lineWidth = 3; ctx.strokeStyle = C.SEAM; ctx.stroke();
  }
  ctx.restore();
  ctx.restore();
  text(ctx, 'Sculpt', 540, 1000, 170, C.TX, { weight: 800, alpha: a, shadow: true, spacing: -2 });
  text(ctx, 'Free and open source', 540, 1140, 58, C.TX, { weight: 600, alpha: easeOut(seg(tt, 0.6, 1.3)), shadow: true });
  text(ctx, 'Android 8.0+  ·  Apache-2.0', 540, 1226, 42, C.MU, { weight: 500, alpha: easeOut(seg(tt, 0.8, 1.5)), shadow: true });
  const pa = easeOut(seg(tt, 1.1, 1.8));
  pill(ctx, 'github.com/Arnav-Deoghare/Sculpt', 540, 1420, 44, { bg: C.ACC, alpha: pa, padX: 70, weight: 700 });
}

// ---------- the timeline ----------

export function draw(ctx, t) {
  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.globalAlpha = 1;
  ctx.clearRect(0, 0, W, H);

  const scenes = [
    { from: 0, to: 4.5, fn: sceneTitle },
    { from: 4, to: 14.5, fn: sceneCamera },
    { from: 14, to: 21.5, fn: sceneCompare },
    { from: 21, to: 25.5, fn: scenePrivacy },
    { from: 25, to: DURATION + 1, fn: sceneEnd }
  ];
  for (const s of scenes) {
    if (t < s.from || t > s.to) continue;
    ctx.save();
    ctx.globalAlpha = s.from === 0 ? 1 : easeOut(seg(t, s.from, s.from + 0.5));
    s.fn(ctx, t);
    ctx.restore();
  }
  ctx.globalAlpha = 1;
}
