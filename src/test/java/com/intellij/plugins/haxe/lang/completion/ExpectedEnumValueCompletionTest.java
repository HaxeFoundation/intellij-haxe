package com.intellij.plugins.haxe.lang.completion;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.plugins.haxe.HaxeLightProjectDescriptors;
import com.intellij.testFramework.LightProjectDescriptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The enums live in their own fixture files: an enum declared in the completed file would
 * also be offered by HaxeEnumValuesCompletionContributor, and these tests must prove the
 * expected-type path supplies the bare value names.
 */
@DisplayName("Completion: expected enum value")
public class ExpectedEnumValueCompletionTest extends HaxeCompletionTestBase {
  public ExpectedEnumValueCompletionTest() {
    super("completion", "expectedEnum");
  }

  // the map cases resolve the std Map/IntMap classes to find their setters
  @Override
  protected LightProjectDescriptor lightProjectDescriptor() {
    return HaxeLightProjectDescriptors.WITH_TOOLKIT;
  }

  @Test
  @DisplayName("var init type tag")
  public void testVarInitTypeTag() throws Throwable {
    doTestInclude("MyEnum.hx");
  }

  @Test
  @DisplayName("assign to tagged local")
  public void testAssignToTaggedLocal() throws Throwable {
    doTestInclude("MyEnum.hx");
  }

  @Test
  @DisplayName("assign to field")
  public void testAssignToField() throws Throwable {
    doTestInclude("MyEnum.hx");
  }

  @Test
  @DisplayName("call argument")
  public void testCallArgument() throws Throwable {
    doTestInclude("MyEnum.hx");
  }

  @Test
  @DisplayName("constructor argument")
  public void testConstructorArgument() throws Throwable {
    doTestInclude("MyEnum.hx");
  }

  @Test
  @DisplayName("parameter default")
  public void testParameterDefault() throws Throwable {
    doTestInclude("MyEnum.hx");
  }

  @Test
  @DisplayName("return type tag")
  public void testReturnTypeTag() throws Throwable {
    doTestInclude("MyEnum.hx");
  }

  @Test
  @DisplayName("abstract enum")
  public void testAbstractEnum() throws Throwable {
    doTestInclude("Mode.hx");
  }

  @Test
  @DisplayName("typedef alias")
  public void testTypedefAlias() throws Throwable {
    doTestInclude("MyEnum.hx");
  }

  @Test
  @DisplayName("map literal key")
  public void testMapLiteralKey() throws Throwable {
    doTestInclude("MyEnum.hx");
  }

  @Test
  @DisplayName("map literal value")
  public void testMapLiteralValue() throws Throwable {
    doTestInclude("MyEnum.hx");
  }

  @Test
  @DisplayName("int map value")
  public void testIntMapValue() throws Throwable {
    doTestInclude("MyEnum.hx");
  }

  @Test
  @DisplayName("reversed map value")
  public void testReversedMapValue() throws Throwable {
    doTestInclude("MyEnum.hx");
  }

  @Test
  @DisplayName("reference chain")
  public void testReferenceChain() throws Throwable {
    doTestInclude("MyEnum.hx");
  }

  @Test
  @DisplayName("var init sorted first")
  public void testVarInitSortedFirst() {
    myFixture.configureByFiles("VarInitTypeTag.hx", "MyEnum.hx");
    myFixture.complete(CompletionType.BASIC, 1);

    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements, "completion must show a lookup");
    String first = elements[0].getLookupString();
    boolean enumValueFirst = first.equals("BAR") || first.equals("BAZ");
    assertTrue(enumValueFirst, "an expected enum value must sort first, got " + first);
  }
}
