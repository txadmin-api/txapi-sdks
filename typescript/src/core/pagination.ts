/**
 * Lazily fetches page after page. Iterate it for the individual items, or call
 * {@link Paginator.pages} for whole pages.
 *
 * ```ts
 * for await (const player of client.players.searchAll({ query: "john" })) {
 *   console.log(player.displayName);
 * }
 * ```
 */
export class Paginator<Item, Page> implements AsyncIterable<Item> {
  readonly #fetchPage: (cursor: Record<string, unknown> | undefined) => Promise<Page>;
  readonly #items: (page: Page) => Item[];
  readonly #next: (page: Page, items: Item[]) => Record<string, unknown> | null;
  readonly #signal: AbortSignal | undefined;

  /** @internal */
  constructor(
    fetchPage: (cursor: Record<string, unknown> | undefined) => Promise<Page>,
    items: (page: Page) => Item[],
    next: (page: Page, items: Item[]) => Record<string, unknown> | null,
    signal?: AbortSignal,
  ) {
    this.#fetchPage = fetchPage;
    this.#items = items;
    this.#next = next;
    this.#signal = signal;
  }

  /** Iterates whole pages (one request per page). */
  async *pages(): AsyncGenerator<Page, void, undefined> {
    let cursor: Record<string, unknown> | undefined;
    let previous: string | undefined;
    for (;;) {
      if (this.#signal?.aborted) {
        throw this.#signal.reason ?? new DOMException("The operation was aborted.", "AbortError");
      }
      const page = await this.#fetchPage(cursor);
      yield page;
      const items = this.#items(page);
      if (items.length === 0) return;
      const next = this.#next(page, items);
      if (!next) return;
      const key = JSON.stringify(next);
      // A cursor that does not move would loop forever — stop instead.
      if (key === previous) return;
      previous = key;
      cursor = next;
    }
  }

  async *[Symbol.asyncIterator](): AsyncGenerator<Item, void, undefined> {
    for await (const page of this.pages()) {
      for (const item of this.#items(page)) yield item;
    }
  }

  /** Collects items into an array, stopping after `limit` items (default: all). */
  async toArray(limit: number = Number.POSITIVE_INFINITY): Promise<Item[]> {
    const out: Item[] = [];
    if (limit <= 0) return out;
    for await (const item of this) {
      out.push(item);
      if (out.length >= limit) break;
    }
    return out;
  }
}

/** Sort keys accepted by `players.search` and how they map onto row fields. @internal */
export const PLAYER_SORT_ALIASES: Record<string, string> = {
  playTime: "playTime",
  tsJoined: "tsJoined",
  tsLastConnection: "tsLastConnection",
  sessions: "tsJoined",
  lastConnection: "tsLastConnection",
};

/** Next-page cursor of a player list (SPEC §7). @internal */
export function nextPlayerCursor(
  page: { hasReachedEnd?: boolean },
  rows: Array<Record<string, unknown>>,
  sortBy: string | undefined,
): Record<string, unknown> | null {
  if (page.hasReachedEnd) return null;
  const last = rows[rows.length - 1];
  if (!last) return null;
  const key = PLAYER_SORT_ALIASES[sortBy ?? "playTime"] ?? sortBy ?? "playTime";
  const offset = last[key];
  const cursor = last.license;
  if (offset === undefined || offset === null || cursor === undefined || cursor === null) return null;
  return { offset, cursor };
}

/** Next-page cursor of a history list (SPEC §7). @internal */
export function nextHistoryCursor(
  page: { hasReachedEnd?: boolean },
  rows: Array<Record<string, unknown>>,
): Record<string, unknown> | null {
  if (page.hasReachedEnd) return null;
  const last = rows[rows.length - 1];
  if (!last) return null;
  const offset = last.timestamp;
  const cursor = last.id;
  if (offset === undefined || offset === null || cursor === undefined || cursor === null) return null;
  return { offset, cursor };
}

/** Next-page cursor of an event list (SPEC §7). @internal */
export function nextEventCursor(page: { nextCursor?: string | null }): Record<string, unknown> | null {
  return page.nextCursor ? { cursor: page.nextCursor } : null;
}
