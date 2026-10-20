package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.display.protocol.ProtocolJson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** The inlay text of the compiler's JSON types: short names, parameters, the IDE's function arrow, nothing for a bare monomorph; type names are the navigable parts. */
@DisplayName("Compiler types: inlay type text")
public class HaxeInlayTypeTextTest {

  private static final String INT = "{\"kind\":\"TAbstract\",\"args\":{\"path\":{\"pack\":[],\"typeName\":\"Int\"},\"params\":[]}}";
  private static final String STRING = "{\"kind\":\"TInst\",\"args\":{\"path\":{\"pack\":[],\"typeName\":\"String\"},\"params\":[]}}";
  private static final String MONO = "{\"kind\":\"TMono\",\"args\":null}";

  /** (wire type, expected inlay text). */
  static final List<Arguments> TEXTS = List.of(
    arguments("{\"kind\":\"TInst\",\"args\":{\"path\":{\"pack\":[\"haxe\",\"ds\"],\"typeName\":\"Map\"},\"params\":[" + STRING + "," + INT + "]}}",
              "Map<String, Int>"),
    arguments("{\"kind\":\"TFun\",\"args\":{\"args\":[{\"name\":\"x\",\"opt\":false,\"t\":" + INT + "}],\"ret\":" + STRING + "}}",
              "(Int)->String"),
    arguments("{\"kind\":\"TInst\",\"args\":{\"path\":{\"pack\":[],\"typeName\":\"Array\"},\"params\":[" + MONO + "]}}",
              "Array<?>"),
    arguments("{\"kind\":\"TDynamic\",\"args\":null}", "Dynamic"),
    arguments("{\"kind\":\"TAnonymous\",\"args\":{\"fields\":[{\"name\":\"x\",\"type\":" + INT + "}],\"status\":{\"kind\":\"AClosed\"}}}",
              "{x:Int}"),
    // nothing to show for a type the compiler has not bound at all
    arguments(MONO, null));

  @ParameterizedTest(name = "{1}")
  @FieldSource("TEXTS")
  @DisplayName("renders wire types")
  public void testRendersWireTypes(String wireType, String expected) {
    JsonTypeRef type = ProtocolJson.MAPPER.readValue(wireType, JsonTypeRef.class);

    HaxeInlayTypeText text = HaxeInlayTypeText.of(type);

    assertEquals(expected, text == null ? null : text.text());
  }

  @Test
  @DisplayName("type names are the parts that carry a wire type")
  public void testTypeNamesAreThePartsThatCarryAWireType() {
    String map = "{\"kind\":\"TInst\",\"args\":{\"path\":{\"pack\":[\"haxe\",\"ds\"],\"typeName\":\"Map\"},\"params\":[" + STRING + "," + INT + "]}}";

    HaxeInlayTypeText text = HaxeInlayTypeText.of(ProtocolJson.MAPPER.readValue(map, JsonTypeRef.class));

    assertNotNull(text);
    List<String> texts = text.parts().stream().map(HaxeInlayTypeText.Part::text).toList();
    List<String> named = text.parts().stream().map(part -> part.namedType() == null ? null : part.namedType().dotPath()).toList();
    assertEquals(List.of("Map", "<", "String", ", ", "Int", ">"), texts);
    assertEquals(Arrays.asList("haxe.ds.Map", null, "String", null, "Int", null), named);
  }
}
