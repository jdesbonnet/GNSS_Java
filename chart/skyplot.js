/*
 * skyplot.js
 *
 * JavaScript module for rendering a live-updating sky plot of GNSS satellites
 * using SVG and D3.js.
 *
 * Usage:
 *   import SkyPlot from './skyplot.js';
 *   const plot = new SkyPlot({
 *     container: '#skyplot',
 *     size: 500,             // diameter in pixels
 *     historyLength: 50      // number of past positions to keep for trails
 *   });
 *
 *   // On receiving NMEA sentences (GPGSV, GLGSV, GAGSV, GNGSV):
 *   plot.update(nmeaSentencesArray);
 */

import * as d3 from 'https://cdn.jsdelivr.net/npm/d3@7/+esm';

export default class SkyPlot {
  /**
   * @param {Object} options
   * @param {string|HTMLElement} options.container - Selector or element to host the SVG
   * @param {number} options.size - Diameter of the plot in pixels
   * @param {number} [options.historyLength=100] - Number of past points to retain for trails
   */
  constructor({ container, size, historyLength = 100 }) {
    this.size = size;
    this.radius = size / 2;
    this.historyLength = historyLength;

    // Create SVG container
    this.svg = d3.select(container)
      .append('svg')
      .attr('width', size)
      .attr('height', size)
      .attr('viewBox', `0 0 ${size} ${size}`);

    const cx = this.radius;
    const cy = this.radius;

    // Horizon circle
    this.svg.append('circle')
      .attr('cx', cx)
      .attr('cy', cy)
      .attr('r', this.radius)
      .attr('fill', '#f9f9f9')
      .attr('stroke', '#ccc');

    // Elevation reference circles (60° and 30°)
    [60, 30].forEach(el => {
      const r = this.radius * (90 - el) / 90;
      this.svg.append('circle')
        .attr('cx', cx)
        .attr('cy', cy)
        .attr('r', r)
        .attr('fill', 'none')
        .attr('stroke', '#aaa')
        .attr('stroke-dasharray', '4,2');
      // Elevation label inside circle
      this.svg.append('text')
        .attr('x', cx + 5)
        .attr('y', cy - r - 5)
        .text(`${el}°`)
        .attr('font-size', '10px')
        .attr('font-family', 'sans-serif')
        .attr('fill', '#555');
    });

    // Cardinal direction labels (outside horizon)
    const offset = 15;
    this.svg.append('text') // North
      .attr('x', cx)
      .attr('y', cy - this.radius - offset)
      .text('N')
      .attr('text-anchor', 'middle')
      .attr('font-size', '12px')
      .attr('font-family', 'sans-serif')
      .attr('fill', '#333');
    this.svg.append('text') // East
      .attr('x', cx + this.radius + offset)
      .attr('y', cy + 4)
      .text('E')
      .attr('text-anchor', 'middle')
      .attr('font-size', '12px')
      .attr('font-family', 'sans-serif')
      .attr('fill', '#333');
    this.svg.append('text') // South
      .attr('x', cx)
      .attr('y', cy + this.radius + offset + 4)
      .text('S')
      .attr('text-anchor', 'middle')
      .attr('font-size', '12px')
      .attr('font-family', 'sans-serif')
      .attr('fill', '#333');
    this.svg.append('text') // West
      .attr('x', cx - this.radius - offset)
      .attr('y', cy + 4)
      .text('W')
      .attr('text-anchor', 'middle')
      .attr('font-size', '12px')
      .attr('font-family', 'sans-serif')
      .attr('fill', '#333');

    // Data structures for satellites
    this.satellites = new Map();

    // Color scale for SNR
    this.colorScale = d3.scaleLinear()
      .domain([20, 25, 30, 40, 55]) // Adjust to expected SNR range
      .range(['red','yellow','green', 'blue',"violet"])
      .clamp(true);

    // Layers for trails and satellite symbols
    this.trailGroup = this.svg.append('g');
    this.satGroup = this.svg.append('g');
  }

  /**
   * Update plot with new NMEA sentences
   * @param {string[]} sentences - Array of NMEA sentences
   */
  update(sentences) {
    sentences.forEach(sentence => {
      const data = this._parseGSV(sentence);
      if (data) data.forEach(sat => this._updateSatellite(sat));
    });
    this._render();
  }

  /**
   * Parse a GSV NMEA sentence for any GNSS constellation
   * @param {string} sentence
   * @returns {Array|null} Array of {id, constellation, az, el, snr}
   */
  _parseGSV(sentence) {
    const parts = sentence.trim().split(',');
    const type = parts[0].slice(1);
    if (!/..GSV/.test(type)) return null;

    const talkerId = parts[0].substring(1,3);
console.log("talkerId=",talkerId);
    const cMap = { GP: 'US', GL: 'RU', GA: 'EU', GB: 'CN', GN: '?' };
    //const cons = constMap[prefix] || prefix;
    const cons =  cMap[talkerId];
    const sats = [];
    for (let i = 4; i < parts.length - 4; i += 4) {
      const prn = parts[i];
      const el = parseFloat(parts[i+1]);
      const az = parseFloat(parts[i+2]);
      const snr = parseFloat(parts[i+3]);
      if (!isNaN(el) && !isNaN(az)) sats.push({ id: `${cons}${prn}`, constellation: cons, el, az, snr });
    }
    return sats;
  }

  /**
   * Update satellite's current position and trail
   * @param {Object} sat
   */
  _updateSatellite({ id, constellation, el, az, snr }) {
    const theta = az * Math.PI / 180;
    const r = this.radius * (90 - el) / 90;
    const x = this.radius + r * Math.sin(theta);
    const y = this.radius - r * Math.cos(theta);

    if (!this.satellites.has(id)) this.satellites.set(id, { trail: [] });
    const obj = this.satellites.get(id);

    obj.trail.push({ x, y });
    if (obj.trail.length > this.historyLength) obj.trail.shift();
    obj.current = { id, constellation, x, y, snr };
  }

  /**
   * Render satellite trails and symbols
   */
  _render() {
    // Trails
    const trailPaths = this.trailGroup.selectAll('path')
      .data(Array.from(this.satellites.values()), d => d.current.id);

    trailPaths.enter().append('path')
      .attr('fill', 'none')
      .attr('stroke-width', 1)
      .merge(trailPaths)
      .attr('stroke', d => this.colorScale(d.current.snr))
      .attr('d', d3.line()
        .x(d => d.x)
        .y(d => d.y)
        .curve(d3.curveBasis)
      );
    trailPaths.exit().remove();

    // Satellites
    const sats = this.satGroup.selectAll('g.sat')
      .data(Array.from(this.satellites.values()).map(d => d.current), d => d.id);

    const enter = sats.enter().append('g').attr('class', 'sat');
    enter.append('circle').attr('r', 6);
    enter.append('text')
      .attr('dy', '-0.8em')
      .attr('text-anchor', 'middle')
      .style('font-size', '10px')
      .style('font-family', 'sans-serif');

    const all = enter.merge(sats);
    all.select('circle')
      .attr('cx', d => d.x)
      .attr('cy', d => d.y)
      .attr('fill', d => this.colorScale(d.snr));
    all.select('text')
      .attr('x', d => d.x)
      .attr('y', d => d.y)
      .text(d => d.constellation + "(" + d.snr + ")");

    sats.exit().remove();
  }
}

