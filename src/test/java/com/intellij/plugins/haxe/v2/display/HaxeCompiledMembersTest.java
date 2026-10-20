package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.display.protocol.JsonPos;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.display.protocol.ProtocolJson;
import com.intellij.plugins.haxe.display.protocol.server.TypeBlueprint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Members land under their file and editor text range: a byte range of the
 * CRLF file on disk becomes a range in the LF text, and a parameter finds its
 * type through the method whose range holds it.
 */
@DisplayName("Compiler types: compiled members")
public class HaxeCompiledMembersTest {

  private static final String MAIN_HX_NAME = "src/Main.hx";
  private static final String MAIN_HX_DISK = "class Main {\r\n  static var count = 1;\r\n  static function run(size, total:Int) {}\r\n}\r\n";
  private static final String MAIN_HX_TEXT = MAIN_HX_DISK.replace("\r\n", "\n");
  private static final String INT = "{\"kind\":\"TAbstract\",\"args\":{\"path\":{\"pack\":[],\"typeName\":\"Int\"},\"params\":[]}}";
  private static final String RUN_TYPE = "{\"kind\":\"TFun\",\"args\":{\"args\":[{\"name\":\"size\",\"opt\":false,\"t\":" + INT + "}],\"ret\":" + INT + "}}";

  @Test
  @DisplayName("members are found by the text offset of a declaration inside them")
  public void testMembersAreFoundByTheTextOffsetOfADeclarationInsideThem() {
    TypeBlueprint main = new TypeBlueprint("Main", "class", List.of(), List.of(
      member("count", INT, byteRange("static var count = 1;")),
      member("run", RUN_TYPE, byteRange("static function run(size, total:Int) {}"))));
    String key = HaxeCompiledMembers.normalizePath(MAIN_HX_NAME, "/work", true);

    HaxeCompiledMembers members = HaxeCompiledMembers.of(List.of(main), "/work", true, path -> MAIN_HX_DISK.getBytes(StandardCharsets.UTF_8));

    assertEquals(2, members.memberCount());
    assertEquals("count", members.memberAt(key, MAIN_HX_TEXT.indexOf("count")).name());
    HaxeCompiledMembers.Member run = members.memberAt(key, MAIN_HX_TEXT.indexOf("size"));
    assertNotNull(run);
    assertEquals("run", run.name());
    assertEquals("Int", run.type().parameterType("size").dotPath());
    assertNull(members.memberAt(key, MAIN_HX_TEXT.indexOf("class")), "the class head belongs to no member");
  }

  @Test
  @DisplayName("a file the reader cannot supply is left out")
  public void testAFileTheReaderCannotSupplyIsLeftOut() {
    TypeBlueprint main = new TypeBlueprint("Main", "class", List.of(), List.of(member("count", INT, byteRange("static var count = 1;"))));

    HaxeCompiledMembers members = HaxeCompiledMembers.of(List.of(main), "/work", true, path -> null);

    assertEquals(0, members.fileCount());
    assertNull(members.memberAt(HaxeCompiledMembers.normalizePath(MAIN_HX_NAME, "/work", true), 0));
  }

  @Test
  @DisplayName("paths normalize to forward slashes relative to the work directory and fold case when the file system does")
  public void testPathsNormalizeToForwardSlashesRelativeToTheWorkDirectoryAndFoldCaseWhenTheFileSystemDoes() {
    assertEquals(HaxeCompiledMembers.normalizePath("C:/work/src/Main.hx", "", true),
                 HaxeCompiledMembers.normalizePath("src\\Main.hx", "C:/work", true));
    assertEquals("c:/work/src/main.hx", HaxeCompiledMembers.normalizePath("C:/Work/src/Main.hx", "", false));
  }

  private static TypeBlueprint.Member member(String name, String wireType, JsonPos pos) {
    return new TypeBlueprint.Member(name, ProtocolJson.MAPPER.readValue(wireType, JsonTypeRef.class), "FVar", pos);
  }

  /** The byte range of a declaration's text in the file on disk. */
  private static JsonPos byteRange(String declaration) {
    int start = MAIN_HX_DISK.indexOf(declaration);
    assertTrue(start >= 0, declaration);
    return new JsonPos(MAIN_HX_NAME, start, start + declaration.length());
  }
}
