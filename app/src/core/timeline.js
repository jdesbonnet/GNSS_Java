function sentenceGroup(type) {
  if (['GGA', 'RMC', 'GSV', 'GSA', 'VTG'].includes(type)) return type;
  return 'OTHER';
}

export class TimelineStore {
  constructor() {
    this.records = [];
    this.positions = [];
    this.epochs = [];
    this.events = [];
  }

  appendRecords(records, config) {
    for (const rec of records) {
      this.records.push(rec);
      rec.group = sentenceGroup(rec.type || '');
      if (rec.kind === 'position' && rec.lat != null && rec.lon != null && rec.time != null && rec.source === config.timeSource) {
        this.positions.push({ time: rec.time, lat: rec.lat, lon: rec.lon, alt: rec.alt, source: rec.source, lineNo: rec.lineNo });
      }
      if (rec.kind === 'raw' && rec.error) {
        this.events.push({ time: rec.time ?? null, label: rec.error, lineNo: rec.lineNo });
      }
    }
    this.positions.sort((a, b) => a.time - b.time);
    this.buildEpochs();
  }

  buildEpochs() {
    const satelliteByTime = new Map();
    for (const rec of this.records) {
      if (rec.kind !== 'satellites') continue;
      const nearestTime = this.positions.findLast((p) => p.lineNo <= rec.lineNo)?.time ?? null;
      if (nearestTime == null) continue;
      if (!satelliteByTime.has(nearestTime)) satelliteByTime.set(nearestTime, []);
      satelliteByTime.get(nearestTime).push(...rec.satellites);
    }

    this.epochs = this.positions.map((p) => ({
      time: p.time,
      position: p,
      satellites: satelliteByTime.get(p.time) || [],
      lines: this.records.filter((r) => Math.abs((r.time ?? p.time) - p.time) <= 1).slice(-60)
    }));
  }

  getDuration() {
    if (!this.positions.length) return 0;
    return this.positions[this.positions.length - 1].time - this.positions[0].time;
  }

  getSummary() {
    const snrs = this.epochs.flatMap((e) => e.satellites.map((s) => s.snr).filter((s) => s != null));
    const fixes = this.records.filter((r) => r.type === 'GGA').length;
    const malformed = this.records.filter((r) => r.error === 'malformed').length;
    return {
      epochs: this.epochs.length,
      durationSeconds: this.getDuration(),
      fixes,
      malformed,
      snrMin: snrs.length ? Math.min(...snrs) : null,
      snrMax: snrs.length ? Math.max(...snrs) : null,
      snrAvg: snrs.length ? snrs.reduce((a, b) => a + b, 0) / snrs.length : null
    };
  }

  findEpochByTime(t) {
    if (!this.epochs.length) return null;
    let best = this.epochs[0];
    let bestDiff = Math.abs(best.time - t);
    for (const ep of this.epochs) {
      const d = Math.abs(ep.time - t);
      if (d < bestDiff) {
        best = ep;
        bestDiff = d;
      }
    }
    return best;
  }

  getJumpEvents() {
    return this.events.slice(-200);
  }
}
