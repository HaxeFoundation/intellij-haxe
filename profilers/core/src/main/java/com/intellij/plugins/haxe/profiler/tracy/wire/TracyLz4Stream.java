package com.intellij.plugins.haxe.profiler.tracy.wire;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * The decompressed view of tracy's data stream: a sequence of
 * {@code [u32 LE compressed size][LZ4 block]} frames, where blocks are
 * compressed in CONTINUE mode — matches may reach up to 64 KB into the
 * PREVIOUS frames' output. Each frame therefore decodes through a fresh
 * commons-compress block reader prefilled with the carried window; frames
 * must never be concatenated (the final-sequence-has-no-match rule is
 * positional). Ends cleanly at a frame boundary; a partial frame means the
 * peer died mid-write and fails as truncated.
 */
public final class TracyLz4Stream extends InputStream {

  /** LZ4's maximum back-reference distance — how much output the next frame may reach into. */
  private static final int WINDOW_SIZE = 64 * 1024;
  /** The client's target size of one uncompressed frame. */
  private static final int MAX_FRAME_SIZE = 256 * 1024;
  private static final byte[] NOTHING = new byte[0];

  private final InputStream source;
  private byte[] window = NOTHING;
  private byte[] frame = NOTHING;
  private int position;

  public TracyLz4Stream(@NotNull InputStream source) {
    this.source = source;
  }

  @Override
  public int read() throws IOException {
    if (!ensureData()) return -1;
    return frame[position++] & 0xFF;
  }

  @Override
  public int read(byte @NotNull [] buffer, int offset, int length) throws IOException {
    if (length == 0) return 0;
    if (!ensureData()) return -1;
    int count = Math.min(length, frame.length - position);
    System.arraycopy(frame, position, buffer, offset, count);
    position += count;
    return count;
  }

  private boolean ensureData() throws IOException {
    while (position == frame.length) {
      if (!nextFrame()) return false;
    }
    return true;
  }

  /** Reads and decompresses the next frame; false on a clean end between frames. */
  private boolean nextFrame() throws IOException {
    int first = source.read();
    if (first < 0) return false;
    int compressedSize = first;
    for (int i = 1; i < 4; i++) {
      int next = source.read();
      if (next < 0) throw new ProfilerFormatException("truncated tracy frame header");
      compressedSize |= next << (8 * i);
    }
    if (compressedSize <= 0 || compressedSize > MAX_FRAME_SIZE * 2) {
      throw new ProfilerFormatException("implausible tracy frame size: " + compressedSize);
    }

    byte[] compressed = source.readNBytes(compressedSize);
    if (compressed.length < compressedSize) {
      throw new ProfilerFormatException("truncated tracy frame (" + compressed.length + " of " + compressedSize + " bytes)");
    }

    try (BlockLZ4CompressorInputStream block = new BlockLZ4CompressorInputStream(new ByteArrayInputStream(compressed))) {
      block.prefill(window);
      frame = block.readNBytes(MAX_FRAME_SIZE + 1);
      if (frame.length > MAX_FRAME_SIZE) {
        throw new ProfilerFormatException("tracy frame exceeds the protocol's frame size");
      }
    }
    catch (EOFException | IllegalArgumentException e) {
      throw new ProfilerFormatException("corrupt tracy frame: " + e.getMessage());
    }
    position = 0;
    carryWindow(frame);
    return true;
  }

  /** The last ≤64 KB of everything produced so far — the next frame's dictionary. */
  private void carryWindow(byte[] produced) {
    if (produced.length >= WINDOW_SIZE) {
      window = new byte[WINDOW_SIZE];
      System.arraycopy(produced, produced.length - WINDOW_SIZE, window, 0, WINDOW_SIZE);
      return;
    }
    int keep = Math.min(window.length, WINDOW_SIZE - produced.length);
    byte[] next = new byte[keep + produced.length];
    System.arraycopy(window, window.length - keep, next, 0, keep);
    System.arraycopy(produced, 0, next, keep, produced.length);
    window = next;
  }

  @Override
  public void close() throws IOException {
    source.close();
  }
}
