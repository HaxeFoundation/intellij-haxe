# browser-debugger

Debugging `haxe -js` builds in a real browser. The plugin drives the browser
through the **vscode debug adapters** — external node processes speaking the
[Debug Adapter Protocol](https://microsoft.github.io/debug-adapter-protocol/specification)
— and maps breakpoints/frames back to `.hx` through the compiler's source maps
(`-debug`). Two browser families:

| Family | Adapter | Sessions |
|---|---|---|
| **Chromium** (Chrome, Edge, ungoogled-chromium, …) | [vscode-js-debug](https://github.com/microsoft/vscode-js-debug) (`js-debug-dap`, pinned GitHub release tarball) | one DAP child session per target (page, each worker); the plugin's `JsDebugSessionMux` presents them as ONE session with workers as threads |
| **Firefox** | [vscode-firefox-debug](https://github.com/firefox-devtools/vscode-firefox-debug) (pinned Open VSX `.vsix`) | one DAP session; the tab and each worker are native DAP threads |

## Family differences

The two adapters differ in ways that shape everything downstream:

| | Chromium (js-debug) | Firefox (vscode-firefox-debug) |
|---|---|---|
| Breakpoint registration | pre-registered through CDP before any script runs — load-time breakpoints bind on the FIRST page load | applied only to a load whose sources the adapter has already seen — needs the `register → load once → reload` arming sequence (see quirks) |
| Session model | one DAP session per target (page, each worker), joined by `JsDebugSessionMux` into one IDE session | one DAP session; the tab and workers are native DAP threads |
| Worker inspection | uniform per-target sessions; object inspection identical to the page | routed through a separate in-worker devtools server with its own failure modes (see quirks) |
| Extra capabilities | `completions` (runtime completion fallback), `stepInTargets` (smart step) | neither |
| Request handling | per-session connections | per-actor FIFO queues that wedge permanently on an unanswered request |

Net effect: Chromium is the more robust target, and the only reasonable one
for worker-heavy debugging; Firefox is solid for single-threaded pages but
degrades around workers for reasons outside this plugin (documented below).

## Supply chain

- Adapters are downloaded **only on the user's explicit request** (the run
  configuration's Download link — sessions never download, and a missing
  adapter fails configuration validation) into
  `PathManager.getSystemPath()/haxe/debug-adapters/<name>/<version>/`,
  verified against **hard-coded SHA-256 pins** before unpacking
  (zip-slip/tar-slip guarded). Never from the VS Marketplace (its ToS forbids
  non-VS-product consumption): Firefox from Open VSX, js-debug from GitHub
  releases. Version bumps are reviewed code changes gated by the live probes.
- **npm is never run** — the bundles are consumed prebuilt, and the only
  thing node executes is the two pinned adapter entry points.
- Node itself is never downloaded: it is found on `PATH` (or via the run
  configuration's override) and validated (`>= 18`) at session start.
- The optional content server is the JDK's built-in `com.sun.net.httpserver`
  (loopback only, `no-store`, traversal-guarded) — no third-party server code.

## Firefox quirks (the gotchas)

All of these are pinned by live probes in `FirefoxAdapterLiveProbe` or were
diagnosed from the adapter's own debug log; the probe/source comments carry
the details.

- **Load-time breakpoints need a reload.** The Firefox thread actor is only
  born when scripts first execute, and the adapter only applies breakpoints
  to a page load when they were registered *before the load that taught it
  the sources*. `register → load once → reload` is the single working
  sequence (bootstrap pages and deferred registration were probed and do NOT
  arm — variants L/M/N). In serve mode the plugin therefore injects a
  one-shot `<meta http-equiv=refresh>` into the first page. In URL mode
  (your own server) reload the page manually once the session is up.
- **The reload can leave zombie worker threads.** A worker paused at a
  breakpoint when that reload fires cannot be terminated by the navigation
  and lingers in the thread list as an inert entry (its requests never
  answer). Clean-instance verified: exactly one zombie per paused worker.
- **Firefox's in-worker devtools server can wedge object inspection**
  (observed in Firefox build 2026-07-15). Firefox runs a separate copy of
  its devtools server *inside* each worker; that copy crashes while
  previewing objects whose getters throw — the spec-mandated restricted
  `Function.prototype.caller`/`.arguments` properties are reliable triggers
  (`ReferenceError: TrustedHTML is not defined` in the worker previewers).
  The crashed object's actor then **never answers again**: expanding such an
  object in a worker times out, while the same expansion on the main thread
  works fine. The plugin contains the damage (8s request timeout + a
  per-pause cache of unresponsive references), but the fix belongs to
  Mozilla. Workarounds: inspect plain data rather than function
  internals/prototype chains in workers, or point the run configuration's
  browser executable at a Firefox ESR that predates the regression.
- **Per-actor FIFO request queues.** A request Firefox never answers wedges
  that actor's queue forever. This is why the plugin never sends speculative
  `pause` requests to this adapter (an interrupt racing a breakpoint pause is
  never answered and used to kill whole threads).
- **The adapter's RDP port defaults to a fixed 6000.** A leftover Firefox
  from a previous session (the Windows launcher process re-parents the real
  browser out of the adapter's process tree, escaping cleanup) would then be
  picked up by the NEXT session's adapter — stale tabs, foreign workers,
  dead actors. The plugin assigns a unique port per session.
- **Ports must be OS-assigned, never picked at random.** Windows reserves
  blocks of the port space as *excluded port ranges* (Hyper-V/WinNAT;
  `netsh interface ipv4 show excludedportrange protocol=tcp`) and a bind
  inside one dies with EACCES — the adapter exits before announcing its DAP
  port, or the RDP listener fails and `launch` errors with an empty message.
  Every port (adapter `--server`, per-session RDP) comes from
  `NetUtils.findAvailableSocketPort()`; the OS never allocates from an
  excluded range. js-debug is immune — it accepts port `0` directly.
- **Breakpoint paths are matched literally** — the plugin converts the IDE's
  forward-slash VFS paths to native separators, or breakpoints silently
  never bind.
- **`evaluate` maps a `ReferenceError` result to the string
  `"not available"`** — an unqualified identifier (Haxe statics live under
  `Class.field` in the generated JS) looks like an unavailable evaluator
  rather than a plain error.

## Chromium notes

- js-debug defers the `launch` response until after `configurationDone`
  (both parent and child sessions) — the plugin sends launch fire-and-forget.
- New targets (workers, iframes) announce themselves via the
  `startDebugging` reverse request; `JsDebugSessionMux` auto-attaches them,
  replays breakpoints/exception filters, and merges their ids into the one
  session (session index in the high bits of every thread/frame/variable id).
- Breakpoint verification is merged across targets: a breakpoint in
  worker-only source verifies once *any* live target verified it.
- Known upstream limitation: `stepInTargets` returns nothing when the
  multi-call line is the LAST statement of its function (the closing brace
  has no source-map entry and js-debug bails); any following mapped
  statement restores the smart-step chooser.

## Diagnostics

`DapConsoleTracer.ENABLED` (a code constant, off by default)
mirrors every DAP request/response/timeout and incoming event into the
session console as grey `[dap]` lines — command, thread/frame/reference ids,
seq numbers and durations. Flip it in code when chasing wire-level issues;
it is deliberately not exposed in the UI.

The adapters have their own verbose logs too: js-debug via the launch config
`"trace": true` (log path announced in the console), vscode-firefox-debug via
`"log": {"fileName": ..., "fileLevel": {"default": "Debug"}}`.

## Layout

| Path | Contents |
|---|---|
| `src/main/java/.../browser/AdapterPin.java` | the version + SHA-256 pins for both adapters |
| `src/main/java/.../browser/AdapterStore.java` | download, verify, unpack (zip + tar.gz via the platform-bundled commons-compress), marker files, override dir |
| `src/main/java/.../browser/BrowserAdapterLauncher.java` | spawns the adapters on node, parses their port announcements |
| `src/main/java/.../browser/ContentHttpServer.java` | the serve-mode loopback server + the firefox first-page refresh |
| `src/main/java/.../browser/JsDebugSessionMux.java` | the Chromium multi-session → one-session multiplexer (workers as threads) |
| `src/test/java/.../browser/` | live probes against the real adapters/browsers (`FirefoxAdapterLiveProbe`, `JsDebugAdapterLiveProbe`) + unit tests |

The IDE-side wiring (run configuration, editor, runners, `BrowserDebugBackend`)
lives in the main plugin under
`src/main/java/com/intellij/plugins/haxe/runner/debugger/browser/`.

Tests are opt-in (`-PdebuggerTests=true`) and self-skip unless node and the
adapters are provisioned under `<project>/node/` (git-ignored via
`.git/info/exclude`; see the gradle test task's `web.debug.node.root`
system property). The browsers under test come from the
`WEB_DEBUG_FIREFOX_EXE` / `WEB_DEBUG_CHROMIUM_EXE` environment variables
when set (e.g. an ESR firefox or an ungoogled-chromium build), else from
the standard installation paths — a set-but-invalid path skips rather than
silently testing a different browser. Beware the gradle daemon: it keeps
the environment it was STARTED with, so export the variable before the
daemon starts (or run with `--no-daemon`, as the compat-matrix does).
