let buffer = '';
let lineNo = 0;
let records = [];
let parserState = {
  total: 0,
  malformed: 0,
  checksumFailed: 0,
  unsupported: 0
};

function parseUtcToSeconds(raw) {
  if (!raw || raw.length < 6) return null;
  const hh = Number(raw.slice(0, 2));
  const mm = Number(raw.slice(2, 4));
  const ss = Number(raw.slice(4));
  if ([hh, mm, ss].some(Number.isNaN)) return null;
  return hh * 3600 + mm * 60 + ss;
}

function parseLatLon(raw, hemi, isLat) {
  if (!raw) return null;
  const split = isLat ? 2 : 3;
  const deg = Number(raw.slice(0, split));
  const min = Number(raw.slice(split));
  if (Number.isNaN(deg) || Number.isNaN(min)) return null;
  let val = deg + min / 60;
  if (hemi === 'S' || hemi === 'W') val *= -1;
  return val;
}

function checksumOk(line) {
  if (!line.startsWith('$') || !line.includes('*')) return false;
  const [payload, cks] = line.slice(1).split('*');
  const computed = payload.split('').reduce((a, c) => a ^ c.charCodeAt(0), 0);
  return computed.toString(16).toUpperCase().padStart(2, '0') === cks.slice(0, 2).toUpperCase();
}

function parseLine(line) {
  const trimmed = line.trim();
  if (!trimmed) return null;
  parserState.total += 1;
  if (!trimmed.startsWith('$') || trimmed.length < 6) {
    parserState.malformed += 1;
    return { kind: 'raw', line: trimmed, error: 'malformed' };
  }
  if (trimmed.includes('*') && !checksumOk(trimmed)) {
    parserState.checksumFailed += 1;
  }
  const body = trimmed.slice(1).split('*')[0];
  const parts = body.split(',');
  const talker = parts[0].slice(0, 2);
  const type = parts[0].slice(2);
  const rec = { kind: 'raw', line: trimmed, talker, type, time: null, lineNo };

  if (type === 'GGA') {
    rec.kind = 'position';
    rec.source = 'GGA';
    rec.time = parseUtcToSeconds(parts[1]);
    rec.lat = parseLatLon(parts[2], parts[3], true);
    rec.lon = parseLatLon(parts[4], parts[5], false);
    rec.alt = Number(parts[9]);
    rec.fixQuality = Number(parts[6]);
    rec.satUsed = Number(parts[7]);
  } else if (type === 'RMC') {
    rec.kind = 'position';
    rec.source = 'RMC';
    rec.time = parseUtcToSeconds(parts[1]);
    rec.status = parts[2];
    rec.lat = parseLatLon(parts[3], parts[4], true);
    rec.lon = parseLatLon(parts[5], parts[6], false);
    rec.speedKnots = Number(parts[7]);
    rec.course = Number(parts[8]);
  } else if (type === 'GSV') {
    rec.kind = 'satellites';
    rec.time = null;
    rec.satellites = [];
    const signalBand = parts[parts.length - 1] || 'U';
    for (let i = 4; i + 3 < parts.length; i += 4) {
      const prn = parts[i];
      const el = Number(parts[i + 1]);
      const az = Number(parts[i + 2]);
      const snr = Number((parts[i + 3] || '').split('*')[0]);
      if (!Number.isNaN(el) && !Number.isNaN(az)) {
        rec.satellites.push({ prn, el, az, snr: Number.isNaN(snr) ? null : snr, talker, band: signalBand });
      }
    }
  } else if (!['GSA', 'VTG', 'GLL', 'GST'].includes(type)) {
    parserState.unsupported += 1;
  }

  return rec;
}

function flushRecords(final = false) {
  const payload = { type: 'records', records, parserState, final };
  postMessage(payload);
  records = [];
}

onmessage = (event) => {
  const data = event.data;
  if (data.type === 'start') {
    buffer = '';
    lineNo = 0;
    records = [];
    parserState = { total: 0, malformed: 0, checksumFailed: 0, unsupported: 0 };
    return;
  }

  if (data.type === 'chunk') {
    buffer += data.text;
    const lines = buffer.split('\n');
    buffer = lines.pop() ?? '';

    for (const line of lines) {
      lineNo += 1;
      const rec = parseLine(line);
      if (rec) records.push(rec);
      if (records.length >= 1500) flushRecords(false);
    }

    if (data.final) {
      if (buffer.trim()) {
        lineNo += 1;
        const rec = parseLine(buffer);
        if (rec) records.push(rec);
      }
      flushRecords(true);
    }
  }
};
