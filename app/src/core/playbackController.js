export class PlaybackController {
  constructor({ onTick }) {
    this.onTick = onTick;
    this.playing = false;
    this.speed = 1;
    this.last = 0;
    this.raf = null;
  }

  setSpeed(speed) {
    this.speed = Number(speed);
  }

  play() {
    if (this.playing) return;
    this.playing = true;
    this.last = performance.now();
    this.loop();
  }

  pause() {
    this.playing = false;
    if (this.raf) cancelAnimationFrame(this.raf);
  }

  loop = () => {
    if (!this.playing) return;
    const now = performance.now();
    const dtSeconds = (now - this.last) / 1000;
    this.last = now;
    this.onTick(dtSeconds * this.speed);
    this.raf = requestAnimationFrame(this.loop);
  }
}
