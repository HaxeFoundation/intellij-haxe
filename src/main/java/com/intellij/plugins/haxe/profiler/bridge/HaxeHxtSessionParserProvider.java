package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.data.HaxeSamplingProfilerData;
import com.intellij.plugins.haxe.profiler.bridge.data.HaxeTracyProfilerData;
import com.intellij.plugins.haxe.profiler.hxt.HxtCapture;
import com.intellij.plugins.haxe.profiler.hxt.HxtSessionTranslator;
import com.intellij.profiler.api.Failure;
import com.intellij.profiler.api.ProfilerDumpFileParser;
import com.intellij.profiler.api.ProfilerDumpFileParsingResult;
import com.intellij.profiler.api.ProfilerDumpParserProvider;
import com.intellij.profiler.api.Success;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * Registers captured HXTS sessions ({@code .hxtsession}) with the IU
 * snapshot import. The container version decides the view: v1 holds sampled
 * telemetry frames (opened as sampled data), v5 and v6 exact tracy zone
 * captures (opened as zone data). Both get the stock views plus the Haxe
 * Call Chart.
 */
public class HaxeHxtSessionParserProvider implements ProfilerDumpParserProvider {

  @Override
  public @NotNull String getId() {
    return "haxe.hxtsession";
  }

  @Override
  public @NotNull String getName() {
    return HaxeProfilerBundle.message("haxe.profiler.hxt.snapshot.name");
  }

  @Override
  public @NotNull String getRequiredFileExtension() {
    return "hxtsession";
  }

  @Override
  public @NotNull ProfilerDumpFileParser createParser(@NotNull Project project) {
    return new HxtSessionFileParser();
  }

  private static final class HxtSessionFileParser implements ProfilerDumpFileParser {
    @Override
    public @NotNull ProfilerDumpFileParsingResult parse(@NotNull File file, @NotNull ProgressIndicator indicator) {
      // zone captures are served FROM the file (they are too big to load
      // whole), so the translator takes the path rather than a stream
      try {
        HxtCapture capture = HxtSessionTranslator.translateCapture(file.toPath());
        return new Success(switch (capture) {
          // the file rides along: a still-live capture of it renders the self-refreshing view
          case HxtCapture.Samples samples -> HaxeSamplingProfilerData.from(samples.snapshot(), file.toPath());
          case HxtCapture.Zones zones -> HaxeTracyProfilerData.from(zones.store());
        });
      }
      catch (IOException e) {
        return new Failure(HaxeProfilerBundle.message("haxe.profiler.parse.failed", e.getMessage()));
      }
    }

    @Override
    public @Nullable String getHelpId() {
      return null;
    }
  }
}
