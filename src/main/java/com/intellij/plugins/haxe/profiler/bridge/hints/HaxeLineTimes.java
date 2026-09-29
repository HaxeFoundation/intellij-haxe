package com.intellij.plugins.haxe.profiler.bridge.hints;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.profiler.bridge.data.HaxeCallStackElement;
import com.intellij.plugins.haxe.profiler.bridge.data.HaxeSamplingProfilerData;
import com.intellij.plugins.haxe.profiler.hxt.HxtZoneStore;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.StackSample;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A capture's time aggregated per source line, for the editor gutter
 * hints. Sampled captures (HL) attribute like the Java hints: every frame's
 * line is the CALL SITE inside its own method, so a line knows the method
 * enclosing it and that method's total. Tracy zones only carry the callee
 * function's declaration line (the caller's current line never reaches the
 * wire), so tracy hints are per function with no enclosing share. A sampled
 * capture WITHOUT positions (flash: its telemetry carries bare qualified
 * names) resolves each symbol to its project declaration instead and
 * attributes per function like tracy. Lines whose total rounds below a
 * microsecond are dropped rather than shown as zero. Files are keyed by
 * their forward-slashed path as the capture spelled it (declarations by
 * their full path); editors match by path suffix.
 */
final class HaxeLineTimes {

  /** {@code enclosing} is the method's displayable name ("get", "setup.2"); null for per-function (tracy) attribution. */
  record LineTime(long totalUs, long selfUs, @Nullable String enclosing, long enclosingTotalUs) {
  }

  private final Map<String, Map<Integer, LineTime>> byFile;
  private final long sessionUs;
  /** Editor paths resolve to the same file table repeatedly; remember the verdict. */
  private final Map<String, Map<Integer, LineTime>> byEditorPath = new HashMap<>();

  private HaxeLineTimes(Map<String, Map<Integer, LineTime>> byFile, long sessionUs) {
    this.byFile = byFile;
    this.sessionUs = Math.max(sessionUs, 1);
  }

  long sessionUs() {
    return sessionUs;
  }

  /** The line table for an editor's file, or null when the capture never touched it. */
  @Nullable
  synchronized Map<Integer, LineTime> forEditorPath(@NotNull String path) {
    String normalized = path.replace('\\', '/');
    if (byEditorPath.containsKey(normalized)) return byEditorPath.get(normalized);
    Map<Integer, LineTime> match = null;
    for (Map.Entry<String, Map<Integer, LineTime>> entry : byFile.entrySet()) {
      if (normalized.endsWith("/" + entry.getKey()) || normalized.equals(entry.getKey())) {
        match = entry.getValue();
        break;
      }
    }
    byEditorPath.put(normalized, match);
    return match;
  }

  /** Tracy captures: exact per-function times from one close-ordered scan of the store. */
  @NotNull
  static HaxeLineTimes fromStore(@NotNull HxtZoneStore store) throws IOException {
    Map<String, Map<Integer, Accumulator>> byFile = new HashMap<>();
    Map<Integer, List<long[]>> pendingByThread = new HashMap<>();
    store.scanZones(-1, 0, Long.MAX_VALUE, 0, (threadId, depth, startNs, endNs, location) -> {
      // a zone's children closed just before it: their durations are
      // pending one level deeper (same fold as the call-tree trie)
      List<long[]> pending = pendingByThread.computeIfAbsent(threadId, id -> new ArrayList<>());
      while (pending.size() <= depth + 1) pending.add(new long[1]);
      long durationNs = endNs - startNs;
      long selfNs = Math.max(durationNs - pending.get(depth + 1)[0], 0);
      pending.get(depth + 1)[0] = 0;
      pending.get(depth)[0] += durationNs;

      if (location.file().isEmpty() || location.line() <= 0) return;
      Accumulator line = byFile
        .computeIfAbsent(location.file().replace('\\', '/'), file -> new HashMap<>())
        .computeIfAbsent(location.line(), key -> new Accumulator());
      line.totalNs += durationNs;
      line.selfNs += selfNs;
    });
    return new HaxeLineTimes(freeze(byFile, Map.of()), store.session().durationNs() / 1000);
  }

