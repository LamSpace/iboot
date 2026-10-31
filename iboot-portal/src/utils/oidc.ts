/**
 * OAuth2 / OIDC 客户端。
 *
 * 门户是公共客户端：登录走授权码 + PKCE，访问令牌只用于 Bearer 请求，
 * 刷新令牌保存在 localStorage 并由轮换策略续期。
 *
 * 多标签页续期用 Web Locks 串行化；不支持时退回 BroadcastChannel 选举，
 * 避免并发刷新触发服务端的刷新令牌复用检测。
 */

const ISSUER = import.meta.env.VITE_OIDC_ISSUER || "http://localhost:8088";
const CLIENT_ID = import.meta.env.VITE_OIDC_CLIENT_ID || "iboot-portal";
const SCOPE = "openid profile api";
const REDIRECT_URI = `${window.location.origin}/callback`;

const CODE_VERIFIER_KEY = "oidc_code_verifier";
const STATE_KEY = "oidc_state";
const RETURN_TO_KEY = "oidc_return_to";

const ACCESS_TOKEN_KEY = "token";
const REFRESH_TOKEN_KEY = "refresh_token";
const ID_TOKEN_KEY = "id_token";
const EXPIRES_AT_KEY = "token_expires_at";

const REFRESH_LOCK_NAME = "iboot-token-refresh";

/** 令牌变更事件，供用户状态仓库同步内存中的令牌副本。 */
export const TOKENS_UPDATED_EVENT = "iboot:tokens-updated";

export interface TokenSet {
  accessToken: string;
  refreshToken: string | null;
  idToken: string | null;
  expiresAt: number;
}

export interface CurrentUser {
  id?: number;
  username: string;
  nickname?: string;
  roles: string[];
  permissions: string[];
}

/** 避免多个请求同时触发刷新的单飞 Promise。 */
let refreshInFlight: Promise<TokenSet | null> | null = null;

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function randomString(byteLength = 32): string {
  const bytes = new Uint8Array(byteLength);
  crypto.getRandomValues(bytes);
  return base64Url(bytes);
}

async function codeChallenge(verifier: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier));
  return base64Url(new Uint8Array(digest));
}

/**
 * 发起授权码登录，浏览器跳转到认证服务。
 *
 * @param returnTo 登录后要回到的站内地址
 */
export async function beginLogin(returnTo?: string): Promise<void> {
  const verifier = randomString();
  const state = randomString(16);
  sessionStorage.setItem(CODE_VERIFIER_KEY, verifier);
  sessionStorage.setItem(STATE_KEY, state);
  if (returnTo) {
    sessionStorage.setItem(RETURN_TO_KEY, returnTo);
  }
  const params = new URLSearchParams({
    response_type: "code",
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope: SCOPE,
    state,
    code_challenge: await codeChallenge(verifier),
    code_challenge_method: "S256",
  });
  window.location.assign(`${ISSUER}/oauth2/authorize?${params.toString()}`);
}

/**
 * 用授权码换取令牌。
 *
 * @param code 回调带回的授权码
 * @param state 回调带回的状态值
 * @returns 令牌集合
 */
