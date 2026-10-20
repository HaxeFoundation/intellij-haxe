package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.client.DisplayFailures;
import com.intellij.plugins.haxe.display.client.HaxeDisplayClient;
import com.intellij.plugins.haxe.display.client.HaxeVersionProbe;
import com.intellij.plugins.haxe.display.client.ObservedChannel;
import com.intellij.plugins.haxe.display.protocol.*;
import com.intellij.plugins.haxe.display.protocol.InitializeResult.SemVer;
import com.intellij.plugins.haxe.display.protocol.server.*;
import com.intellij.plugins.haxe.display.transport.DisplayChannel;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.DisplayResponse;
import java.util.ArrayList;
import java.util.List;

/// Haxe 3's display interface behind [HaxeDisplayClient]. A request is the
/// build's arguments plus `--display <file>@<byte offset>[@<mode>]`; the
/// answer is XML, JSON or plain text by mode, with no envelope, and a failure
/// is the compiler's error text with the error marker. Haxe 3 has no method
/// list: [#METHODS] is what it answers, and the server introspection,
/// completion-item resolve, metadata and `typer/compiledTypes` requests throw.
///
/// Haxe 3 counts a request's offset and its results' columns in UTF-8 bytes;
/// the client converts them from and to characters ([Haxe3SourceText]). An
/// unsaved buffer travels with the request (`-D display-stdin`) and replaces
/// only the request's own file. The server reads that file anew on every
/// request and any other file whose modification time changed, so
/// [#invalidate] sends nothing.
public final class Haxe3DisplayClient implements HaxeDisplayClient {

  /** The methods a haxe 3 server answers. */
  public static final List<String> METHODS = List.of(
    DisplayMethods.DIAGNOSTICS,
    DisplayMethods.HOVER,
    DisplayMethods.GOTO_DEFINITION,
    DisplayMethods.FIND_REFERENCES,
    DisplayMethods.COMPLETION);

  private static final String DISPLAY_FLAG = "--display";
  private static final List<String> DISPLAY_STDIN_DEFINE = List.of("-D", "display-stdin");
  // the server reads everything after a request's first \x01 as the display file's text; the
  // transport ends this last argument with a line break too, which lies past every offset
  private static final char STDIN_MARK = '\u0001';
  /** The error a haxe 3 server answers when there is nothing to display at the position. */
  private static final String NOTHING_AT_POSITION = "No completion point was found";

  private static final String TYPE_MODE = "type";
  private static final String POSITION_MODE = "position";
  private static final String USAGE_MODE = "usage";
  private static final String TOPLEVEL_MODE = "toplevel";
  private static final String DIAGNOSTICS_MODE = "diagnostics";
  private static final String MEMORY_MODE = "memory";
  private static final String FIELD_MODE = "";

  private final ObservedChannel channel;
  private final int readTimeoutMs;

  public Haxe3DisplayClient(DisplayChannel channel) {
    this(channel, DEFAULT_READ_TIMEOUT_MS);
  }

