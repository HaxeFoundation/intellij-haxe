package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.protocol.*;
import com.intellij.plugins.haxe.display.protocol.server.*;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.MalformedPayloadException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.MissingNode;

/**
 * Encodes display requests as JSON-RPC and decodes the responses into the
 * protocol DTOs, binding each answer to records ({@link ProtocolJson}): the
 * DTOs themselves where they match the wire, else the wire records of
 * {@link DisplayWire} and {@link CompletionWire}. A response nests its data
 * twice: the JSON-RPC {@code result} holds a std {@code Response<T>}, and
 * that object's own {@code result} holds the data. Data the records cannot
 * bind is a {@link MalformedPayloadException}.
 */
public final class DisplayJson {

  private static final TypeReference<List<FileDiagnostics>> FILE_DIAGNOSTICS = new TypeReference<>() {
  };
  private static final TypeReference<List<Location>> LOCATIONS = new TypeReference<>() {
  };
  private static final TypeReference<List<DisplayWire.Context>> CONTEXTS = new TypeReference<>() {
  };
  private static final TypeReference<List<MetadataEntry>> METADATA = new TypeReference<>() {
  };
  private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
  };
  private static final TypeReference<List<DisplayWire.ModuleType>> MODULE_TYPES = new TypeReference<>() {
  };

  private DisplayJson() {
  }

  public static String encodeRequest(String method, Map<String, Object> params) {
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("jsonrpc", "2.0");
    envelope.put("id", 1);
    envelope.put("method", method);
    envelope.put("params", params);
    return ProtocolJson.MAPPER.writeValueAsString(envelope);
  }

  /**
   * The data inside a response envelope. Throws {@link MalformedPayloadException}
   * when the payload is not JSON, and a plain {@link DisplayRequestException}
   * when the envelope carries a JSON-RPC error.
   */
  public static JsonNode unwrap(String payload) throws DisplayRequestException {
    DisplayWire.Envelope envelope = bind(parse(payload), DisplayWire.Envelope.class);
    if (envelope == null) return MissingNode.getInstance();
    DisplayWire.EnvelopeError error = envelope.error();
    if (error != null) {
      String errorMessage = error.message().isEmpty() ? "unknown error" : error.message();
      int errorCode = error.code() != null ? error.code() : -1;
      // a compiler error lists its diagnostics; the first one says what went wrong
      String detail = error.data().isEmpty() ? "" : error.data().getFirst().message();
      String suffix = detail.isEmpty() ? "" : ": " + abbreviate(detail);
      throw new DisplayRequestException("Display request failed: " + errorMessage + " (code " + errorCode + ")" + suffix, true);
    }
    JsonNode data = envelope.result() == null ? null : envelope.result().result();
    return data != null ? data : MissingNode.getInstance();
  }

  /** The JSON of a payload without an envelope, as haxe 3's JSON display modes answer. */
  public static JsonNode parse(String payload) throws MalformedPayloadException {
    try {
      return ProtocolJson.MAPPER.readTree(payload);
    } catch (RuntimeException e) {
      throw new MalformedPayloadException("Malformed display response: " + abbreviate(payload), e);
    }
  }

  private static String abbreviate(String text) {
    return text.length() > 200 ? text.substring(0, 200) + "..." : text;
  }

  public static InitializeResult decodeInitialize(JsonNode data) throws MalformedPayloadException {
    return bind(data, InitializeResult.class);
  }

  public static List<FileDiagnostics> decodeDiagnostics(JsonNode data) throws MalformedPayloadException {
    return bindList(data, FILE_DIAGNOSTICS);
  }

  public static List<Location> decodeLocations(JsonNode data) throws MalformedPayloadException {
    return bindList(data, LOCATIONS);
  }

  /** Null when there is nothing under the cursor. */
  public static HoverInfo decodeHover(JsonNode data) throws MalformedPayloadException {
    DisplayWire.Hover hover = bind(data, DisplayWire.Hover.class);
    return hover == null ? null : hover.toHoverInfo();
  }

  /** Null when the compiler has nothing to complete at the position. */
  public static CompletionList decodeCompletion(JsonNode data) throws MalformedPayloadException {
    CompletionWire.Result result = bind(data, CompletionWire.Result.class);
    return result == null ? null : result.toList();
  }

  /** The item of a {@code display/completionItem/resolve} result: a completion item with its doc comment. */
  public static CompletionItem decodeResolvedCompletionItem(JsonNode data, int index) throws MalformedPayloadException {
    CompletionWire.Resolved resolved = bind(data, CompletionWire.Resolved.class);
    return resolved == null || resolved.item() == null ? null : resolved.item().toItem(index);
  }

  public static List<HaxeServerContext> decodeContexts(JsonNode data) throws MalformedPayloadException {
    return bindList(data, CONTEXTS).stream()
      .map(DisplayWire.Context::toContext)
      .toList();
  }

  public static ServerMemory decodeServerMemory(JsonNode data) throws MalformedPayloadException {
    DisplayWire.Memory memory = bind(data, DisplayWire.Memory.class);
    return memory == null ? new ServerMemory(0, List.of()) : memory.toServerMemory();
  }

  public static List<MetadataEntry> decodeMetadataList(JsonNode data) throws MalformedPayloadException {
    return bindList(data, METADATA);
  }

  public static List<String> decodeStringList(JsonNode data) throws MalformedPayloadException {
    return bindList(data, STRINGS);
  }

  public static ModuleInfo decodeModule(JsonNode data) throws MalformedPayloadException {
    DisplayWire.Module module = bind(data, DisplayWire.Module.class);
    return module == null ? new ModuleInfo("", List.of(), List.of()) : module.toModuleInfo();
  }

  /** The {@code typer/compiledTypes} answer: every type of the compile as a blueprint, members with positions. */
  public static List<TypeBlueprint> decodeCompiledTypes(JsonNode data) throws MalformedPayloadException {
    return bindList(data, MODULE_TYPES).stream()
      .map(type -> type.toBlueprint(type.name()))
      .toList();
  }

  public static TypeBlueprint decodeTypeBlueprint(String typeName, JsonNode data) throws MalformedPayloadException {
    DisplayWire.ModuleType type = bind(data, DisplayWire.ModuleType.class);
    if (type == null) return new TypeBlueprint(typeName, "", List.of(), List.of());
    return type.toBlueprint(typeName);
  }

  /** The data bound to a list type; empty for absent or null data. */
  private static <T> List<T> bindList(JsonNode data, TypeReference<List<T>> type) throws MalformedPayloadException {
    List<T> bound = bind(data, ProtocolJson.MAPPER.constructType(type));
    return bound != null ? bound : List.of();
  }

  private static <T> T bind(JsonNode data, Class<T> type) throws MalformedPayloadException {
    return bind(data, ProtocolJson.MAPPER.constructType(type));
  }

  /** The data bound to the type; null for absent or null data. */
  private static <T> T bind(JsonNode data, JavaType type) throws MalformedPayloadException {
    if (data == null || data.isMissingNode() || data.isNull()) return null;
    try {
      return ProtocolJson.MAPPER.convertValue(data, type);
    } catch (JacksonException | IllegalArgumentException e) {
      String message = "Display data does not bind to " + type.toCanonical() + ": " + abbreviate(data.toString());
      throw new MalformedPayloadException(message, e);
    }
  }
}
