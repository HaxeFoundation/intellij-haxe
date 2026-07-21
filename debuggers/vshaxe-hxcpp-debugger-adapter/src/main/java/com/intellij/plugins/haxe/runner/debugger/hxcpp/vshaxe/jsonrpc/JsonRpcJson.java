package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Encodes/decodes hxcpp-debug-server jsonrpc messages.
 *
 * Decoding discriminates on the envelope: a message carrying an {@code id}
 * is the response to one of our requests; one carrying only a {@code method}
 * is a notification. The server never sends its own requests. Note that the
 * server answers by sending the REQUEST OBJECT back with {@code result} or
 * {@code error} filled in (Server.hx sendResponse), so responses also carry
 * the request's {@code method} and {@code params} — id alone decides.
 */
public final class JsonRpcJson {
  private static final ObjectMapper MAPPER = JsonMapper.builder()
    .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build();

  private JsonRpcJson() {
  }

  /** The shared (immutable) mapper, for decoding typed result/params trees. */
  public static ObjectMapper mapper() {
    return MAPPER;
  }

  public static String encode(JsonRpcRequest request) {
    ObjectNode root = MAPPER.createObjectNode();
    root.put("id", request.id());
    root.put("method", request.method());
    root.set("params", request.params() == null
                       ? MAPPER.createObjectNode()
                       : MAPPER.valueToTree(request.params()));
    return MAPPER.writeValueAsString(root);
  }

  public static JsonRpcServerMessage decode(String json) {
    JsonNode root = MAPPER.readTree(json);
    if (root.hasNonNull("id")) {
      JsonRpcError error = null;
      JsonNode errorNode = root.get("error");
      if (errorNode != null && !errorNode.isNull()) {
        error = new JsonRpcError(errorNode.path("code").asInt(0),
                                 errorNode.path("message").asString(""));
      }
      return new JsonRpcResponse(root.get("id").asInt(), root.get("result"), error);
    }
    if (root.hasNonNull("method")) {
      return new JsonRpcNotification(root.get("method").asString(), root.get("params"));
    }
    throw new IllegalArgumentException("Not a jsonrpc response or notification: " + json);
  }
}
