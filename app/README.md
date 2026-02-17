# GNSS Log Explorer Webapps

This folder now contains **three main parts**:

1. **Common GNSS visualization library** (`src/common/gnssVisualizationLibrary.js` + `src/core` + `src/panels`) shared by all UI variants.
2. **Svelte implementation** (`index.html`, `src/main.js`, `src/App.svelte`).
3. **Vue + HTML implementation** (`vue.html`, `src/vue/main.js`, `src/vue/vueApp.js`).

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

Open:

- Svelte app: `http://localhost:5173/`
- Vue app: `http://localhost:5173/vue.html`

Optional convenience commands:

```bash
npm run dev:svelte
npm run dev:vue
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
