package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.concurrency.virtualThreads.IntelliJVirtualThreads;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Matcher;

/**
 * The debug session's content server: serves ONE local directory (the compiled
 * haxe -js output: index.html, app.js, app.js.map, assets) to the browser being
 * debugged, so pages load over {@code http://127.0.0.1:<port>/} instead of
 * {@code file://} (relative XHR and source-map fetching behave like production
 * there). Implemented on the JDK's built-in {@code com.sun.net.httpserver} —
 * deliberately dependency-free; the web debugger's supply-chain rules allow no
 * third-party server code.
 *
 * Scope and safety, in line with those rules:
 * <ul>
 *   <li>binds 127.0.0.1 ONLY, on an ephemeral port;</li>
 *   <li>GET/HEAD only; no directory listings (a directory serves its
 *       {@code index.html} or 404s);</li>
 *   <li>requests resolving outside the root (traversal) are 404, never served;</li>
 *   <li>every response is {@code no-store}: the user recompiles between
 *       stops, and a cached stale app.js would desync every breakpoint.</li>
 * </ul>
 *
 * Owned by the debug backend; {@link #close()} runs on session teardown
 * (backend.close() is invoked on every teardown path, including failed starts).
 */
public final class ContentHttpServer implements Closeable {
  private static final Map<String, String> MIME = Map.ofEntries(
    Map.entry("html", "text/html; charset=utf-8"),
    Map.entry("htm", "text/html; charset=utf-8"),
    Map.entry("js", "text/javascript; charset=utf-8"),
    Map.entry("mjs", "text/javascript; charset=utf-8"),
    Map.entry("map", "application/json; charset=utf-8"),
    Map.entry("json", "application/json; charset=utf-8"),
    Map.entry("css", "text/css; charset=utf-8"),
    Map.entry("wasm", "application/wasm"),
    Map.entry("svg", "image/svg+xml"),
    Map.entry("png", "image/png"),
    Map.entry("jpg", "image/jpeg"),
    Map.entry("jpeg", "image/jpeg"),
    Map.entry("gif", "image/gif"),
    Map.entry("ico", "image/x-icon"),
    Map.entry("txt", "text/plain; charset=utf-8"));
  private static final String FALLBACK_MIME = "application/octet-stream";

  private final Path root;
  private final HttpServer server;
  private final ExecutorService executor;
  /** Optional observer of served requests ("GET /app.js -> 200"); for tests/diagnostics. */
  private volatile Consumer<String> requestListener;

  public void setRequestListener(Consumer<String> listener) {
    requestListener = listener;
  }

  private void notifyRequest(String line) {
    Consumer<String> listener = requestListener;
    if (listener != null) {
      listener.accept(line);
    }
  }

  /**
   * Starts serving {@code root} on an ephemeral loopback port immediately.
   * The root must be an existing directory.
   */
  public ContentHttpServer(Path root) throws IOException {
    if (!Files.isDirectory(root)) {
      throw new IOException("Content root is not a directory: " + root);
    }
    // toRealPath anchors the traversal check to the true location (symlinks,
    // 8.3 names, case) - everything served must stay under THIS path
    this.root = root.toRealPath();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    // One virtual thread per exchange: an asset-heavy target (a game loading
    // atlases, audio and json) opens as many connections as the browser
    // allows, across the page, its workers and any iframes — a fixed pool
    // below that number leaves the browser waiting on the server. Reads of big files
    // pin their carrier, so the real parallelism is the carrier pool rather
    // than unlimited, but it scales with the machine instead of a constant.
    // Built through IntelliJVirtualThreads rather than Thread.ofVirtual so the
    // platform can decorate the virtual threads it hosts.
    executor = Executors.newThreadPerTaskExecutor(
      IntelliJVirtualThreads.ofVirtual()
        .name("haxe-web-content-server-", 0)
        .factory());
    server.setExecutor(executor);
    server.createContext("/", this::handle);
    server.start();
  }

  /** The bound port on 127.0.0.1. */
  public int getPort() {
    return server.getAddress().getPort();
  }

  /** The base URL to point the browser at (trailing slash included). */
  public String getBaseUrl() {
    return "http://127.0.0.1:" + getPort() + "/";
  }

  /**
   * Debug-launch support: the NEXT html response gets a one-shot
   * {@code <meta http-equiv=refresh>} injected. A browser tab's very first
   * load always races the debugger attach (the JS thread actor is only born
   * when scripts first execute, so nothing can pause that first run); the
   * injected refresh makes the page reload itself once, and the SECOND load
   * happens on the already-attached thread where entry pauses and armed
   * breakpoints work.
   */
  public void refreshFirstPage(int seconds) {
    refreshOnceSeconds.set(seconds);
  }

  private final AtomicInteger refreshOnceSeconds = new AtomicInteger(-1);

