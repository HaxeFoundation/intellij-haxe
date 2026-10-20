package com.intellij.plugins.haxe.display.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    assertEquals(qualifiedName, ProtocolJson.MAPPER.readValue(typePath, JsonTypePath.class).qualifiedName());
  }

  @Test
  @DisplayName("a function binds its arguments by their wire names")
  public void aFunctionBindsItsArgumentsByTheirWireNames() {
    String wireType = """
      {"kind":"TFun","args":{"args":[
        {"name":"value","opt":false,"t":{"kind":"TAbstract","args":{"path":{"pack":[],"moduleName":"StdTypes","typeName":"Int"},"params":[]}}},
        {"name":"","opt":true,"t":{"kind":"TMono","args":null}}],
        "ret":{"kind":"TDynamic","args":null}}}""";

    JsonTypeRef type = ProtocolJson.MAPPER.readValue(wireType, JsonTypeRef.class);

    List<JsonTypeRef.Argument> arguments = type.functionArguments();
    assertEquals("value", arguments.getFirst().name());
    assertEquals("Int", arguments.getFirst().type().dotPath());
    assertNull(arguments.get(1).name(), "an empty wire name is no name");
    assertInstanceOf(JsonTypeRef.Monomorph.class, arguments.get(1).type());
    assertInstanceOf(JsonTypeRef.DynamicType.class, type.returnType());
  }

  @Test
  @DisplayName("a dynamic keeps its parameter and an unknown kind reads as other")
  public void aDynamicKeepsItsParameterAndAnUnknownKindReadsAsOther() {
    String dynamicOfString = """
      {"kind":"TDynamic","args":{"kind":"TInst","args":{"path":{"pack":[],"moduleName":"String","typeName":"String"},"params":[]}}}""";

    JsonTypeRef dynamic = ProtocolJson.MAPPER.readValue(dynamicOfString, JsonTypeRef.class);
    JsonTypeRef lazy = ProtocolJson.MAPPER.readValue("{\"kind\":\"TLazy\",\"args\":{}}", JsonTypeRef.class);

    assertEquals("String", assertInstanceOf(JsonTypeRef.DynamicType.class, dynamic).args().dotPath());
    assertEquals("TLazy", assertInstanceOf(JsonTypeRef.OtherType.class, lazy).kind());
  }
}
