package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.codeInsight.daemon.impl.analysis.XmlUnboundNsPrefixInspection;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

  /** (file name, source text) - config elements wherever lime reads them; any other prefix stays unbound, an unknown element stays invalid. */
  static final List<Arguments> CONFIG_ELEMENT_FILES = List.of(
    arguments("project.xml", """
      <project>
        <haxelib name="openfl"/>
        <config:air sdk-version="32.0"/>
        <config:android permission="android.permission.INTERNET" if="android"/>
        <config:ios>
          <deployment value="12.0"/>
        </config:ios>
        <config:hxtelemetry host="127.0.0.1"/>
        <section if="html5">
          <config:html5 dependency-path="lib"/>
        </section>
        <<error descr="Element other:element is not allowed here"><error descr="Namespace 'other' is not bound">other</error>:element</error>/>
        <<error descr="Element bogus is not allowed here">bogus</error>/>
      </project>
      """),
    arguments("include.xml", """
      <extension>
        <config:android target-sdk-version="29"/>
      </extension>
      """));

  @ParameterizedTest(name = "{0}")
  @FieldSource("CONFIG_ELEMENT_FILES")
  @DisplayName("config elements validate")
  public void testConfigElementsValidate(String fileName, String source) {
    myFixture.enableInspections(new XmlUnboundNsPrefixInspection());
    myFixture.configureByText(fileName, source);

    myFixture.checkHighlighting();
  }

  @Test
  @DisplayName("a config prefix completes the config sections")
  public void testAConfigPrefixCompletesTheConfigSections() {
    myFixture.configureByText("project.xml", """
      <project>
        <haxelib name="openfl"/>
        <config:<caret>
      </project>
      """);

    myFixture.completeBasic();

    List<String> sections = myFixture.getLookupElementStrings();
    assertNotNull(sections);
    boolean offered = sections.containsAll(List.of("air", "android", "ios"));
    assertTrue(offered, sections.toString());
    assertFalse(sections.contains("app"), sections.toString());
  }
}
