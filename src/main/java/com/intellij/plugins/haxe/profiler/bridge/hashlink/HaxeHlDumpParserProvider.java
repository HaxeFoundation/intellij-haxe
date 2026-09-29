package com.intellij.plugins.haxe.profiler.bridge.hashlink;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.HaxeStreamDumpParser;
import com.intellij.plugins.haxe.profiler.bridge.data.HaxeSamplingProfilerData;
import com.intellij.plugins.haxe.profiler.hl.HlProfDumpTranslator;
import com.intellij.profiler.api.ProfilerDumpFileParser;
import com.intellij.profiler.api.SignatureBasedProfilerDumpParserProvider;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Registers HashLink's {@code hlprofile.dump} with the IU profiler's snapshot
 * import ("Open Profiler Snapshot" and drag-and-drop). The dump's {@code .dump}
 * extension is generic, so the PROF magic doubles as the file signature and a
 * foreign file parses to a Failure instead of an error.
 */
public class HaxeHlDumpParserProvider implements SignatureBasedProfilerDumpParserProvider {

  @Override
  public @NotNull String getId() {
    return "haxe.hashlink";
  }

  @Override
  public @NotNull String getName() {
    return HaxeProfilerBundle.message("haxe.profiler.hl.snapshot.name");
  }

  @Override
  public @NotNull String getRequiredFileExtension() {
    return "dump";
  }

  @Override
  public @NotNull List<byte[]> getSupportedFileSignatures() {
    return List.of("PROF".getBytes(StandardCharsets.US_ASCII));
  }

  @Override
  public @NotNull ProfilerDumpFileParser createParser(@NotNull Project project) {
    return new HaxeStreamDumpParser(in -> HaxeSamplingProfilerData.from(HlProfDumpTranslator.translate(in)));
  }
}
