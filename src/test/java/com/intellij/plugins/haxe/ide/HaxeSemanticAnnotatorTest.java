/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.inspections.operators.HaxeIsTypeExpressionInspection;
import com.intellij.plugins.haxe.ide.inspections.resolve.HaxeUnresolvedSymbolInspection;
import com.intellij.testFramework.junit5.RunInEdt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

@DisplayName("Annotation: semantic annotator")
public class HaxeSemanticAnnotatorTest extends HaxeSemanticAnnotatorTestBase {

  @Override
  public void setUp() throws Exception {
    // for use when idempotence check problems occur and we need consistent results.
    //Registry.get("platform.random.idempotence.check.rate").setValue(1, getTestRootDisposable());
    super.setUp();
    setTestStyleSettings(2);
  }

  @Override
  protected String getBasePath() {
    return "/annotation.semantic/";
  }

  @Test
  @DisplayName("char dot code")
  public void testCharDotCode() throws Exception {
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("interface methods should have type tags")
  public void testInterfaceMethodsShouldHaveTypeTags() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("optional field syntax")
  public void testOptionalFieldSyntax() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("optional method syntax")
  public void testOptionalMethodSyntax() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("non constant argument")
  public void testNonConstantArgument() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("non constant argument abstract enum")
  public void testNonConstantArgumentAbstractEnum() throws Exception {
    doTestNoFixWithWarnings("test/SampleAbstractEnum.hx");
  }

  @Test
  @DisplayName("constructor must not be static")
  public void testConstructorMustNotBeStatic() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("null coalescing test")
  public void testNullCoalescingTest() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("init magic method should be static")
  public void testInitMagicMethodShouldBeStatic() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("repeated argument name")
  public void testRepeatedArgumentName() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("null function")
  public void testNullFunction() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("Uppercase - first class name")
  public void testUcFirstClassName() throws Exception {
    doTestActions("Change name");
  }

  @Test
  @DisplayName("Uppercase - first class name 2 - lowercase names flagged in extends and params")
  public void testUcFirstClassName2() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("extends anonymous type")
  public void testExtendsAnonymousType() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("extends self")
  public void testExtendsSelf() throws Exception {
    doTestNoFixWithWarnings("test/Bar.hx", "test/IBar.hx", "test/TBar.hx");
  }

  @Test
  @DisplayName("extension methods for function types")
  public void testExtensionMethodsForFunctionTypes() throws Exception {
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
    doTestNoFixWithWarnings("extensions/FunctionExtensions.hx");
  }

  @Test
  @DisplayName("int iterator extension methods")
  public void testIntIteratorExtensionMethods() throws Exception {
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
    doTestNoFixWithWarnings("extensions/IntIteratorExtensions.hx");
  }

  @Test
  @DisplayName("multiple class modifiers")
  public void testMultipleClassModifiers() throws Exception {
    doTestNoFixWithWarnings();
  }
  @Test
  @DisplayName("missing return statement")
  public void testMissingReturnStatement() throws Exception {
      doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("crash while annotating")
  public void testCrashWhileAnnotating() throws Exception {
    // A stack overflow was occurring while annotating, and there's no better place to
    // put this test at the moment, soo....
    doTestNoFixWithoutWarnings();
  }

  @Test
  @DisplayName("is keyword for haxe 4.2 - unparenthesized is allowed in 4.2")
  public void testIsKeywordFor4_2() throws Throwable {
    // the 4.1-compat option defaults to off, so 4.2 semantics apply
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("is keyword for haxe 4.1 - unparenthesized is flagged pre 4.2")
  public void testIsKeywordFor4_1() throws Throwable {
    HaxeIsTypeExpressionInspection compat41 = new HaxeIsTypeExpressionInspection();
    compat41.enforce41Semantics = true;
    doTestReplacingInspection(compat41);
  }

  @Test
  @DisplayName("string interpolation")
  public void testStringInterpolation() throws Throwable {
    doTestNoFixWithWarnings();
  }
  @Test
  @DisplayName("resolve type from usage")
  public void testResolveTypeFromUsage() throws Throwable {
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("return statement annotation")
  public void testReturnStatementAnnotation() throws Throwable {
    doTestNoFixWithWeakWarnings();
  }
  @Test
  @DisplayName("safe cast expressions")
  public void testSafeCastExpressions() throws Throwable {
    doTestNoFixWithWeakWarnings();
  }

  @Test
  @DisplayName("macro semantics")
  public void testMacroSemantics() throws Throwable {
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
    doTestNoFixWithWarnings();
  }
  @Test
  @DisplayName("macro stub return type is not void")
  public void testMacroStubReturnTypeIsNotVoid() throws Throwable {
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
    doTestNoFixWithWarnings();
  }
  @Test
  @DisplayName("iterator type resolve")
  public void testIteratorTypeResolve() throws Throwable {
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
    doTestNoFixWithWarnings();
  }
  @Test
  @DisplayName("type tags types in parentheses")
  public void testTypeTagsTypesInParentheses() throws Throwable {
    doTestNoFixWithWarnings();
  }

  @Test
  @DisplayName("unification rules")
  public void testUnificationRules() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("unknown type propagation")
  class UnknownTypePropagation {
    @Test
    @DisplayName("assign unknown twice")
    public void testAssignUnknownTwice() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("update unknown in generics")
    public void testUpdateUnknownInGenerics() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("update unknown on lambdas")
    public void testUpdateUnknownOnLambdas() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("simple assign unknown generic")
    public void testSimpleAssignUnknownGeneric() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("allow unknown generics 1 - mismatch still reported on known type args")
    public void testAllowUnknownGenerics1() throws Exception {
      doTestNoFixWithWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("quick fix actions")
  class QuickFixActions {
    @Test
    @DisplayName("fix package")
    public void testFixPackage() throws Exception {
      doTestActions("Fix package");
    }

    @Test
    @DisplayName("remove override")
    public void testRemoveOverride() throws Exception {
      doTestActions("Remove override");
    }

    @Test
    @DisplayName("remove final")
    public void testRemoveFinal() throws Exception {
      doTestActions("Remove final from Base.test");  // @:final, but the @: is no longer in the fix message.
    }

    @Test
    @DisplayName("change argument type")
    public void testChangeArgumentType() throws Exception {
      doTestActions(HaxeBundle.message("haxe.quickfix.change.parameter.type"));
    }

    @Test
    @DisplayName("remove argument init")
    public void testRemoveArgumentInit() throws Exception {
      doTestActions(HaxeBundle.message("haxe.quickfix.remove.initializer"));
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("abstract types")
  class AbstractTypes {
    @Test
    @DisplayName("abstract from to")
    public void testAbstractFromTo() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("abstract callable casts")
    public void testAbstractCallableCasts() throws Exception {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("abstract class method implementation")
    public void testAbstractClassMethodImplementation() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("abstract keywords")
    public void testAbstractKeywords() throws Exception {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("abstract operator overload")
    public void testAbstractOperatorOverload() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("abstract from to metadata")
    public void testAbstractFromToMetadata() throws Exception {
      doTestNoFixWithWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("abstract assignment from/to casts")
  class AbstractAssignmentCasts {
    @Test
    @DisplayName("1 - from cast allows array literal")
    public void testAbstractAssignmentFromTo1() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("2 - no casts rejects array literal")
    public void testAbstractAssignmentFromTo2() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("3 - to cast alone rejects array literal")
    public void testAbstractAssignmentFromTo3() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("4 - from and to allow both directions")
    public void testAbstractAssignmentFromTo4() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("5 - from alone rejects assignment to array")
    public void testAbstractAssignmentFromTo5() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("6 - chained abstract casts")
    public void testAbstractAssignmentFromTo6() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("functions")
    public void testAbstractAssignmentFromToFunctions() throws Exception {
      doTestNoFixWithWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("override and implement signatures")
  class OverrideAndImplementSignatures {
    @Test
    @DisplayName("override visibility")
    public void testOverrideVisibility() throws Exception {
      doTestNoFixWithWeakWarnings();
    }

    @Test
    @DisplayName("override signature")
    public void testOverrideSignature() throws Exception {
      doTestActions("Remove argument");
    }

    @Test
    @DisplayName("override signature 2 - wrong types arity and overriding nothing")
    public void testOverrideSignature2() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("override signature 3 - extra optional and required args")
    public void testOverrideSignature3() throws Exception {
      doTestActions("Remove argument");
    }

    @Test
    @DisplayName("override signature 4 - args added to parameterless base")
    public void testOverrideSignature4() throws Exception {
      doTestActions("Remove argument");
    }

    @Test
    @DisplayName("override signature 5 - rtti override without errors")
    public void testOverrideSignature5() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("override signature generics")
    public void testOverrideSignatureGenerics() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("implement signature generics")
    public void testImplementSignatureGenerics() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("implement signature inheritance param")
    public void testImplementSignatureInheritanceParam() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("implement signature inheritance return")
    public void testImplementSignatureInheritanceReturn() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("implement signature")
    public void testImplementSignature() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("implement methods")
    public void testImplementMethods() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("implement extern interface")
    public void testImplementExternInterface() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error on override definition")
    public void testNoErrorOnOverrideDefinition() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error on constrained generic overrides")
    public void testNoErrorOnConstrainedGenericOverrides() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("missing interface methods on constrained generic overrides")
    public void testMissingInterfaceMethodsOnConstrainedGenericOverrides() throws Exception {
        doTestNoFixWithWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("fields and properties")
  class FieldsAndProperties {
    @Test
    @DisplayName("repeated fields")
    public void testRepeatedFields() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("properties simple check")
    public void testPropertiesSimpleCheck() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("property allow non constant initialization")
    public void testPropertyAllowNonConstantInitialization() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("field initializer check")
    public void testFieldInitializerCheck() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("field property constraint")
    public void testFieldPropertyConstraint() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("field type hint requirement")
    public void testFieldTypeHintRequirement() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("static field access")
    public void testStaticFieldAccess() throws Exception {
      doTestNoFixWithoutWarnings();
    }

    @Test
    @DisplayName("statics in extended")
    public void testStaticsInExtended() throws Exception {
      doTestNoFixWithoutWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("final and immutability")
  class FinalAndImmutability {
    @Test
    @DisplayName("final keyword")
    public void testFinalKeyword() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("final keyword enum")
    public void testFinalKeywordEnum() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("final keyword in interface")
    public void testFinalKeywordInInterface() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("local var immutability")
    public void testLocalVarImmutability() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("field immutability")
    public void testFieldImmutability() throws Exception {
      doTestNoFixWithWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("variable scoping")
  class VariableScoping {
    @Test
    @DisplayName("variable redefinition")
    public void testVariableRedefinition() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("variable shadowing")
    public void testVariableShadowing() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("capture var shadowing")
    public void testCaptureVarShadowing() throws Throwable {
      // unresolved symbols are used to confirm correct returned type
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWeakWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("arrays and collections")
  class ArraysAndCollections {
    @Test
    @DisplayName("array assignment from empty")
    public void testArrayAssignmentFromEmpty() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("array assignment bad function type")
    public void testArrayAssignmentBadFunctionType() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("array assignment wrong type")
    public void testArrayAssignmentWrongType() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("array comprehensions with cast")
    public void testArrayComprehensionsWithCast() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("array comprehensions with expression")
    public void testArrayComprehensionsWithExpression() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("array assignment bad arrow function")
    public void testArrayAssignmentBadArrowFunction() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("array assignment with abstract")
    public void testArrayAssignmentWithAbstract() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("array assignment with arrow functions")
    public void testArrayAssignmentWithArrowFunctions() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("array literal type detection and cast")
    public void testArrayLiteralTypeDetectionAndCast() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("immediate string array indexing")
    public void testImmediateStringArrayIndexing() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign empty collection")
    public void testAssignEmptyCollection() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign collection without parameter types")
    public void testAssignCollectionWithoutParameterTypes() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("literal collection arguments")
    public void testLiteralCollectionArguments() throws Throwable {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("initialize string map with map literal")
    public void testInitializeStringMapWithMapLiteral() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("initialize int map with map literal")
    public void testInitializeIntMapWithMapLiteral() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("initialize enum map with map literal")
    public void testInitializeEnumMapWithMapLiteral() throws Exception {
      doTestNoFixWithWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("primitive and coercion assignments")
  class PrimitiveCoercionAssignments {
    @Test
    @DisplayName("null t assignment 1 - int literal accepted")
    public void testNullTAssignment1() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("null t assignment 2 - string rejected")
    public void testNullTAssignment2() throws Exception {
      doTestNoFixWithWarnings();
    }

    // var a:Int = 10/2;
    @Test
    @DisplayName("initialize int with float")
    public void testInitializeIntWithFloat() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign float to int")
    public void testAssignFloatToInt() throws Exception {
      doTestNoFixWithWarnings();
    }

    // var a:Int = "3.1416";
    @Test
    @DisplayName("initialize float with string")
    public void testInitializeFloatWithString() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign string to float")
    public void testAssignStringToFloat() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign struct init")
    public void testAssignStructInit() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign anonymous type")
    public void testAssignAnonymousType() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign anonymous type struct")
    public void testAssignAnonymousTypeStruct() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign string to dynamic")
    public void testAssignStringToDynamic() throws Exception {
      doTestNoFixWithWarnings();
    }

    // var a:Int = (10.0 : Float);
    @Test
    @DisplayName("initialize int with type check float")
    public void testInitializeIntWithTypeCheckFloat() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign type check float to int")
    public void testAssignTypeCheckFloatToInt() throws Exception {
      doTestNoFixWithWarnings();
    }

    // var a:Int = (10 : Float);
    @Test
    @DisplayName("initialize int with type checked")
    public void testInitializeIntWithTypeChecked() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign with type checker")
    public void testAssignWithTypeChecker() throws Exception {
      doTestNoFixWithWarnings();
    }

    // var a:String = 3.1416;
    @Test
    @DisplayName("initialize string with float")
    public void testInitializeStringWithFloat() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign float to string")
    public void testAssignFloatToString() throws Exception {
      doTestNoFixWithWarnings();
    }

    // var a:String = 10;
    @Test
    @DisplayName("initialize string with int")
    public void testInitializeStringWithInt() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("initialize with loops")
    public void testInitializeWithLoops() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign int to string")
    public void testAssignIntToString() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign new expression with generics")
    public void testAssignNewExpressionWithGenerics() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign object literal")
    public void testAssignObjectLiteral() throws Exception {
      doTestNoFixWithWarnings();
    }

    // var a:Float = 100;
    @Test
    @DisplayName("initialize float with int")
    public void testInitializeFloatWithInt() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign int to float")
    public void testAssignIntToFloat() throws Exception {
      doTestNoFixWithWarnings();
    }

    // var f:Float = 100; i:Int = (f);
    @Test
    @DisplayName("initialize int with parenthesized float")
    public void testInitializeIntWithParenthesizedFloat() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign parenthesized float to int")
    public void testAssignParenthesizedFloatToInt() throws Exception {
      doTestNoFixWithWarnings();
    }

  // NOT Working yet.
    // var c:Int = {x:1, y:2};
    //@Test public void testInitializeIntWithAnonymousStruct() throws Exception {
    //  doTestNoFixWithWarnings();
    //}
    //
    //@Test public void testAssignAnonymousStructToInt() throws Exception {
    //  doTestNoFixWithWarnings();
    //}

    // typedef Pt = {x:Int; y:Int;}; var c:Int = new Pt();
    @Test
    @DisplayName("initialize int with typedef")
    public void testInitializeIntWithTypedef() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("initialize typedef with optional fields")
    public void testInitializeTypedefWithOptionalFields() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("object literal with class and instance types")
    public void testObjectLiteralWithClassAndInstanceTypes() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("object literal with typedef array fields")
    public void testObjectLiteralWithTypedefArrayFields() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign typedef to int")
    public void testAssignTypedefToInt() throws Exception {
      doTestNoFixWithWarnings();
    }

    // class Point {...}; var c:Int = new Point(1,2);
    @Test
    @DisplayName("initialize int with class")
    public void testInitializeIntWithClass() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign class to int")
    public void testAssignClassToInt() throws Exception {
      doTestNoFixWithWarnings();
    }

  // NOT working yet.
    // class Test{ var somevar; function new() { somevar = 3.1; }
    //@Test public void testUnknownClassVariable() throws Exception {
    //  doTestNoFixWithWarnings();
    //}

    // class Test{ var somevar:Int; function new() { somevar = 3; }
    @Test
    @DisplayName("assign float to typed class var declaration")
    public void testAssignFloatToTypedClassVarDeclaration() throws Exception {
      doTestNoFixWithWarnings();
    }

    // class Test{ var somevar = 10; function new() {somevar = 3.1;} }
    @Test
    @DisplayName("assign float to inferred class var int")
    public void testAssignFloatToInferredClassVarInt() throws Exception {
      doTestNoFixWithWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("type and enum assignments")
  class TypeAndEnumAssignments {
    @Test
    @DisplayName("assign type to class")
    public void testAssignTypeToClass() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign dynamic method")
    public void testAssignDynamicMethod() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign type to enum")
    public void testAssignTypeToEnum() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign unresolved type")
    public void testAssignUnresolvedType() throws Exception {
      doTestNoFixWithWeakWarnings();
    }

    @Test
    @DisplayName("assign enum value")
    public void testAssignEnumValue() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign expr of")
    public void testAssignExprOf() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("implicit cast")
    public void testImplicitCast() throws Exception {
      doTestNoFixWithWarnings();
    }

    //Issue #981
    @Test
    @DisplayName("assign reflection type to dynamic")
    public void testAssignReflectionTypeToDynamic() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("downcast test")
    public void testDowncastTest() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("either type test")
    public void testEitherTypeTest() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign from recursive method")
    public void testAssignFromRecursiveMethod() throws Throwable {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error on multiple null t")
    public void testNoErrorOnMultipleNullT() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no incompatible type error on map")
    public void testNoIncompatibleTypeErrorOnMap() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no incompatible type error on chained maps")
    public void testNoIncompatibleTypeErrorOnChainedMaps() throws Exception {
      doTestNoFixWithWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("generics and type parameters")
  class GenericsAndTypeParameters {
    @Test
    @DisplayName("no error when type parameter is self class")
    public void testNoErrorWhenTypeParameterIsSelfClass() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error accessing parameterized array")
    public void testNoErrorAccessingParameterizedArray() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error accessing fields through paramaterized methods")
    public void testNoErrorAccessingFieldsThroughParamaterizedMethods() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error assigning from parameterized function")
    public void testNoErrorAssigningFromParameterizedFunction() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error assigning to parameterized array element")
    public void testNoErrorAssigningToParameterizedArrayElement() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error assigning parameterized type during var init")
    public void testNoErrorAssigningParameterizedTypeDuringVarInit() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("monomorphism")
    public void testMonomorphism() throws Exception {
        doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("multi level generic inheritance")
    public void testMultiLevelGenericInheritance() throws Exception {
        doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("inherit unspecified type parameters")
    public void testInheritUnspecifiedTypeParameters() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("parameterized functions")
    public void testParameterizedFunctions() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("generic from class arg")
    public void testGenericFromClassArg() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("initialize object with generic function")
    public void testInitializeObjectWithGenericFunction() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("type parameter constraints")
    public void testTypeParameterConstraints() throws Throwable {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("type parameter inherit constraints")
    public void testTypeParameterInheritConstraints() throws Throwable {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWeakWarnings();
    }

    @Test
    @DisplayName("type parameter arguments")
    public void testTypeParameterArguments() throws Throwable {
      // unresolved symbols are used to confirm correct returned type
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWeakWarnings();
    }

    @Test
    @DisplayName("type parameter function arguments")
    public void testTypeParameterFunctionArguments() throws Throwable {
      // unresolved symbols are used to confirm correct returned type
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWeakWarnings();
    }

    @Test
    @DisplayName("type parameter count")
    public void testTypeParameterCount() throws Throwable {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("type parameter defaults")
    public void testTypeParameterDefaults() throws Throwable {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("type from constraints")
    public void testTypeFromConstraints() throws Throwable {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("type parameter anonymous structure")
    public void testTypeParameterAnonymousStructure() throws Throwable {
      doTestNoFixWithWarnings();
    }

   @Test
   @DisplayName("generic build type parameters")
   public void testGenericBuildTypeParameters() throws Exception {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWeakWarnings();
   }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("optional parameters and constant defaults")
  class OptionalParameterConstants {
    @Test
    @DisplayName("parameter default values")
    public void testParameterDefaultValues() throws Exception {
      doTestNoFixWithWeakWarnings();
    }

    @Test
    @DisplayName("no error on optional parameter with field constant")
    public void testNoErrorOnOptionalParameterWithFieldConstant() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error on optional parameter with int field constant")
    public void testNoErrorOnOptionalParameterWithIntFieldConstant() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error on optional parameter with simple string field constant")
    public void testNoErrorOnOptionalParameterWithSimpleStringFieldConstant() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error on optional parameter with parenthesized string field constant")
    public void testNoErrorOnOptionalParameterWithParenthesizedStringFieldConstant() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error on optional parameter with parenthesized numeric field constant")
    public void testNoErrorOnOptionalParameterWithParenthesizedNumericFieldConstant() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("type error on optional parameter with parenthesized numeric field constant")
    public void testTypeErrorOnOptionalParameterWithParenthesizedNumericFieldConstant() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("error on optional parameter with non const method")
    public void testErrorOnOptionalParameterWithNonConstMethod() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error on optional parameter with doubly referenced var")
    public void testNoErrorOnOptionalParameterWithDoublyReferencedVar() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error on enum constant")
    public void testNoErrorOnEnumConstant() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error on optional null float constant")
    public void testNoErrorOnOptionalNullFloatConstant() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error on constant function type")
    public void testNoErrorOnConstantFunctionType() throws Exception {
      doTestNoFixWithWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("function types and binding")
  class FunctionTypesAndBinding {
    @Test
    @DisplayName("no error on inline function assignment")
    public void testNoErrorOnInlineFunctionAssignment() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error on function assignment")
    public void testNoErrorOnFunctionAssignment() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("no error on function unification")
    public void testNoErrorOnFunctionUnification() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("inferred function type assignment")
    public void testInferredFunctionTypeAssignment() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("calling function types")
    public void testCallingFunctionTypes() throws Throwable {
      // unresolved symbols are used to confirm correct returned type
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWeakWarnings();
    }

    @Test
    @DisplayName("1 - bind on function typed variable")
    public void testFunctionBind1() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("2 - bind on instance method")
    public void testFunctionBind2() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign 1 - argument variance across class hierarchy")
    public void testAssignFunctionType1() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign 2 - dynamic and null wrapped arguments")
    public void testAssignFunctionType2() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign 3 - optional and default arguments")
    public void testAssignFunctionType3() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign 4 - explicit abstract casts on arguments")
    public void testAssignFunctionType4() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign 5 - abstract typed function as argument")
    public void testAssignFunctionType5() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("assign 6 - superclass argument inside function argument")
    public void testAssignFunctionType6() throws Exception {
      doTestNoFixWithWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("call and new expressions")
  class CallAndNewExpressions {
    @Test
    @DisplayName("call expression")
    public void testCallExpression() throws Throwable {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("call expression rests")
    public void testCallExpressionRests() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("new expression")
    public void testNewExpression() throws Throwable {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("method rest arguments")
    public void testMethodRestArguments() throws Throwable {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("super constructor")
    public void testSuperConstructor() throws Throwable {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("operators")
  class Operators {
    @Test
    @DisplayName("postfix operator")
    public void testPostfixOperator() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("operator primitive test")
    public void testOperatorPrimitiveTest() throws Throwable {
      doTestNoFixWithWeakWarnings();
    }

    @Test
    @DisplayName("operator abstract test")
    public void testOperatorAbstractTest() throws Throwable {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("operator abstract test 2 - in operator overload type check")
    public void testOperatorAbstractTest2() throws Throwable {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("operator on constraints")
    public void testOperatorOnConstraints() throws Throwable {
      doTestNoFixWithWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("extern overloads")
  class ExternOverloads {
    @Test
    @DisplayName("extern overload local")
    public void testExternOverloadLocal() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("extern overload constructor")
    public void testExternOverloadConstructor() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("metadata overloads")
    public void testMetadataOverloads() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("extern overload instance")
    public void testExternOverloadInstance() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("extern overload static")
    public void testExternOverloadStatic() throws Exception {
      doTestNoFixWithWarnings();
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("enums")
  class Enums {
    @Test
    @DisplayName("same name enum values and constructors")
    public void testSameNameEnumValuesAndConstructors() throws Exception {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("enum has enum value members")
    public void testEnumHasEnumValueMembers() throws Throwable {
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("enum tools resolve")
    public void testEnumToolsResolve() throws Throwable {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWarnings();
    }

    // this might not be the right place for this test as its testing the resolver logic
    // but to verify the resolved results we need to do type compare
    @Test
    @DisplayName("enum type hints")
    public void testEnumTypeHints() throws Throwable {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("enum value match function")
    public void testEnumValueMatchFunction() throws Throwable {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("enum match pattern")
    public void testEnumMatchPattern() throws Throwable {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("enum switch resolve")
    public void testEnumSwitchResolve() throws Throwable {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("type and enum identical names")
    public void testTypeAndEnumIdenticalNames() throws Throwable {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWarnings("test/AbstractEnum.hx");
    }

    @Test
    @DisplayName("type tags should not resolve to enum value")
    public void testTypeTagsShouldNotResolveToEnumValue() throws Throwable {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("type tags should not resolve to enum value 2 - class named enum values coexist")
    public void testTypeTagsShouldNotResolveToEnumValue2() throws Throwable {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWarnings("test/EnumWithClassNameValues.hx");
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("switch and pattern matching")
  class SwitchPatternMatching {
    @Test
    @DisplayName("switch pattern matching")
    public void testSwitchPatternMatching() throws Throwable {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("switch pattern matching arrays")
    public void testSwitchPatternMatchingArrays() throws Throwable {
      myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
      doTestNoFixWithWarnings();
    }

    @Test
    @DisplayName("switch statements")
    public void testSwitchStatements() throws Throwable {
      doTestNoFixWithWarnings();
    }
  }
}
