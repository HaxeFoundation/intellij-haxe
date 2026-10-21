package com.intellij.plugins.haxe.v2.wizard;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Project generator: template files")
public class HaxeTemplateFilesTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/wizard/";
  }

  @Test
  @DisplayName("hxml renders main class, dce and the target output line")
  public void testHxmlRendersMainClassDceAndTheTargetOutputLine() {
    String hl = hxml(spec(HaxeTemplateFiles.HxmlTargetOption.HASHLINK_VM));
    assertTrue(hl.contains("-cp src"), "classpath line expected");
    assertTrue(hl.contains("-main Main"), "main line expected");
    assertTrue(hl.contains("-dce std"), "dce line expected for every target, got:\n" + hl);
    assertTrue(hl.contains("--hl out/app.hl"), "hl bytecode output expected, got:\n" + hl);

    String hlc = hxml(spec(HaxeTemplateFiles.HxmlTargetOption.HASHLINK_C));
    assertTrue(hlc.contains("--hl out/c/main.c"), "a .c output selects HL/C generation, got:\n" + hlc);

    String jvm = hxml(spec(HaxeTemplateFiles.HxmlTargetOption.JVM));
    assertTrue(jvm.contains("--jvm out/app.jar"), "jvm renders a jar, got:\n" + jvm);
  }

  @Test
  @DisplayName("hxml legacy source targets pull their support library")
  public void testHxmlLegacySourceTargetsPullTheirSupportLibrary() {
    String java = hxml(spec(HaxeTemplateFiles.HxmlTargetOption.JAVA_LEGACY));
    assertTrue(java.contains("-lib hxjava"), "legacy java needs hxjava, got:\n" + java);
    assertTrue(java.contains("--java out/java"), "directory output expected, got:\n" + java);

    String cs = hxml(spec(HaxeTemplateFiles.HxmlTargetOption.CSHARP));
    assertTrue(cs.contains("-lib hxcs"), "c# needs hxcs, got:\n" + cs);
  }

  @Test
  @DisplayName("hxml interpreter target has no output argument")
  public void testHxmlInterpreterTargetHasNoOutputArgument() {
    String interp = hxml(spec(HaxeTemplateFiles.HxmlTargetOption.INTERP));
    assertTrue(interp.contains("--interp"), "interp flag expected, got:\n" + interp);
    assertFalse(interp.contains("--interp "), "no output argument may follow --interp, got:\n" + interp);
  }

  @Test
  @DisplayName("hxml renders the javascript source map and the flash header")
  public void testHxmlRendersTheJavascriptSourceMapAndTheFlashHeader() {
    String js = hxml(new HaxeTemplateFiles.HxmlSpec(
      HaxeTemplateFiles.HxmlTargetOption.JAVASCRIPT, "App", "web/app.js", "full", true, "", ""));
    assertTrue(js.contains("-main App"), "custom main class expected, got:\n" + js);
    assertTrue(js.contains("--js web/app.js"), "custom output expected, got:\n" + js);
    assertTrue(js.contains("-dce full"), "chosen dce mode expected, got:\n" + js);
    assertTrue(js.contains("-D source-map"), "source map define expected, got:\n" + js);

    String swf = hxml(new HaxeTemplateFiles.HxmlSpec(
      HaxeTemplateFiles.HxmlTargetOption.FLASH, "Main", "out/app.swf", "std", false, "15", "960:640:60:f68712"));
    assertTrue(swf.contains("--swf out/app.swf"), "swf output expected, got:\n" + swf);
    assertTrue(swf.contains("--swf-version 15"), "swf version expected, got:\n" + swf);
    assertTrue(swf.contains("--swf-header 960:640:60:f68712"), "swf header expected, got:\n" + swf);
  }

  @Test
  @DisplayName("starter main class is named after the configured main class")
  public void testStarterMainClassIsNamedAfterTheConfiguredMainClass() {
    String starter = HaxeTemplateFiles.INSTANCE.starterMainHx(getProject(), "App");
    assertTrue(starter.contains("class App"), "class named after the main class, got:\n" + starter);
    assertTrue(starter.contains("static function main()"), "entry point expected, got:\n" + starter);
  }

  @Test
  @DisplayName("lime project xml carries the configured values")
  public void testLimeProjectXmlCarriesTheConfiguredValues() {
    String xml = HaxeTemplateFiles.INSTANCE.limeProjectXml(getProject(), "openfl", "My Game", "com.example.mygame", 800, 600, 30);
    assertTrue(xml.contains("title=\"My Game\""), "title expected");
    assertTrue(xml.contains("package=\"com.example.mygame\""), "package expected");
    assertTrue(xml.contains("width=\"800\" height=\"600\" fps=\"30\""), "window settings expected");
    assertTrue(xml.contains("<haxelib name=\"openfl\" />"), "framework haxelib expected");
    assertTrue(xml.contains("file=\"MyGame\""), "file name derived from the title without spaces");
    assertTrue(xml.contains("<source path=\"src\" />"), "source path expected");
  }

  @Test
  @DisplayName("xml special characters in titles are escaped")
  public void testXmlSpecialCharactersInTitlesAreEscaped() {
    String xml = HaxeTemplateFiles.INSTANCE.limeProjectXml(getProject(), "lime", "Fish & Chips <deluxe>", "com.example.app", 1280, 720, 60);
    assertTrue(xml.contains("title=\"Fish &amp; Chips &lt;deluxe&gt;\""), "title must be XML-escaped, got:\n" + xml);
  }

  @Test
  @DisplayName("nmml project file carries app and window settings")
  public void testNmmlProjectFileCarriesAppAndWindowSettings() {
    String nmml = HaxeTemplateFiles.INSTANCE.nmmlProjectXml(getProject(), "Retro", "com.example.retro", 640, 480, 30);
    assertTrue(nmml.contains("title=\"Retro\""), "title expected");
    assertTrue(nmml.contains("main=\"Main\""), "main class expected");
    assertTrue(nmml.contains("width=\"640\" height=\"480\" fps=\"30\""), "window settings expected");
    assertTrue(nmml.contains("<haxelib name=\"nme\" />"), "nme haxelib expected");
    assertTrue(nmml.contains("<classpath name=\"src\" />"), "classpath expected");
  }

  @Test
  @DisplayName("haxelib json contains every schema-required field")
  public void testHaxelibJsonContainsEverySchemaRequiredField() {
    String json = HaxeTemplateFiles.INSTANCE.haxelibJson(
      getProject(), "mylib", "MIT", "0.0.1", "A \"useful\" lib", "https://example.org",
      List.of("utility", "cross"), List.of("someone"), "Initial release");

    assertTrue(json.contains("\"name\": \"mylib\""), "name required");
    assertTrue(json.contains("\"license\": \"MIT\""), "license required");
    assertTrue(json.contains("\"version\": \"0.0.1\""), "version required");
    assertTrue(json.contains("\"releasenote\": \"Initial release\""), "releasenote required");
    assertTrue(json.contains("\"contributors\": [\"someone\"]"), "contributors required");
    assertTrue(json.contains("\"classPath\": \"src/\""), "classPath fixed to the source folder");
    assertTrue(json.contains("\"tags\": [\"utility\", \"cross\"]"), "tags listed");
    assertTrue(json.contains("\\\"useful\\\""), "quotes in free text must be JSON-escaped");
  }

  @Test
  @DisplayName("haxelib dev hxml compiles the starter class in the interpreter")
  public void testHaxelibDevHxmlCompilesTheStarterClassInTheInterpreter() {
    String hxml = HaxeTemplateFiles.INSTANCE.haxelibDevHxml(getProject(), "Cooltool");
    assertTrue(hxml.contains("-cp src"), "library classpath expected");
    assertTrue(hxml.contains("--interp"), "eval target: no artifacts, full std support");
    assertTrue(hxml.contains("\nCooltool\n"), "the starter class must be a compile root - libraries have no -main");
    assertTrue(hxml.contains("# Development build"), "the explanatory hxml comment must survive template rendering, got:\n" + hxml);
  }

  @Test
  @DisplayName("haxelib starter class name is sanitized and capitalized")
  public void testHaxelibStarterClassNameIsSanitizedAndCapitalized() {
    assertEquals("Mylib", HaxeTemplateFiles.INSTANCE.haxelibClassNameOf("my-lib"));
    assertEquals("Lib", HaxeTemplateFiles.INSTANCE.haxelibClassNameOf("---"));
    String content = HaxeTemplateFiles.INSTANCE.haxelibStarterClass(getProject(), "cooltool").getSecond();
    assertTrue(content.contains("class Cooltool"), "class named after the lib");
    assertFalse(content.contains("--"), "no stray characters");
  }

  private String hxml(HaxeTemplateFiles.HxmlSpec spec) {
    return HaxeTemplateFiles.INSTANCE.hxml(getProject(), spec);
  }

  private static HaxeTemplateFiles.HxmlSpec spec(HaxeTemplateFiles.HxmlTargetOption target) {
    return new HaxeTemplateFiles.HxmlSpec(target, "Main", target.getDefaultOutput(), "std", false, "", "");
  }
}
