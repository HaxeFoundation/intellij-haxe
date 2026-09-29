package com.intellij.plugins.haxe.profiler.tracy;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * A zone's origin. hxcpp fills {@code function} with the haxe qualified
 * name ({@code ProfPump.burn}) and {@code file} with the .hx path, so it
 * maps directly onto the resolver's symbol conventions.
 */
public record TracySourceLocation(@NotNull String function, @NotNull String file, int line, int color) {

  /**
   * Parses the SourceLocationPayload blob (its 2-byte length prefix already
   * stripped by the client): u32 color, u32 line, function\0, file[\0 name].
   */
  @NotNull
  static TracySourceLocation parse(byte @NotNull [] blob) throws IOException {
    if (blob.length < 8) throw new ProfilerFormatException("source location payload too short: " + blob.length);
    ByteBuffer header = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN);
    int color = header.getInt(0);
    int line = header.getInt(4);

    int functionEnd = 8;
    while (functionEnd < blob.length && blob[functionEnd] != 0) functionEnd++;
    if (functionEnd >= blob.length) throw new ProfilerFormatException("unterminated source location function name");
    String function = new String(blob, 8, functionEnd - 8, StandardCharsets.UTF_8);

    int fileStart = functionEnd + 1;
    int fileEnd = fileStart;
    while (fileEnd < blob.length && blob[fileEnd] != 0) fileEnd++;
    String file = new String(blob, fileStart, fileEnd - fileStart, StandardCharsets.UTF_8);

    return new TracySourceLocation(function, file, line, color);
  }

}
