package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.v2.buildtools.HaxeRuntimeTool.Environment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.intellij.plugins.haxe.util.HaxeEnvironmentVariables.HASHLINK_BIN;
import static com.intellij.plugins.haxe.util.HaxeEnvironmentVariables.NEKOPATH;
import static com.intellij.plugins.haxe.util.HaxeEnvironmentVariables.NEKO_INSTPATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Pure unit test: the environment is injected, the executables are empty files under a temporary directory. */
@DisplayName("Build tools: runtime tool table")
public class HaxeRuntimeToolTest {
  @TempDir
  Path temp;

  @Test
  @DisplayName("neko reads its install folder before its search path")
  public void nekoReadsItsInstallFolderBeforeItsSearchPath() throws IOException {
    Path installed = executableIn("install", HaxeRuntimeTool.NEKO);
    Path onSearchPath = executableIn("searchpath", HaxeRuntimeTool.NEKO);
    Environment env = env(Map.of(NEKO_INSTPATH, installed.getParent().toString(), NEKOPATH, onSearchPath.getParent().toString()));

    assertEquals(installed.toString(), HaxeRuntimeTool.NEKO.fromEnvironment(env));
  }

  @Test
  @DisplayName("a search path entry without the executable is skipped")
  public void aSearchPathEntryWithoutTheExecutableIsSkipped() throws IOException {
    Path libraryFolder = Files.createDirectories(temp.resolve("lib").resolve("neko"));
    Path onSearchPath = executableIn("bin", HaxeRuntimeTool.NEKO);
    Environment env = env(Map.of(NEKOPATH, libraryFolder + ";" + onSearchPath.getParent()));

    assertEquals(onSearchPath.toString(), HaxeRuntimeTool.NEKO.fromEnvironment(env));
  }

  @Test
  @DisplayName("the sdk executable wins over the environment and the override over both")
  public void theSdkExecutableWinsOverTheEnvironmentAndTheOverrideOverBoth() throws IOException {
    Path fromSdk = executableIn("sdk", HaxeRuntimeTool.HASHLINK);
    Path fromEnvironment = executableIn("env", HaxeRuntimeTool.HASHLINK);
    Path override = executableIn("override", HaxeRuntimeTool.HASHLINK);
    Environment env = env(Map.of(HASHLINK_BIN, fromEnvironment.toString()));

    assertEquals(fromSdk.toString(), HaxeRuntimeTool.HASHLINK.resolve(null, fromSdk.toString(), env));
    assertEquals(override.toString(), HaxeRuntimeTool.HASHLINK.resolve(override.getParent().toString(), fromSdk.toString(), env));
    assertNull(HaxeRuntimeTool.HASHLINK.resolve(temp.resolve("missing").toString(), fromSdk.toString(), env), "a broken override names nothing");
  }

  @Test
  @DisplayName("a path lookup finds the executable under its name on this os")
  public void aPathLookupFindsTheExecutableUnderItsNameOnThisOs() throws IOException {
    Path onPath = executableIn("path", HaxeRuntimeTool.NODE);
    Environment env = env(Map.of("PATH", temp.resolve("empty") + File.pathSeparator + onPath.getParent()));

    assertEquals(onPath.toAbsolutePath().toString(), HaxeRuntimeTool.NODE.fromPath(env));
    assertNull(HaxeRuntimeTool.NODE.fromPath(env(Map.of())), "no PATH, no lookup");
  }

  private Path executableIn(String directory, HaxeRuntimeTool tool) throws IOException {
    Path dir = Files.createDirectories(temp.resolve(directory));
    Path executable = Files.createFile(dir.resolve(tool.executableName()));
    // a PATH lookup takes only executable files; a new file is one on windows alone
    executable.toFile().setExecutable(true);
    return executable;
  }

  private static Environment env(Map<String, String> values) {
    return values::get;
  }
}
