package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.protocol.*;
import com.intellij.plugins.haxe.display.protocol.server.*;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.MalformedPayloadException;
import com.intellij.plugins.haxe.display.transport.DisplayResponse;
import com.intellij.plugins.haxe.display.transport.HaxeDisplayTransport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/// Typed requests against one `haxe --wait <port>` server. Every call is one
/// connect-request-close exchange.
///
/// `baseArgs` is the build's normal argument list. The display request is
/// appended to it, and the arguments select the cache context the server
/// answers from.
///
/// A `contents` argument sends an unsaved editor buffer. The server ignores it
/// for a module it has already cached until that file is passed to
/// [#invalidate].
///
/// An instance holds no connection, only the server address and an optional
/// observer, so creating one per request is fine.
public class HaxeDisplayClient {

  /** Notified after every request. The IDE records per-server metrics from it. */
  public interface RequestObserver {
    void afterRequest(String method, long millis, boolean success);
  }

  private static final int DEFAULT_READ_TIMEOUT_MS = 30_000;

  private final String host;
  private final int port;
  private final int readTimeoutMs;
  private RequestObserver observer;

  public HaxeDisplayClient(String host, int port) {
    this(host, port, DEFAULT_READ_TIMEOUT_MS);
  }

  public HaxeDisplayClient(String host, int port, int readTimeoutMs) {
    this.host = host;
    this.port = port;
    this.readTimeoutMs = readTimeoutMs;
  }

  public void setObserver(RequestObserver observer) {
    this.observer = observer;
  }

  // --- lifecycle / capability ---

  public InitializeResult initialize(List<String> baseArgs) throws DisplayRequestException {
    return DisplayJson.decodeInitialize(call(baseArgs, DisplayMethods.INITIALIZE, Map.of("supportsResolve", true)));
  }

  // --- display ---

  /** Diagnostics for one file. A non-null {@code contents} replaces the text on disk. */
  public List<FileDiagnostics> diagnostics(List<String> baseArgs, String file, String contents)
    throws DisplayRequestException {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("file", file);
    if (contents != null) {
      params.put("contents", contents);
    }
    return DisplayJson.decodeDiagnostics(call(baseArgs, DisplayMethods.DIAGNOSTICS, params));
  }

  /**
   * Diagnostics for every file the compile touches. Unlike the per-file
   * request, this also reports parse errors in dependencies. The per-file
   * request parses a broken dependency error-tolerantly and reports nothing
   * about it.
   */
  public List<FileDiagnostics> projectDiagnostics(List<String> baseArgs) throws DisplayRequestException {
    return DisplayJson.decodeDiagnostics(call(baseArgs, DisplayMethods.DIAGNOSTICS, Map.of()));
  }

  public HoverInfo hover(List<String> baseArgs, String file, int offset, String contents)
    throws DisplayRequestException {
    return DisplayJson.decodeHover(call(baseArgs, DisplayMethods.HOVER, positionParams(file, offset, contents)));
  }

  public List<Location> definition(List<String> baseArgs, String file, int offset, String contents)
    throws DisplayRequestException {
    return DisplayJson.decodeLocations(
      call(baseArgs, DisplayMethods.GOTO_DEFINITION, positionParams(file, offset, contents)));
  }

  public List<Location> references(List<String> baseArgs, String file, int offset, String contents,
                                   FindReferencesKind kind) throws DisplayRequestException {
    Map<String, Object> params = positionParams(file, offset, contents);
    params.put("kind", kind.wireValue());
    return DisplayJson.decodeLocations(call(baseArgs, DisplayMethods.FIND_REFERENCES, params));
  }

  /**
   * The compiler's completion items at the offset. The completion mode the
   * compiler picks for the position decides which kinds it offers: locals,
   * fields, types, packages, keywords, literals, metadata or defines.
   * {@code autoTriggered} tells the compiler that the user did not open the
   * popup explicitly.
   */
  public CompletionList completion(List<String> baseArgs, String file, int offset, String contents, boolean autoTriggered)
    throws DisplayRequestException {
    Map<String, Object> params = positionParams(file, offset, contents);
    params.put("wasAutoTriggered", autoTriggered);
    return DisplayJson.decodeCompletion(call(baseArgs, DisplayMethods.COMPLETION, params));
  }

  /**
   * The item at {@code index} in the LAST completion this server answered,
   * with its doc comment. A completion item already carries the doc when its
   * declaration has one, so this matters only for an item listed without it.
   * Null when the server has no completion to resolve against.
   */
  public CompletionItem resolveCompletionItem(List<String> baseArgs, int index) throws DisplayRequestException {
    return DisplayJson.decodeResolvedCompletionItem(call(baseArgs, DisplayMethods.COMPLETION_ITEM_RESOLVE, Map.of("index", index)), index);
  }