  public Haxe3DisplayClient(DisplayChannel channel, int readTimeoutMs) {
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

  /** The version {@code -version} reports and {@link #METHODS}. */
  @Override
  public InitializeResult initialize(List<String> baseArgs) throws DisplayRequestException {
    SemVer version = channel.observed(DisplayMethods.INITIALIZE, () -> HaxeVersionProbe.versionOf(channel.channel(), readTimeoutMs));
    if (version == null) throw new DisplayRequestException("The server's -version answer names no version");
    return new InitializeResult(null, version, METHODS);
  }

  @Override
  public List<FileDiagnostics> diagnostics(List<String> baseArgs, String file, String contents)
    throws DisplayRequestException {
    List<String> args = displayArgs(baseArgs, file, 0, DIAGNOSTICS_MODE, contents);
    String payload = display(DisplayMethods.DIAGNOSTICS, args);
    if (payload == null) return List.of();
    return Haxe3Answers.diagnostics(payload, Haxe3SourceTexts.forRequest(baseArgs, file, contents));
  }

  @Override
  public List<FileDiagnostics> projectDiagnostics(List<String> baseArgs) throws DisplayRequestException {
    return projectDiagnostics(baseArgs, readTimeoutMs);
  }

  /** Every file the compile types, the std's included, from the files on disk. */
  @Override
  public List<FileDiagnostics> projectDiagnostics(List<String> baseArgs, int timeoutMs) throws DisplayRequestException {
    List<String> args = new ArrayList<>(baseArgs);
    args.add(DISPLAY_FLAG);
    args.add(DIAGNOSTICS_MODE);
    String payload = display(DisplayMethods.DIAGNOSTICS, args, timeoutMs);
    if (payload == null) return List.of();
    return Haxe3Answers.diagnostics(payload, Haxe3SourceTexts.forProject(baseArgs));
  }

  @Override
  public HoverInfo hover(List<String> baseArgs, String file, int offset, String contents) throws DisplayRequestException {
    Haxe3SourceText text = Haxe3SourceTexts.forRequest(baseArgs, file, contents).requestText();
    int byteOffset = text.byteOffset(Haxe3Offsets.insideIdentifier(text.text(), offset));
    List<String> args = displayArgs(baseArgs, file, byteOffset, TYPE_MODE, contents);
    String payload = display(DisplayMethods.HOVER, args);
    return payload == null ? null : Haxe3Answers.hover(payload, text);
  }

  @Override
  public List<Location> definition(List<String> baseArgs, String file, int offset, String contents)
    throws DisplayRequestException {
    return locations(DisplayMethods.GOTO_DEFINITION, POSITION_MODE, baseArgs, file, offset, contents);
  }

  /** The direct usages of the declaration at the offset, whatever the kind: haxe 3's {@code @usage} lists no others. */
  // TODO: implementers and overrides for the non-direct kinds from @statistics on the declaring file
  @Override
  public List<Location> references(List<String> baseArgs, String file, int offset, String contents,
                                   FindReferencesKind kind) throws DisplayRequestException {
    return locations(DisplayMethods.FIND_REFERENCES, USAGE_MODE, baseArgs, file, offset, contents);
  }

  @Override
  public CompletionList completion(List<String> baseArgs, String file, int offset, String contents, boolean autoTriggered)
    throws DisplayRequestException {
    Haxe3SourceText text = Haxe3SourceTexts.forRequest(baseArgs, file, contents).requestText();
    Haxe3Completion.Query query = Haxe3Completion.Query.at(text.text(), offset);
    String mode = query.fieldAccess() ? FIELD_MODE : TOPLEVEL_MODE;
    List<String> args = displayArgs(baseArgs, file, text.byteOffset(query.requestOffset()), mode, contents);
    String payload = display(DisplayMethods.COMPLETION, args);
    if (payload == null) return Haxe3Completion.NOTHING;
    return query.fieldAccess() ? Haxe3Completion.fieldList(payload, query) : Haxe3Completion.toplevelList(payload, query);
  }

  @Override
  public CompletionItem resolveCompletionItem(List<String> baseArgs, int index) throws DisplayRequestException {
    throw unsupported(DisplayMethods.COMPLETION_ITEM_RESOLVE);
  }

  // TODO: the compiler's metadata from the --help-metas text
  @Override
  public List<MetadataEntry> metadata(List<String> baseArgs) throws DisplayRequestException {
    throw unsupported(DisplayMethods.METADATA);
  }

  @Override
  public List<HaxeServerContext> contexts(List<String> baseArgs) throws DisplayRequestException {
    throw unsupported(DisplayMethods.SERVER_CONTEXTS);
  }

  @Override
  public ServerMemory serverMemory(List<String> baseArgs) throws DisplayRequestException {
    throw unsupported(DisplayMethods.SERVER_MEMORY);
  }

  /**
   * Asks for the server's cache report. Haxe 3 has no {@code server/memory}:
   * it answers {@code --display memory} by printing the report to its own
   * standard output, so nothing comes back to the requester.
   */
  public void requestMemoryReport() throws DisplayRequestException {
    List<String> args = List.of(DISPLAY_FLAG, MEMORY_MODE);
    DisplayResponse response = channel.observed(DisplayMethods.SERVER_MEMORY, () -> channel.channel().request(args, readTimeoutMs));
    if (!response.hasError()) return;
    String detail = DisplayFailures.failureDetail(response);
    throw new DisplayRequestException("Display request '%s' failed: %s".formatted(DisplayMethods.SERVER_MEMORY, detail), true);
  }

  @Override
  public List<String> modules(List<String> baseArgs, String signature) throws DisplayRequestException {
    throw unsupported(DisplayMethods.SERVER_MODULES);
  }

  @Override
  public ModuleInfo module(List<String> baseArgs, String signature, String modulePath) throws DisplayRequestException {
    throw unsupported(DisplayMethods.SERVER_MODULE);
  }

  @Override
  public TypeBlueprint typeBlueprint(List<String> baseArgs, String signature, String modulePath, String typeName)
    throws DisplayRequestException {
    throw unsupported(DisplayMethods.SERVER_TYPE);
  }

  @Override
  public List<TypeBlueprint> compiledTypes(List<String> baseArgs) throws DisplayRequestException {
    throw unsupported(DisplayMethods.COMPILED_TYPES);
  }

  @Override
  public List<TypeBlueprint> compiledTypes(List<String> baseArgs, int timeoutMs) throws DisplayRequestException {
    throw unsupported(DisplayMethods.COMPILED_TYPES);
  }

  @Override
  public void invalidate(List<String> baseArgs, String file) {
  }

  // --- plumbing ---

  /** The locations a {@code @position} or {@code @usage} request answers for the identifier at the offset. */
  private List<Location> locations(String method, String mode, List<String> baseArgs, String file, int offset, String contents)
    throws DisplayRequestException {
    Haxe3SourceTexts texts = Haxe3SourceTexts.forRequest(baseArgs, file, contents);
    Haxe3SourceText text = texts.requestText();
    int byteOffset = text.byteOffset(Haxe3Offsets.insideIdentifier(text.text(), offset));
    List<String> args = displayArgs(baseArgs, file, byteOffset, mode, contents);
    String payload = display(method, args);
    return payload == null ? List.of() : Haxe3Answers.locations(payload, texts);
  }

  /** The request's arguments; an unsaved buffer goes last, after the {@code \x01} that starts it. */
  private static List<String> displayArgs(List<String> baseArgs, String file, int byteOffset, String mode, String contents) {
    List<String> args = new ArrayList<>(baseArgs);
    if (contents != null) args.addAll(DISPLAY_STDIN_DEFINE);
    args.add(DISPLAY_FLAG);
    args.add(file + "@" + byteOffset + (mode.isEmpty() ? "" : "@" + mode));
    if (contents != null) args.add(STDIN_MARK + contents);
    return args;
  }

  /** The answer's payload within the client's read timeout; null when the server found nothing at the position. */
  private String display(String method, List<String> args) throws DisplayRequestException {
    return display(method, args, readTimeoutMs);
  }

  private String display(String method, List<String> args, int timeoutMs) throws DisplayRequestException {
    return channel.observed(method, () -> payloadOf(method, channel.channel().request(args, timeoutMs)));
  }

  private static String payloadOf(String method, DisplayResponse response) throws DisplayRequestException {
    if (!response.hasError()) return DisplayFailures.requirePayload(method, response);
    if (response.payload().contains(NOTHING_AT_POSITION)) return null;
    String message = "Display request '" + method + "' failed: " + DisplayFailures.failureDetail(response);
    throw new DisplayRequestException(message, true);
  }

  private static DisplayRequestException unsupported(String method) {
    return new DisplayRequestException("Haxe 3 has no '" + method + "' request");
  }
}
