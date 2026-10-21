package com.intellij.plugins.haxe.runner.debugger.hashlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import com.intellij.plugins.haxe.v2.buildtools.HaxeRuntimeTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The HashLink executable resolution beyond what {@code HaxeRuntimeToolTest}
 * pins for the override / SDK / environment precedence: an SDK path naming a
 * directory, the HASHLINK_BIN/HASHLINK/HASHLINKPATH order, the PATH fallback
 * and blank or dangling entries. Pure unit test — the environment is injected.
 */
@DisplayName("Debugger: hl executable resolver")
public class HlExecutableResolverTest {
  @TempDir
  Path temp;

  @Test
  @DisplayName("sdk path may be a directory")
  public void sdkPathMayBeADirectory() throws IOException {
    Path sdkHl = executableIn("sdkdir");
    Optional<Path> resolved =
      HlExecutableResolver.resolve(sdkHl.getParent().toString(), env(Map.of()));
    assertEquals(sdkHl, resolved.orElseThrow());
  }

  @Test
  @DisplayName("environment variables are tried in order")
  public void environmentVariablesAreTriedInOrder() throws IOException {
    Path second = executableIn("second");
    Path third = executableIn("third");
    Optional<Path> resolved = HlExecutableResolver.resolve(null,
      env(Map.of("HASHLINK", second.toString(), "HASHLINKPATH", third.toString())));
    assertEquals(second, resolved.orElseThrow(), "HASHLINK outranks HASHLINKPATH");
  }

  @Test
  @DisplayName("falls back to path directories")
  public void fallsBackToPathDirectories() throws IOException {
    Path onPath = executableIn("bin");
    Optional<Path> resolved = HlExecutableResolver.resolve(null,
      env(Map.of("PATH", temp.resolve("empty") + java.io.File.pathSeparator
                          + onPath.getParent())));
    assertEquals(onPath, resolved.orElseThrow());
  }

  @Test
  @DisplayName("empty when nothing configured")
  public void emptyWhenNothingConfigured() {
    assertTrue(HlExecutableResolver.resolve(null, env(Map.of())).isEmpty());
  }

  @Test
  @DisplayName("blank and broken entries are skipped")
  public void blankAndBrokenEntriesAreSkipped() throws IOException {
    Path envHl = executableIn("working");
    Optional<Path> resolved = HlExecutableResolver.resolve("   ",
      env(Map.of("HASHLINK_BIN", temp.resolve("missing").toString(),
                 "HASHLINK", envHl.toString())));
    assertEquals(envHl, resolved.orElseThrow(), "skips the blank SDK path and the dangling env entry");
  }

  private Path executableIn(String directory) throws IOException {
    Path dir = Files.createDirectories(temp.resolve(directory));
    Path exe = dir.resolve(HaxeRuntimeTool.HASHLINK.executableName());
    Files.createFile(exe);
    // a PATH lookup takes only executable files; a new file is one on windows alone
    exe.toFile().setExecutable(true);
    return exe;
  }

  private static HaxeRuntimeTool.Environment env(Map<String, String> values) {
    return values::get;
  }
}
