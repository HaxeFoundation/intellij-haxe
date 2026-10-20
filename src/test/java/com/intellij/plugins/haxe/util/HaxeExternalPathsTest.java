package com.intellij.plugins.haxe.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Security: external paths")
public class HaxeExternalPathsTest {

  /** (text, whether it names a usable path at all). */
  static final List<Arguments> USABLE_PATHS = List.of(
    arguments("src/main", true),
    arguments("C:/HaxeToolkit/haxe/lib/lime/8,1,2/src/", true),
    // network shares, in both separator styles
    arguments("\\\\server\\share\\lib", false),
    arguments("//server/share/lib", false),
    arguments("lib\nsrc", false),
    arguments("lib\u0000src", false),
    arguments("   ", false),
    arguments(null, false));

  @ParameterizedTest(name = "[{index}] {0}")
  @FieldSource("USABLE_PATHS")
  @DisplayName("a path is usable unless blank broken or on a network share")
  public void aPathIsUsableUnlessBlankBrokenOrOnANetworkShare(String text, boolean usable) {
    assertEquals(usable, HaxeExternalPaths.pathOf(text) != null);
  }

  /** (text, whether it names an absolute path). */
  static final List<Arguments> ABSOLUTE_PATHS = List.of(
    arguments("C:/HaxeToolkit/haxe/lib/lime/8,1,2/src/", true),
    arguments("C:\\HaxeToolkit\\haxe\\lib\\lime\\8,1,2\\src\\", true),
    arguments("/home/username/haxelib/lime/8,1,2/src/", true),
    arguments("src/main", false),
    // the notice a newer haxelib prints among its classpaths
    arguments("Warning: Repository requires reformatting. To reformat, run `haxelib fixrepo`.", false),
    arguments("Error: Library nosuchlib is not installed", false),
    arguments("\\\\server\\share\\lib", false));

  @ParameterizedTest(name = "[{index}] {0}")
  @FieldSource("ABSOLUTE_PATHS")
  @DisplayName("an absolute path names a drive or the root on any os")
  public void anAbsolutePathNamesADriveOrTheRootOnAnyOs(String text, boolean absolute) {
    assertEquals(absolute, HaxeExternalPaths.absolutePathOf(text) != null);
  }
}
