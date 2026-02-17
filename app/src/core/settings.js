const KEY = 'gnss-log-explorer-settings';

export const defaultSettings = {
  mapEngine: 'leaflet',
  timeSource: 'GGA',
  speed: 1,
  syncMode: 'epoch',
  constellations: { GP: true, GL: true, GA: true, GB: true, GN: true },
  sentenceFilter: { GGA: true, RMC: true, GSV: true, GSA: true, VTG: true, OTHER: true }
};

export function loadSettings() {
  try {
    const parsed = JSON.parse(localStorage.getItem(KEY) || '{}');
    return {
      ...defaultSettings,
      ...parsed,
      constellations: { ...defaultSettings.constellations, ...(parsed.constellations || {}) },
      sentenceFilter: { ...defaultSettings.sentenceFilter, ...(parsed.sentenceFilter || {}) }
    };
  } catch {
    return { ...defaultSettings };
  }
}

export function saveSettings(settings) {
  localStorage.setItem(KEY, JSON.stringify(settings));
}
