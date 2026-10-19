#!/usr/bin/env node
/**
 * Capture portal page screenshots through the Chrome DevTools Protocol.
 *
 * Usage:
 *   node scripts/ui-capture.mjs --out <dir> [--theme light|dark|system]
 *     [--widths 1440,1280,1024,768] [--base http://localhost:5173]
 *
 * The script logs in once with the local development account and then captures
 * each page at every requested viewport width.
 */
import { execFileSync, spawn } from "node:child_process";
import { mkdirSync, rmSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { setTimeout as sleep } from "node:timers/promises";

const PAGES = [
  { name: "home", path: "/", auth: false },
  { name: "login", path: "/login", auth: false },
  { name: "register", path: "/register", auth: false },
  { name: "dashboard", path: "/dashboard", auth: true },
  { name: "user", path: "/dashboard/user", auth: true },
  { name: "role", path: "/dashboard/role", auth: true },
  { name: "file", path: "/dashboard/file-manage", auth: true },
  { name: "usage", path: "/dashboard/usage", auth: true },
  { name: "report", path: "/dashboard/report", auth: true },
  { name: "message-center", path: "/dashboard/message-center", auth: true },
];

function parseArgs(argv) {
  const args = { out: "", theme: "light", widths: [1440, 1280, 1024, 768], base: "http://localhost:5173" };
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--out") args.out = argv[++i];
    else if (arg === "--theme") args.theme = argv[++i];
    else if (arg === "--widths") args.widths = argv[++i].split(",").map(Number);
    else if (arg === "--base") args.base = argv[++i];
  }
  if (!args.out) throw new Error("--out is required");
  return args;
}

class Cdp {
  constructor(ws) {
    this.ws = ws;
    this.id = 0;
    this.pending = new Map();
    ws.addEventListener("message", (event) => {
      const message = JSON.parse(event.data);
      if (message.id && this.pending.has(message.id)) {
        const { resolve: done, reject } = this.pending.get(message.id);
        this.pending.delete(message.id);
        if (message.error) reject(new Error(message.error.message));
        else done(message.result);
      }
    });
  }

  send(method, params = {}) {
    const id = ++this.id;
    return new Promise((done, reject) => {
      this.pending.set(id, { resolve: done, reject });
      this.ws.send(JSON.stringify({ id, method, params }));
      setTimeout(() => {
        if (this.pending.has(id)) {
          this.pending.delete(id);
          reject(new Error(`CDP timeout: ${method}`));
        }
      }, 30000);
    });
  }
}

async function waitForHttp(url, attempts = 60) {
  for (let i = 0; i < attempts; i += 1) {
    try {
      const response = await fetch(url);
      if (response.ok) return;
    } catch {
      // keep waiting
    }
    await sleep(500);
  }
  throw new Error(`Timed out waiting for ${url}`);
}

async function launch() {
  try {
    execFileSync("pkill", ["-f", "remote-debugging-port=9222"], { stdio: "ignore" });
  } catch {
    // No previous browser to stop.
  }
  await sleep(700);
  const profile = resolve(".chrome-profile");
  rmSync(profile, { recursive: true, force: true });
  const chrome = spawn(
    "/snap/bin/chromium",
    [
      "--headless=new",
      "--disable-gpu",
      "--no-sandbox",
      "--hide-scrollbars",
      "--remote-debugging-port=9222",
      `--user-data-dir=${profile}`,
      "--window-size=1440,900",
      "about:blank",
    ],
    { stdio: "ignore", detached: true },
  );
  chrome.unref();
  await waitForHttp("http://127.0.0.1:9222/json/version");
  return chrome;
}

async function openPage() {
  const response = await fetch("http://127.0.0.1:9222/json/new?about:blank", {
    method: "PUT",
  });
  const target = await response.json();
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((done, reject) => {
    ws.addEventListener("open", done, { once: true });
    ws.addEventListener("error", reject, { once: true });
  });
  return new Cdp(ws);
}

async function navigate(cdp, url) {
  await cdp.send("Page.navigate", { url });
  await sleep(2500);
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const outDir = resolve(args.out);
  mkdirSync(outDir, { recursive: true });

  const chrome = await launch();
  const cdp = await openPage();
  await cdp.send("Page.enable");
  await cdp.send("Runtime.enable");
  await cdp.send("Emulation.setEmulatedMedia", {
    features: [
      {
        name: "prefers-color-scheme",
        value: args.theme === "dark" ? "dark" : "light",
      },
    ],
  });
  await cdp.send("Page.addScriptToEvaluateOnNewDocument", {
    source: `try { localStorage.setItem("theme", ${JSON.stringify(args.theme)}); } catch (e) {}`,
  });

  await navigate(cdp, `${args.base}/login`);
  const login = await cdp.send("Runtime.evaluate", {
    awaitPromise: true,
    returnByValue: true,
    expression: `(async () => {
      const response = await fetch("/iboot/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: "admin", password: "admin123456" }),
      });
      const payload = await response.json();
      const inner = payload && payload.data && payload.data.code !== undefined ? payload.data : payload;
      const token = inner && inner.data && inner.data.token;
      if (!token) return { ok: false, payload };
      localStorage.setItem("token", token);
      return { ok: true };
    })()`,
  });
  const loginResult = login.result?.value;
  if (!loginResult?.ok) {
    throw new Error(`Login failed: ${JSON.stringify(loginResult)}`);
  }

  const failures = [];
  for (const page of PAGES) {
    for (const width of args.widths) {
      await cdp.send("Emulation.setDeviceMetricsOverride", {
        width,
        height: 900,
        deviceScaleFactor: 1,
        mobile: false,
      });
      try {
        await navigate(cdp, `${args.base}${page.path}`);
        await sleep(1200);
        const shot = await cdp.send("Page.captureScreenshot", {
          format: "png",
          captureBeyondViewport: false,
        });
        writeFileSync(join(outDir, `${page.name}-${width}.png`), Buffer.from(shot.data, "base64"));
        console.log(`captured ${page.name}-${width}.png`);
      } catch (error) {
        failures.push(`${page.name}-${width}: ${error.message}`);
        console.error(`failed ${page.name}-${width}: ${error.message}`);
      }
    }
  }

  try {
    chrome.kill();
  } catch {
    // ignore
  }
  try {
    execFileSync("pkill", ["-f", "remote-debugging-port=9222"], { stdio: "ignore" });
  } catch {
    // ignore
  }
  if (failures.length) {
    console.error(failures.join("\n"));
    process.exit(1);
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
