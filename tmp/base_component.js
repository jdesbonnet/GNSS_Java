/**
 * Base class to implement UI components. 
 */
export class BaseComponent {
	#config = {};
	#events = new EventTarget();
	#abort = null;

	#debugMode = false;
	
	constructor(config = {}) {
		this.#config = { debug: false, name: "Component", ...config };
	}

	init() {
		this.#abort = new AbortController();
		this.emit("init");
		return this;
	}

	destroy() {
		this.#abort?.abort();
		this.#abort = null;
		this.emit("destroy");
	}

	on(type, handler, options) {
		this.#events.addEventListener(type, handler, options);
		return () => this.#events.removeEventListener(type, handler, options);
	}

	emit(type, detail) {
		if (this.#debugMode === true) {
			console.log("emiting event ", type, "detail=",detail);
		}
		this.#events.dispatchEvent(new CustomEvent(type, { detail }));
	}


	
	get signal() {
		return this.#abort?.signal;
	}

	log(...args) {
		if (this.#config.debug) console.log(`[${this.config.name}]`, ...args);
	}

	resolveEl(elOrSelector, { optional = false } = {}) {
		if (!elOrSelector) return optional ? null : (() => { throw new Error("Missing element"); })();
		if (elOrSelector instanceof HTMLElement) return elOrSelector;
		const el = document.querySelector(elOrSelector);
		if (!el && !optional) throw new Error(`Element not found: ${elOrSelector}`);
		return el;
	}
	
	// -------------------------
	// Configuration: getters/setters
	// -------------------------

	/** Returns a shallow copy of current config */
	getConfig() {
		return { ...this.#config };
	}

	/** Read a specific config key */
	get(key) {
		return this.#config[key];
	}
	
	setDebug(onOrOff) {
		this.#debugMode = onOrOff;
	}
	isDebug() {
		return this.#debugMode;
	}
	
	/** Normalize angle range -180 to 180° */
	normalizeAngle (a) {
		if (a < -180) {
			return a+360;
		}
		if (a > 180) {
			return a-360;
		}
		return a;
	}
	
}
