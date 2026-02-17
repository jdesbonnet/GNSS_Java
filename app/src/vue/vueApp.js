import {
  loadSettings,
  saveSettings,
  PlaybackController,
  createNmeaParserWorker,
  TimelineStore,
  exportPositionsCSV,
  exportTimelineJSON,
  MapPanel,
  SkyPlotPanel,
  SignalBarPanel,
  TextLogPanel
} from '../common/gnssVisualizationLibrary.js';

export function createGnssVueApp() {
  return {
    data() {
      const settings = loadSettings();
      return {
        settings,
        parserState: { total: 0, malformed: 0, checksumFailed: 0, unsupported: 0 },
        summary: {},
        loading: false,
        timeline: new TimelineStore(),
        minT: 0,
        maxT: 0,
        currentT: 0,
        rangeStart: 0,
        rangeEnd: 0,
        parser: createNmeaParserWorker(),
        mapPanel: null,
        skyPanel: null,
        signalPanel: null,
        textPanel: null,
        playback: null
      };
    },
    mounted() {
      this.mapPanel = new MapPanel({ container: this.$refs.mapContainer, engine: this.settings.mapEngine });
      this.skyPanel = new SkyPlotPanel({ container: this.$refs.skyContainer });
      this.signalPanel = new SignalBarPanel({ container: this.$refs.signalContainer });
      this.textPanel = new TextLogPanel({ container: this.$refs.logContainer });

      this.playback = new PlaybackController({
        onTick: (dt) => {
          this.currentT = Math.min(this.rangeEnd, this.currentT + dt);
          this.refreshPanels();
          if (this.currentT >= this.rangeEnd) this.playback.pause();
        }
      });
      this.playback.setSpeed(this.settings.speed);
    },
    beforeUnmount() {
      this.playback?.pause();
      this.parser.dispose();
      this.mapPanel?.destroy();
      this.skyPanel?.destroy();
      this.signalPanel?.destroy();
      this.textPanel?.destroy();
    },
    methods: {
      updateSettings() {
        saveSettings(this.settings);
        if (this.mapPanel) this.mapPanel.setEngine(this.$refs.mapContainer, this.settings.mapEngine);
        this.refreshPanels();
      },
      refreshPanels() {
        if (!this.timeline.epochs.length) return;
        const epoch = this.timeline.findEpochByTime(this.currentT);
        if (!epoch) return;

        const visibleTrack = this.timeline.positions.filter((p) => p.time >= this.rangeStart && p.time <= this.rangeEnd);
        this.mapPanel.update({ track: visibleTrack, cursor: epoch.position });
        this.skyPanel.update({ satellites: epoch.satellites });
        this.signalPanel.update({ satellites: epoch.satellites });
        this.textPanel.update({
          lines: epoch.lines,
          sentenceFilter: this.settings.sentenceFilter,
          constellations: this.settings.constellations
        });
      },
      togglePlay() {
        if (!this.playback) return;
        if (this.playback.playing) this.playback.pause();
        else this.playback.play();
      },
      jumpEvent(lineNo) {
        const rec = this.timeline.records.find((r) => r.lineNo === Number(lineNo));
        if (!rec) return;
        const t = rec.time ?? this.timeline.positions.find((p) => p.lineNo >= rec.lineNo)?.time;
        if (t != null) {
          this.currentT = t;
          this.refreshPanels();
        }
      },
      async ingest(file) {
        this.loading = true;
        this.timeline = new TimelineStore();
        await this.parser.parseFile(file, (records, state, final) => {
          this.parserState = state;
          this.timeline.appendRecords(records, { timeSource: this.settings.timeSource });
          if (final) {
            this.summary = this.timeline.getSummary();
            if (this.timeline.positions.length) {
              this.minT = this.timeline.positions[0].time;
              this.maxT = this.timeline.positions[this.timeline.positions.length - 1].time;
              this.currentT = this.minT;
              this.rangeStart = this.minT;
              this.rangeEnd = this.maxT;
              this.refreshPanels();
            }
          }
        });
        this.loading = false;
      },
      onDrop(ev) {
        ev.preventDefault();
        const file = ev.dataTransfer?.files?.[0];
        if (file) this.ingest(file);
      },
      onInputFile(ev) {
        const file = ev.target.files?.[0];
        if (file) this.ingest(file);
      },
      exportTimeline() {
        exportTimelineJSON(this.timeline.epochs);
      },
      exportPositions() {
        exportPositionsCSV(this.timeline.positions);
      }
    },
    template: `
      <div class="app-shell">
        <h1>GNSS Log Explorer (Vue + HTML)</h1>
        <div class="drag-zone" role="button" tabindex="0" @dragover.prevent @drop="onDrop">
          Drop NMEA log file here or choose one:
          <input type="file" accept=".nmea,.txt,.log" @change="onInputFile" />
        </div>

        <div class="toolbar">
          <div class="control-group">
            <label>Map Engine</label>
            <select aria-label="Map Engine" v-model="settings.mapEngine" @change="updateSettings">
              <option value="leaflet">Leaflet</option>
              <option value="cesium">Cesium</option>
            </select>
          </div>

          <div class="control-group">
            <label>Primary Time Source</label>
            <select aria-label="Primary Time Source" v-model="settings.timeSource" @change="updateSettings">
              <option value="GGA">GGA (default)</option>
              <option value="RMC">RMC</option>
            </select>
          </div>

          <div class="control-group">
            <label>Playback Speed</label>
            <select aria-label="Playback Speed" v-model="settings.speed" @change="playback?.setSpeed(settings.speed); updateSettings();">
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
              <button @click="togglePlay">{{ playback?.playing ? 'Pause' : 'Play' }}</button>
              <button @click="currentT = rangeStart; refreshPanels();">Reset</button>
              <button @click="exportTimeline">Export JSON</button>
              <button @click="exportPositions">Export CSV</button>
            </div>
          </div>

          <div class="control-group">
            <label>Parser Status</label>
            <ul class="status-list">
              <li>Total: {{ parserState.total }}</li>
              <li>Malformed: {{ parserState.malformed }}</li>
              <li>Checksum Failed: {{ parserState.checksumFailed }}</li>
              <li>Unsupported: {{ parserState.unsupported }}</li>
            </ul>
          </div>
        </div>

        <div class="timeline">
          <label>Time slider: {{ Number(currentT || 0).toFixed(1) }} s</label>
          <input aria-label="Time slider" type="range" :min="minT" :max="maxT" step="0.1" v-model.number="currentT" @input="refreshPanels" :disabled="!timeline.epochs.length" />

          <label>Range zoom ({{ Number(rangeStart || 0).toFixed(1) }} to {{ Number(rangeEnd || 0).toFixed(1) }})</label>
          <input aria-label="Range start" type="range" :min="minT" :max="maxT" step="1" v-model.number="rangeStart" @input="refreshPanels" :disabled="!timeline.epochs.length" />
          <input aria-label="Range end" type="range" :min="minT" :max="maxT" step="1" v-model.number="rangeEnd" @input="refreshPanels" :disabled="!timeline.epochs.length" />

          <label>Jump to parse event</label>
          <select aria-label="Jump to parse event" @change="jumpEvent($event.target.value)">
            <option value="">Select event</option>
            <option v-for="ev in timeline.getJumpEvents()" :key="ev.lineNo" :value="ev.lineNo">Line {{ ev.lineNo }}: {{ ev.label }}</option>
          </select>
          <div class="small" v-if="loading">Parsing file...</div>
        </div>

        <div class="grid">
          <section class="panel"><h3>Map</h3><div class="panel-body" ref="mapContainer"></div></section>
          <section class="panel"><h3>Sky Plot</h3><div class="panel-body" ref="skyContainer"></div></section>
          <section class="panel"><h3>Signal Bars (PRN/Band)</h3><div class="panel-body" ref="signalContainer"></div></section>
          <section class="panel"><h3>Current GNSS Text Log</h3><div class="panel-body" ref="logContainer"></div></section>
        </div>
      </div>
    `
  };
}
