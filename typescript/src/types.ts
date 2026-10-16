/**
 * Response models, typed after the `responseExample`s of the API catalog.
 *
 * The API adds fields in minor versions, so every model is open: unknown fields are
 * kept as-is (the SDK never strips or rejects them) and are reachable through the
 * index signature. Fields the API omits when they have no value are optional.
 */

/** Any JSON value. Used for txAdmin passthrough endpoints whose shape txAdmin owns. */
export type JsonValue = string | number | boolean | null | JsonValue[] | { [key: string]: JsonValue };

/** A JSON object of unknown shape. */
export type JsonObject = { [key: string]: unknown };

// ── Common ──────────────────────────────────────────────────────────────────

/** Result of a single Discord server for a Discord ban/unban (API 1.8.0). */
export interface DiscordGuildResult {
  guildId: string;
  guildName?: string;
  ok: boolean;
  /** `banned`, `unbanned`, `not_banned`, `disabled`, `missing_permission`, `hierarchy`, `owner`, `unknown_user`, `invalid_user`, `not_in_guild`, `bot_unavailable`, `failed` … */
  code: string;
  message?: string;
  [key: string]: unknown;
}

/** Outcome of `discordBan` / `discordUnban`. Only present when the flag was set. */
export interface DiscordModerationResult {
  discordId?: string;
  /** Not attempted at all: `no_discord_id`, `no_linked_guild`, `not_a_ban`. */
  skipped?: string;
  message?: string;
  guilds?: DiscordGuildResult[];
  [key: string]: unknown;
}

/** Returned by every mutation. `message` is txAdmin's own confirmation text. */
export interface MutationResult {
  success: boolean;
  message?: string;
  /** Only for `players.ban` with `discordBan` and `history.revoke` with `discordUnban`. */
  discord?: DiscordModerationResult;
  [key: string]: unknown;
}

// ── Account ─────────────────────────────────────────────────────────────────

export interface AccountPlan {
  code: string;
  /** Effective features: plan ∩ key scopes. */
  features: string[];
  serverLimit?: number;
  logRetentionDays?: number;
  [key: string]: unknown;
}

export interface AccountQuota {
  unlimited: boolean;
  used: number;
  /** Absent when `unlimited`. */
  limit?: number;
  /** Absent when `unlimited`. */
  remaining?: number;
  /** ISO-8601; the 1st of the next month, 00:00 UTC. */
  resetsAt?: string;
  [key: string]: unknown;
}

export interface AccountKey {
  id: number;
  name: string;
  prefix: string;
  scopes: string[];
  allowedIps: string[];
  /** Absent for keys not bound to a server. */
  serverId?: number;
  expiresAt?: string;
  [key: string]: unknown;
}

/** `GET /api/v1/me`. */
export interface AccountInfo {
  success: boolean;
  accountId: number;
  plan: AccountPlan;
  quota: AccountQuota;
  rpmLimit?: number;
  rpmUnlimited: boolean;
  /** `apiKey` or `discordBot`. */
  auth: "apiKey" | "discordBot" | (string & {});
  /** Absent when called through the Discord bot. */
  key?: AccountKey;
  [key: string]: unknown;
}

export interface ServerSummary {
  id: number;
  name: string;
  txServerName?: string;
  active: boolean;
  /** `ONLINE`, `STOPPED`, `UNREACHABLE` — only with offline alerts enabled. */
  health?: "ONLINE" | "STOPPED" | "UNREACHABLE" | (string & {});
  healthCheckedAt?: string;
  offlineAlerts?: boolean;
  eventLog?: boolean;
  banSync?: boolean;
  [key: string]: unknown;
}

/** `GET /api/v1/servers`. */
export interface ServerList {
  success: boolean;
  count: number;
  /** Present for server-bound keys. */
  boundServerId?: number;
  servers: ServerSummary[];
  [key: string]: unknown;
}

// ── Players ─────────────────────────────────────────────────────────────────

export interface PlayerRow {
  license: string;
  displayName: string;
  /** Minutes. */
  playTime: number;
  /** Unix seconds. */
  tsJoined: number;
  /** Unix seconds. */
  tsLastConnection: number;
  notes?: string;
  isAdmin?: boolean;
  isOnline?: boolean;
  isWhitelisted?: boolean;
  [key: string]: unknown;
}

