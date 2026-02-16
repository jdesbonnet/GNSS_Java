export class SkyPlotPanel {
  constructor({ container }) {
    this.container = container;
    this.canvas = document.createElement('canvas');
    this.canvas.width = 420;
    this.canvas.height = 420;
    this.container.appendChild(this.canvas);
    this.ctx = this.canvas.getContext('2d');
  }

  update({ satellites }) {
    const ctx = this.ctx;
    const { width, height } = this.canvas;
    const cx = width / 2;
    const cy = height / 2;
    const r = Math.min(cx, cy) - 20;

    ctx.clearRect(0, 0, width, height);
    ctx.strokeStyle = '#9ca3af';
    [1, 2 / 3, 1 / 3].forEach((m) => {
      ctx.beginPath();
      ctx.arc(cx, cy, r * m, 0, Math.PI * 2);
      ctx.stroke();
    });

    for (const sat of satellites || []) {
      const theta = (sat.az || 0) * Math.PI / 180;
      const rr = r * (90 - (sat.el || 0)) / 90;
      const x = cx + rr * Math.sin(theta);
      const y = cy - rr * Math.cos(theta);
      ctx.fillStyle = sat.snr > 35 ? '#10b981' : '#f59e0b';
      ctx.beginPath();
      ctx.arc(x, y, 5, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = '#111827';
      ctx.font = '11px sans-serif';
      ctx.fillText(`${sat.talker}${sat.prn}/${sat.band}`, x + 6, y);
    }
  }

  destroy() {
    this.canvas.remove();
  }
}
