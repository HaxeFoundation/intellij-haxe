package com.intellij.plugins.haxe.display.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.intellij.plugins.haxe.display.protocol.*;
import com.intellij.plugins.haxe.display.protocol.server.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * The wire shapes of the JSON-RPC answers {@link DisplayJson} decodes, where
 * they differ from the protocol DTOs: each record binds one answer and turns
 * itself into its DTO. Completion has its own, {@link CompletionWire}.
 */
final class DisplayWire {

  private DisplayWire() {
  }

  /** A JSON-RPC response: an error, or the std {@code Response<T>} as its result. */
  record Envelope(EnvelopeError error, Response result) {
  }

  /** The std {@code Response<T>}, whose own {@code result} holds the data the method decides the shape of. */
  record Response(JsonNode result) {
  }

  /** A JSON-RPC error; {@code data} is one detail or a list of them. */
  record EnvelopeError(String message,
                       Integer code,
                       @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<ErrorDetail> data) {
  }

  /** One detail of an error: a diagnostic ({@code {message, location}}) or a plain string. */
  record ErrorDetail(String message) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    ErrorDetail {
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static ErrorDetail of(String message) {
      return new ErrorDetail(message);
    }
  }

  /** A {@code display/hover} result: the hovered range and the item there. */
  record Hover(Range range, HoverItem item) {

    HoverInfo toHoverInfo() {
      return item == null ? new HoverInfo(range, "", null) : new HoverInfo(range, item.kind(), item.type());
    }
  }

  record HoverItem(String kind, JsonTypeRef type) {
  }

  /** A {@code server/contexts} entry; its defines are a list of key-value pairs. */
  record Context(String desc, String signature, String platform, List<Define> defines) {

    HaxeServerContext toContext() {
      Map<String, String> defineValues = new LinkedHashMap<>();
      for (Define define : defines) {
        defineValues.put(define.key(), define.value());
      }
      return new HaxeServerContext(desc, signature, platform, defineValues);
    }
  }

  record Define(String key, String value) {
  }

  /** A {@code server/memory} result: the total cache size and each context's share. */
  record Memory(Totals memory, List<ContextSize> contexts) {

    ServerMemory toServerMemory() {
      List<ServerMemory.ContextSize> sizes = contexts.stream()
        .map(ContextSize::toContextSize)
        .toList();
      return new ServerMemory(memory == null ? 0 : memory.totalCache(), sizes);
    }
  }

  record Totals(long totalCache) {
  }

  record ContextSize(Context context, long size) {

    private static final HaxeServerContext NO_CONTEXT = new HaxeServerContext("", "", "", Map.of());

    ServerMemory.ContextSize toContextSize() {
      return new ServerMemory.ContextSize(context == null ? NO_CONTEXT : context.toContext(), size);
    }
  }

  /** A {@code server/module} result: its signature, the types it declares and the modules it depends on. */
  record Module(String sign, List<JsonTypePath> types, List<Dependency> dependencies) {

    ModuleInfo toModuleInfo() {
      List<String> typeNames = types.stream()
        .map(JsonTypePath::qualifiedName)
        .toList();
      List<String> dependencyPaths = dependencies.stream()
        .map(Dependency::path)
        .toList();
      return new ModuleInfo(sign, typeNames, dependencyPaths);
    }
  }

  record Dependency(String path) {
  }

  /** A {@code JsonModuleType}: the {@code server/type} answer, and each entry of {@code typer/compiledTypes}. */
  record ModuleType(String name, String kind, Members args) {

    TypeBlueprint toBlueprint(String typeName) {
      Members members = args != null ? args : Members.NONE;
      return new TypeBlueprint(typeName, kind, membersOf(members.fields()), membersOf(members.statics()));
    }

    private static List<TypeBlueprint.Member> membersOf(List<JsonClassField> fields) {
      return fields.stream()
        .map(field -> new TypeBlueprint.Member(field.name(), field.type(), field.kindName(), field.pos()))
        .toList();
    }
  }

  record Members(List<JsonClassField> fields, List<JsonClassField> statics) {
    static final Members NONE = new Members(List.of(), List.of());
  }
}
