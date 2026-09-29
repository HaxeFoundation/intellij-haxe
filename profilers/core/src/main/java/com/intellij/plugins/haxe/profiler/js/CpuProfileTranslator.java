package com.intellij.plugins.haxe.profiler.js;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.StackSample;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a V8 {@code .cpuprofile} (Chrome DevTools save, CDP
 * {@code Profiler.stop} output) into the neutral model. The JSON carries a
 * node tree ({@code callFrame} + {@code children}), the sampled node ids
 * and per-sample microsecond deltas; a sample's stack is the root path to
 * its node and its weight is its delta, with the model's tick set to one
 * microsecond — so totals reproduce V8's own numbers exactly. V8's
 * synthetic nodes: {@code (garbage collector)} samples flag {@code inGc},
 * {@code (idle)} and {@code (program)} samples are dropped (the viewers
 * read sample gaps as idle), {@code (root)} never appears in stacks. Line
 * numbers arrive 0-based and shift to the model's 1-based convention;
 * {@code file://} URLs become plain paths.
 */
public final class CpuProfileTranslator {

  public static final String TARGET = "v8";
  // V8's synthetic nodes: time the engine spent outside any script function
  static final String IDLE_NODE = "(idle)";
  static final String PROGRAM_NODE = "(program)";
  static final String GC_NODE = "(garbage collector)";
  private static final String ROOT_NODE = "(root)";
  /** Sample times are exact microseconds, so the model ticks once per microsecond. */
  private static final int MICROSECOND_TICKS = 1_000_000;

  private CpuProfileTranslator() {
  }

  @NotNull
  public static ProfilerSnapshot translate(@NotNull InputStream in) throws IOException {
    JsonNode profile;
    try {
      profile = new ObjectMapper().readTree(in);
    }
    catch (JacksonException malformed) {
      throw new ProfilerFormatException("not a cpuprofile: " + malformed.getMessage());
    }
    NodeTree tree = NodeTree.of(profile);

    List<StackSample> converted = new ArrayList<>();
    JsonNode samples = profile.path("samples");
    JsonNode deltas = profile.path("timeDeltas");
    long timeUs = profile.path("startTime").asLong(0);
    for (int i = 0; i < samples.size(); i++) {
      long deltaUs = deltas.path(i).asLong(0);
      // Chrome writes occasional negative deltas (timer adjustments); clamping keeps time monotonic
      timeUs += Math.max(deltaUs, 0);
      int nodeId = samples.path(i).asInt(-1);
      String function = tree.functionOf(nodeId);
      if (isIdleNode(function)) continue;
      List<StackFrame> stack = tree.stackOf(nodeId);
      if (stack.isEmpty()) continue;
      long weight = Math.max(deltaUs, 1);
      converted.add(new StackSample(timeUs / 1_000_000.0, 0, stack, weight, GC_NODE.equals(function)));
    }

    List<ProfilerThread> threads = List.of(ProfilerThread.main());
    return new ProfilerSnapshot(TARGET, 1, MICROSECOND_TICKS, threads, List.copyOf(converted), List.of(), List.of());
  }

  /** Whether a sample on this function is the engine idling: {@code (idle)} or {@code (program)}. */
  static boolean isIdleNode(@NotNull String function) {
    return IDLE_NODE.equals(function) || PROGRAM_NODE.equals(function);
  }

  /** Builds a model frame from one raw V8 call frame; the session builder swaps in a source-mapping variant. */
  interface FrameResolver {
    @NotNull
    StackFrame resolve(@NotNull String functionName, @NotNull String url, int line0, int column0);
  }

  /** One profile's node table: id lookups, parent links and cached root paths. Shared with {@link CpuProfileSessionBuilder}. */
  static final class NodeTree {
    private final Map<Integer, JsonNode> byId = new HashMap<>();
    private final Map<Integer, Integer> parents = new HashMap<>();
    private final Map<Integer, List<StackFrame>> stacks = new HashMap<>();
    private final FrameResolver resolver;

    private NodeTree(FrameResolver resolver) {
      this.resolver = resolver;
    }

    static NodeTree of(JsonNode profile) throws IOException {
      return of(profile, CpuProfileTranslator::frameOf);
    }

    static NodeTree of(JsonNode profile, FrameResolver resolver) throws IOException {
      JsonNode nodes = profile.path("nodes");
      if (!nodes.isArray() || !profile.path("samples").isArray() || !profile.path("timeDeltas").isArray()) {
        throw new ProfilerFormatException("not a cpuprofile: nodes/samples/timeDeltas missing");
      }
      NodeTree tree = new NodeTree(resolver);
      for (JsonNode node : nodes) {
        int id = node.path("id").asInt(-1);
        tree.byId.put(id, node);
        for (JsonNode child : node.path("children")) {
          tree.parents.put(child.asInt(-1), id);
        }
      }
      return tree;
    }

    String functionOf(int nodeId) throws IOException {
      JsonNode node = byId.get(nodeId);
      if (node == null) {
        throw new ProfilerFormatException("sample references unknown node " + nodeId);
      }
      return node.path("callFrame").path("functionName").asString("");
    }

    /** The root path to {@code nodeId}, cached per node; V8's {@code (root)} node stays out of stacks. */
    List<StackFrame> stackOf(int nodeId) {
      List<StackFrame> cached = stacks.get(nodeId);
      if (cached != null) return cached;

      List<StackFrame> stack = new ArrayList<>();
      Integer current = nodeId;
      while (current != null && stack.size() <= byId.size()) {
        JsonNode node = byId.get(current);
        if (node == null) break;
        JsonNode callFrame = node.path("callFrame");
        String function = callFrame.path("functionName").asString("");
        if (!ROOT_NODE.equals(function)) {
          String url = callFrame.path("url").asString("");
          int line0 = callFrame.path("lineNumber").asInt(-1);
          int column0 = callFrame.path("columnNumber").asInt(-1);
          stack.add(resolver.resolve(function, url, line0, column0));
        }
        current = parents.get(current);
      }
      List<StackFrame> rootFirst = List.copyOf(stack.reversed());
      stacks.put(nodeId, rootFirst);
      return rootFirst;
    }
  }

  /** The plain resolver: the generated file's own url and 0-based line, shifted to the model's 1-based convention. */
  static StackFrame frameOf(String function, String url, int line0, int column0) {
    String symbol = function.isEmpty() ? "(anonymous)" : function;
    String file = fileOf(url);
    return new StackFrame(symbol, file, file != null && line0 >= 0 ? line0 + 1 : StackFrame.NO_LINE);
  }

  @Nullable
  private static String fileOf(String url) {
    if (url.isEmpty()) return null;
    String path = JsSourceMap.pathOfFileUrl(url);
    return path != null ? path : url;
  }
}
