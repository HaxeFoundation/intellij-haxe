package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.psi.xml.XmlFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static com.intellij.plugins.haxe.buildsystem.ProjectXmlFixtures.CLASSPATH_ONLY_PROJECT_SOURCE;
import static com.intellij.plugins.haxe.buildsystem.ProjectXmlFixtures.LIBRARY_INCLUDE_SOURCE;
import static com.intellij.plugins.haxe.buildsystem.ProjectXmlFixtures.MAVEN_POM_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build system: lime xml schema provider")
public class LimeXmlSchemaProviderTest extends HaxeLightFixtureTestCase {

  /** An include file using elements lime reads that the published schema lacks: a template folder and a module of classes. */
  private static final String MODULE_INCLUDE_XML_SOURCE = """
    <extension>
      <templatePath name="templates"/>
      <module name="std" if="html5">
        <source path="src" package="haxe.ds" exclude="haxe.ds.StringMap"/>
        <class name="haxe.Log"/>
        <include type="haxe.Timer"/>
      </module>
    </extension>
    """;

  @Override
  protected String getBasePath() {
    // every fixture is inline
    return "";
  }

  /** (file path, source text, whether the lime schema applies). */
  static final List<Arguments> SCHEMA_FILES = List.of(
    arguments("project.xml", CLASSPATH_ONLY_PROJECT_SOURCE, true),
    // a library's include file by its name, under either root lime reads
    arguments("lib/include.xml", "<extension/>", true),
    arguments("lib/include.lime", "<project/>", true),
    // under another name, an <extension> root declaring a library or a source path
    arguments("lib/extras.xml", LIBRARY_INCLUDE_SOURCE, true),
    // another tool's <extension> manifest declares neither
    arguments("manifest.xml", "<extension><files folder=\"site\"/></extension>", false),
    arguments("pom.xml", MAVEN_POM_SOURCE, false));

  @ParameterizedTest(name = "{0} -> {2}")
  @FieldSource("SCHEMA_FILES")
  @DisplayName("offers the schema to lime files")
  public void testOffersTheSchemaToLimeFiles(String path, String source, boolean expected) {
    XmlFile file = (XmlFile)myFixture.addFileToProject(path, source);

    assertEquals(expected, new LimeXmlSchemaProvider().isAvailable(file));
  }

  @Test
  @DisplayName("an include file completes the project elements")
  public void testAnIncludeFileCompletesTheProjectElements() {
    myFixture.configureByText("include.xml", """
      <extension>
        <<caret>
      </extension>
      """);

    myFixture.completeBasic();

    List<String> elements = myFixture.getLookupElementStrings();
    assertNotNull(elements);
    boolean offered = elements.containsAll(List.of("haxelib", "module", "templatePath"));
    assertTrue(offered, elements.toString());
  }

  @Test
  @DisplayName("module and template path elements validate")
  public void testModuleAndTemplatePathElementsValidate() {
    myFixture.configureByText("include.xml", MODULE_INCLUDE_XML_SOURCE);

    myFixture.checkHighlighting();
  }
}
