package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.RunExecutorSettings;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.plugins.haxe.profiler.HaxeProfilerExecutorSupport;
import com.intellij.plugins.haxe.profiler.bridge.flash.HaxeFlashProfilerConfigurationState;
import com.intellij.plugins.haxe.profiler.bridge.hashlink.HaxeHlProfilerConfigurationState;
import com.intellij.plugins.haxe.profiler.bridge.hxcpp.HaxeHxcppProfilerConfigurationState;
import com.intellij.plugins.haxe.profiler.bridge.js.HaxeJsProfilerConfigurationState;
import com.intellij.plugins.haxe.profiler.bridge.tracy.HaxeHxcppTracyProfilerConfigurationState;
import com.intellij.profiler.DefaultProfilerExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Resolves profiler child executors back to their Haxe profiler
 * configurations: the HashLink entry's sampling rate, the hxcpp entry's
 * compile additions (its profiler defines plus the bundled start/stop
 * bootstrap macro, extracted beside the IDE's system directory), and the
 * lane-matching executor for tool-window Profile actions.
 */
public class HaxeIuProfilerExecutorSupport implements HaxeProfilerExecutorSupport {

  private static final Logger LOG = Logger.getInstance(HaxeIuProfilerExecutorSupport.class);
  /** The bundled bootstrap: the init macro wrapping main/System.exit, its idempotent-stop runtime, and the collectors. */
  private static final List<String> BOOT_RESOURCES = List.of(
    "/haxe/profilerboot/ijhaxe/ProfilerBoot.hx",
    "/haxe/profilerboot/ijhaxe/ProfilerRun.hx",
    "/haxe/profilerboot/ijhaxe/TelemetryRun.hx",
    "/haxe/profilerboot/ijhaxe/CppTelemetry.hx");

  @Override
  public @Nullable Integer hashlinkSamplesPerSecondFor(@NotNull Executor executor) {
    if (!(HaxeProfilerConfigurations.stateFor(executor) instanceof HaxeHlProfilerConfigurationState state)) return null;
    return state.getSamplesPerSecond();
  }

  @Override
  public @Nullable List<String> hxcppProfilingAdditionsFor(@NotNull Executor executor, boolean limeFamily, @NotNull Path dumpPath) {
    if (!(HaxeProfilerConfigurations.stateFor(executor) instanceof HaxeHxcppProfilerConfigurationState)) return null;
    Path macroRoot = extractBootMacro();
    if (macroRoot == null) return null;

    // the dump path is baked into the binary as a haxe string literal:
    // forward slashes keep backslash escaping out of the equation
    String macroCall = "ijhaxe.ProfilerBoot.use('" + dumpPath.toString().replace('\\', '/') + "')";
    String classpath = macroRoot.toString().replace('\\', '/');
    // HXCPP_TELEMETRY builds the runtime's telemetry hooks in (alloc/GC,
    // dormant until started); they coexist with the report profiler
    List<String> additions = defineArgs(limeFamily, List.of("HXCPP_PROFILER", "HXCPP_STACK_TRACE", "HXCPP_TELEMETRY"));
    if (limeFamily) {
      // lime turns each --haxeflag value into one hxml line, where a flag
      // takes the rest of the line as its argument - spaces need no quoting
      additions.addAll(List.of("--haxeflag=-cp " + classpath, "--haxeflag=--macro " + macroCall));
    }
    else {
      additions.addAll(List.of("-cp", classpath, "--macro", macroCall));
    }
    return additions;
  }

  @Override
  public @Nullable List<String> hxcppTracyAdditionsFor(@NotNull Executor executor, boolean limeFamily) {
    if (!(HaxeProfilerConfigurations.stateFor(executor) instanceof HaxeHxcppTracyProfilerConfigurationState state)) return null;
    // HXCPP_TELEMETRY is the master switch, HXCPP_TRACY picks the tracy
    // implementation, and the zones need the stack-frame instrumentation.
    // HXCPP_STACK_LINE is REQUIRED, not optional: hxcpp's tracy integration
    // reads each stack frame's line number unconditionally, and that field
    // only exists with the define, so a tracy build without it fails to
    // compile. HXCPP_TRACY_MEMORY adds the GC alloc/free hooks feeding the
    // memory curves and GC lane; the settings page toggles it (runtime cost).
    List<String> defines = new ArrayList<>(List.of("HXCPP_TELEMETRY", "HXCPP_TRACY",
                                                   "HXCPP_STACK_TRACE", "HXCPP_STACK_LINE"));
    if (state.isCaptureMemory()) {
      defines.add("HXCPP_TRACY_MEMORY");
    }
    return defineArgs(limeFamily, defines);
  }

  @Override
  public @Nullable List<String> hlProfilingAdditionsFor(@NotNull Executor executor, boolean limeFamily) {
    if (!(HaxeProfilerConfigurations.stateFor(executor) instanceof HaxeHlProfilerConfigurationState)) return null;
    // hl_profile is the compile-side profiling switch by convention: heaps'
    // main loop emits an end-of-frame marker and pauses sampling across
    // present() under it, so frames align and vsync wait stays out of the
    // samples. Code without the guard is unaffected - an unknown define is inert.
    return defineArgs(limeFamily, List.of("hl_profile"));
  }

  @Override
  public @Nullable List<String> flashProfilingAdditionsFor(@NotNull Executor executor, boolean limeFamily) {
    if (!(HaxeProfilerConfigurations.stateFor(executor) instanceof HaxeFlashProfilerConfigurationState)) return null;
    // advanced-telemetry embeds the EnableTelemetry swf tag (swf-version 17+):
    // the runtime then streams its own Scout telemetry - frames, render
    // spans, sampler stacks, memory, GC - to the address in ~/.telemetry.cfg,
    // which the capture points at itself. No code is injected; the sampler
    // ticks only on the debugger runtime, so adl launches in debug mode.
    return limeFamily
           ? List.of("--haxeflag=-D advanced-telemetry")
           : List.of("-D", "advanced-telemetry");
  }

  @Override
  public @Nullable Integer jsSamplingIntervalUsFor(@NotNull Executor executor) {
    if (!(HaxeProfilerConfigurations.stateFor(executor) instanceof HaxeJsProfilerConfigurationState state)) return null;
    return state.getSamplingIntervalUs();
  }

  @Override
  public @Nullable List<String> jsProfilingAdditionsFor(@NotNull Executor executor, boolean limeFamily) {
    if (!(HaxeProfilerConfigurations.stateFor(executor) instanceof HaxeJsProfilerConfigurationState)) return null;
    // the sampled positions map back to .hx through the compiler's js
    // source map; both define spellings cover haxe versions before and
    // after the js-source-map -> source-map rename (an unknown define is
    // inert, so shipping both is safe)
    return limeFamily
           ? List.of("--haxeflag=-D js-source-map", "--haxeflag=-D source-map")
           : List.of("-D", "js-source-map", "-D", "source-map");
  }

  @Override
  public boolean hxcppTracyElevatedFor(@NotNull Executor executor) {
    // tracy's system tracing has Windows and Linux backends only;
    // elevating on macOS would gain nothing
    if (!SystemInfo.isWindows && !SystemInfo.isLinux) return false;
    return HaxeProfilerConfigurations.stateFor(executor) instanceof HaxeHxcppTracyProfilerConfigurationState state
           && state.isCollectProcessCpu();
  }

  @Override
  public @NotNull List<ProfilerEntry> profilerExecutorsFor(HaxeProfilableRunConfiguration.@NotNull Lane lane) {
    DefaultProfilerExecutorGroup group = DefaultProfilerExecutorGroup.Companion.getInstance();
    if (group == null) return List.of();
    Set<String> typeIds = HaxeProfilerConfigurations.typeIdsFor(lane);

    List<ProfilerEntry> entries = new ArrayList<>();
    for (Executor child : group.childExecutors()) {
      RunExecutorSettings settings = group.getRegisteredSettings(child.getId());
      if (settings instanceof DefaultProfilerExecutorGroup.ProfilerExecutorSettings profilerSettings
          && typeIds.contains(profilerSettings.getState().getConfigurationTypeId())) {
        entries.add(new ProfilerEntry(child, profilerSettings.getState().getDisplayName()));
      }
    }
    return entries;
  }

  /** Compile defines in the build tool's spelling: lime takes {@code -Dname}, plain haxe {@code -D name}. */
  private static List<String> defineArgs(boolean limeFamily, List<String> names) {
    List<String> args = new ArrayList<>();
    for (String name : names) {
      if (limeFamily) {
        args.add("-D" + name);
      }
      else {
        args.add("-D");
        args.add(name);
      }
    }
    return args;
  }

  /**
   * The classpath root holding the bundled bootstrap sources, extracted (and
   * kept current) under the IDE system directory; null when extraction fails —
   * the launch then proceeds unprofiled rather than failing the build.
   */
  @Nullable
  private static Path extractBootMacro() {
    Path root = Path.of(PathManager.getSystemPath(), "haxe-profiler");
    try {
      for (String resource : BOOT_RESOURCES) {
        // the resource path mirrors the haxe package layout under the root
        Path target = root.resolve(resource.substring(resource.lastIndexOf("/ijhaxe/") + 1));
        try (InputStream source = HaxeIuProfilerExecutorSupport.class.getResourceAsStream(resource)) {
          if (source == null) {
            LOG.warn("bundled profiler bootstrap missing: " + resource);
            return null;
          }
          byte[] content = source.readAllBytes();
          // rewrite on content change so a plugin update replaces stale extractions
          if (!Files.exists(target) || !Arrays.equals(content, Files.readAllBytes(target))) {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
          }
        }
      }
      return root;
    }
    catch (IOException e) {
      LOG.warn("could not extract the profiler bootstrap", e);
      return null;
    }
  }
}
