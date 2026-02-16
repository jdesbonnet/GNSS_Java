import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

class LeafletAdapter {
  init(container) {
    this.map = L.map(container).setView([0, 0], 14);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);
    this.track = L.polyline([], { color: '#2563eb' }).addTo(this.map);
    this.cursor = L.circleMarker([0, 0], { radius: 7, color: '#dc2626' }).addTo(this.map);
  }
  setTrack(points) {
    const latlngs = points.map((p) => [p.lat, p.lon]);
    this.track.setLatLngs(latlngs);
    if (latlngs.length > 1) this.map.fitBounds(this.track.getBounds(), { padding: [20, 20] });
  }
  setCursor(point) {
    if (!point) return;
    this.cursor.setLatLng([point.lat, point.lon]);
  }
  destroy() { this.map?.remove(); }
}

class CesiumAdapter {
  async init(container) {
    if (!window.Cesium) {
      await new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = 'https://cesium.com/downloads/cesiumjs/releases/1.118/Build/Cesium/Cesium.js';
        script.onload = resolve;
        script.onerror = reject;
        document.head.appendChild(script);
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = 'https://cesium.com/downloads/cesiumjs/releases/1.118/Build/Cesium/Widgets/widgets.css';
        document.head.appendChild(link);
      });
    }
    this.viewer = new window.Cesium.Viewer(container, { timeline: false, animation: false });
    this.trackEntity = null;
    this.cursorEntity = this.viewer.entities.add({ position: window.Cesium.Cartesian3.fromDegrees(0, 0), point: { pixelSize: 10, color: window.Cesium.Color.RED } });
  }
  setTrack(points) {
    if (!this.viewer || points.length < 2) return;
    if (this.trackEntity) this.viewer.entities.remove(this.trackEntity);
    const cart = points.map((p) => window.Cesium.Cartesian3.fromDegrees(p.lon, p.lat));
    this.trackEntity = this.viewer.entities.add({ polyline: { positions: cart, width: 3, material: window.Cesium.Color.BLUE } });
    this.viewer.zoomTo(this.trackEntity);
  }
  setCursor(point) {
    if (!this.cursorEntity || !point) return;
    this.cursorEntity.position = window.Cesium.Cartesian3.fromDegrees(point.lon, point.lat);
  }
  destroy() { this.viewer?.destroy(); }
}

export class MapPanel {
  constructor({ container, engine = 'leaflet' }) {
    this.engineName = engine;
    this.adapter = engine === 'cesium' ? new CesiumAdapter() : new LeafletAdapter();
    this.adapter.init(container);
  }

  setEngine(container, engine) {
    if (engine === this.engineName) return;
    this.adapter.destroy();
    this.engineName = engine;
    this.adapter = engine === 'cesium' ? new CesiumAdapter() : new LeafletAdapter();
    this.adapter.init(container);
  }

  update({ track, cursor }) {
    this.adapter.setTrack(track || []);
    this.adapter.setCursor(cursor || null);
  }

  destroy() { this.adapter.destroy(); }
}
