package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.profiler.HaxeProfilerProcessUi;
import com.intellij.plugins.haxe.profiler.bridge.hashlink.HaxeHlProfilerConfigurationType;
import com.intellij.plugins.haxe.profiler.bridge.hxcpp.HaxeHxcppProfilerConfigurationType;
import com.intellij.plugins.haxe.profiler.bridge.tracy.HaxeHxcppTracyProfilerConfigurationType;
import com.intellij.profiler.ProfilerToolWindowManager;
import com.intellij.profiler.ToolWindowActivationProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Opens the live profiler-process tab for a profiled Haxe run. Adding the
 * tab is what produces the whole Java-parity surface: the platform's green
 * "Profiler attached" balloon on the tool window button, the live
 * indicator on the tool window icon, the duration-counting placeholder,
 * and the results swapping into the same tab when the capture completes.
 */
public class HaxeIuProfilerProcessUi implements HaxeProfilerProcessUi {

  @Override
  public @Nullable Session attached(@NotNull Project project, @NotNull String displayName, @NotNull Path dumpFile) {
    return open(project, displayName, dumpFile, typeIdByExtension(dumpFile));
  }

  /** Direct entry for the bridge captures, which know their configuration type exactly. */
  public static Session open(@NotNull Project project, @NotNull String displayName,
                             @NotNull Path dumpFile, @NotNull String configurationTypeId) {
    HaxeIuProfilerProcess process = new HaxeIuProfilerProcess(project, displayName, dumpFile.toFile(), configurationTypeId);
    process.markAttached();
    ToolWindowActivationProperties properties = new ToolWindowActivationProperties(false, true, () -> true);
    ProfilerToolWindowManager.getInstance(project)
      .addProfilerProcessTab(process, properties);
    return new Session() {
      @Override
      public void dataReady() {
        process.captureFinished();
      }

      @Override
      public void failed(@NotNull String reason) {
        process.captureFailed(reason);
      }
    };
  }

  /** Dump file extension → the profiler configuration type that produces it. */
  private static String typeIdByExtension(Path dumpFile) {
    String name = dumpFile.getFileName().toString();
    if (name.endsWith(".dump")) return HaxeHlProfilerConfigurationType.ID;
    if (name.endsWith(".hxcppprof")) return HaxeHxcppProfilerConfigurationType.ID;
    // an .hxtsession through this interface is the tracy lane; the telemetry
    // capture opens its tab through open() with its own type id
    return HaxeHxcppTracyProfilerConfigurationType.ID;
  }
}
