// Renders every frame of a scene at its exact time, blends a few sub-frames for motion blur, and
// encodes the result to an H.264 MP4 with WebCodecs. Because each frame is drawn from its time
// alone, the video is perfectly smooth however long the rendering takes.

import { Muxer, ArrayBufferTarget } from './node_modules/mp4-muxer/build/mp4-muxer.mjs';

export async function encodeVideo({
  canvas, draw, duration, fps = 60, bitrate = 6_000_000, samples = 4, shutter = 0.5,
  outWidth = canvas.width, outHeight = canvas.height
}) {
  const ctx = canvas.getContext('2d');
  const muxer = new Muxer({
    target: new ArrayBufferTarget(),
    video: { codec: 'avc', width: outWidth, height: outHeight },
    fastStart: 'in-memory'
  });
  const encoder = new VideoEncoder({
    output: (chunk, meta) => muxer.addVideoChunk(chunk, meta),
    error: (e) => { window.__error = String(e); }
  });
  const config = { codec: 'avc1.64002A', width: outWidth, height: outHeight, bitrate, framerate: fps };
  const support = await VideoEncoder.isConfigSupported(config);
  if (!support.supported) throw new Error('H.264 encoding is not supported by this browser');
  encoder.configure(config);

  const blend = document.createElement('canvas');
  blend.width = canvas.width;
  blend.height = canvas.height;
  const b = blend.getContext('2d');
  const out = document.createElement('canvas');
  out.width = outWidth;
  out.height = outHeight;
  const o = out.getContext('2d');
  o.imageSmoothingQuality = 'high';
  const total = Math.round(duration * fps);

  for (let i = 0; i < total; i++) {
    const base = i / fps;
    for (let k = 0; k < samples; k++) {
      const t = Math.max(0, base + ((k + 0.5) / samples - 0.5) * (shutter / fps));
      draw(ctx, t);
      b.globalAlpha = 1 / (k + 1);
      b.drawImage(canvas, 0, 0);
    }
    o.drawImage(blend, 0, 0, outWidth, outHeight);
    const frame = new VideoFrame(out, { timestamp: Math.round((i * 1e6) / fps), duration: Math.round(1e6 / fps) });
    encoder.encode(frame, { keyFrame: i % (fps * 2) === 0 });
    frame.close();
    while (encoder.encodeQueueSize > 6) await new Promise((r) => setTimeout(r, 4));
    window.__progress = i + 1;
    if (i % 10 === 0) await new Promise((r) => setTimeout(r));
  }
  await encoder.flush();
  muxer.finalize();
  window.__video = new Uint8Array(muxer.target.buffer);
  window.__done = true;
}
