/**
 * Base class for imperative UI components used by the GNSS visualisation app.
 *
 * Responsibilities:
 * - lifecycle state (init/destroy)
 * - teardown-safe async/listener wiring via AbortController
 * - local event bus
 * - config management
 * - scoped element resolution
 */
export class BaseComponent {
  #config;
  #events = new EventTarget();
  #abort = null;
  #state = 'created';
  #cleanup = new Set();

  constructor(config = {}) {
    this.#config = { debug: false, name: 'Component', ...config };
  }

  /** Initializes component lifecycle. Safe to call repeatedly. */
  init() {
    if (this.#state === 'destroyed') {
      throw new Error(`[${this.#config.name}] cannot init() after destroy()`);
    }
    if (this.#state === 'initialized') return this;

    this.#abort = new AbortController();
    this.#state = 'initialized';
    this.emit('init');
    return this;
  }

  /** Returns true when init() has been called and component not yet destroyed. */
  isInitialized() {
    return this.#state === 'initialized';
  }

  /** Returns true if destroy() has been called. */
  isDestroyed() {
    return this.#state === 'destroyed';
  }

  /**
   * Destroys component lifecycle resources. Safe to call repeatedly.
   */
  destroy() {
    if (this.#state === 'destroyed') return;

    for (const off of this.#cleanup) off();
    this.#cleanup.clear();

    this.#abort?.abort();
    this.#abort = null;

    this.#state = 'destroyed';
    this.emit('destroy');
  }

  /**
   * Subscribe to internal component events.
   * @returns {() => void} unsubscribe fn
   */
  on(type, handler, options) {
    this.#events.addEventListener(type, handler, options);
    return () => this.#events.removeEventListener(type, handler, options);
  }

  /** Dispatch internal component event. */
  emit(type, detail) {
    if (this.#config.debug) {
      console.log(`[${this.#config.name}] emit:`, type, detail ?? '');
    }
    this.#events.dispatchEvent(new CustomEvent(type, { detail }));
  }

  /** AbortSignal bound to this component lifecycle (available after init()). */
  get signal() {
    return this.#abort?.signal;
  }

  /** Debug logger, enabled by config.debug. */
  log(...args) {
    if (this.#config.debug) console.log(`[${this.#config.name}]`, ...args);
  }

  /**
   * Registers event listener with automatic teardown.
   * Uses signal-based cleanup when available, with manual fallback.
   */
  listen(target, type, handler, options = {}) {
    if (!target?.addEventListener) {
      throw new Error(`[${this.#config.name}] listen target must support addEventListener`);
    }

    const initialized = this.isInitialized();
    const signal = initialized ? this.signal : null;

    if (signal) {
      target.addEventListener(type, handler, { ...options, signal });
      return () => target.removeEventListener(type, handler, options);
    }

    target.addEventListener(type, handler, options);
    const off = () => target.removeEventListener(type, handler, options);
    this.#cleanup.add(off);
    return () => {
      off();
      this.#cleanup.delete(off);
    };
  }

  /** Resolve an element from selector or element input. */
  resolveEl(elOrSelector, { optional = false, root } = {}) {
    if (!elOrSelector) {
      if (optional) return null;
      throw new Error(`[${this.#config.name}] Missing element`);
    }

    if (elOrSelector instanceof Element) return elOrSelector;

    if (typeof document === 'undefined') {
      throw new Error(`[${this.#config.name}] resolveEl requires a browser document`);
    }

    const context = root ?? document;
    const el = context.querySelector(elOrSelector);
    if (!el && !optional) {
      throw new Error(`[${this.#config.name}] Element not found: ${elOrSelector}`);
    }
    return el;
  }

  /** Returns a shallow copy of current config. */
  getConfig() {
    return { ...this.#config };
  }

  /** Read a config key. */
  get(key) {
    return this.#config[key];
  }

  /** Set one config key. Emits configchange. */
  set(key, value) {
    return this.updateConfig({ [key]: value });
  }

  /**
   * Merge config patch and emit configchange.
   * Subclasses may implement validateConfig(nextConfig).
   */
  updateConfig(patch = {}) {
    const prev = this.getConfig();
    const next = { ...this.#config, ...patch };

    if (typeof this.validateConfig === 'function') {
      this.validateConfig(next);
    }

    this.#config = next;
    const changedKeys = Object.keys(patch);
    this.emit('configchange', { prev, next: this.getConfig(), changedKeys });
    return this;
  }
}
