package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.protocol.*;
import com.intellij.plugins.haxe.display.protocol.server.*;
import com.intellij.plugins.haxe.display.transport.DisplayChannel;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.MalformedPayloadException;
import com.intellij.plugins.haxe.display.transport.DisplayResponse;
import com.intellij.plugins.haxe.display.transport.HaxeDisplayTransport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/// The JSON-RPC display protocol of haxe 4 and later, over a
/// [DisplayChannel]: a `--wait <port>` server reached with one connection per
/// request, or a `--server-connect` process on its persistent framed
/// connection.
///
/// The server ignores a request's `contents` for a module it has already
/// cached until that file is passed to [#invalidate].
///
/// An instance holds the channel and an optional observer; over the
/// per-request channel, creating one per request is fine.
public class JsonRpcDisplayClient implements HaxeDisplayClient {

  private final ObservedChannel channel;
  private final int readTimeoutMs;

  public JsonRpcDisplayClient(String host, int port) {
    this(host, port, DEFAULT_READ_TIMEOUT_MS);
  }

  public JsonRpcDisplayClient(String host, int port, int readTimeoutMs) {
    this(HaxeDisplayTransport.socketPerRequest(host, port), readTimeoutMs);
  }

  public JsonRpcDisplayClient(DisplayChannel channel) {
    this(channel, DEFAULT_READ_TIMEOUT_MS);
  }

  public JsonRpcDisplayClient(DisplayChannel channel, int readTimeoutMs) {
    this.channel = new ObservedChannel(channel);
    this.readTimeoutMs = readTimeoutMs;
  }

  @Override
  public DisplayResponse exchange(List<String> args, int timeoutMs) throws DisplayRequestException {
    return channel.observed(EXCHANGE_METHOD, () -> channel.channel().request(args, timeoutMs));
  }

  @Override
  public void setObserver(RequestObserver observer) {
    channel.setObserver(observer);
  }

  // --- lifecycle / capability ---

  @Override
  public InitializeResult initialize(List<String> baseArgs) throws DisplayRequestException {
    return DisplayJson.decodeInitialize(call(baseArgs, DisplayMethods.INITIALIZE, Map.of("supportsResolve", true)));
  }

  // --- display ---

  @Override
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
  @Override
  public List<FileDiagnostics> projectDiagnostics(List<String> baseArgs) throws DisplayRequestException {
    return projectDiagnostics(baseArgs, readTimeoutMs);
  }

  @Override
  public List<FileDiagnostics> projectDiagnostics(List<String> baseArgs, int timeoutMs) throws DisplayRequestException {
    return DisplayJson.decodeDiagnostics(call(baseArgs, DisplayMethods.DIAGNOSTICS, Map.of(), timeoutMs));
  }

  @Override
  public HoverInfo hover(List<String> baseArgs, String file, int offset, String contents)
    throws DisplayRequestException {
    return DisplayJson.decodeHover(call(baseArgs, DisplayMethods.HOVER, positionParams(file, offset, contents)));
  }

  @Override
  public List<Location> definition(List<String> baseArgs, String file, int offset, String contents)
    throws DisplayRequestException {
    return DisplayJson.decodeLocations(
      call(baseArgs, DisplayMethods.GOTO_DEFINITION, positionParams(file, offset, contents)));
  }

  @Override
  public List<Location> references(List<String> baseArgs, String file, int offset, String contents,
                                   FindReferencesKind kind) throws DisplayRequestException {
    Map<String, Object> params = positionParams(file, offset, contents);
    params.put("kind", kind.wireValue());
    return DisplayJson.decodeLocations(call(baseArgs, DisplayMethods.FIND_REFERENCES, params));
  }