/** A page of players (max. 100 rows). */
export interface PlayerPage {
  players: PlayerRow[];
  hasReachedEnd: boolean;
  [key: string]: unknown;
}

// ── History ─────────────────────────────────────────────────────────────────

export interface HistoryRow {
  id: string;
  type: "ban" | "warn" | (string & {});
  playerName?: string;
  author?: string;
  reason?: string;
  /** Unix seconds. */
  timestamp: number;
  isRevoked?: boolean;
  /** Bans: `permanent` | `active` | `expired`. */
  banExpiration?: "permanent" | "active" | "expired" | (string & {});
  /** Warns: did the player acknowledge it. */
  warnAcked?: boolean;
  [key: string]: unknown;
}

/** A page of bans/warns (max. 100 rows). */
export interface HistoryPage {
  history: HistoryRow[];
  hasReachedEnd: boolean;
  [key: string]: unknown;
}

export interface BanTemplate {
  id: string;
  reason: string;
  /** `permanent` or `{ value, unit }`. */
  duration: "permanent" | { value: number; unit: string } | (string & {});
  [key: string]: unknown;
}

// ── Server ──────────────────────────────────────────────────────────────────

export interface ServerStatus {
  serverName?: string;
  txaVersion?: string;
  fxsVersion?: string;
  /** FXServer process running. Absent when txAdmin's diagnostics could not be read. */
  online?: boolean;
  /** The txAdmin panel answers. */
  txAdminReachable?: boolean;
  /** txAdmin's player database statistics (not the current player count). */
  playerStats?: {
    total?: number;
    playedLast24h?: number;
    joinedLast24h?: number;
    joinedLast7d?: number;
    [key: string]: unknown;
  };
  [key: string]: unknown;
}

// ── Console & server log ────────────────────────────────────────────────────

export interface ConsoleBuffer {
  /** One element per console line, with FXServer's ANSI colour codes. */
  lines: string[];
  count: number;
  [key: string]: unknown;
}

/** A structured txAdmin server-log entry. */
export interface ServerLogEntryObject {
  /** Epoch milliseconds. */
  ts?: number;
  type?: string;
  src?: { id?: string; name?: string; [key: string]: unknown };
  msg?: string;
  [key: string]: unknown;
}

/** Server-log entries are objects when txAdmin structured them, otherwise strings. */
export type ServerLogEntry = ServerLogEntryObject | string;

export interface ServerLogBuffer {
  entries: ServerLogEntry[];
  count: number;
  [key: string]: unknown;
}

export interface ServerLogHistory {
  /** txAdmin's own spelling: `true` when the end of its buffer was reached. */
  boundry?: boolean;
  log: ServerLogEntry[];
  [key: string]: unknown;
}

// ── Events ──────────────────────────────────────────────────────────────────

/** A stored event. Fields depend on `type`. */
export interface ServerEvent {
  id: number;
  /** ISO-8601. */
  ts: string;
  /** join, leave, join_denied, death, chat, command, menu, admin, ban, warn, system, other */
  type: string;
  /** serverlog, history, adminlog … */
  source: string;
  player?: string;
  playerRef?: string;
  victim?: string;
  victimRef?: string;
  killer?: string;
  killerRef?: string;
  cause?: string;
  pvp?: boolean;
  admin?: string;
  message?: string;
  kind?: string;
  loggedTime?: string;
  actionId?: string;
  reason?: string;
  expiration?: string;
  revoked?: boolean;
  session?: string;
  identifiers?: string[];
  [key: string]: unknown;
}

/** Common envelope of every event endpoint. */
export interface EventEnvelope {
  success: boolean;
  serverId: number;
  /** Whether the event log is enabled for this server. */
  eventLog: boolean;
  [key: string]: unknown;
}

/** A page of events, newest first. Pass `nextCursor` as `cursor` for the next page. */
export interface EventPage extends EventEnvelope {
  count: number;
  events: ServerEvent[];
  /** Absent on the last page. */
  nextCursor?: string;
  /** `events.chat` only: whether chat logging is currently on. */
  chatLog?: boolean;
}

