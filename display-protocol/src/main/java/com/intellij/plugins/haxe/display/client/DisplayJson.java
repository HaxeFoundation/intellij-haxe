package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.protocol.*;
import com.intellij.plugins.haxe.display.protocol.server.*;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.MalformedPayloadException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Encodes display requests as JSON-RPC and decodes the responses into the
 * protocol DTOs. A response nests its data twice: the JSON-RPC
 * {@code result} holds a std {@code Response<T>}, and that object's own
 * {@code result} holds the data.
 */
public final class DisplayJson {

  private static final ObjectMapper MAPPER = JsonMapper.builder()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build();

  private DisplayJson() {
  }

  public static String encodeRequest(String method, Map<String, Object> params) {
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("jsonrpc", "2.0");
    envelope.put("id", 1);
    envelope.put("method", method);
    envelope.put("params", params);
    return MAPPER.writeValueAsString(envelope);
  }

  /**
   * The data inside a response envelope. Throws {@link MalformedPayloadException}
   * when the payload is not JSON, and a plain {@link DisplayRequestException}
   * when the envelope carries a JSON-RPC error.
   */
  public static JsonNode unwrap(String payload) throws DisplayRequestException {
    JsonNode root;
    try {
      root = MAPPER.readTree(payload);
    } catch (RuntimeException e) {
      throw new MalformedPayloadException("Malformed display response: " + abbreviate(payload), e);
    }

    JsonNode error = root.path("error");
    if (!error.isMissingNode()) {
      String errorMessage = error.path("message").asString("unknown error");
      int errorCode = error.path("code").asInt(-1);
      throw new DisplayRequestException("Display request failed: " + errorMessage + " (code " + errorCode + ")");
    }
    return root.path("result").path("result");
  }

  private static String abbreviate(String text) {
    return text.length() > 200 ? text.substring(0, 200) + "..." : text;
  }

  // --- result decoders ---

  public static InitializeResult decodeInitialize(JsonNode data) {
    List<String> methods = new ArrayList<>();
    for (JsonNode method : data.path("methods")) {
      methods.add(method.asString(""));
    }
    return new InitializeResult(
      decodeSemVer(data.path("protocolVersion")),
      decodeSemVer(data.path("haxeVersion")),
      List.copyOf(methods));
  }

  private static InitializeResult.SemVer decodeSemVer(JsonNode node) {
    return new InitializeResult.SemVer(
      node.path("major").asInt(0),
      node.path("minor").asInt(0),
      node.path("patch").asInt(0),
      node.path("pre").asString(null),
      node.path("build").asString(null));
  }

  public static List<FileDiagnostics> decodeDiagnostics(JsonNode data) {
    List<FileDiagnostics> files = new ArrayList<>();
    for (JsonNode fileEntry : data) {
      List<Diagnostic> diagnostics = new ArrayList<>();
      for (JsonNode entry : fileEntry.path("diagnostics")) {
        diagnostics.add(decodeDiagnostic(entry));
      }
      files.add(new FileDiagnostics(fileEntry.path("file").asString(""), List.copyOf(diagnostics)));
    }
    return List.copyOf(files);
  }

  private static Diagnostic decodeDiagnostic(JsonNode entry) {
    DiagnosticKind kind = DiagnosticKind.fromCode(entry.path("kind").asInt(-1));
    DiagnosticSeverity severity = DiagnosticSeverity.fromCode(entry.path("severity").asInt(-1));
    String code = entry.path("code").isString() ? entry.path("code").asString() : null;
    return new Diagnostic(kind, Range.fromJson(entry.path("range")), severity, entry.path("args"), code);
  }

  public static List<Location> decodeLocations(JsonNode data) {
    List<Location> locations = new ArrayList<>();
    for (JsonNode entry : data) {
      locations.add(decodeLocation(entry));
    }
    return List.copyOf(locations);
  }

  private static Location decodeLocation(JsonNode node) {
    return new Location(node.path("file").asString(""), Range.fromJson(node.path("range")));
  }