  /**
   * The completion mode the compiler picks for the position decides which
   * kinds it offers: locals, fields, types, packages, keywords, literals,
   * metadata or defines.
   */
  @Override
  public CompletionList completion(List<String> baseArgs, String file, int offset, String contents, boolean autoTriggered)
    throws DisplayRequestException {
    Map<String, Object> params = positionParams(file, offset, contents);
    params.put("wasAutoTriggered", autoTriggered);
    return DisplayJson.decodeCompletion(call(baseArgs, DisplayMethods.COMPLETION, params));
  }

  /** A completion item already carries the doc when its declaration has one, so this matters only for an item listed without it. */
  @Override
  public CompletionItem resolveCompletionItem(List<String> baseArgs, int index) throws DisplayRequestException {
    return DisplayJson.decodeResolvedCompletionItem(call(baseArgs, DisplayMethods.COMPLETION_ITEM_RESOLVE, Map.of("index", index)), index);
  }

  @Override
  public List<MetadataEntry> metadata(List<String> baseArgs) throws DisplayRequestException {
    return DisplayJson.decodeMetadataList(call(baseArgs, DisplayMethods.METADATA, Map.of(
      "compiler", true,
      "user", true)));
  }

  // --- server introspection (requires a compile with the same args first) ---

  @Override
  public List<HaxeServerContext> contexts(List<String> baseArgs) throws DisplayRequestException {
    return DisplayJson.decodeContexts(call(baseArgs, DisplayMethods.SERVER_CONTEXTS, Map.of()));
  }

  @Override
  public ServerMemory serverMemory(List<String> baseArgs) throws DisplayRequestException {
    return DisplayJson.decodeServerMemory(call(baseArgs, DisplayMethods.SERVER_MEMORY, Map.of()));
  }

  @Override
  public List<String> modules(List<String> baseArgs, String signature) throws DisplayRequestException {
    return DisplayJson.decodeStringList(
      call(baseArgs, DisplayMethods.SERVER_MODULES, Map.of("signature", signature)));
  }

  @Override
  public ModuleInfo module(List<String> baseArgs, String signature, String modulePath)
    throws DisplayRequestException {
    return DisplayJson.decodeModule(call(baseArgs, DisplayMethods.SERVER_MODULE, Map.of(
      "signature", signature,
      "path", modulePath)));
  }

  @Override
  public TypeBlueprint typeBlueprint(List<String> baseArgs, String signature, String modulePath, String typeName)
    throws DisplayRequestException {
    JsonNode data = call(baseArgs, DisplayMethods.SERVER_TYPE, Map.of(
      "signature", signature,
      "modulePath", modulePath,
      "typeName", typeName));
    return DisplayJson.decodeTypeBlueprint(typeName, data);
  }

  @Override
  public List<TypeBlueprint> compiledTypes(List<String> baseArgs) throws DisplayRequestException {
    return compiledTypes(baseArgs, readTimeoutMs);
  }

  @Override
  public List<TypeBlueprint> compiledTypes(List<String> baseArgs, int timeoutMs) throws DisplayRequestException {
    return DisplayJson.decodeCompiledTypes(call(baseArgs, DisplayMethods.COMPILED_TYPES, Map.of(), timeoutMs));
  }

  @Override
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
    return call(baseArgs, method, params, readTimeoutMs);
  }

  private JsonNode call(List<String> baseArgs, String method, Map<String, Object> params, int timeoutMs)
    throws DisplayRequestException {
    List<String> args = new ArrayList<>(baseArgs);
    args.add("--display");
    args.add(DisplayJson.encodeRequest(method, params));
    return channel.observed(method, () -> sendDisplayRequest(args, method, timeoutMs));
  }

  private JsonNode sendDisplayRequest(List<String> args, String method, int timeoutMs) throws DisplayRequestException {
    DisplayResponse response = channel.channel().request(args, timeoutMs);
    DisplayFailures.requirePayload(method, response);
    return unwrapClassified(method, response);
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
      String message = "Display request '" + method + "' failed: " + DisplayFailures.failureDetail(response);
      throw new DisplayRequestException(message, true, e);
    }
  }
}
