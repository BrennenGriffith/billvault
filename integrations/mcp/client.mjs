const loopback = host => ["127.0.0.1", "localhost", "[::1]"].includes(host);

export class BillVaultClient {
  constructor(env = process.env, fetcher = fetch) {
    this.base = new URL(env.BILLVAULT_URL || "http://127.0.0.1:8765");
    if (this.base.username || this.base.password || this.base.search || this.base.hash ||
        !["http:", "https:"].includes(this.base.protocol) ||
        (this.base.protocol !== "https:" && !loopback(this.base.hostname)))
      throw new Error("BILLVAULT_URL requires HTTPS except on loopback, with no URL credentials/query/fragment.");
    this.base.pathname = this.base.pathname.replace(/\/$/, "");
    this.mode = env.BILLVAULT_MODE || "desktop";
    if (!["desktop", "web"].includes(this.mode)) throw new Error("BILLVAULT_MODE must be desktop or web.");
    this.token = env.BILLVAULT_MCP_TOKEN;
    this.username = env.BILLVAULT_USERNAME;
    this.password = env.BILLVAULT_PASSWORD;
    if (this.mode === "desktop" &&
        (this.base.hostname !== "127.0.0.1" || !/^[A-Za-z0-9_-]{32,256}$/.test(this.token || "")))
      throw new Error("Desktop mode requires 127.0.0.1 and a 32–256 character BILLVAULT_MCP_TOKEN.");
    if (this.mode === "web" && (!this.username || !this.password))
      throw new Error("Web mode requires BILLVAULT_USERNAME and BILLVAULT_PASSWORD in the process environment.");
    this.fetcher = fetcher;
    this.cookies = new Map();
    this.csrf = null;
    this.loggedIn = false;
  }

  async raw(path, method = "GET", body, extra = {}) {
    const headers = { Accept: "application/json", ...extra };
    if (this.mode === "desktop") headers.Authorization = `Bearer ${this.token}`;
    else if (this.cookies.size) headers.Cookie = [...this.cookies].map(([k,v]) => `${k}=${v}`).join("; ");
    if (this.mode === "web" && method !== "GET" && this.csrf)
      headers[this.csrf.headerName] = this.csrf.token;
    if (body !== undefined && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
    const response = await this.fetcher(`${this.base.href.replace(/\/$/, "")}${path}`, {
      method, headers, body: body === undefined ? undefined :
        (headers["Content-Type"] === "application/json" ? JSON.stringify(body) : body),
      redirect: "error", signal: AbortSignal.timeout(20000)
    });
    for (const cookie of response.headers.getSetCookie()) {
      const pair = cookie.split(";", 1)[0], split = pair.indexOf("=");
      this.cookies.set(pair.slice(0, split), pair.slice(split + 1));
    }
    if (!response.ok) {
      if (response.status === 401 || response.status === 403) { this.loggedIn = false; this.csrf = null; }
      await response.body?.cancel();
      // Avoid reflecting response bodies, secrets, or financial data in errors.
      throw new Error(`BillVault request rejected (HTTP ${response.status}). Check connection/login; for 409 retry the same preview ID, or prepare a new preview if stale/expired.`);
    }
    if (response.status === 204) return null;
    const reader = response.body.getReader(); let total = 0; const chunks = [];
    for (;;) {
      const {done, value} = await reader.read(); if (done) break;
      total += value.byteLength;
      if (total > 2097152) { await reader.cancel(); throw new Error("BillVault response exceeds the integration limit."); }
      chunks.push(Buffer.from(value));
    }
    return JSON.parse(Buffer.concat(chunks).toString("utf8"));
  }

  async request(path, method = "GET", body) {
    if (this.mode === "web" && !this.loggedIn) {
      this.csrf = await this.raw("/api/auth/csrf");
      await this.raw("/api/auth/login", "POST", new URLSearchParams({
        username: this.username, password: this.password
      }).toString(), {"Content-Type": "application/x-www-form-urlencoded"});
      this.csrf = await this.raw("/api/auth/csrf");
      this.loggedIn = true;
    }
    return this.raw(path, method, body);
  }
}

export function expenseSummary(page) {
  return {...page, content: page.content.map(({id,name,kind,categoryId,categoryName,amount,
      currency,frequency,customInterval,customUnit,startDate,nextDueDate,endDate,status,variableAmount,version}) =>
    ({id,name,kind,categoryId,categoryName,amount,currency,frequency,customInterval,customUnit,
      startDate,nextDueDate,endDate,status,variableAmount,version}))};
}
