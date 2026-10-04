package com.intellij.plugins.haxe.buildsystem;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.buildsystem.lime.LimeXmlFileDescription;
import com.intellij.plugins.haxe.buildsystem.lime.OpenflXmlFileDescription;
import com.intellij.plugins.haxe.buildsystem.nmml.NMMLFileDescription;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.icons.RowIcon;
import com.intellij.util.xml.DomFileDescription;
import com.intellij.util.xml.DomManager;
import icons.HaxeIcons;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import javax.swing.Icon;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build system: project xml file description")
public class ProjectXmlFileDescriptionTest extends HaxeCodeInsightFixtureTestCase {

  static final String LIME_PROJECT_SOURCE = """
    <project>
      <app main="Main" path="export" file="Shapes"/>
      <source path="src"/>
      <haxelib name="lime"/>
    </project>
    """;

  static final String OPENFL_PROJECT_SOURCE = """
    <project>
      <meta title="Shapes"/>
      <app main="Main"/>
      <source path="src"/>
      <haxelib name="openfl"/>
    </project>
    """;

  static final String CLASSPATH_ONLY_PROJECT_SOURCE = """
    <project>
      <app main="Main"/>
      <classpath path="src"/>
    </project>
    """;

  static final String NME_SAMPLE_PROJECT_SOURCE = """
    <project>
      <app main="Main" file="Sample"/>
      <window width="800" height="600"/>
    </project>
    """;

  static final String MAVEN_POM_SOURCE = """
    <project>
      <modelVersion>4.0.0</modelVersion>
      <artifactId>shapes</artifactId>
    </project>
    """;

  /** (file name, source text, the description claiming the file - null for none, its file icon). */
  static final List<Arguments> CLAIMS = List.of(
    arguments("lime-project.xml", LIME_PROJECT_SOURCE, LimeXmlFileDescription.class, HaxeIcons.LIME_LOGO),
    arguments("openfl-project.xml", OPENFL_PROJECT_SOURCE, OpenflXmlFileDescription.class, HaxeIcons.OPENFL_LOGO),
    // lime's own extension classifies like xml
    arguments("lime-project.lime", LIME_PROJECT_SOURCE, LimeXmlFileDescription.class, HaxeIcons.LIME_LOGO),
    arguments("openfl-project.lime", OPENFL_PROJECT_SOURCE, OpenflXmlFileDescription.class, HaxeIcons.OPENFL_LOGO),
    // <classpath> is lime's alias of <source>
    arguments("classpath-only.xml", CLASSPATH_ONLY_PROJECT_SOURCE, LimeXmlFileDescription.class, HaxeIcons.LIME_LOGO),
    arguments("sample.nmml", NME_SAMPLE_PROJECT_SOURCE, NMMLFileDescription.class, HaxeIcons.NMML_LOGO),
    // the extension picks NME even when the project declares openfl
    arguments("openfl-project.nmml", OPENFL_PROJECT_SOURCE, NMMLFileDescription.class, HaxeIcons.NMML_LOGO),
    // a maven-shaped <project> (under a name the maven plugin does not claim) is nobody's
    arguments("settings.xml", MAVEN_POM_SOURCE, null, null));

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @ParameterizedTest(name = "{0} -> {2}")
  @FieldSource("CLAIMS")
  public void testClaimsTheFileAndCarriesItsIcon(String fileName, String source, Class<?> expectedDescription, Icon expectedIcon) {
    XmlFile file = (XmlFile)myFixture.addFileToProject(fileName, source);

    DomFileDescription<?> description = DomManager.getDomManager(getProject()).getDomFileDescription(file);

    Class<?> descriptionClass = description == null ? null : description.getClass();
    Icon icon = description == null ? null : description.getFileIcon(0);
    assertEquals(expectedDescription, descriptionClass);
    assertEquals(expectedIcon, icon);
  }

  /** The platform restores a file's description from its root element class name, so each kind needs its own. */
  @Test
  @DisplayName("each description has its own root element class")
  public void testEachDescriptionHasItsOwnRootElementClass() {
    List<Class<?>> rootClasses = List.of(new LimeXmlFileDescription().getRootElementClass(),
                                         new OpenflXmlFileDescription().getRootElementClass(),
                                         new NMMLFileDescription().getRootElementClass());

    assertEquals(rootClasses.size(), Set.copyOf(rootClasses).size());
  }

  @Test
  @DisplayName("project view icon follows the description")
  public void testProjectViewIconFollowsTheDescription() {
    XmlFile openfl = (XmlFile)myFixture.addFileToProject("project.xml", OPENFL_PROJECT_SOURCE);
    XmlFile lime = (XmlFile)myFixture.addFileToProject("project.lime", LIME_PROJECT_SOURCE);

    assertEquals(HaxeIcons.OPENFL_LOGO, fileTypeLayerOf(openfl.getIcon(0)));
    assertEquals(HaxeIcons.LIME_LOGO, fileTypeLayerOf(lime.getIcon(0)));
  }

  /** The file-type layer of a Project-view icon: the platform wraps it in a RowIcon, and in a LayeredIcon when markers apply. */
  private static Icon fileTypeLayerOf(Icon icon) {
    if (icon instanceof RowIcon row) return fileTypeLayerOf(row.getIcon(0));
    if (icon instanceof LayeredIcon layered) return fileTypeLayerOf(layered.getIcon(0));
    return icon;
  }
}
