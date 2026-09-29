package com.intellij.plugins.haxe.profiler.bridge.hxcpp;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.HaxeStreamDumpParser;
import com.intellij.plugins.haxe.profiler.bridge.data.HaxeHxcppProfilerData;
import com.intellij.plugins.haxe.profiler.hxcpp.HxcppProfileTranslator;
import com.intellij.profiler.api.ProfilerDumpFileParser;
import com.intellij.profiler.api.ProfilerDumpParserProvider;
import org.jetbrains.annotations.NotNull;


/**
 * Registers hxcpp profiler reports with the IU snapshot import. The report
 * ({@code cpp.vm.Profiler.start/stop} in a {@code -debug -D HXCPP_PROFILER}
 * build) is headerless text, so routing is by the {@code .hxcppprof}
 * extension, the file name to pass to {@code Profiler.start()}. A foreign
 * text file fails the parse with the offending line.
 */
public class HaxeHxcppDumpParserProvider implements ProfilerDumpParserProvider {

  @Override
  public @NotNull String getId() {
    return "haxe.hxcpp";
  }

  @Override
  public @NotNull String getName() {
    return HaxeProfilerBundle.message("haxe.profiler.hxcpp.snapshot.name");
  }

  @Override
  public @NotNull String getRequiredFileExtension() {
    return "hxcppprof";
  }

  @Override
  public @NotNull ProfilerDumpFileParser createParser(@NotNull Project project) {
    return new HaxeStreamDumpParser(in -> HaxeHxcppProfilerData.from(HxcppProfileTranslator.translate(in)));
  }
}
