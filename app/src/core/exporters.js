function download(name, blob, type = 'application/octet-stream') {
  const url = URL.createObjectURL(new Blob([blob], { type }));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  URL.revokeObjectURL(url);
}

export function exportTimelineJSON(timeline) {
  download('timeline.json', JSON.stringify(timeline, null, 2), 'application/json');
}

export function exportPositionsCSV(positions) {
  const rows = ['time,lat,lon,alt,source'];
  for (const p of positions) rows.push(`${p.time},${p.lat},${p.lon},${p.alt ?? ''},${p.source}`);
  download('positions.csv', rows.join('\n'), 'text/csv');
}
