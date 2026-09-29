package com.intellij.plugins.haxe.profiler.tracy.connect;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyWelcome;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * The server side of tracy's connection opening: send the 8-byte
 * shibboleth plus the protocol version offered, read the client's one-byte
 * verdict, then its packed welcome message in that version's layout. The
 * client compares versions for equality only, so a refusal surfaces as
 * {@link TracyProtocolMismatchException} for the caller to offer another.
 */
public final class TracyHandshake {

  private static final byte[] SHIBBOLETH = "TracyPrf".getBytes(StandardCharsets.US_ASCII);
  // HandshakeStatus, by wire value
  private static final String[] STATUS_NAMES = {"pending", "welcome", "protocol mismatch", "not available", "dropped"};
  private static final int STATUS_WELCOME = 1;
  private static final int STATUS_PROTOCOL_MISMATCH = 2;

  private TracyHandshake() {
  }

  @NotNull
  public static TracyWelcome perform(@NotNull InputStream in, @NotNull OutputStream out,
                                     @NotNull TracyProtocolVersion version) throws IOException {
    out.write(SHIBBOLETH);
    byte[] wireVersion = new byte[4];
    ByteBuffer.wrap(wireVersion).order(ByteOrder.LITTLE_ENDIAN).putInt(version.wire());
    out.write(wireVersion);
    out.flush();

    int status = in.read();
    if (status == STATUS_PROTOCOL_MISMATCH) throw new TracyProtocolMismatchException(version);
    if (status != STATUS_WELCOME) {
      String name = status >= 0 && status < STATUS_NAMES.length ? STATUS_NAMES[status] : "unknown (" + status + ")";
      throw new ProfilerFormatException("""
        tracy client refused the connection: %s (receiver offered protocol %d)\
        """.formatted(name, version.wire()));
    }

    int welcomeSize = version.format().welcomeSize();
    byte[] welcome = in.readNBytes(welcomeSize);
    if (welcome.length < welcomeSize) {
      throw new ProfilerFormatException("truncated tracy welcome message");
    }
    return version.format().parseWelcome(welcome, version);
  }
}
