package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.protocol.*;
import com.intellij.plugins.haxe.display.protocol.server.*;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.DisplayResponse;
import java.util.List;

/// Typed requests against one haxe server. [JsonRpcDisplayClient] speaks the
/// JSON-RPC display protocol of haxe 4 and later; `Haxe3DisplayClient` speaks
/// haxe 3's `--display <file>@<offset>@<mode>` interface, which serves a
/// subset. [#initialize] lists the methods a server answers; a method it does
/// not list throws [DisplayRequestException].
///
/// `baseArgs` is the build's normal argument list. The display request is
/// appended to it, and the arguments select the cache context the server
/// answers from.
///
/// A `contents` argument sends an unsaved editor buffer for the request's
/// file. An `offset` counts characters into the text the server reads: that
/// buffer when the request sends one, the file on disk otherwise. Result
/// positions are 0-based lines and characters.
public interface HaxeDisplayClient {

  /** Notified around every request. The IDE records per-server metrics from it. */
  interface RequestObserver {
    /** The request is about to be sent; it may still wait for the channel. */
    default void beforeRequest(String method) {
    }

    /** The request was answered or gave up; {@code failure} is null for a success, else why it failed. */
    void afterRequest(String method, long millis, DisplayRequestException failure);
  }

  /** The method name a plain compiler run ({@link #exchange}) reports to the observer, which has no display method. */
  String EXCHANGE_METHOD = "compile";

  /** How long a client waits for one display answer unless it is created with its own timeout. */
  int DEFAULT_READ_TIMEOUT_MS = 30_000;

  /** A plain compiler run through the channel (a compile, a dump): the raw response. The observer counts it as {@link #EXCHANGE_METHOD}. */
  DisplayResponse exchange(List<String> args, int timeoutMs) throws DisplayRequestException;

  void setObserver(RequestObserver observer);

  // --- lifecycle / capability ---

  /** The server's haxe version and the methods it answers. */
  InitializeResult initialize(List<String> baseArgs) throws DisplayRequestException;

  // --- display ---

  /** Diagnostics for one file. A non-null {@code contents} replaces the text on disk. */
  List<FileDiagnostics> diagnostics(List<String> baseArgs, String file, String contents) throws DisplayRequestException;

  /** Diagnostics for every file the compile touches. */
  List<FileDiagnostics> projectDiagnostics(List<String> baseArgs) throws DisplayRequestException;

  /** Like {@link #projectDiagnostics(List)} with its own read timeout: type-checking a whole context on a cold server takes longer than one display answer. */
  List<FileDiagnostics> projectDiagnostics(List<String> baseArgs, int timeoutMs) throws DisplayRequestException;

  /** The type at the offset; null when there is nothing there. */
  HoverInfo hover(List<String> baseArgs, String file, int offset, String contents) throws DisplayRequestException;

  List<Location> definition(List<String> baseArgs, String file, int offset, String contents) throws DisplayRequestException;

  List<Location> references(List<String> baseArgs, String file, int offset, String contents, FindReferencesKind kind)
    throws DisplayRequestException;

  /**
   * The compiler's completion items at the offset. {@code autoTriggered}
   * tells the compiler that the user did not open the popup explicitly.
   */
  CompletionList completion(List<String> baseArgs, String file, int offset, String contents, boolean autoTriggered)
    throws DisplayRequestException;

  /**
   * The item at {@code index} in the LAST completion this server answered,
   * with its doc comment. Null when the server has no completion to resolve
   * against.
   */
  CompletionItem resolveCompletionItem(List<String> baseArgs, int index) throws DisplayRequestException;

  /** The compiler's metadata registry: the built-in metadata plus the custom metadata libraries register. */
  List<MetadataEntry> metadata(List<String> baseArgs) throws DisplayRequestException;

  // --- server introspection (requires a compile with the same args first) ---

  List<HaxeServerContext> contexts(List<String> baseArgs) throws DisplayRequestException;

  /** Cache memory per compilation context. The answer covers the whole server, so empty base args suffice. */
  ServerMemory serverMemory(List<String> baseArgs) throws DisplayRequestException;

  List<String> modules(List<String> baseArgs, String signature) throws DisplayRequestException;

  ModuleInfo module(List<String> baseArgs, String signature, String modulePath) throws DisplayRequestException;

  /** The post-macro blueprint of one type: all its members with their types. */
  TypeBlueprint typeBlueprint(List<String> baseArgs, String signature, String modulePath, String typeName)
    throws DisplayRequestException;

  /**
   * Every type of a compile with the arguments, as blueprints whose members
   * carry their positions: the whole program in one answer. It is a compile,
   * so it reads the files on disk; pass {@code --no-output} in the arguments.
   */
  List<TypeBlueprint> compiledTypes(List<String> baseArgs) throws DisplayRequestException;

  /** Like {@link #compiledTypes(List)} with its own read timeout; it is a whole compile, see {@link #projectDiagnostics(List, int)}. */
  List<TypeBlueprint> compiledTypes(List<String> baseArgs, int timeoutMs) throws DisplayRequestException;

  /** Makes the server read the file anew on its next request, whatever its modification time. */
  void invalidate(List<String> baseArgs, String file) throws DisplayRequestException;
}
