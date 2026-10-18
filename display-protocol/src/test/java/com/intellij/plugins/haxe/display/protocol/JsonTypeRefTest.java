package com.intellij.plugins.haxe.display.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Display protocol: json type ref")
public class JsonTypeRefTest {

  /** (wire type path, the qualified name the class indexes know it by). */
  static final List<Arguments> QUALIFIED_NAMES = List.of(
    arguments("{\"pack\":[\"haxe\",\"ds\"],\"moduleName\":\"StringMap\",\"typeName\":\"StringMap\"}", "haxe.ds.StringMap"),
    arguments("{\"pack\":[],\"moduleName\":\"StdTypes\",\"typeName\":\"Void\"}", "StdTypes.Void"),
    // a module-private type: the compiler adds the "_" + module segment
    arguments("{\"pack\":[\"hxparse\",\"_ParserBuilderImpl\"],\"moduleName\":\"ParserBuilderImpl\",\"typeName\":\"ParserCase\"}",
              "hxparse.ParserBuilderImpl.ParserCase"),
    arguments("{\"pack\":[\"_Main\"],\"moduleName\":\"Main\",\"typeName\":\"Secret\"}", "Main.Secret"),
    // a module and type named with a leading underscore, public and with a private neighbour
    arguments("{\"pack\":[],\"moduleName\":\"_ClassTest\",\"typeName\":\"_ClassTest\"}", "_ClassTest"),
    arguments("{\"pack\":[\"__ClassTest\"],\"moduleName\":\"_ClassTest\",\"typeName\":\"Hidden\"}", "_ClassTest.Hidden"),
    // a package segment merely ending like the marker stays
    arguments("{\"pack\":[\"lib\",\"x_Main\"],\"moduleName\":\"Main\",\"typeName\":\"Shown\"}", "lib.x_Main.Main.Shown"));

  @ParameterizedTest(name = "{1}")
  @FieldSource("QUALIFIED_NAMES")
  @DisplayName("a type path names the declaration the indexes know")
  public void aTypePathNamesTheDeclarationTheIndexesKnow(String typePath, String qualifiedName) {
    assertEquals(qualifiedName, JsonTypeRef.qualifiedNameOf(JsonMapper.builder().build().readTree(typePath)));
  }
}
