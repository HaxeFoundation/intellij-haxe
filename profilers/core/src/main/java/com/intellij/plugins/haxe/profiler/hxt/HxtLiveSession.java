package com.intellij.plugins.haxe.profiler.hxt;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Incremental reader for a GROWING v1 session file: {@link #poll} consumes
 * only the records appended since the last poll (a trailing partial record
 * waits for the next one) and {@link #snapshot} serves the accumulated
 * capture, so a live view's per-tick cost follows the NEW data, not the
 * session length. Single consumer: poll and snapshot from one thread at a
 * time; the snapshot's lists are copies, safe to hand across threads.
 */
public final class HxtLiveSession {

  private final Path file;
  private final HxtFormat.Header header;
  private final HxtSampleAccumulator capture;
  /** The next unread byte; only COMPLETE records advance it. */
  private long offset;

  private HxtLiveSession(Path file, HxtFormat.Header header) throws ProfilerFormatException {
    this.file = file;
    this.header = header;
    this.capture = new HxtSampleAccumulator(header.tickHz(), header.startStamp());
    this.offset = header.byteLength();
  }

  /** Opens a file whose v1 header is already on disk; records start streaming in via {@link #poll}. */
  @NotNull
  public static HxtLiveSession open(@NotNull Path file) throws IOException {
    try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
      HxtFormat.Header header = HxtFormat.readHeader(new DataInputStream(in));
      if (header.version() != HxtSessionTranslator.SAMPLES_VERSION) {
        throw new ProfilerFormatException("live reading needs a v1 (sampled) session, not version " + header.version());
      }
      return new HxtLiveSession(file, header);
    }
  }

  /** Consumes the complete records appended since the last poll; true when any landed. */
  public boolean poll() throws IOException {
    boolean grew = false;
    try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
      long size = channel.size();
      while (size - offset >= HxtFormat.RECORD_HEADER_BYTES) {
        ByteBuffer recordHeader = ByteBuffer.allocate(HxtFormat.RECORD_HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        readFully(channel, recordHeader, offset);
        int type = recordHeader.get(0) & 0xFF;
        int length = recordHeader.getInt(1);
        long available = size - offset - HxtFormat.RECORD_HEADER_BYTES;
        if (length < 0 || available < length) break; // partial (or corrupt) tail - next poll
        ByteBuffer payload = ByteBuffer.allocate(length);
        readFully(channel, payload, offset + HxtFormat.RECORD_HEADER_BYTES);
        offset += HxtFormat.RECORD_HEADER_BYTES + length;
        if (type != HxtSessionTranslator.FRAME_RECORD) continue;
        try {
          capture.readFrame(payload.array());
          grew = true;
        }
        catch (EOFException corruptRecord) {
          // a record shorter than its fields promises - skip it, keep streaming
        }
      }
    }
    return grew;
  }

  /** The capture accumulated so far; list copies, safe to hand to another thread. */
  @NotNull
  public ProfilerSnapshot snapshot() {
    return capture.snapshot(header.target(), header.version());
  }

  private static void readFully(FileChannel channel, ByteBuffer buffer, long position) throws IOException {
    long at = position;
    while (buffer.hasRemaining()) {
      int read = channel.read(buffer, at);
      if (read < 0) throw new EOFException("session file shrank under the live reader");
      at += read;
    }
  }
}
