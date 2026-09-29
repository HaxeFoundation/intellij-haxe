package com.intellij.plugins.haxe.profiler.bridge.js;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.HaxeStreamDumpParser;
import com.intellij.plugins.haxe.profiler.bridge.data.HaxeSamplingProfilerData;
import com.intellij.plugins.haxe.profiler.js.CpuProfileTranslator;
import com.intellij.profiler.api.ProfilerDumpFileParser;
import com.intellij.profiler.api.ProfilerDumpParserProvider;
import org.jetbrains.annotations.NotNull;


/**
 * Registers V8 {@code .cpuprofile} files (Chrome DevTools saves, CDP
 * {@code Profiler.stop} output — what a JS-target capture produces) with
 * the IU snapshot import. Opens as sampled data with the stock views plus
 * the Haxe Call Chart; positions point at the generated JavaScript.
 */
public class HaxeJsCpuProfileParserProvider implements ProfilerDumpParserProvider {

  @Override
  public @NotNull String getId() {
    return "haxe.cpuprofile";
  }

  @Override
  public @NotNull String getName() {
    return HaxeProfilerBundle.message("haxe.profiler.js.snapshot.name");
  }

  @Override
  public @NotNull String getRequiredFileExtension() {
    return "cpuprofile";
  }

  @Override
  public @NotNull ProfilerDumpFileParser createParser(@NotNull Project project) {
    return new HaxeStreamDumpParser(in -> HaxeSamplingProfilerData.from(CpuProfileTranslator.translate(in)));
  }
}