  /** Null when there is nothing under the cursor. */
  public static HoverInfo decodeHover(JsonNode data) {
    if (data.isNull() || data.isMissingNode()) return null;
    JsonNode item = data.path("item");
    return new HoverInfo(Range.fromJson(data.path("range")), item.path("kind").asString(""), JsonTypeRef.of(item.path("type")));
  }

  /** Null when the compiler has nothing to complete at the position. */
  public static CompletionList decodeCompletion(JsonNode data) {
    if (data.isNull() || data.isMissingNode()) return null;
    List<CompletionItem> items = new ArrayList<>();
    int position = 0;
    for (JsonNode entry : data.path("items")) {
      // A resolve request names an item by its position in this list. The
      // item's own index key repeats that position when present.
      CompletionItem item = decodeCompletionItem(entry, position++);
      if (item != null) items.add(item);
    }
    JsonNode replaceRange = data.path("replaceRange");
    JsonNode mode = data.path("mode");
    return new CompletionList(List.copyOf(items),
                              mode.path("kind").asInt(-1),
                              expectedTypeOf(mode.path("args")),
                              replaceRange.isMissingNode() || replaceRange.isNull() ? null : Range.fromJson(replaceRange),
                              data.path("isIncomplete").asBoolean(false));
  }

  /** The mode's expected type with typedefs followed, else the raw one; null when the position expects nothing. */
  private static JsonTypeRef expectedTypeOf(JsonNode modeArgs) {
    for (JsonNode candidate : List.of(modeArgs.path("expectedTypeFollowed"), modeArgs.path("expectedType"))) {
      if (!candidate.isMissingNode() && !candidate.isNull()) return JsonTypeRef.of(candidate);
    }
    return null;
  }

  /** The item of a {@code display/completionItem/resolve} result: a completion item with its doc comment. */
  public static CompletionItem decodeResolvedCompletionItem(JsonNode data, int index) {
    if (data.isNull() || data.isMissingNode()) return null;
    return decodeCompletionItem(data.path("item"), index);
  }

  /**
   * Reads the item's name and detail from where its kind keeps them. Null for
   * kinds without a name, such as an anonymous structure or an expression.
   */
  private static CompletionItem decodeCompletionItem(JsonNode entry, int position) {
    String kind = entry.path("kind").asString("");
    JsonNode args = entry.path("args");
    JsonTypeRef type = completionItemType(entry, args);
    String doc = completionItemDoc(args);
    int index = entry.path("index").asInt(position);
    return switch (kind) {
      case "Local", "Literal", "Keyword", "Metadata", "Define", "TypeParameter" ->
        new CompletionItem(kind, args.path("name").asString(""), null, null, type, doc, index);
      case "ClassField", "EnumAbstractField", "EnumField" ->
        new CompletionItem(kind, args.path("field").path("name").asString(""), null, null, type, doc, index);
      case "Type" -> new CompletionItem(kind, args.path("path").path("typeName").asString(""),
                                        JsonTypeRef.qualifiedNameOf(args.path("path")), args.path("kind").asString(""), type, doc, index);
      case "Package" -> new CompletionItem(kind, lastSegment(args.path("path").path("pack")), dotPath(args.path("path").path("pack")),
                                           null, type, doc, index);
      case "Module" -> new CompletionItem(kind, args.path("path").path("moduleName").asString(""),
                                          dotPath(args.path("path").path("pack")), null, type, doc, index);
      default -> null;
    };
  }

  /** The item-level type. When that is absent, a local carries its type in its args and a field in its field args. */
  private static JsonTypeRef completionItemType(JsonNode entry, JsonNode args) {
    for (JsonNode candidate : List.of(entry.path("type"), args.path("type"), args.path("field").path("type"))) {
      if (!candidate.isMissingNode() && !candidate.isNull()) return JsonTypeRef.of(candidate);
    }
    return null;
  }

