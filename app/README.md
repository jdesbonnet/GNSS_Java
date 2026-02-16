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