  /** The compiler's metadata registry: the built-in metadata plus the custom metadata libraries register. */
  public List<MetadataEntry> metadata(List<String> baseArgs) throws DisplayRequestException {
    return DisplayJson.decodeMetadataList(call(baseArgs, DisplayMethods.METADATA, Map.of(
      "compiler", true,
      "user", true)));
  }

  // --- server introspection (requires a compile with the same args first) ---

  public List<HaxeServerContext> contexts(List<String> baseArgs) throws DisplayRequestException {
    return DisplayJson.decodeContexts(call(baseArgs, DisplayMethods.SERVER_CONTEXTS, Map.of()));
  }

  /** Cache memory per compilation context. The answer covers the whole server, so empty base args suffice. */
  public ServerMemory serverMemory(List<String> baseArgs) throws DisplayRequestException {
    return DisplayJson.decodeServerMemory(call(baseArgs, DisplayMethods.SERVER_MEMORY, Map.of()));
  }

  public List<String> modules(List<String> baseArgs, String signature) throws DisplayRequestException {
    return DisplayJson.decodeStringList(
      call(baseArgs, DisplayMethods.SERVER_MODULES, Map.of("signature", signature)));
  }

  public ModuleInfo module(List<String> baseArgs, String signature, String modulePath)
    throws DisplayRequestException {
    return DisplayJson.decodeModule(call(baseArgs, DisplayMethods.SERVER_MODULE, Map.of(
      "signature", signature,
      "path", modulePath)));
  }

  /** The post-macro blueprint of one type: all its members with their types. */
  public TypeBlueprint typeBlueprint(List<String> baseArgs, String signature, String modulePath, String typeName)
    throws DisplayRequestException {
    JsonNode data = call(baseArgs, DisplayMethods.SERVER_TYPE, Map.of(
      "signature", signature,
      "modulePath", modulePath,
      "typeName", typeName));
    return DisplayJson.decodeTypeBlueprint(typeName, data);
  }

  public void invalidate(List<String> baseArgs, String file) throws DisplayRequestException {
    call(baseArgs, DisplayMethods.SERVER_INVALIDATE, Map.of("file", file));
  }

  // --- plumbing ---

  private static Map<String, Object> positionParams(String file, int offset, String contents) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("file", file);
    params.put("offset", offset);
    if (contents != null) {
      params.put("contents", contents);
    }
    return params;
  }

  /** Sends one display request and returns its result data. The observer hears of failed requests too. */
  private JsonNode call(List<String> baseArgs, String method, Map<String, Object> params)
    throws DisplayRequestException {
    long start = System.nanoTime();
    boolean success = false;
    try {
      List<String> args = new ArrayList<>(baseArgs);
      args.add("--display");
      args.add(DisplayJson.encodeRequest(method, params));
      DisplayResponse response = HaxeDisplayTransport.request(host, port, args, readTimeoutMs);
      if (response.payload().isEmpty()) {
        throw new DisplayRequestException("Display request '" + method + "' got no result: " + failureDetail(response));
      }
      JsonNode result = unwrapClassified(method, response);
      success = true;
      return result;
    }
    finally {
      if (observer != null) {
        observer.afterRequest(method, (System.nanoTime() - start) / 1_000_000, success);
      }
    }
  }

  /**
   * A failed compile answers with the compiler's error text instead of a
   * JSON envelope. An unparseable payload together with the error marker is
   * therefore a compiler failure, not a malformed response.
   */
  private static JsonNode unwrapClassified(String method, DisplayResponse response) throws DisplayRequestException {
    try {
      return DisplayJson.unwrap(response.payload());
    } catch (MalformedPayloadException e) {
      if (!response.hasError()) throw e;
      throw new DisplayRequestException("Display request '" + method + "' failed: " + failureDetail(response), e);
    }
  }

  /**
   * The compiler's explanation of a failure: the last few log lines when
   * there are any, otherwise the payload of a plain-text error report.
   */
  private static String failureDetail(DisplayResponse response) {
    List<String> lines = response.logs().stream()
      .filter(line -> !line.isBlank())
      .toList();
    if (lines.isEmpty()) {
      if (response.hasError() && !response.payload().isBlank()) {
        return "compiler error: " + response.payload();
      }
      return response.hasError() ? "compiler reported an error" : "empty response";
    }
    String tail = String.join(" | ", lines.subList(Math.max(0, lines.size() - 3), lines.size()));
    return (response.hasError() ? "compiler error: " : "") + tail;
  }
}
