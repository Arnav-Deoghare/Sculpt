# Promo videos

The Sculpt promo videos and their thumbnails are drawn with the canvas API in `scene.js` and rendered with a local copy of Chrome. The art is all vector: no screenshots and no real photos are used, so nobody's body ends up in the promo.

## Render

```bash
cd tools/promo-video
npm install

node render.mjs video  ~/Desktop/sculpt-demo.mp4              # the 28 second portrait video, 720x1280
node render.mjs poster ~/Desktop/sculpt-poster.jpg 8.8        # one portrait frame, as a JPEG
node render.mjs stills /tmp/stills                           # a set of still frames, for checking edits
```

### Landscape version (for X, YouTube and desktop)

`scene169.js` is a 20 second, 1920x1080 cut with headlines beside the phone. It reuses the app screens from `scene.js`. It is rendered frame by frame at an exact time and encoded to H.264 in the browser, so the motion is perfectly smooth at 60 fps, with a little motion blur blended in:

```bash
node render.mjs video169  ~/Desktop/sculpt-promo-landscape.mp4 60   # takes a few minutes
node render.mjs poster169 ~/Desktop/sculpt-promo-thumbnail.jpg 5.5  # a thumbnail frame
node render.mjs stills169 /tmp/stills169                            # still frames, for checking edits
```

The landscape video has no audio. X plays videos muted by default, so the headlines carry the story.

Set `CHROME_PATH` if Chrome isn't in its usual place. The portrait video uses a low bitrate on purpose: flat vector art compresses well, and the file stays around 6 MB.

## Editing

`draw(ctx, t)` in `scene.js` paints the frame at time `t` seconds. The timeline at the bottom of the file lists the scenes (title, camera, compare, privacy, end card). Use `stills` to check a change, then re-record the video.

If the app's look changes (colors, the guide, the Compare screen), update `scene.js` to match.
