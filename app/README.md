# GNSS Log Explorer App (Svelte)

Serverless web app for loading and exploring NMEA logs with synchronized panels.

## Features

- File picker + drag/drop local NMEA logs
- 2x2 panel matrix: map, sky plot, signal bars (PRN + band), text log
- Time slider and playback controls (1x,2x,4x,8x,16x)
- Leaflet default map + optional Cesium switch
- Configurable primary time source (GGA default, RMC optional)
- Parser status, summary statistics, filters, jump-to-event, range zoom
- Export JSON/CSV
- LocalStorage persistence of settings

## Run

```bash
cd app
npm install
npm run dev
```

Build:

```bash
npm run build
```

## GitHub Pages hosting notes

- The Vite config uses `base: './'` so generated asset/script paths are
  relative. This allows deploying `index.html` from a subdirectory instead of
  only from a site root.
- If you add client-side routing later, remember that GitHub Pages does not
  provide SPA rewrites by default; deep links may require a `404.html`
  fallback strategy.
