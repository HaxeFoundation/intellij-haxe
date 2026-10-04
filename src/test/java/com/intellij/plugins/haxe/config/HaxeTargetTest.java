package com.intellij.plugins.haxe.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** The compiler's per-platform defines (haxe 4.3 {@code Common.init_platform} over {@code get_config}). */
@DisplayName("Config: haxe target")
public class HaxeTargetTest {

  /** (target, every define name the compiler sets for it). */
  static final List<Arguments> TARGET_DEFINES = List.of(
    arguments(HaxeTarget.JAVA_SCRIPT, Set.of("js", "target.name", "utf16", "target.utf16", "target.unicode", "target.atomics")),
    arguments(HaxeTarget.LUA, Set.of("lua", "target.name", "sys", "target.sys", "target.unicode")),
    // neko is the one platform without unicode strings
    arguments(HaxeTarget.NEKO, Set.of("neko", "target.name", "sys", "target.sys", "target.threaded")),
    // flash forbids the sys package but is a static platform
    arguments(HaxeTarget.FLASH, Set.of("flash", "target.name", "static", "target.static", "utf16", "target.utf16",
                                       "target.unicode")),
    arguments(HaxeTarget.PHP, Set.of("php", "target.name", "sys", "target.sys", "target.unicode")),
    arguments(HaxeTarget.CPP, Set.of("cpp", "target.name", "static", "target.static", "sys", "target.sys",
                                     "utf16", "target.utf16", "target.threaded", "target.unicode", "target.atomics")),
    // -cppia is the cpp platform plus its own flag
    arguments(HaxeTarget.CPPIA, Set.of("cppia", "cpp", "target.name", "static", "target.static", "sys", "target.sys",
                                       "utf16", "target.utf16", "target.threaded", "target.unicode", "target.atomics")),
    arguments(HaxeTarget.CSHARP, Set.of("cs", "target.name", "static", "target.static", "sys", "target.sys",
                                        "utf16", "target.utf16", "target.threaded", "target.unicode", "target.atomics")),
    arguments(HaxeTarget.JAVA, Set.of("jvm", "java", "target.name", "static", "target.static", "sys", "target.sys",
                                      "utf16", "target.utf16", "target.threaded", "target.unicode", "target.atomics")),
    arguments(HaxeTarget.PYTHON, Set.of("python", "target.name", "sys", "target.sys", "target.threaded", "target.unicode")),
    arguments(HaxeTarget.HL, Set.of("hl", "target.name", "static", "target.static", "sys", "target.sys",
                                    "utf16", "target.utf16", "target.threaded", "target.unicode", "target.atomics")),
    // --interp runs the eval platform and additionally defines interp
    arguments(HaxeTarget.INTERP, Set.of("interp", "eval", "target.name", "sys", "target.sys", "target.threaded",
                                        "target.unicode")));

  @ParameterizedTest(name = "{0}")
  @FieldSource("TARGET_DEFINES")
  @DisplayName("target defines match the compiler platform table")
  public void testTargetDefinesMatchTheCompilerPlatformTable(HaxeTarget target, Set<String> expected) {
    assertEquals(expected, target.getDefines().keySet());
  }

  @Test
  @DisplayName("target name carries the platform name and flags the flag value")
  public void testTargetNameCarriesThePlatformNameAndFlagsTheFlagValue() {
    assertEquals("cpp", HaxeTarget.CPPIA.getDefines().get("target.name"));
    assertEquals("eval", HaxeTarget.INTERP.getDefines().get("target.name"));
    assertEquals(HaxeTarget.FLAG_DEFINE_VALUE, HaxeTarget.CPPIA.getDefines().get("cppia"));
    assertEquals(HaxeTarget.FLAG_DEFINE_VALUE, HaxeTarget.HL.getDefines().get("sys"));
  }
}
