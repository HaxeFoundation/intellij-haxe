package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.HaxeToolkitLightFixtureTestCase;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.display.protocol.ProtocolJson;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificFunctionReference;
import com.intellij.psi.PsiFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Wire types become the plugin's type model: named types through the class
 * indexes (std and project), function types with their arguments, Dynamic
 * as is. Monomorphs, anonymous structures and unknown classes give no
 * result, so the caller falls back to the IDE's own evaluation.
 */
@DisplayName("Compiler types: converter")
public class HaxeCompilerTypeConverterTest extends HaxeToolkitLightFixtureTestCase {

  private static final String MAIN_HX_SOURCE = """
    enum Color {
      Red;
      Green;
    }
    typedef Alias = String;
    private typedef Secret = Int;
    class Main {
      static function main() {}
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a std class with type parameters")
  public void testAStdClassWithTypeParameters() {
    PsiFile main = myFixture.addFileToProject("Main.hx", MAIN_HX_SOURCE);

    ResultHolder type = convert(instance("Array", "Array", abstractType("Int")), main);

    assertEquals("Array<Int>", type.toStringWithoutConstant());
  }

  @Test
  @DisplayName("a project enum and typedef")
  public void testAProjectEnumAndTypedef() {
    PsiFile main = myFixture.addFileToProject("Main.hx", MAIN_HX_SOURCE);

    ResultHolder enumType = convert(wireType("TEnum", "Main", "Color"), main);
    ResultHolder typedefType = convert(wireType("TType", "Main", "Alias"), main);

    assertEquals("Color", enumType.toStringWithoutConstant());
    assertEquals("Alias", typedefType.toStringWithoutConstant());
  }

  @Test
  @DisplayName("a module private typedef")
  public void testAModulePrivateTypedef() {
    PsiFile main = myFixture.addFileToProject("Main.hx", MAIN_HX_SOURCE);

    // the compiler files a private type under an extra "_" + module package segment
    ResultHolder type = convert(wireType("TType", "_Main.Main", "Secret"), main);

    assertEquals("Secret", type.toStringWithoutConstant());
  }

  @Test
  @DisplayName("a function type keeps its arguments and return type")
  public void testAFunctionTypeKeepsItsArgumentsAndReturnType() {
    PsiFile main = myFixture.addFileToProject("Main.hx", MAIN_HX_SOURCE);
    String function = """
      {"kind":"TFun","args":{"args":[
        {"name":"a","opt":false,"t":%s},
        {"name":"b","opt":true,"t":%s}],
        "ret":%s}}""".formatted(abstractType("Int"), instance("String", "String"), abstractType("Void"));

    ResultHolder type = convert(function, main);

    SpecificFunctionReference functionType = assertInstanceOf(SpecificFunctionReference.class, type.getType());
    assertEquals(2, functionType.getArguments().size());
    assertEquals("Int", functionType.getArguments().get(0).getType().toStringWithoutConstant());
    assertTrue(functionType.getArguments().get(1).isOptional());
    assertEquals("Void", functionType.getReturnType().toStringWithoutConstant());
  }

  @Test
  @DisplayName("dynamic converts, monomorphs and anonymous structures do not")
  public void testDynamicConvertsMonomorphsAndAnonymousStructuresDoNot() {
    PsiFile main = myFixture.addFileToProject("Main.hx", MAIN_HX_SOURCE);

    ResultHolder dynamic = convert("{\"kind\":\"TDynamic\",\"args\":null}", main);
    ResultHolder mono = convert("{\"kind\":\"TMono\",\"args\":null}", main);
    ResultHolder anonymous = convert("{\"kind\":\"TAnonymous\",\"args\":{\"fields\":[],\"status\":{\"kind\":\"AClosed\"}}}", main);

    assertTrue(dynamic.isDynamic());
    assertNull(mono);
    assertNull(anonymous);
  }

  @Test
  @DisplayName("an unknown class or an unconvertible type parameter gives no result")
  public void testAnUnknownClassOrAnUnconvertibleTypeParameterGivesNoResult() {
    PsiFile main = myFixture.addFileToProject("Main.hx", MAIN_HX_SOURCE);

    ResultHolder unknownClass = convert(wireType("TInst", "nowhere.Missing", "Missing"), main);
    ResultHolder monoParameter = convert(instance("Array", "Array", "{\"kind\":\"TMono\",\"args\":null}"), main);

    assertNull(unknownClass);
    assertNull(monoParameter);
  }

  private static ResultHolder convert(String json, PsiFile context) {
    JsonTypeRef type = ProtocolJson.MAPPER.readValue(json, JsonTypeRef.class);
    return HaxeCompilerTypeConverter.toResultHolder(type, context);
  }

  /** A TInst of a class in the root package, with optional type parameters. */
  private static String instance(String moduleName, String typeName, String... params) {
    return "{\"kind\":\"TInst\",\"args\":{\"path\":" + path("", moduleName, typeName) + ",\"params\":[" + String.join(",", params) + "]}}";
  }

  /** A TAbstract of a StdTypes primitive. */
  private static String abstractType(String typeName) {
    return "{\"kind\":\"TAbstract\",\"args\":{\"path\":" + path("", "StdTypes", typeName) + ",\"params\":[]}}";
  }

  private static String wireType(String kind, String dottedModule, String typeName) {
    int lastDot = dottedModule.lastIndexOf('.');
    String pack = lastDot < 0 ? "" : dottedModule.substring(0, lastDot);
    String moduleName = lastDot < 0 ? dottedModule : dottedModule.substring(lastDot + 1);
    return "{\"kind\":\"" + kind + "\",\"args\":{\"path\":" + path(pack, moduleName, typeName) + ",\"params\":[]}}";
  }

  private static String path(String pack, String moduleName, String typeName) {
    String packs = pack.isEmpty() ? "" : "\"" + pack.replace(".", "\",\"") + "\"";
    return "{\"pack\":[" + packs + "],\"moduleName\":\"" + moduleName + "\",\"typeName\":\"" + typeName + "\"}";
  }
}
