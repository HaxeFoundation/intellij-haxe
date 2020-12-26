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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.build.IdeaTarget;
import com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotator;
import com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections;
import com.intellij.plugins.haxe.ide.annotator.HaxeTypeAnnotator;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.util.ArrayUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.*;

public class HaxeSemanticAnnotatorTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  public void setUp() throws Exception {
    useHaxeToolkit();
    super.setUp();
    setTestStyleSettings(2);
  }

  @Override
  protected String getBasePath() {
    return "/annotation.semantic/";
  }

  private void doTest(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings,
                      @Nullable Set<Class<? extends LocalInspectionTool>> unsetInspections,
                      String... additionalFiles)
    throws Exception
  {
    myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{getTestName(false) + ".hx"}, additionalFiles));
    LanguageAnnotators.INSTANCE.addExplicitExtension(HaxeLanguage.INSTANCE, new HaxeTypeAnnotator());
    myFixture.enableInspections(getAnnotatorBasedInspection());
    registerInspectionsForTesting(HaxeSemanticAnnotator.getInspectionProvider(), myFixture.getProject(), unsetInspections);
    myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings);
  }

  public void registerInspectionsForTesting(InspectionToolProvider provider, Project project,
                                            @Nullable Set<Class<? extends LocalInspectionTool>> unsetInspections) {
    InspectionProfileManager mgr = InspectionProfileManager.getInstance(project);
    InspectionProfileImpl profile = mgr.getCurrentProfile();

    try {
      Class<? extends LocalInspectionTool>[] classes = provider.getInspectionClasses();
      for (Class<? extends LocalInspectionTool> c : classes) {
        if (null != unsetInspections && unsetInspections.contains(c)) continue;

        Constructor<? extends LocalInspectionTool> constructor = c.getDeclaredConstructor();
        constructor.setAccessible(true);
        InspectionToolWrapper<?, ?> wrapper = new LocalInspectionToolWrapper(constructor.newInstance());

        Map<String, List<String>> dependencies = new HashMap<>();
        profile.addTool(project, wrapper, dependencies);
        profile.enableTool(wrapper.getShortName(), project);
      }
    } catch (Exception ex) {
      assertNotNull(ex.toString());
    }
  }

  private void doTestSkippingAnnotators(Set<Class<? extends LocalInspectionTool>> unsetInspections) throws Exception {
    doTest(true, false, false, unsetInspections);
  }

  private void doTestNoFixWithWarnings(String... additionalFiles) throws Exception {
    doTest(true, false, false, null, additionalFiles);
  }

  private void doTestNoFixWithoutWarnings(String... additionalFiles) throws Exception {
    doTest(false, false, false, null, additionalFiles);
  }

  private void doTestActions(String... filters) throws Exception {
    doTest(false, false, false, null);

    List<IntentionAction> intentions = myFixture.getAvailableIntentions();
    for (final IntentionAction action : intentions) {
      if (Arrays.asList(filters).contains(action.getText())) {
        System.out.println("Applying intent " + action.getText());
        myFixture.launchAction(action);
      } else {
        System.out.println("Ignoring intent " + action.getText() + ", not matching " + StringUtils.join(filters, ","));
      }
    }
    FileDocumentManager.getInstance().saveAllDocuments();
    myFixture.checkResultByFile(getTestName(false) + "_expected.hx");
  }

  public void testFixPackage() throws Exception {
    doTestActions("Fix package");
  }

  public void testRemoveOverride() throws Exception {
    doTestActions("Remove override");
  }

  public void testRemoveFinal() throws Exception {
    doTestActions("Remove final from Base.test");  // @:final, but the @: is no longer in the fix message.
  }

  public void testChangeArgumentType() throws Exception {
    doTestActions(HaxeBundle.message("haxe.quickfix.change.variable.type"));
  }

  public void testRemoveArgumentInit() throws Exception {
    doTestActions(HaxeBundle.message("haxe.quickfix.remove.initializer"));
  }

  public void testInterfaceMethodsShouldHaveTypeTags() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOptionalFieldSyntax() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOptionalMethodSyntax() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOptionalWithInitWarning() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNonOptionalArgumentsAfterOptionalOnes() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNonConstantArgument() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNonConstantArgumentAbstractEnum() throws Exception {
    doTestNoFixWithWarnings("test/SampleAbstractEnum.hx");
  }

  public void testConstructorMustNotBeStatic() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testInitMagicMethodShouldBeStatic() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testRepeatedArgumentName() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAbstractFromTo() throws Exception {
    doTestNoFixWithWarnings();
  }
  public void testAbstractFromToMetadata() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAbstractAssignmentFromTo1() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAbstractAssignmentFromTo2() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAbstractAssignmentFromTo3() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAbstractAssignmentFromTo4() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAbstractAssignmentFromTo5() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAbstractAssignmentFromTo6() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNullFunction() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOverrideVisibility() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testUcFirstClassName() throws Exception {
    doTestActions("Change name");
  }

  public void testUcFirstClassName2() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testRepeatedFields() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testPropertiesSimpleCheck() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testPropertyAllowNonConstantInitialization() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOverrideSignature() throws Exception {
    doTestActions("Remove argument");
  }

  public void testOverrideSignature2() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOverrideSignature3() throws Exception {
    doTestActions("Remove argument");
  }

  public void testOverrideSignature4() throws Exception {
    doTestActions("Remove argument");
  }

  public void testOverrideSignature5() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testImplementSignature() throws Exception {
    doTestNoFixWithWarnings();
  }
  public void testImplementMethods() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testImplementExternInterface() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testSimpleAssignUnknownGeneric() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testExtendsAnonymousType() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testExtendsSelf() throws Exception {
    doTestNoFixWithWarnings("test/Bar.hx", "test/IBar.hx", "test/TBar.hx");
  }

  public void testFieldInitializerCheck() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testVariableRedefinition() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testFinalKeyword() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testFinalKeywordInInterface() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testStaticsInExtended() throws Exception {
    doTestNoFixWithoutWarnings();
  }

  public void testArrayAssignmentFromEmpty() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testArrayAssignmentBadFunctionType() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testArrayAssignmentWrongType() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testArrayAssignmentBadArrowFunction() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testArrayAssignmentWithAbstract() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testArrayAssignmentWithArrowFunctions() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNullTAssignment1() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNullTAssignment2() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var a:Int = 10/2;
  public void testInitializeIntWithFloat() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAssignFloatToInt() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var a:Int = "3.1416";
  public void testInitializeFloatWithString() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAssignStringToFloat() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAssignStringToDynamic() throws Exception {
    doTestNoFixWithWarnings();
  }

  // var a:Int = (10.0 : Float);
  public void testInitializeIntWithTypeCheckFloat() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAssignTypeCheckFloatToInt() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var a:Int = (10 : Float);
  public void testInitializeIntWithIntTypeCheckedToFloat() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAssignIntWithIntTypeCheckedToFloat() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var a:String = 3.1416;
  public void testInitializeStringWithFloat() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAssignFloatToString() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var a:String = 10;
  public void testInitializeStringWithInt() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAssignIntToString() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var a:Float = 100;
  public void testInitializeFloatWithInt() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAssignIntToFloat() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var f:Float = 100; i:Int = (f);
  public void testInitializeIntWithParenthesizedFloat() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAssignParenthesizedFloatToInt() throws Exception {
    doTestNoFixWithWarnings();
  }


