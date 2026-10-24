#!/usr/bin/env node
/**
 * Responsive and i18n audit through the Chrome DevTools Protocol.
 *
 * Usage:
 *   node scripts/ui-audit.mjs --out <dir> [--theme light|dark]
 *     [--widths 1440,1280,1024,768] [--locales zh-CN,en-US]
 *
 * Checks every route at the requested widths in zh-CN and at 1440 in en-US by
 * default, reporting page-level horizontal overflow and offending elements.
 * Each browser session handles a small batch, which keeps long audits stable.
 */
import { execFileSync, spawn } from "node:child_process";
import { mkdirSync, rmSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { setTimeout as sleep } from "node:timers/promises";

const PORT = 9226;
const ROUTES = [
  "/",
  "/login",
  "/register",
  "/dashboard",
  "/dashboard/user",
  "/dashboard/role",
  "/dashboard/menu",
  "/dashboard/dict",
  "/dashboard/config",
  "/dashboard/dept",
  "/dashboard/post",
  "/dashboard/org-chart",
  "/dashboard/notice",
  "/dashboard/message-center",
  "/dashboard/login-log",
  "/dashboard/operate-log",
  "/dashboard/audit-log",
  "/dashboard/file-manage",
  "/dashboard/online-user",
  "/dashboard/server-monitor",
  "/dashboard/redis-monitor",
  "/dashboard/sql-monitor",
  "/dashboard/minio-monitor",
  "/dashboard/swagger",
  "/dashboard/performance",
  "/dashboard/job",
  "/dashboard/backup",
  "/dashboard/report",
  "/dashboard/usage",
  "/dashboard/prometheus-monitor",
  "/dashboard/grafana-monitor",
  "/dashboard/kibana-monitor",
  "/dashboard/alertmanager",
  "/dashboard/thanos-query",
];

const CHECK = `(() => {
  const viewport = window.innerWidth;
  const skip = ".el-scrollbar__wrap, .el-table__body-wrapper, .el-table__header-wrapper, .el-table__inner-wrapper, .sidebar, .el-menu, iframe, .el-select-dropdown, .el-popper";
  const offenders = [...document.querySelectorAll("body *")]
    .filter((element) => {
      if (element.closest(skip)) return false;
      const style = getComputedStyle(element);
      if (style.position === "fixed") return false;
      const rect = element.getBoundingClientRect();
      return rect.width > 0 && rect.right > viewport + 2;
    })
    .slice(0, 5)
    .map((element) => {
      const rect = element.getBoundingClientRect();
      return element.tagName.toLowerCase() + "." + (element.className || "").toString().split(" ").filter(Boolean).slice(0, 2).join(".") + " (" + Math.round(rect.right) + "px)";
    });
  return {
    pageOverflow: document.documentElement.scrollWidth > viewport + 1,
    bodyOverflow: document.body.scrollWidth > viewport + 1,
    offenders,
  };
})()`;

function parseArgs(argv) {
  const args = {
    out: "",
    theme: "dark",
    base: "http://localhost:5173",
    widths: null,
    locales: ["zh-CN", "en-US"],
  };
  for (let i = 0; i < argv.length; i += 1) {
    if (argv[i] === "--out") args.out = argv[++i];
    else if (argv[i] === "--theme") args.theme = argv[++i];
    else if (argv[i] === "--base") args.base = argv[++i];
    else if (argv[i] === "--widths") args.widths = argv[++i].split(",").map(Number);
    else if (argv[i] === "--locales") args.locales = argv[++i].split(",");
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
        message.error ? reject(new Error(message.error.message)) : done(message.result);
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
      }, 15000);
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

function killChrome() {
  try {
    execFileSync("pkill", ["-f", `remote-debugging-port=${PORT}`], { stdio: "ignore" });
  } catch {
    // No browser running.
  }
}

async function launch() {
  killChrome();
  await sleep(700);
  const profile = resolve(".chrome-profile-audit");
  rmSync(profile, { recursive: true, force: true });
  const chrome = spawn(
    "/snap/bin/chromium",
    [
      "--headless=new",
      "--disable-gpu",
      "--no-sandbox",
      "--hide-scrollbars",
      `--remote-debugging-port=${PORT}`,
      `--user-data-dir=${profile}`,
      "--window-size=1440,900",
      "about:blank",
    ],
    { stdio: "ignore", detached: true },
  );
  chrome.unref();
  await waitForHttp(`http://127.0.0.1:${PORT}/json/version`);
}

async function openPage() {
  const response = await fetch(`http://127.0.0.1:${PORT}/json/new?about:blank`, {
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

async function startSession(args) {
  await launch();
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
  await cdp.send("Page.navigate", { url: `${args.base}/login` });
  await sleep(2200);
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
      const inner = payload.data && payload.data.code !== undefined ? payload.data : payload;
      const token = inner && inner.data && inner.data.token;
      if (token) localStorage.setItem("token", token);
      return Boolean(token);
    })()`,
  });
  if (!login.result?.value) throw new Error("Login failed");
  await cdp.send("Runtime.evaluate", {
    expression: `localStorage.setItem("theme", ${JSON.stringify(args.theme)});`,
  });
  return cdp;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const outDir = resolve(args.out);
  mkdirSync(outDir, { recursive: true });

  const checks = [];
  for (const locale of args.locales) {
    const widths = args.widths ?? (locale === "zh-CN" ? [1440, 1280, 1024, 768] : [1440]);
    for (const route of ROUTES) {
      for (const width of widths) checks.push({ route, width, locale });
    }
  }

  const results = [];
  const partialReport = () =>
    writeFileSync(
      join(outDir, "responsive-i18n-report.json"),
      JSON.stringify(results, null, 2),
    );
  const BATCH = 12;
  for (let offset = 0; offset < checks.length; offset += BATCH) {
    const batch = checks.slice(offset, offset + BATCH);
    let cdp = await startSession(args);
    let index = 0;
    let attempts = 0;
    while (index < batch.length) {
      const check = batch[index];
      try {
        await cdp.send("Runtime.evaluate", {
          expression: `localStorage.setItem("locale", ${JSON.stringify(check.locale)});`,
        });
        await cdp.send("Emulation.setDeviceMetricsOverride", {
          width: check.width,
          height: 900,
          deviceScaleFactor: 1,
          mobile: false,
        });
        await cdp.send("Page.navigate", { url: `${args.base}${check.route}` });
        await sleep(1800);
        const evaluated = await cdp.send("Runtime.evaluate", {
          returnByValue: true,
          expression: CHECK,
        });
        const value = evaluated.result?.value ?? {
          pageOverflow: true,
          bodyOverflow: true,
          offenders: ["evaluation failed"],
        };
        results.push({
          ...check,
          path: check.route,
          ...value,
          error: evaluated.exceptionDetails?.text ?? null,
        });
        const flag = value.pageOverflow || value.bodyOverflow ? "OVERFLOW" : "ok";
        console.log(`${check.locale} ${String(check.width).padStart(4)} ${flag.padEnd(8)} ${check.route}`);
        index += 1;
        attempts = 0;
      } catch (error) {
        attempts += 1;
        killChrome();
        await sleep(700);
        if (attempts > 2) {
          results.push({
            ...check,
            path: check.route,
            pageOverflow: false,
            bodyOverflow: false,
            offenders: [],
            error: error.message,
          });
          console.error(`failed ${check.locale} ${check.width} ${check.route}: ${error.message}`);
          index += 1;
          attempts = 0;
        }
        cdp = await startSession(args);
      }
    }
    killChrome();
    await sleep(400);
    partialReport();
  }

  writeFileSync(
    join(outDir, "responsive-i18n-report.json"),
    JSON.stringify(results, null, 2),
  );

  const failures = results.filter(
    (row) => row.pageOverflow || row.bodyOverflow || row.error,
  );
  const lines = [
    "# Responsive and i18n audit",
    "",
    `Routes: ${ROUTES.length}; checks: ${results.length}; failures: ${failures.length}`,
    "",
    "| Route | Width | Locale | Page overflow | Offenders / Error |",
    "|---|---:|---|---|---|",
    ...results.map((row) => {
      const detail = row.error || (row.offenders.length ? row.offenders.join("; ") : "-");
      return `| \`${row.route}\` | ${row.width} | ${row.locale} | ${
        row.pageOverflow || row.bodyOverflow ? "yes" : "no"
      } | ${detail} |`;
    }),
  ];
  writeFileSync(join(outDir, "responsive-i18n-report.md"), `${lines.join("\n")}\n`);

  if (failures.length) {
    console.error(`${failures.length} check(s) failed`);
    process.exit(1);
  }
  console.log(`All ${results.length} checks passed.`);
}

main().catch((error) => {
  killChrome();
  console.error(error);
  process.exit(1);
});