export interface LeaderboardRow {
  rank: number;
  player: string;
  kills: number;
  deaths: number;
  kd: number;
  [key: string]: unknown;
}

export interface Leaderboard extends EventEnvelope {
  since: string;
  sort: string;
  players: LeaderboardRow[];
}

export interface NameCount {
  name: string;
  count: number;
  [key: string]: unknown;
}

export interface PlayerEventStats extends EventEnvelope {
  player: string;
  /** Present when the player was given as an identifier. */
  identifier?: string;
  found: boolean;
  names?: string[];
  since?: string;
  kills?: number;
  deaths?: number;
  pvpDeaths?: number;
  kd?: number;
  topVictims?: NameCount[];
  topKillers?: NameCount[];
  topWeapons?: NameCount[];
  sessions?: number;
  firstSeen?: string;
  lastSeen?: string;
  bans?: number;
  warns?: number;
  recent?: ServerEvent[];
}

export interface EventSummary extends EventEnvelope {
  since: string;
  byType: Record<string, number>;
  kills: number;
  uniquePlayers: number;
  topKiller?: LeaderboardRow;
  topCauses?: NameCount[];
  oldestEvent?: string;
  newestEvent?: string;
  storedEvents?: number;
}

export interface PlaytimeRow {
  player: string;
  identifier?: string;
  sessions: number;
  seconds: number;
  longestSeconds?: number;
  online?: boolean;
  lastSeen?: string;
  [key: string]: unknown;
}

export interface Playtime extends EventEnvelope {
  since: string;
  totalSeconds?: number;
  sessionCount?: number;
  /** Absent when filtered by `player`. */
  unclosedSessions?: number;
  players: PlaytimeRow[];
  /** Only with `player`. */
  recentSessions?: JsonObject[];
}

export interface ActivityCounts {
  joins: number;
  deaths: number;
  kills: number;
  chat: number;
  commands: number;
  bans: number;
  warns: number;
  [key: string]: unknown;
}

export interface ActivityBucket extends ActivityCounts {
  /** Bucket start, ISO-8601 with offset. */
  start: string;
  /** Unique players in this bucket. */
  players: number;
}

export interface Activity extends EventEnvelope {
  since: string;
  bucket: "hour" | "day" | (string & {});
  timeZone: string;
  totals: ActivityCounts;
  buckets: ActivityBucket[];
}

export interface ActionLogLine {
  time: string;
  admin: string;
  message: string;
  kind?: string;
  [key: string]: unknown;
}

export interface ActionLog {
  lines: ActionLogLine[];
  count: number;
  total: number;
  [key: string]: unknown;
}

// ── Whitelist ───────────────────────────────────────────────────────────────

export interface WhitelistApproval {
  identifier: string;
  playerName?: string;
  playerAvatar?: string | null;
  /** Unix seconds. */
  tsApproved?: number;
  approvedBy?: string;
  [key: string]: unknown;
}

// ── txAdmin panel admins ────────────────────────────────────────────────────

export interface AdminSummary {
  name: string;
  hasPassword?: boolean;
  hasCitizenfx?: boolean;
  hasDiscord?: boolean;
  /** `Master Account`, `All Permissions` or a count. */
  permsSummary?: string;
  isMaster?: boolean;
  /** The account the bridge itself is logged in with. */
  isSelf?: boolean;
  [key: string]: unknown;
}

export interface AdminList {
  admins: AdminSummary[];
  total: number;
  [key: string]: unknown;
}

export interface AdminDetails {
  name: string;
  /** Empty string when not set. Note txAdmin's spelling `ID`. */
  citizenfxID: string;
  discordID: string;
  permissions: string[];
  [key: string]: unknown;
}

export interface AdminPermission {
  id: string;
  label: string;
  [key: string]: unknown;
}

export interface AdminPermissionList {
  permissions: AdminPermission[];
  [key: string]: unknown;
}

/** `admins.add`: `password`/`note` only for a new password login. */
export interface AdminCreated extends MutationResult {
  /** One-time generated password — store it now, it is never shown again. */
  password?: string;
  note?: string;
  /** Whether the admin list should be reloaded. */
  refresh?: boolean;
}