  // NOTE (probed, variants L/M/N in FirefoxAdapterLiveProbe - N re-verified
  // on a CLEAN firefox instance): neither a synthetic BOOTSTRAP page (empty
  // page + meta refresh to the app, with or without an inert script; L/M)
  // nor DEFERRING the breakpoints until the reloaded page requests its first
  // script (server-held response; N) arms load-time breakpoints. The adapter
  // only applies breakpoints to a load when they were registered BEFORE the
  // load that taught it the sources: register -> load once -> reload is the
  // single working sequence. Its cost is real (clean-verified): a worker
  // PAUSED at a breakpoint when the reload fires is never terminated and
  // lingers as an inert zombie thread - accepted, and documented in the
  // module README (Chromium is the recommended family for worker debugging).

  private byte[] maybeInjectRefresh(byte[] body) {
    // one-shot, and the pool serves requests concurrently: claim the value
    // atomically so two html responses (a page plus an iframe, say) cannot
    // both inject and reload the page twice
    int seconds = refreshOnceSeconds.getAndSet(-1);
    if (seconds < 0) {
      return body;
    }
    String html = new String(body, StandardCharsets.UTF_8);
    String tag = "<meta http-equiv=\"refresh\" content=\"" + seconds + "\">";
    String injected = html.replaceFirst("(?i)<head[^>]*>", "$0" + Matcher.quoteReplacement(tag));
    if (injected.equals(html)) {
      injected = tag + html; // headless html: prepend (browsers tolerate it)
    }
    return injected.getBytes(StandardCharsets.UTF_8);
  }

  private void handle(HttpExchange exchange) throws IOException {
    try (exchange) {
      String method = exchange.getRequestMethod();
      if (!"GET".equals(method) && !"HEAD".equals(method)) {
        exchange.getResponseHeaders().set("Allow", "GET, HEAD");
        exchange.sendResponseHeaders(405, -1);
        notifyRequest(method + " " + exchange.getRequestURI().getPath() + " -> 405");
        return;
      }
      Path file = resolveRequest(exchange.getRequestURI().getPath());
      if (file == null) {
        exchange.sendResponseHeaders(404, -1);
        notifyRequest(method + " " + exchange.getRequestURI().getPath() + " -> 404");
        return;
      }
      notifyRequest(method + " " + exchange.getRequestURI().getPath() + " -> 200 (" + file.getFileName() + ")");
      byte[] body = Files.readAllBytes(file);
      if (isHtml(file)) {
        body = maybeInjectRefresh(body);
      }
      exchange.getResponseHeaders().set("Content-Type", mimeOf(file));
      // never cache: the user recompiles between runs and stale generated JS
      // would silently desync the source map and every breakpoint with it
      exchange.getResponseHeaders().set("Cache-Control", "no-store");
      if ("HEAD".equals(method)) {
        exchange.sendResponseHeaders(200, -1);
        return;
      }
      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(body);
      }
    }
  }

  /**
   * The regular file for a request path, or null for anything that must 404:
   * escapes from the root, missing files, and directories without index.html.
   * Nothing outside the content root is ever resolvable — not via {@code ..}
   * (raw or percent-encoded), backslashes, absolute paths, unparseable names,
   * or links inside the root pointing elsewhere.
   */
  private Path resolveRequest(String rawPath) {
    // URI.getPath is already percent-decoded; a path with an embedded NUL or
    // backslash is never a legitimate request for served content
    if (rawPath.indexOf('\0') >= 0 || rawPath.indexOf('\\') >= 0) {
      return null;
    }
    String relative = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
    try {
      Path candidate = root.resolve(relative).normalize();
      if (!candidate.startsWith(root)) {
        return null; // traversal attempt (also catches absolute-path requests)
      }
      if (Files.isDirectory(candidate)) {
        candidate = candidate.resolve("index.html");
      }
      if (!Files.isRegularFile(candidate)) {
        return null;
      }
      // the TRUE location must be inside the root too: a symlink/junction in
      // the content dir must not become a portal to the rest of the disk
      // (root itself is a real path - see the constructor)
      Path real = candidate.toRealPath();
      return real.startsWith(root) ? real : null;
    } catch (InvalidPathException | IOException e) {
      // unparseable name (e.g. "C:x", "f::$DATA" on Windows) or a filesystem
      // refusal while realpathing - nothing servable either way
      return null;
    }
  }

  private static boolean isHtml(Path file) {
    String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
    return name.endsWith(".html") || name.endsWith(".htm");
  }

  private static String mimeOf(Path file) {
    String name = file.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String extension = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    return MIME.getOrDefault(extension, FALLBACK_MIME);
  }

  @Override
  public void close() {
    server.stop(0);
    executor.shutdownNow();
  }
}
