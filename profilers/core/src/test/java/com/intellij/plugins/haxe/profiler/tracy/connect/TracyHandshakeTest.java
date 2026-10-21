package com.intellij.plugins.haxe.profiler.tracy.connect;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyWelcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("tracy receiver: handshake")
public class TracyHandshakeTest {

  @Test
  @DisplayName("parses the live captured welcome message")
  public void testParsesTheLiveCapturedWelcomeMessage() throws IOException {
    TracyWelcome welcome = TracyProtocolVersion.V74.format().parseWelcome(welcomeFixture(), TracyProtocolVersion.V74);

    assertEquals("ProfPump-debug.exe", welcome.programName());
    assertTrue(welcome.timerMul() > 0.01 && welcome.timerMul() < 100, "ticks-to-ns factor: " + welcome.timerMul());
    assertTrue(welcome.pid() > 0);
    assertTrue(welcome.epoch() > 1_700_000_000L, "unix start time: " + welcome.epoch());
  }

  @Test
  @DisplayName("perform sends the shibboleth and version then reads the welcome")
  public void testPerformSendsTheShibbolethAndVersionThenReadsTheWelcome() throws IOException {
    byte[] fixture = welcomeFixture();
    byte[] fromClient = new byte[1 + fixture.length];
    fromClient[0] = 1; // HandshakeWelcome
    System.arraycopy(fixture, 0, fromClient, 1, fixture.length);
    ByteArrayOutputStream toClient = new ByteArrayOutputStream();

    TracyWelcome welcome = TracyHandshake.perform(new ByteArrayInputStream(fromClient), toClient, TracyProtocolVersion.V74);

    byte[] sent = toClient.toByteArray();
    assertEquals("TracyPrf", new String(Arrays.copyOf(sent, 8), StandardCharsets.US_ASCII));
    assertEquals(74, sent[8] & 0xFF | (sent[9] & 0xFF) << 8);
    assertEquals(12, sent.length);
    assertEquals("ProfPump-debug.exe", welcome.programName());
    assertEquals(TracyProtocolVersion.V74, welcome.protocolVersion());
  }

  @Test
  @DisplayName("a v76 welcome has no delay field")
  public void testAV76WelcomeHasNoDelayField() throws IOException {
    byte[] v74 = welcomeFixture();
    // v76 dropped the u64 delay that sat after initEnd (offset 24)
    byte[] v76 = new byte[v74.length - 8];
    System.arraycopy(v74, 0, v76, 0, 24);
    System.arraycopy(v74, 32, v76, 24, v74.length - 32);
    assertEquals(TracyProtocolVersion.V76.format().welcomeSize(), v76.length);
    TracyWelcome reference = TracyProtocolVersion.V74.format().parseWelcome(v74, TracyProtocolVersion.V74);

    TracyWelcome welcome = TracyProtocolVersion.V76.format().parseWelcome(v76, TracyProtocolVersion.V76);

    assertEquals("ProfPump-debug.exe", welcome.programName());
    assertEquals(reference.pid(), welcome.pid());
    assertEquals(reference.epoch(), welcome.epoch());
    assertEquals(reference.timerMul(), welcome.timerMul());
    assertEquals(0, welcome.delay());
  }

  @Test
  @DisplayName("a protocol mismatch is its own failure naming the offered version")
  public void testAProtocolMismatchIsItsOwnFailureNamingTheOfferedVersion() {
    ByteArrayInputStream fromClient = new ByteArrayInputStream(new byte[]{2}); // HandshakeProtocolMismatch

    TracyProtocolMismatchException failure = assertThrows(TracyProtocolMismatchException.class,
      () -> TracyHandshake.perform(fromClient, new ByteArrayOutputStream(), TracyProtocolVersion.V76));
    assertEquals(TracyProtocolVersion.V76, failure.offered());
  }

  @Test
  @DisplayName("other refusals fail loudly with the verdict name")
  public void testOtherRefusalsFailLoudlyWithTheVerdictName() {
    ByteArrayInputStream fromClient = new ByteArrayInputStream(new byte[]{3}); // HandshakeNotAvailable

    ProfilerFormatException failure = assertThrows(ProfilerFormatException.class,
      () -> TracyHandshake.perform(fromClient, new ByteArrayOutputStream(), TracyProtocolVersion.V74));
    assertTrue(failure.getMessage().contains("not available"), failure.getMessage());
  }

  private static byte[] welcomeFixture() throws IOException {
    try (InputStream in = TracyHandshakeTest.class.getResourceAsStream("/tracy/welcome-v74.bin")) {
      return in.readAllBytes();
    }
  }
}