  /**
   * Sampled captures, attributed like the Java hints: every frame's line is
   * a call site inside that frame's own method, the leaf line gets the self
   * time, and each line remembers its method and the method's total. The
   * hints are SOURCE-level while HL compiles a generic into one function
   * per type argument ({@code Pool_pack_Item.get}), so methods are keyed by
   * file and displayed name: a line's "% of method" denominator covers
   * every specialization of the source method, never just its own.
   */
  @NotNull
  static HaxeLineTimes fromSnapshot(@NotNull ProfilerSnapshot snapshot) {
    long periodNs = HaxeSamplingProfilerData.samplePeriodUs(snapshot) * 1000;
    Map<String, Map<Integer, Accumulator>> byFile = new HashMap<>();
    Map<String, Long> methodTotalsNs = new HashMap<>();
    long sessionNs = 0;
    Set<String> chargedLines = new HashSet<>();
    Set<String> chargedMethods = new HashSet<>();
    for (StackSample sample : snapshot.samples()) {
      long timeNs = sample.weight() * periodNs;
      sessionNs += timeNs;
      chargedLines.clear();
      chargedMethods.clear();
      List<StackFrame> frames = sample.frames();
      for (int i = 0; i < frames.size(); i++) {
        StackFrame frame = frames.get(i);
        if (frame.file() == null || frame.line() <= 0) continue;
        String file = frame.file().replace('\\', '/');
        String methodName = displayMethodName(frame.symbol());
        String methodKey = methodKey(file, methodName);
        // recursion (and same-line siblings across specializations) charge
        // a method once per sample, not once per frame
        if (chargedMethods.add(methodKey)) {
          methodTotalsNs.merge(methodKey, timeNs, Long::sum);
        }
        if (!chargedLines.add(file + ":" + frame.line())) continue;
        Accumulator line = byFile
          .computeIfAbsent(file, key -> new HashMap<>())
          .computeIfAbsent(frame.line(), key -> new Accumulator());
        line.totalNs += timeNs;
        line.enclosing = methodName;
        if (i == frames.size() - 1) line.selfNs += timeNs;
      }
    }
    return new HaxeLineTimes(freeze(byFile, methodTotalsNs), sessionNs / 1000);
  }

  /** Whether any frame carries a source position — without one the declaration fallback is the only attribution. */
  static boolean carriesPositions(@NotNull ProfilerSnapshot snapshot) {
    for (StackSample sample : snapshot.samples()) {
      for (StackFrame frame : sample.frames()) {
        if (frame.file() != null && frame.line() > 0) return true;
      }
    }
    return false;
  }

  /**
   * Sampled captures whose frames carry NO positions: every distinct
   * symbol resolves once to its project declaration (a smart-mode read
   * action — the chips wait for indexing) and the sampled time lands per
   * FUNCTION on the declaration line, like the tracy hints. Symbols the
   * project cannot name (player builtins, "(unknown)") get no chip.
   */
  @NotNull
  static HaxeLineTimes fromDeclarations(@NotNull Project project, @NotNull ProfilerSnapshot snapshot) {
    long periodNs = HaxeSamplingProfilerData.samplePeriodUs(snapshot) * 1000;
    Map<String, Accumulator> bySymbol = new HashMap<>();
    long sessionNs = 0;
    Set<String> charged = new HashSet<>();
    for (StackSample sample : snapshot.samples()) {
      long timeNs = sample.weight() * periodNs;
      sessionNs += timeNs;
      charged.clear();
      List<StackFrame> frames = sample.frames();
      for (int i = 0; i < frames.size(); i++) {
        String symbol = frames.get(i).symbol();
        Accumulator function = bySymbol.computeIfAbsent(symbol, key -> new Accumulator());
        // recursion charges a function once per sample, not once per frame
        if (charged.add(symbol)) {
          function.totalNs += timeNs;
        }
        if (i == frames.size() - 1) function.selfNs += timeNs;
      }
    }

    Map<String, Declaration> declarations =
      ReadAction.nonBlocking(() -> resolveDeclarations(project, bySymbol.keySet()))
        .inSmartMode(project)
        .executeSynchronously();
    Map<String, Map<Integer, Accumulator>> byFile = new HashMap<>();
    bySymbol.forEach((symbol, function) -> {
      Declaration declaration = declarations.get(symbol);
      if (declaration == null) return;
      // closures of one method share its declaration - their times merge
      Accumulator line = byFile
        .computeIfAbsent(declaration.path(), file -> new HashMap<>())
        .computeIfAbsent(declaration.line(), key -> new Accumulator());
      line.totalNs += function.totalNs;
      line.selfNs += function.selfNs;
    });
    return new HaxeLineTimes(freeze(byFile, Map.of()), sessionNs / 1000);
  }

