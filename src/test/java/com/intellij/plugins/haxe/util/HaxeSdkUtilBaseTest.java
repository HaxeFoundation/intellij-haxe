package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SDK: sdk util base")
public class HaxeSdkUtilBaseTest {

  private static final String SDK_HOME = "/opt/haxe";

  @Test
  @DisplayName("patch environment extends the PATH entry whatever its spelling")
  public void testPatchEnvironmentExtendsThePathEntryWhateverItsSpelling() {
    Map<String, String> env = new HashMap<>(Map.of("Path", "/usr/bin"));

    Map<String, String> patched = HaxeSdkUtilBase.patchEnvironment(env, new HaxeSdkData(SDK_HOME, "4.3.7"));

    assertEquals(Set.of("Path"), patched.keySet(), "the entry keeps its spelling");
    assertTrue(patched.get("Path").startsWith(SDK_HOME + File.pathSeparator), patched.toString());
    assertTrue(patched.get("Path").endsWith(File.pathSeparator + "/usr/bin"), patched.toString());
  }

  @Test
  @DisplayName("patch environment without a PATH entry adds the SDK folders alone")
  public void testPatchEnvironmentWithoutAPathEntryAddsTheSdkFoldersAlone() {
    Map<String, String> patched = HaxeSdkUtilBase.patchEnvironment(new HashMap<>(), new HaxeSdkData(SDK_HOME, "4.3.7"));

    String path = patched.values().iterator().next();
    assertEquals(1, patched.size(), patched.toString());
    assertFalse(path.contains("null"), path);
    assertTrue(path.startsWith(SDK_HOME), path);
  }
}
