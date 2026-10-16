import type { Transport } from "../core/http";

/** @internal */
export abstract class Resource {
  /** @internal */
  protected readonly _t: Transport;

  /** @internal */
  constructor(transport: Transport) {
    this._t = transport;
  }
}