// NOT Working yet.
  // var c:Int = {x:1, y:2};
  //public void testInitializeIntWithAnonymousStruct() throws Exception {
  //  doTestNoFixWithWarnings();
  //}
  //
  //public void testAssignAnonymousStructToInt() throws Exception {
  //  doTestNoFixWithWarnings();
  //}


  // typedef Pt = {x:Int; y:Int;}; var c:Int = new Pt();
  public void testInitializeIntWithTypedef() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAssignTypedefToInt() throws Exception {
    doTestNoFixWithWarnings();
  }

  // class Point {...}; var c:Int = new Point(1,2);
  public void testInitializeIntWithClass() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAssignClassToInt() throws Exception {
    doTestNoFixWithWarnings();
  }

// NOT working yet.
  // class Test{ var somevar; function new() { somevar = 3.1; }
  //public void testUnknownClassVariable() throws Exception {
  //  doTestNoFixWithWarnings();
  //}

  // class Test{ var somevar:Int; function new() { somevar = 3; }
  public void testAssignFloatToTypedClassVarDeclaration() throws Exception {
    doTestNoFixWithWarnings();
  }

  // class Test{ var somevar = 10; function new() {somevar = 3.1;} }
  public void testAssignFloatToInferredClassVarInt() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testMultipleClassModifiers() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorOnMultipleNullT() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoIncompatibleTypeErrorOnMap() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoIncompatibleTypeErrorOnChainedMaps() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testEitherTypeTest() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorWhenTypeParameterIsSelfClass() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorAccessingParameterizedArray() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorAccessingFieldsThroughParamaterizedMethods() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorAssigningFromParameterizedFunction() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorAssigningToParameterizedArrayElement() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorAssigningParameterizedTypeDuringVarInit() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorOnOverrideDefinition() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorOnConstrainedGenericOverrides() throws Exception {
      doTestNoFixWithWarnings();
  }

  public void testMissingInterfaceMethodsOnConstrainedGenericOverrides() throws Exception {
    // Pre-2020.1, the underlying (Java) class/symbol map code worked a bit differently, and the
    // missing overrides were not detected.
    if (IdeaTarget.IS_VERSION_20_1_COMPATIBLE) {
      doTestNoFixWithWarnings();
    }
  }

  //public void testAssignmentOfParameterizedType() throws Exception {
  //  doTestNoFixWithWarnings();
  //}

  public void testNoErrorOnOptionalParameterWithIntFieldConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorOnOptionalParameterWithSimpleStringFieldConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorOnOptionalParameterWithParenthesizedStringFieldConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorOnOptionalParameterWithParenthesizedNumericFieldConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testTypeErrorOnOptionalParameterWithParenthesizedNumericFieldConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testErrorOnOptionalParameterWithNonConstMethod() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorOnOptionalParameterWithDoublyReferencedVar() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorOnEnumConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorOnOptionalNullFloatConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorOnConstantFunctionType() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorOnInlineFunctionAssignment() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorOnFunctionAssignment() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNoErrorOnFunctionUnification() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testInferredFunctionTypeAssignment() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testParameterizedFunctions() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testImmediateStringArrayIndexing() throws Exception {
    doTestNoFixWithWarnings();
  }

  //Issue #981
  public void testAssignReflectionTypeToDynamic() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testInitializeStringMapWithMapLiteral() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testInitializeIntMapWithMapLiteral() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testInitializeEnumMapWithMapLiteral() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testCrashWhileAnnotating() throws Exception {
    // A stack overflow was occurring while annotating, and there's no better place to
    // put this test at the moment, soo....
    doTestNoFixWithoutWarnings();
  }

  public void testAssignTypeToClass() throws Exception {
    doTestNoFixWithWarnings();
  }
  public void testAssignTypeToEnum() throws Exception {
    doTestNoFixWithWarnings();
  }
  public void testAssignEnumValue() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAssignEmptyCollection() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testImplicitCast() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testInitializeObjectWithGenericFunction() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testIsKeywordFor4_2() throws Throwable {
    HashSet skipAnnotators = new HashSet();
    skipAnnotators.add(HaxeSemanticAnnotatorInspections.IsTypeExpressionInspection4dot1Compatible.class);
    doTestSkippingAnnotators(skipAnnotators);
  }

  public void testIsKeywordFor4_1() throws Throwable {
    doTestSkippingAnnotators(new HashSet<>());
  }

  public void testEnumHasEnumValueMembers() throws Throwable {
    doTestSkippingAnnotators(new HashSet<>());
  }

  public void testTypeParameterConstraints() throws Throwable {
    doTestSkippingAnnotators(new HashSet<>());
  }

  public void testStringInterpolation() throws Throwable {
    doTestNoFixWithWarnings();
  }
}
