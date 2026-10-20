package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildtools.LimeProjects;
import com.intellij.plugins.haxe.v2.diagram.HaxeLibraryIncludeFiles.LibraryInclude;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Library diagram: include files")
public class HaxeLibraryIncludeFilesTest extends HaxeLightFixtureTestCase {

  /**
   * An include file declaring a library twice under exclusive conditions, one
   * it never uses on this target and one by variable, after a condition with
   * the raw {@code <} the tools accept.
   */
  private static final String OPENFL_INCLUDE_XML_SOURCE = """
    <extension>
      <log error="Haxe ${haxe_ver} is too old" if="${haxe_ver < 4.0}" />
      <haxedef name="openfl-native" if="cpp || neko" />
      <haxedef name="openfl-debug" if="openfl-telemetry" />
      <section unless="lime || custom-backend">
        <haxelib name="lime" version="${lime-version}" if="lime-version" />
        <haxelib name="lime" unless="lime-version" />
      </section>
      <haxelib name="hxtelemetry" unless="flash" />
      <haxelib name="${custom-backend}" if="custom-backend" />
    </extension>
    """;
  private static final String LIME_INCLUDE_XML_SOURCE = """
    <extension>
      <haxedef name="lime-native" if="native" />
      <haxedef name="lime-ps4" if="ps4" />
      <haxelib name="lime-ps4" if="ps4" />
    </extension>
    """;
  /** What the build's evaluation uses: openfl and lime, with the native defines. */
  private static final HaxeBuildFileInfo BUILD = new HaxeBuildFileInfo(
    null, null,
    List.of(new HaxeDefine("openfl-native", null), new HaxeDefine("lime-native", null)),
    List.of(new HaxeLibDependency("openfl", null), new HaxeLibDependency("lime", null)),
    List.of());

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("the walk follows include files to the libraries the build uses")
  public void testTheWalkFollowsIncludeFilesToTheLibrariesTheBuildUses() {
    VirtualFile openfl = folderWith("lib/openfl/9,5,2/include.xml", OPENFL_INCLUDE_XML_SOURCE);
    VirtualFile lime = folderWith("lib/lime/8,3,2/include.xml", LIME_INCLUDE_XML_SOURCE);
    Map<String, VirtualFile> roots = Map.of("openfl", openfl, "lime", lime);

    List<LibraryInclude> includes = HaxeLibraryIncludeFiles.walk(List.of("openfl"), roots::get, BUILD);

    List<String> walked = includes.stream().map(LibraryInclude::library).toList();
    assertEquals(List.of("openfl", "lime"), walked);
    assertEquals(List.of(new HaxeLibDependency("lime", null)), includes.getFirst().declares(),
                 "the build does not use hxtelemetry, the variable names no library, and both lime lines are one");
    assertEquals(List.of(new HaxeDefine("openfl-native", null)), includes.getFirst().defines());
    assertEquals(List.of(), includes.get(1).declares(), "lime-ps4 is not part of the build");
    assertEquals(List.of(new HaxeDefine("lime-native", null)), includes.get(1).defines());
  }

  @Test
  @DisplayName("every build walks the extra params files")
  public void testEveryBuildWalksTheExtraParamsFiles() {
    VirtualFile widgets = folderWith("lib/widgets/1,7,0/extraParams.hxml", """
      --macro define('widgets_macro')
      -D widgets
      -lib widgets-core
      """);
    myFixture.addFileToProject("lib/widgets/1,7,0/include.xml", "<extension><haxelib name=\"other\"/></extension>");
    VirtualFile core = folderWith("lib/widgets-core/1,0,0/extraParams.hxml", "-D widgets_core\n");
    Map<String, VirtualFile> roots = Map.of("widgets", widgets, "widgets-core", core);

    // null: an hxml build, whose tool merges no include files
    List<LibraryInclude> includes = HaxeLibraryIncludeFiles.walk(List.of("widgets"), roots::get, null);

    List<String> files = includes.stream().map(include -> include.library() + "/" + include.file().getName()).toList();
    assertEquals(List.of("widgets/extraParams.hxml", "widgets-core/extraParams.hxml"), files);
    assertEquals(List.of(new HaxeLibDependency("widgets-core", null)), includes.getFirst().declares());
    assertEquals(List.of(new HaxeDefine("widgets", null)), includes.getFirst().defines(), "a define the macro sets is the compiler's to know");
  }

  @Test
  @DisplayName("include lime wins over include xml")
  public void testIncludeLimeWinsOverIncludeXml() {
    VirtualFile root = folderWith("lib/pkg/include.xml", "<extension/>");
    myFixture.addFileToProject("lib/pkg/include.lime", "<extension/>");

    VirtualFile include = LimeProjects.libraryIncludeFile(root);

    assertEquals("include.lime", include.getName());
  }

  @Test
  @DisplayName("an include script is listed without declarations")
  public void testAnIncludeScriptIsListedWithoutDeclarations() {
    VirtualFile root = folderWith("lib/pkg/include.hxp", "class Include extends HXProject {}");

    List<LibraryInclude> includes = HaxeLibraryIncludeFiles.walk(List.of("pkg"), library -> root, BUILD);

    List<String> files = includes.stream().map(include -> include.file().getName()).toList();
    assertEquals(List.of("include.hxp"), files);
    assertEquals(List.of(), includes.getFirst().declares());
  }

  /** (include file path, the title of its box; null for a file that is no library include file). */
  static final List<Arguments> TITLES = List.of(
    arguments("C:/HaxeToolkit/haxe/lib/openfl/9,5,2/include.xml", "openfl/include.xml"),
    arguments("C:/HaxeToolkit/haxe/lib/hxparse/git/include.xml", "hxparse/include.xml"),
    // a dev checkout lives in a folder of its own choosing
    arguments("C:/dev/mylib/include.lime", "mylib/include.lime"),
    arguments("C:/dev/mylib/include.hxp", "mylib/include.hxp"),
    arguments("C:/HaxeToolkit/haxe/lib/openfl/9,5,2/extraParams.hxml", "openfl/extraParams.hxml"),
    arguments("C:/project/project.xml", null));

  @ParameterizedTest(name = "{0}")
  @FieldSource("TITLES")
  @DisplayName("an include file is titled with its library folder")
  public void testAnIncludeFileIsTitledWithItsLibraryFolder(String path, String title) {
    assertEquals(title, HaxeLibraryIncludeFiles.titleOf(path));
  }

  private VirtualFile folderWith(String path, String text) {
    return myFixture.addFileToProject(path, text).getVirtualFile().getParent();
  }
}
