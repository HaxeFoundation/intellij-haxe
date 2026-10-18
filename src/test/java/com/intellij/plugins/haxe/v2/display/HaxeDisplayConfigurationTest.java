package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.display.HaxeDisplayConfiguration.OverrideArguments;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Compiler services: display configuration")
public class HaxeDisplayConfigurationTest {

  @Test
  @DisplayName("set overrides append as later defines")
  public void testSetOverridesAppendAsLaterDefines() {
    List<String> base = List.of("--cwd", "dir", "build.hxml");
    OverrideArguments overrides = new OverrideArguments(Set.of(), List.of("-D", "console=ps4", "-D", "debug_hud"), null);

    List<String> applied = HaxeDisplayConfiguration.applyOverrides(base, overrides);

    assertEquals(List.of("--cwd", "dir", "build.hxml", "-D", "console=ps4", "-D", "debug_hud"), applied);
  }

  @Test
  @DisplayName("empty overrides leave the arguments untouched")
  public void testEmptyOverridesLeaveTheArgumentsUntouched() {
    List<String> base = List.of("--cwd", "dir", "build.hxml");

    assertSame(base, HaxeDisplayConfiguration.applyOverrides(base, OverrideArguments.EMPTY));
  }

  @Test
  @DisplayName("remove strips both define spellings and valued defines")
  public void testRemoveStripsBothDefineSpellingsAndValuedDefines() {
    List<String> base = List.of("-D", "alpha", "-D", "beta=2", "--define", "gamma=x", "-cp", "src");
    OverrideArguments overrides = new OverrideArguments(Set.of("alpha", "gamma"), List.of(), null);

    List<String> applied = HaxeDisplayConfiguration.applyOverrides(base, overrides);

    assertEquals(List.of("-D", "beta=2", "-cp", "src"), applied);
  }

  /** The compiler defines my_flag for -D my-flag, the name the editor greys branches under; a remove in either spelling strips the define from the server's arguments. */
  @Test
  @DisplayName("remove matches the compiler name of a dashed define")
  public void testRemoveMatchesTheCompilerNameOfADashedDefine() {
    List<String> base = List.of("-D", "my-flag", "-D", "other-flag=1", "-main", "Main");
    OverrideArguments overrides = new OverrideArguments(Set.of("my_flag", "other-flag"), List.of(), null);

    List<String> applied = HaxeDisplayConfiguration.applyOverrides(base, overrides);

    assertEquals(List.of("-main", "Main"), applied);
  }

  @Test
  @DisplayName("removing debug strips the debug flag")
  public void testRemovingDebugStripsTheDebugFlag() {
    List<String> base = List.of("-cp", "src", "-debug", "--debug", "-D", "debug");
    OverrideArguments overrides = new OverrideArguments(Set.of("debug"), List.of(), null);

    List<String> applied = HaxeDisplayConfiguration.applyOverrides(base, overrides);

    assertEquals(List.of("-cp", "src"), applied);
  }

  @Test
  @DisplayName("remove reaches a define nested two includes down")
  public void testRemoveReachesADefineNestedTwoIncludesDown(@TempDir Path directory) throws IOException {
    Files.writeString(directory.resolve("build.hxml"), """
      common.hxml
      -main Main
      """);
    Files.writeString(directory.resolve("common.hxml"), """
      defines.hxml
      -cp src
      """);
    Files.writeString(directory.resolve("defines.hxml"), "-D legacy_api\n");
    List<String> base = List.of("--cwd", directory.toString(), "build.hxml");
    OverrideArguments overrides = new OverrideArguments(Set.of("legacy_api"), List.of(), null);

    List<String> applied = HaxeDisplayConfiguration.applyOverrides(base, overrides);

    assertEquals(List.of("--cwd", directory.toString(), "-cp", "src", "-main", "Main"), applied);
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
    OverrideArguments overrides = new OverrideArguments(Set.of("legacy_api"), List.of("-D", "modern_api"), null);

    List<String> applied = HaxeDisplayConfiguration.applyOverrides(base, overrides);

    assertFalse(applied.contains("legacy_api"), "removed define must not survive expansion, got: " + applied);
    assertTrue(applied.containsAll(List.of("-cp", "src", "-main", "Main")), "expanded hxml flags expected, got: " + applied);
    assertTrue(applied.containsAll(List.of("-D", "modern_api")), "set override appended, got: " + applied);
    assertFalse(applied.contains("build.hxml"), "the hxml reference is replaced by its expansion, got: " + applied);
  }

  @Test
  @DisplayName("a custom target is appended after the defines")
  public void testACustomTargetIsAppendedAfterTheDefines() {
    List<String> base = List.of("--cwd", "dir", "extraParams.hxml");
    OverrideArguments overrides = new OverrideArguments(Set.of(), List.of("-D", "hx2go"), "go");

    List<String> applied = HaxeDisplayConfiguration.applyOverrides(base, overrides);

    assertEquals(List.of("--cwd", "dir", "extraParams.hxml", "-D", "hx2go", "--custom-target", "go"), applied);
  }

  /** (the custom target analysis uses, the container's compiler, what is forwarded). */
  static final List<Arguments> CUSTOM_TARGET_FORWARDING = List.of(
    arguments("go", HaxeLanguageLevel.HAXE_5_0, "go"),
    // Haxe 4 has no --custom-target flag
    arguments("go", HaxeLanguageLevel.HAXE_4_3, null),
    // no registered SDK: the version is unknown, nothing is risked
    arguments("go", null, null),
    arguments(null, HaxeLanguageLevel.HAXE_5_0, null));

  @ParameterizedTest(name = "{0} on {1}")
  @FieldSource("CUSTOM_TARGET_FORWARDING")
  @DisplayName("custom target to forward")
  public void testCustomTargetToForward(String customTarget, HaxeLanguageLevel compiler, String forwarded) {
    assertEquals(forwarded, HaxeDisplayConfiguration.customTargetToForward(customTarget, compiler));
  }

  @Test
  @DisplayName("a custom target separates server contexts")
  public void testACustomTargetSeparatesServerContexts() {
    OverrideArguments withoutTarget = new OverrideArguments(Set.of(), List.of("-D", "b"), null);
    OverrideArguments withTarget = new OverrideArguments(Set.of(), List.of("-D", "b"), "go");

    assertNotEquals(withoutTarget.signature(), withTarget.signature());
    assertFalse(new OverrideArguments(Set.of(), List.of(), "go").isEmpty());
  }

  @Test
  @DisplayName("override signatures separate server contexts")
  public void testOverrideSignaturesSeparateServerContexts() {
    OverrideArguments first = new OverrideArguments(Set.of("a"), List.of("-D", "b"), null);
    OverrideArguments second = new OverrideArguments(Set.of("a"), List.of("-D", "c"), null);

    assertNotEquals(first.signature(), second.signature());
    assertTrue(OverrideArguments.EMPTY.signature().isEmpty());
  }
}
