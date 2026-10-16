type AnyListener = (...args: unknown[]) => void;

/** Minimal typed event emitter (no dependency, works in every runtime). */
export class TypedEmitter<Events extends { [K in keyof Events]: unknown[] }> {
  readonly #listeners = new Map<keyof Events, Set<AnyListener>>();

  /** Adds a listener. Returns a function that removes it again. */
  on<K extends keyof Events>(event: K, listener: (...args: Events[K]) => void): () => void {
    let set = this.#listeners.get(event);
    if (!set) {
      set = new Set();
      this.#listeners.set(event, set);
    }
    set.add(listener as unknown as AnyListener);
    return () => this.off(event, listener);
  }

  /** Adds a listener that is removed after its first call. */
  once<K extends keyof Events>(event: K, listener: (...args: Events[K]) => void): () => void {
    const off = this.on(event, (...args: Events[K]) => {
      off();
      listener(...args);
    });
    return off;
  }

  /** Removes a listener. */
  off<K extends keyof Events>(event: K, listener: (...args: Events[K]) => void): void {
    this.#listeners.get(event)?.delete(listener as unknown as AnyListener);
  }

  /** @internal Calls every listener; a throwing listener never breaks the stream. */
  protected emit<K extends keyof Events>(event: K, ...args: Events[K]): void {
    const set = this.#listeners.get(event);
    if (!set) return;
    for (const listener of [...set]) {
      try {
        listener(...(args as unknown[]));
      } catch (err) {
        queueMicrotask(() => {
          throw err;
        });
      }
    }
  }
}

/** Single-consumer async queue used by the stream iterators. @internal */
export class AsyncQueue<T> implements AsyncIterableIterator<T> {
  readonly #items: T[] = [];
  readonly #waiters: Array<{ resolve: (r: IteratorResult<T>) => void; reject: (e: unknown) => void }> = [];
  #done = false;
  #error: unknown = undefined;
  readonly #onReturn: () => void;

  constructor(onReturn: () => void) {
    this.#onReturn = onReturn;
  }

  push(value: T): void {
    if (this.#done) return;
    const waiter = this.#waiters.shift();
    if (waiter) waiter.resolve({ value, done: false });
    else this.#items.push(value);
  }

  end(error?: unknown): void {
    if (this.#done) return;
    this.#done = true;
    this.#error = error;
    for (const waiter of this.#waiters.splice(0)) {
      if (error !== undefined && this.#items.length === 0) waiter.reject(error);
      else waiter.resolve({ value: undefined, done: true });
    }
  }

  next(): Promise<IteratorResult<T>> {
    if (this.#items.length > 0) {
      return Promise.resolve({ value: this.#items.shift() as T, done: false });
    }
    if (this.#done) {
      if (this.#error !== undefined) {
        const error = this.#error;
        this.#error = undefined;
        return Promise.reject(error);
      }
      return Promise.resolve({ value: undefined, done: true });
    }
    return new Promise((resolve, reject) => this.#waiters.push({ resolve, reject }));
  }

  return(): Promise<IteratorResult<T>> {
    this.#done = true;
    this.#items.length = 0;
    this.#onReturn();
    for (const waiter of this.#waiters.splice(0)) waiter.resolve({ value: undefined, done: true });
    return Promise.resolve({ value: undefined, done: true });
  }

  [Symbol.asyncIterator](): AsyncIterableIterator<T> {
    return this;
  }
}