  /** A resolved symbol's declaring file (full path) and 1-based line. */
  private record Declaration(String path, int line) {
  }

  /** Runs inside the read action: each symbol's declaration position, resolved via the project's classes. */
  private static Map<String, Declaration> resolveDeclarations(Project project, Collection<String> symbols) {
    Map<String, Declaration> resolved = new HashMap<>();
    for (String symbol : symbols) {
      NavigatablePsiElement declaration = HaxeCallStackElement.declarationOf(project, symbol);
      if (declaration == null) continue;
      PsiFile psiFile = declaration.getContainingFile();
      VirtualFile virtualFile = psiFile == null ? null : psiFile.getVirtualFile();
      Document document = psiFile == null ? null : PsiDocumentManager.getInstance(project).getDocument(psiFile);
      if (virtualFile == null || document == null) continue;
      int offset = Math.min(declaration.getTextOffset(), document.getTextLength());
      resolved.put(symbol, new Declaration(virtualFile.getPath(), document.getLineNumber(offset) + 1));
    }
    return resolved;
  }

  /**
   * The symbol's last name segment, extended left across purely numeric
   * segments so a closure ("Main.setup.2") reads "setup.2" rather than a
   * bare "2" that names nothing. This is both the tooltip text and (with
   * the file) the method-total key, so the shown name and the shown share
   * always describe the same thing.
   */
  static String displayMethodName(String symbol) {
    // split on dots - HL symbols are Class.method with one numeric segment
    // appended per closure nesting level
    String[] segments = symbol.split("\\.");
    int first = segments.length - 1;
    while (first > 0 && segments[first].chars().allMatch(Character::isDigit)) first--;
    return String.join(".", Arrays.asList(segments).subList(first, segments.length));
  }

  /** Converts to µs once, drops sub-µs lines, and resolves each line's enclosing-method total. */
  private static Map<String, Map<Integer, LineTime>> freeze(Map<String, Map<Integer, Accumulator>> byFile,
                                                            Map<String, Long> methodTotalsNs) {
    Map<String, Map<Integer, LineTime>> frozen = new HashMap<>();
    byFile.forEach((file, lines) -> {
      Map<Integer, LineTime> frozenLines = new HashMap<>();
      lines.forEach((line, accumulator) -> {
        long totalUs = accumulator.totalNs / 1000;
        if (totalUs <= 0) return;
        long enclosingTotalUs = accumulator.enclosing == null
                                ? 0
                                : methodTotalsNs.getOrDefault(methodKey(file, accumulator.enclosing), 0L) / 1000;
        long selfUs = accumulator.selfNs / 1000;
        frozenLines.put(line, new LineTime(totalUs, selfUs, accumulator.enclosing, enclosingTotalUs));
      });
      if (!frozenLines.isEmpty()) frozen.put(file, Map.copyOf(frozenLines));
    });
    return Map.copyOf(frozen);
  }

  /** A source method's identity across its compiled specializations: file plus displayed name. */
  private static String methodKey(String file, String methodName) {
    return file + "#" + methodName;
  }

  private static final class Accumulator {
    long totalNs;
    long selfNs;
    @Nullable String enclosing;
  }
}
