package com.intellij.plugins.haxe.hashlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Precedence tests for the HashLink executable resolution: SDK path first,
 * then the HASHLINK_BIN/HASHLINK/HASHLINKPATH environment variables, then
 * PATH. Pure unit test — the environment is injected.
 */
public class HlExecutableResolverTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void sdkPathWinsOverEnvironment() throws IOException {
    Path sdkHl = executableIn("sdk");
    Path envHl = executableIn("env");
    Optional<Path> resolved =
      HlExecutableResolver.resolve(sdkHl.toString(), env(Map.of("HASHLINK_BIN", envHl.toString())));
    assertEquals(sdkHl, resolved.orElseThrow());
  }

  @Test
  public void sdkPathMayBeADirectory() throws IOException {
    Path sdkHl = executableIn("sdkdir");
    Optional<Path> resolved =
      HlExecutableResolver.resolve(sdkHl.getParent().toString(), env(Map.of()));
    assertEquals(sdkHl, resolved.orElseThrow());
  }

  @Test
  public void environmentVariablesAreTriedInOrder() throws IOException {
    Path second = executableIn("second");
    Path third = executableIn("third");
    Optional<Path> resolved = HlExecutableResolver.resolve(null,
      env(Map.of("HASHLINK", second.toString(), "HASHLINKPATH", third.toString())));
    assertEquals("HASHLINK outranks HASHLINKPATH", second, resolved.orElseThrow());
  }

  @Test
  public void fallsBackToPathDirectories() throws IOException {
    Path onPath = executableIn("bin");
    Optional<Path> resolved = HlExecutableResolver.resolve(null,
      env(Map.of("PATH", temp.getRoot().toPath().resolve("empty") + java.io.File.pathSeparator
                          + onPath.getParent())));
    assertEquals(onPath, resolved.orElseThrow());
  }

  @Test
  public void emptyWhenNothingConfigured() {
    assertTrue(HlExecutableResolver.resolve(null, env(Map.of())).isEmpty());
  }

  @Test
  public void blankAndBrokenEntriesAreSkipped() throws IOException {
    Path envHl = executableIn("working");
    Optional<Path> resolved = HlExecutableResolver.resolve("   ",
      env(Map.of("HASHLINK_BIN", temp.getRoot().toPath().resolve("missing").toString(),
                 "HASHLINK", envHl.toString())));
    assertEquals("skips the blank SDK path and the dangling env entry", envHl, resolved.orElseThrow());
  }

  private Path executableIn(String directory) throws IOException {
    Path dir = temp.newFolder(directory).toPath();
    Path exe = dir.resolve("hl.exe");
    Files.createFile(exe);
    return exe;
  }

  private static HlExecutableResolver.Environment env(Map<String, String> values) {
    return values::get;
  }
}