  /**
   * The item's doc comment, or null. A field keeps it in its field args; a
   * type, metadata or define keeps it directly in its args.
   */
  private static String completionItemDoc(JsonNode args) {
    for (JsonNode candidate : List.of(args.path("field").path("doc"), args.path("doc"))) {
      if (candidate.isString() && !candidate.asString("").isBlank()) return candidate.asString("");
    }
    return null;
  }

  private static String lastSegment(JsonNode pack) {
    String last = "";
    for (JsonNode segment : pack) last = segment.asString("");
    return last;
  }

  private static String dotPath(JsonNode pack) {
    List<String> segments = new ArrayList<>();
    for (JsonNode segment : pack) segments.add(segment.asString(""));
    return String.join(".", segments);
  }

  public static List<HaxeServerContext> decodeContexts(JsonNode data) {
    List<HaxeServerContext> contexts = new ArrayList<>();
    for (JsonNode entry : data) {
      contexts.add(decodeContext(entry));
    }
    return List.copyOf(contexts);
  }

  private static HaxeServerContext decodeContext(JsonNode entry) {
    Map<String, String> defines = new LinkedHashMap<>();
    for (JsonNode define : entry.path("defines")) {
      defines.put(define.path("key").asString(""), define.path("value").asString(""));
    }
    String description = entry.path("desc").asString("");
    String signature = entry.path("signature").asString("");
    String platform = entry.path("platform").asString("");
    return new HaxeServerContext(description, signature, platform, defines);
  }

  public static ServerMemory decodeServerMemory(JsonNode data) {
    List<ServerMemory.ContextSize> contexts = new ArrayList<>();
    for (JsonNode entry : data.path("contexts")) {
      HaxeServerContext context = decodeContext(entry.path("context"));
      contexts.add(new ServerMemory.ContextSize(context, entry.path("size").asLong(0)));
    }
    long totalCache = data.path("memory").path("totalCache").asLong(0);
    return new ServerMemory(totalCache, List.copyOf(contexts));
  }

  public static List<MetadataEntry> decodeMetadataList(JsonNode data) {
    List<MetadataEntry> entries = new ArrayList<>();
    for (JsonNode entry : data) {
      String name = entry.path("name").asString("");
      String doc = entry.path("doc").asString("");
      boolean internal = entry.path("internal").asBoolean(false);
      entries.add(new MetadataEntry(name, doc, internal));
    }
    return List.copyOf(entries);
  }

  public static List<String> decodeStringList(JsonNode data) {
    List<String> values = new ArrayList<>();
    for (JsonNode entry : data) {
      values.add(entry.asString(""));
    }
    return List.copyOf(values);
  }

  public static ModuleInfo decodeModule(JsonNode data) {
    List<String> types = new ArrayList<>();
    for (JsonNode typePath : data.path("types")) {
      types.add(JsonTypeRef.qualifiedNameOf(typePath));
    }
    return new ModuleInfo(data.path("sign").asString(""), List.copyOf(types), decodeModulePaths(data.path("dependencies")));
  }

  private static List<String> decodeModulePaths(JsonNode node) {
    List<String> paths = new ArrayList<>();
    for (JsonNode entry : node) {
      paths.add(entry.path("path").asString(""));
    }
    return List.copyOf(paths);
  }

  public static TypeBlueprint decodeTypeBlueprint(String typeName, JsonNode data) {
    JsonNode args = data.path("args");
    return new TypeBlueprint(
      typeName,
      data.path("kind").asString(""),
      decodeMembers(args.path("fields")),
      decodeMembers(args.path("statics")));
  }

  private static List<TypeBlueprint.Member> decodeMembers(JsonNode node) {
    List<TypeBlueprint.Member> members = new ArrayList<>();
    for (JsonNode field : node) {
      members.add(new TypeBlueprint.Member(
        field.path("name").asString(""),
        JsonTypeRef.of(field.path("type")),
        field.path("kind").path("kind").asString("")));
    }
    return List.copyOf(members);
  }
}
