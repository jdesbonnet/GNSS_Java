<script>
  import { onDestroy, onMount } from 'svelte';
  import { loadSettings, saveSettings } from './core/settings.js';
  import { PlaybackController } from './core/playbackController.js';
  import { createNmeaParserWorker } from './core/nmeaService.js';
  import { TimelineStore } from './core/timeline.js';
  import { exportPositionsCSV, exportTimelineJSON } from './core/exporters.js';
  import { MapPanel } from './panels/MapPanel.js';
  import { SkyPlotPanel } from './panels/SkyPlotPanel.js';
  import { SignalBarPanel } from './panels/SignalBarPanel.js';
  import { TextLogPanel } from './panels/TextLogPanel.js';

  let settings = loadSettings();
  let parserState = { total: 0, malformed: 0, checksumFailed: 0, unsupported: 0 };
  let summary = {};
  let loading = false;

  let mapContainer;
  let skyContainer;
  let signalContainer;
  let logContainer;

  let mapPanel;
  let skyPanel;
  let signalPanel;
  let textPanel;

  let timeline = new TimelineStore();
  const parser = createNmeaParserWorker();

  let minT = 0;
  let maxT = 0;
  let currentT = 0;
  let rangeStart = 0;
  let rangeEnd = 0;
  let epoch = null;
  let playback;

  function updateSettings() {
    saveSettings(settings);
    if (mapPanel && mapContainer) mapPanel.setEngine(mapContainer, settings.mapEngine);
    refreshPanels();
  }

  function refreshPanels() {
    if (!timeline.epochs.length) return;
    epoch = timeline.findEpochByTime(currentT);
    if (!epoch) return;

    const visibleTrack = timeline.positions.filter((p) => p.time >= rangeStart && p.time <= rangeEnd);
    mapPanel.update({ track: visibleTrack, cursor: epoch.position });
    skyPanel.update({ satellites: epoch.satellites });
    signalPanel.update({ satellites: epoch.satellites });
    textPanel.update({ lines: epoch.lines, sentenceFilter: settings.sentenceFilter, constellations: settings.constellations });
  }

  function togglePlay() {
    if (!playback) return;
    if (playback.playing) playback.pause();
    else playback.play();
  }

  function jumpEvent(lineNo) {
    const rec = timeline.records.find((r) => r.lineNo === Number(lineNo));
    if (!rec) return;
    const t = rec.time ?? timeline.positions.find((p) => p.lineNo >= rec.lineNo)?.time;
    if (t != null) {
      currentT = t;
      refreshPanels();
    }
  }

  async function ingest(file) {
    loading = true;
    timeline = new TimelineStore();
    await parser.parseFile(file, (records, state, final) => {
      parserState = state;
      timeline.appendRecords(records, { timeSource: settings.timeSource });
      if (final) {
        summary = timeline.getSummary();
        if (timeline.positions.length) {
          minT = timeline.positions[0].time;
          maxT = timeline.positions[timeline.positions.length - 1].time;
          currentT = minT;
          rangeStart = minT;
          rangeEnd = maxT;
          refreshPanels();
        }
      }
    });
    loading = false;
  }

  function onDrop(ev) {
    ev.preventDefault();
    const file = ev.dataTransfer?.files?.[0];
    if (file) ingest(file);
  }

  function onInputFile(ev) {
    const file = ev.target.files?.[0];
    if (file) ingest(file);
  }

  onMount(() => {
    mapPanel = new MapPanel({ container: mapContainer, engine: settings.mapEngine });
    skyPanel = new SkyPlotPanel({ container: skyContainer });
    signalPanel = new SignalBarPanel({ container: signalContainer });
    textPanel = new TextLogPanel({ container: logContainer });

    playback = new PlaybackController({
      onTick: (dt) => {
        currentT = Math.min(rangeEnd, currentT + dt);
        refreshPanels();
        if (currentT >= rangeEnd) playback.pause();
      }
    });
    playback.setSpeed(settings.speed);
  });

  onDestroy(() => {
    playback?.pause();
    parser.dispose();
    mapPanel?.destroy();
    skyPanel?.destroy();
  });
</script>

