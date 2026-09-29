package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Build system: project xml parser")
public class ProjectXmlParserTest {

  @Test
  @DisplayName("collects haxelibs and defines")
  public void collectsHaxelibsAndDefines() {
    HaxeBuildFileInfo info = ProjectXmlParser.parse("""
      <?xml version="1.0" encoding="utf-8"?>
      <project>
        <meta title="Example" package="com.example.app"/>
        <haxelib name="openfl" version="9.2.0"/>
        <haxelib name="actuate"/>
        <haxedef name="no-traces"/>
        <define name="fdb" if="debug"/>
        <haxedef name="source-map" value="on"/>
      </project>
      """);

    assertNull(info.target());
    assertEquals(List.of(new HaxeLibDependency("openfl", "9.2.0"), new HaxeLibDependency("actuate", null)),
                 info.libraries());
    assertEquals(List.of(new HaxeDefine("no-traces", null), new HaxeDefine("fdb", null), new HaxeDefine("source-map", "on")),
                 info.defines());
  }

  @Test
  @DisplayName("empty version attribute becomes null")
  public void emptyVersionAttributeBecomesNull() {
    HaxeBuildFileInfo info = ProjectXmlParser.parse("<project><haxelib name=\"lime\" version=\"\"/></project>");
    assertEquals(List.of(new HaxeLibDependency("lime", null)), info.libraries());
  }

  @Test
  @DisplayName("malformed xml keeps entries parsed so far")
  public void malformedXmlKeepsEntriesParsedSoFar() {
    HaxeBuildFileInfo info = ProjectXmlParser.parse("""
      <project>
        <haxelib name="openfl"/>
        <haxelib name="broken"
      """);
    assertEquals(List.of(new HaxeLibDependency("openfl", null)), info.libraries());
  }

  @Test
  @DisplayName("parses app file attribute")
  public void parsesAppFileAttribute() {
    String appFile = ProjectXmlParser.parseAppFile("""
      <?xml version="1.0" encoding="utf-8"?>
      <project>
        <meta title="Nyan Cat"/>
        <app main="Main" path="Export" file="NyanCat"/>
      </project>
      """);
    assertEquals("NyanCat", appFile);
  }

  @Test
  @DisplayName("app without file attribute yields null")
  public void appWithoutFileAttributeYieldsNull() {
    assertNull(ProjectXmlParser.parseAppFile("<project><app main=\"Main\" path=\"Export\"/></project>"));
  }

  @Test
  @DisplayName("collects source and classpath elements")
  public void collectsSourceAndClasspathElements() {
    HaxeBuildFileInfo info = ProjectXmlParser.parse("""
      <?xml version="1.0" encoding="utf-8"?>
      <project>
        <source path="src"/>
        <source path="gen"/>
        <classpath name="legacy-src"/>
      </project>
      """);
    assertEquals(List.of("src", "gen", "legacy-src"), info.classpaths());
  }

  @Test
  @DisplayName("parses app path attribute")
  public void parsesAppPathAttribute() {
    String appPath = ProjectXmlParser.parseAppPath("""
      <?xml version="1.0" encoding="utf-8"?>
      <project>
        <app main="Main" path="Export" file="NyanCat"/>
      </project>
      """);
    assertEquals("Export", appPath);
  }

  @Test
  @DisplayName("external entities are not resolved")
  public void externalEntitiesAreNotResolved() {
    HaxeBuildFileInfo info = ProjectXmlParser.parse("""
      <?xml version="1.0"?>
      <!DOCTYPE project [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
      <project><haxelib name="&xxe;"/></project>
      """);
    assertTrue(info.libraries().isEmpty());
  }
}
