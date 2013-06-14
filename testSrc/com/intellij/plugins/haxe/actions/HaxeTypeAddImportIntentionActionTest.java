/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.actions;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.ide.actions.HaxeTypeAddImportIntentionAction;
import com.intellij.plugins.haxe.ide.index.HaxeComponentIndex;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeTypeAddImportIntentionActionTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/addImportIntention/";
  }

  public void doTest() {
    final PsiFile file = PsiDocumentManager.getInstance(myFixture.getProject()).getPsiFile(myFixture.getEditor().getDocument());
    assertNotNull(file);
    final HaxeType type = PsiTreeUtil.getParentOfType(file.findElementAt(myFixture.getCaretOffset()), HaxeType.class, false);
    assertNotNull(type);
    final GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(type);
    new HaxeTypeAddImportIntentionAction(type, HaxeComponentIndex
      .getItemsByName(type.getReferenceExpression().getText(), type.getProject(), scope))
      .execute();
    myFixture.checkResultByFile(getTestName(false) + ".txt");
  }

  public void testSimple() throws Throwable {
    myFixture.configureByFiles(getTestName(false) + ".hx", "foo/Bar.hx");
    doTest();
  }

  public void testHelper() throws Throwable {
    myFixture.configureByFiles(getTestName(false) + ".hx", "foo/Bar.hx");
    doTest();
  }
}
