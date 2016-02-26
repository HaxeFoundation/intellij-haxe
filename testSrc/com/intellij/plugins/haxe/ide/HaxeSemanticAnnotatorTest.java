/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
import com.intellij.codeInspection.DefaultHighlightVisitorBasedInspection;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.annotator.HaxeTypeAnnotator;
import com.intellij.util.ArrayUtil;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

public class HaxeSemanticAnnotatorTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/annotation.semantic/";
  }

  private void doTestNoFix(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings, String... additionalFiles) throws Exception {
    myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{getTestName(false) + ".hx"}, additionalFiles));
    final HaxeTypeAnnotator annotator = new HaxeTypeAnnotator();
    LanguageAnnotators.INSTANCE.addExplicitExtension(HaxeLanguage.INSTANCE, annotator);
    myFixture.enableInspections(new DefaultHighlightVisitorBasedInspection.AnnotatorBasedInspection());
    myFixture.testHighlighting(true, false, false);
  }

  private void doTestNoFixWithWarnings(String... additionalFiles) throws Exception {
    doTestNoFix(true, false, false, additionalFiles);
  }

  private void doTestNoFixWithoutWarnings(String... additionalFiles) throws Exception {
    doTestNoFix(false, false, false, additionalFiles);
  }

  private void doTest(String... filters) throws Exception {
    doTestNoFixWithoutWarnings();
    for (final IntentionAction action : myFixture.getAvailableIntentions()) {
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
    doTest("Fix package");
  }

  public void testRemoveOverride() throws Exception {
    doTest("Remove override");
  }

  public void testRemoveFinal() throws Exception {
    doTest("Remove @:final from Base.test");
  }

  public void testChangeArgumentType() throws Exception {
    doTest("Change type");
  }

  public void testRemoveArgumentInit() throws Exception {
    doTest("Remove init");
  }

  public void testInterfaceMethodsShouldHaveTypeTags() throws Exception {
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

  public void testNullFunction() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOverrideVisibility() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testUcFirstClassName() throws Exception {
    doTest("Change name");
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
    doTest("Remove argument");
  }

  public void testOverrideSignature2() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOverrideSignature3() throws Exception {
    doTest("Remove argument");
  }

  public void testOverrideSignature4() throws Exception {
    doTest("Remove argument");
  }

  public void testImplementSignature() throws Exception {
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
}
