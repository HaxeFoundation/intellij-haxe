/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Eric Bishton
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
package com.intellij.plugins.haxe.ide.refactoring.introduce;

import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author: Fedor.Korotkov
 */
public abstract class HaxeIntroduceTestBase extends HaxeCodeInsightFixtureTestCase {
  protected void doTestSuggestions(Class<? extends HaxeExpression> parentClass, String... expectedNames) {
    final Collection<String> names = buildSuggestions(parentClass);
    for (String expectedName : expectedNames) {
      assertTrue(StringUtil.join(names, ", "), names.contains(expectedName));
    }
  }

  protected Collection<String> buildSuggestions(Class<? extends HaxeExpression> parentClass) {
    String name = getTestName(true);
    name = convertStringFirstLetterToUppercase(name);
    myFixture.configureByFile(name + ".hx");
    HaxeIntroduceHandler handler = createHandler();
    HaxeExpression expr = PsiTreeUtil.getParentOfType(
      myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset()),
      parentClass
    );
    return handler.getSuggestedNames(expr);
  }

  protected abstract HaxeIntroduceHandler createHandler();

  protected void doTest() {
    doTest(null, true);
  }

  protected void doTest(@Nullable Consumer<HaxeIntroduceOperation> customization, boolean replaceAll) {
    doTest(customization, replaceAll, "foo", false);
  }

  protected void doTest(@Nullable Consumer<HaxeIntroduceOperation> customization, boolean replaceAll, String newname, boolean inPlace) {
    String testName = getTestName(true);
    testName = convertStringFirstLetterToUppercase(testName);
    myFixture.configureByFile(testName + ".hx");
    boolean inplaceEnabled = myFixture.getEditor().getSettings().isVariableInplaceRenameEnabled();
    try {
      myFixture.getEditor().getSettings().setVariableInplaceRenameEnabled(inPlace);
      HaxeIntroduceHandler handler = createHandler();
      final HaxeIntroduceOperation operation =
        new HaxeIntroduceOperation(myFixture.getProject(), myFixture.getEditor(), myFixture.getFile(), newname);
      operation.setReplaceAll(replaceAll);
      if (customization != null) {
        customization.consume(operation);
      }
      handler.performAction(operation);
      myFixture.checkResultByFile(testName + ".after.hx");
    }
    finally {
      myFixture.getEditor().getSettings().setVariableInplaceRenameEnabled(inplaceEnabled);
    }
  }

  private String convertStringFirstLetterToUppercase(String testName) {
    testName = Character.toUpperCase(testName.charAt(0)) + testName.substring(1);
    return testName;
  }

  protected void doTestInplace(@Nullable Consumer<HaxeIntroduceOperation> customization) {
    doTestInplace(customization, true, "a");
  }

  protected void doTestInplace(@Nullable Consumer<HaxeIntroduceOperation> customization, boolean replaceAll, String newname) {
    doTest(customization, replaceAll, newname, true);
  }
}
