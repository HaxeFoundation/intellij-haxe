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

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.DefaultHighlightVisitorBasedInspection;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.annotator.HaxeTypeAnnotator;
import com.intellij.plugins.haxe.ide.inspections.HaxeUnresolvedSymbolInspection;
import com.intellij.util.ActionRunner;
import com.intellij.util.ArrayUtil;

public class HaxeSemanticAnnotatorTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/annotation.semantic/";
  }

  private void doTest(String... additionalPaths) throws Exception {
    final String[] paths = ArrayUtil.append(additionalPaths, getTestName(false) + ".hx");
    myFixture.configureByFiles(ArrayUtil.reverseArray(paths));
    final HaxeTypeAnnotator annotator = new HaxeTypeAnnotator();
    LanguageAnnotators.INSTANCE.addExplicitExtension(HaxeLanguage.INSTANCE, annotator);
    myFixture.enableInspections(new DefaultHighlightVisitorBasedInspection.AnnotatorBasedInspection());
    try {
      myFixture.testHighlighting(true, true, true, myFixture.getFile().getVirtualFile());
    }
    finally {
      LanguageAnnotators.INSTANCE.removeExplicitExtension(HaxeLanguage.INSTANCE, annotator);
    }
  }

  public void testFixPackage() throws Exception {
    doTest2();
  }

  private void doTest2() throws Exception {
    doTest();
    for (final IntentionAction action : myFixture.getAvailableIntentions()) {
      WriteCommandAction.runWriteCommandAction(getProject(), new Runnable() {
        @Override
        public void run() {
          //System.out.println(action);
          //if (CodeInsightUtilBase.prepareEditorForWrite(myFixture.getEditor())) {
          action.invoke(myFixture.getProject(), myFixture.getEditor(), myFixture.getFile());
          //}
        }
      });
    }
    FileDocumentManager.getInstance().saveAllDocuments();
    myFixture.checkResultByFile(getTestName(true) + "_expected.hx");
    //myFixture.getEditor().getDocument().insertString();
  }

  /*
    myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{getTestName(true) + ".hx"}, additionalFiles));
    OptimizeImportsAction.actionPerformedImpl(DataManager.getInstance().getDataContext(myFixture.getEditor().getContentComponent()));
    FileDocumentManager.getInstance().saveAllDocuments();
    myFixture.checkResultByFile(getTestName(true) + "_expected.hx");
   */
}
