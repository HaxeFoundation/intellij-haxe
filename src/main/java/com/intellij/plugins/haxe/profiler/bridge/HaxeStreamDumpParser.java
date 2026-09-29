package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.profiler.api.Failure;
import com.intellij.profiler.api.ProfilerData;
import com.intellij.profiler.api.ProfilerDumpFileParser;
import com.intellij.profiler.api.ProfilerDumpFileParsingResult;
import com.intellij.profiler.api.Success;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * A snapshot parser that reads the whole dump through one stream the
 * platform's cancel button can interrupt; a read failure becomes a
 * {@link Failure} naming the cause instead of an error.
 */
public final class HaxeStreamDumpParser implements ProfilerDumpFileParser {

  /** Translates the dump into the data the profiler tool window shows. */
  public interface Reader {
    @NotNull
    ProfilerData read(@NotNull InputStream in) throws IOException;
  }

  private final Reader reader;

  public HaxeStreamDumpParser(@NotNull Reader reader) {
    this.reader = reader;
  }

  @Override
  public @NotNull ProfilerDumpFileParsingResult parse(@NotNull File file, @NotNull ProgressIndicator indicator) {
    try (InputStream in = new CancellableStream(new BufferedInputStream(Files.newInputStream(file.toPath())), indicator)) {
      return new Success(reader.read(in));
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