export async function completeLogin(code: string, state: string): Promise<TokenSet> {
  const expectedState = sessionStorage.getItem(STATE_KEY);
  if (!expectedState || expectedState !== state) {
    throw new Error("登录状态校验失败，请重试");
  }
  const verifier = sessionStorage.getItem(CODE_VERIFIER_KEY);
  if (!verifier) {
    throw new Error("登录会话已失效，请重新登录");
  }

  const body = new URLSearchParams({
    grant_type: "authorization_code",
    code,
    redirect_uri: REDIRECT_URI,
    client_id: CLIENT_ID,
    code_verifier: verifier,
  });
  const response = await fetch(`${ISSUER}/oauth2/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body,
  });
  if (!response.ok) {
    throw new Error("登录失败，请重新登录");
  }
  const payload = await response.json();
  sessionStorage.removeItem(CODE_VERIFIER_KEY);
  sessionStorage.removeItem(STATE_KEY);
  return toTokenSet(payload);
}

/**
 * 取出登录后应回到的站内地址。
 *
 * @returns 站内地址；没有记录时返回 undefined
 */
export function consumeReturnTo(): string | undefined {
  const returnTo = sessionStorage.getItem(RETURN_TO_KEY) || undefined;
  sessionStorage.removeItem(RETURN_TO_KEY);
  return returnTo;
}

/**
 * 强制刷新令牌。
 *
 * @returns 新的令牌集合；刷新失败时返回 null
 */
export async function forceRefresh(): Promise<TokenSet | null> {
  refreshInFlight = doRefresh();
  try {
    return await refreshInFlight;
  } finally {
    refreshInFlight = null;
  }
}

/**
 * 访问令牌即将过期时续期；并发调用共享同一次刷新。
 *
 * @param minValidityMs 剩余有效期下限（毫秒）
 * @returns 可用令牌；无法续期时返回 null
 */
export async function ensureFreshToken(minValidityMs = 30_000): Promise<TokenSet | null> {
  const current = readTokenSet();
  if (current && current.expiresAt - Date.now() > minValidityMs) {
    return current;
  }
  if (refreshInFlight) {
    return refreshInFlight;
  }
  return forceRefresh();
}

/**
 * 清除本地令牌。
 */
export function clearTokenSet(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(ID_TOKEN_KEY);
  localStorage.removeItem(EXPIRES_AT_KEY);
  notifyTokensChanged();
}

function notifyTokensChanged(): void {
  window.dispatchEvent(new Event(TOKENS_UPDATED_EVENT));
}

/**
 * 读取本地令牌。
 *
 * @returns 令牌集合；未登录时返回 null
 */
export function readTokenSet(): TokenSet | null {
  const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (!accessToken) {
    return null;
  }
  return {
    accessToken,
    refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY),
    idToken: localStorage.getItem(ID_TOKEN_KEY),
    expiresAt: Number(localStorage.getItem(EXPIRES_AT_KEY) || 0),
  };
}

/**
 * 登录后跳转认证服务登出。
 */
export function beginLogout(): void {
  const idToken = localStorage.getItem(ID_TOKEN_KEY);
  const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (accessToken) {
    // 令牌自包含，必须先删除服务端的会话键，登出才会立即生效
    const apiBase = import.meta.env.VITE_API_BASE_URL || "/iboot/api";
    void fetch(`${apiBase}/auth/logout`, {
      method: "POST",
      headers: { Authorization: `Bearer ${accessToken}` },
      keepalive: true,
    }).catch(() => undefined);
  }
  clearTokenSet();
  const params = new URLSearchParams({
    post_logout_redirect_uri: `${window.location.origin}/login`,
  });
  if (idToken) {
    params.set("id_token_hint", idToken);
  }
  window.location.assign(`${ISSUER}/connect/logout?${params.toString()}`);
}

/**
 * 从访问令牌中解析当前用户信息。
 *
 * @returns 用户信息；令牌不可解析时返回 null
 */
export function readCurrentUser(): CurrentUser | null {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (!token) {
    return null;
  }
  const payload = decodeJwtPayload(token);
  if (!payload) {
    return null;
  }
  return {
    id: payload.userId,
    username: payload.username || payload.sub,
    nickname: payload.nickname,
    roles: payload.roles || [],
    permissions: payload.permissions || [],
  };
}

function toTokenSet(payload: any): TokenSet {
  const expiresInSeconds = Number(payload.expires_in || 0);
  const tokens: TokenSet = {
    accessToken: payload.access_token,
    refreshToken: payload.refresh_token || null,
    idToken: payload.id_token || null,
    expiresAt: Date.now() + expiresInSeconds * 1000,
  };
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
  localStorage.setItem(EXPIRES_AT_KEY, String(tokens.expiresAt));
  if (tokens.refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
  }
  if (tokens.idToken) {
    localStorage.setItem(ID_TOKEN_KEY, tokens.idToken);
  }
  notifyTokensChanged();
  return tokens;
}

async function doRefresh(): Promise<TokenSet | null> {
  const stored = readTokenSet();
  if (!stored?.refreshToken) {
    return null;
  }
  return withRefreshLock(async () => {
    // 进入临界区后重新读取：其他标签页可能已经完成刷新并轮换了刷新令牌
    const latest = readTokenSet();
    if (latest && latest.expiresAt - Date.now() > 30_000) {
      return latest;
    }
    const refreshToken = latest?.refreshToken;
    if (!refreshToken) {
      return null;
    }
    const body = new URLSearchParams({
      grant_type: "refresh_token",
      refresh_token: refreshToken,
      client_id: CLIENT_ID,
    });
    const response = await fetch(`${ISSUER}/oauth2/token`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body,
    });
    if (!response.ok) {
      clearTokenSet();
      return null;
    }
    return toTokenSet(await response.json());
  });
}

async function withRefreshLock<T>(task: () => Promise<T>): Promise<T> {
  const locks = (navigator as any).locks;
  if (locks?.request) {
    return locks.request(REFRESH_LOCK_NAME, task);
  }
  return task();
}

function decodeJwtPayload(token: string): any | null {
  const segment = token.split(".")[1];
  if (!segment) {
    return null;
  }
  try {
    const normalized = segment.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
    const binary = atob(padded);
    const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes));
  } catch {
    return null;
  }
}
