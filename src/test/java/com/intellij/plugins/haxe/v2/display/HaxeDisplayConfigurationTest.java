package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.v2.display.HaxeDisplayConfiguration.DefineOverrides;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Compiler services: display configuration")
public class HaxeDisplayConfigurationTest {

  @Test
  @DisplayName("set overrides append as later defines")
  public void testSetOverridesAppendAsLaterDefines() {
    List<String> base = List.of("--cwd", "dir", "build.hxml");
    DefineOverrides overrides = new DefineOverrides(Set.of(), List.of("-D", "console=ps4", "-D", "debug_hud"));

    List<String> applied = HaxeDisplayConfiguration.applyOverrides(base, overrides);

    assertEquals(List.of("--cwd", "dir", "build.hxml", "-D", "console=ps4", "-D", "debug_hud"), applied);
  }

  @Test
  @DisplayName("empty overrides leave the arguments untouched")
  public void testEmptyOverridesLeaveTheArgumentsUntouched() {
    List<String> base = List.of("--cwd", "dir", "build.hxml");

    assertSame(base, HaxeDisplayConfiguration.applyOverrides(base, DefineOverrides.EMPTY));
  }

  @Test
  @DisplayName("remove strips both define spellings and valued defines")
  public void testRemoveStripsBothDefineSpellingsAndValuedDefines() {
    List<String> base = List.of("-D", "alpha", "-D", "beta=2", "--define", "gamma=x", "-cp", "src");
    DefineOverrides overrides = new DefineOverrides(Set.of("alpha", "gamma"), List.of());

    List<String> applied = HaxeDisplayConfiguration.applyOverrides(base, overrides);

    assertEquals(List.of("-D", "beta=2", "-cp", "src"), applied);
  }

  @Test
  @DisplayName("remove expands an hxml reference to reach its defines")
  public void testRemoveExpandsAnHxmlReferenceToReachItsDefines(@TempDir Path directory) throws IOException {
    Files.writeString(directory.resolve("build.hxml"), """
      -cp src
      -D legacy_api
      -main Main
      """);
    List<String> base = List.of("--cwd", directory.toString(), "build.hxml");
    DefineOverrides overrides = new DefineOverrides(Set.of("legacy_api"), List.of("-D", "modern_api"));

    List<String> applied = HaxeDisplayConfiguration.applyOverrides(base, overrides);

    assertFalse(applied.contains("legacy_api"), "removed define must not survive expansion, got: " + applied);
    assertTrue(applied.containsAll(List.of("-cp", "src", "-main", "Main")), "expanded hxml flags expected, got: " + applied);
    assertTrue(applied.containsAll(List.of("-D", "modern_api")), "set override appended, got: " + applied);
    assertFalse(applied.contains("build.hxml"), "the hxml reference is replaced by its expansion, got: " + applied);
  }

  @Test
  @DisplayName("override signatures separate server contexts")
  public void testOverrideSignaturesSeparateServerContexts() {
    DefineOverrides first = new DefineOverrides(Set.of("a"), List.of("-D", "b"));
    DefineOverrides second = new DefineOverrides(Set.of("a"), List.of("-D", "c"));

    assertNotEquals(first.signature(), second.signature());
    assertTrue(DefineOverrides.EMPTY.signature().isEmpty());
  }
}