<div class="app-shell">
  <div class="drag-zone" role="button" tabindex="0" on:dragover|preventDefault on:drop={onDrop}>
    Drop NMEA log file here or choose one:
    <input type="file" accept=".nmea,.txt,.log" on:change={onInputFile} />
  </div>

  <div class="toolbar">
    <div class="control-group">
      <label>Map Engine</label>
      <select aria-label="Map Engine" bind:value={settings.mapEngine} on:change={updateSettings}>
        <option value="leaflet">Leaflet</option>
        <option value="cesium">Cesium</option>
      </select>
    </div>

    <div class="control-group">
      <label>Primary Time Source</label>
      <select aria-label="Primary Time Source" bind:value={settings.timeSource} on:change={updateSettings}>
        <option value="GGA">GGA (default)</option>
        <option value="RMC">RMC</option>
      </select>
    </div>

    <div class="control-group">
      <label>Sync Mode</label>
      <select aria-label="Sync Mode" bind:value={settings.syncMode} on:change={updateSettings}>
        <option value="epoch">Epoch-by-epoch</option>
        <option value="sentence">Sentence-by-sentence</option>
      </select>
    </div>

    <div class="control-group">
      <label>Playback Speed</label>
      <select aria-label="Playback Speed" bind:value={settings.speed} on:change={() => { playback?.setSpeed(settings.speed); updateSettings(); }}>
        <option value="1">1x</option>
        <option value="2">2x</option>
        <option value="4">4x</option>
        <option value="8">8x</option>
        <option value="16">16x</option>
      </select>
    </div>

    <div class="control-group">
      <label>Controls</label>
      <div>
        <button on:click={togglePlay}>{playback?.playing ? 'Pause' : 'Play'}</button>
        <button on:click={() => { currentT = rangeStart; refreshPanels(); }}>Reset</button>
        <button on:click={() => exportTimelineJSON(timeline.epochs)}>Export JSON</button>
        <button on:click={() => exportPositionsCSV(timeline.positions)}>Export CSV</button>
      </div>
    </div>

    <div class="control-group">
      <label>Constellation Filter</label>
      <div class="small">
        {#each Object.keys(settings.constellations) as c}
          <label><input type="checkbox" bind:checked={settings.constellations[c]} on:change={updateSettings} /> {c}</label>
        {/each}
      </div>
    </div>

    <div class="control-group">
      <label>Sentence Filter</label>
      <div class="small">
        {#each Object.keys(settings.sentenceFilter) as t}
          <label><input type="checkbox" bind:checked={settings.sentenceFilter[t]} on:change={updateSettings} /> {t}</label>
        {/each}
      </div>
    </div>

    <div class="control-group">
      <label>Parser Status</label>
      <ul class="status-list">
        <li>Total: {parserState.total}</li>
        <li>Malformed: {parserState.malformed}</li>
        <li>Checksum Failed: {parserState.checksumFailed}</li>
        <li>Unsupported: {parserState.unsupported}</li>
      </ul>
    </div>

    <div class="control-group">
      <label>Summary</label>
      <ul class="status-list">
        <li>Epochs: {summary.epochs || 0}</li>
        <li>Duration(s): {Math.round(summary.durationSeconds || 0)}</li>
        <li>SNR avg: {summary.snrAvg ? summary.snrAvg.toFixed(1) : '-'}</li>
        <li>SNR min/max: {summary.snrMin ?? '-'} / {summary.snrMax ?? '-'}</li>
      </ul>
    </div>
  </div>

  <div class="timeline">
    <label>Time slider: {currentT.toFixed(1)} s</label>
    <input aria-label="Time slider" type="range" min={minT} max={maxT} step="0.1" bind:value={currentT} on:input={refreshPanels} disabled={!timeline.epochs.length} />

    <label>Range zoom ({rangeStart.toFixed(1)} to {rangeEnd.toFixed(1)})</label>
    <input aria-label="Range start" type="range" min={minT} max={maxT} step="1" bind:value={rangeStart} on:input={refreshPanels} disabled={!timeline.epochs.length} />
    <input aria-label="Range end" type="range" min={minT} max={maxT} step="1" bind:value={rangeEnd} on:input={refreshPanels} disabled={!timeline.epochs.length} />

    <label>Jump to parse event</label>
    <select aria-label="Jump to parse event" on:change={(e) => jumpEvent(e.target.value)}>
      <option value="">Select event</option>
      {#each timeline.getJumpEvents() as ev}
        <option value={ev.lineNo}>Line {ev.lineNo}: {ev.label}</option>
      {/each}
    </select>
    {#if loading}<div class="small">Parsing file...</div>{/if}
  </div>

  <div class="grid">
    <section class="panel"><h3>Map</h3><div class="panel-body" bind:this={mapContainer}></div></section>
    <section class="panel"><h3>Sky Plot</h3><div class="panel-body" bind:this={skyContainer}></div></section>
    <section class="panel"><h3>Signal Bars (PRN/Band)</h3><div class="panel-body" bind:this={signalContainer}></div></section>
    <section class="panel"><h3>Current GNSS Text Log</h3><div class="panel-body" bind:this={logContainer}></div></section>
  </div>
</div>
