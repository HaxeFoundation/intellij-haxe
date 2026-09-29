package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Lets the platform's cancel button interrupt a large-dump parse. */
public final class CancellableStream extends FilterInputStream {
  private final ProgressIndicator indicator;

  public CancellableStream(@NotNull InputStream in, @NotNull ProgressIndicator indicator) {
    super(in);
    this.indicator = indicator;
  }

  @Override
  public int read(byte @NotNull [] buffer, int offset, int length) throws IOException {
    indicator.checkCanceled();
    return super.read(buffer, offset, length);
  }
}
