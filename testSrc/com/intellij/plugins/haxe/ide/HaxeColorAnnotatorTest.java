/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.annotator.HaxeColorAnnotator;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.testFramework.ExpectedHighlightingData;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import com.intellij.testFramework.fixtures.impl.JavaCodeInsightTestFixtureImpl;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeColorAnnotatorTest extends HaxeCodeInsightFixtureTestCase {

  protected String getBasePath() {
    return "/annotation/";
  }

  // Lifted out of CodeInsightTestFixtureImpl.java
  private PsiFile getHostFile() {
    return InjectedLanguageUtil.getTopLevelFile(myFixture.getFile());
  }


  public void doTest(String... additionalPaths) throws Exception {
    final String[] paths = ArrayUtil.append(additionalPaths, getTestName(false) + ".hx");
    myFixture.configureByFiles(ArrayUtil.reverseArray(paths));

    final HaxeColorAnnotator annotator = new HaxeColorAnnotator();
    try {
      LanguageAnnotators.INSTANCE.addExplicitExtension(HaxeLanguage.INSTANCE, annotator);
      myFixture.enableInspections(getAnnotatorBasedInspection());
      try {
//        myFixture.testHighlighting(true, true, true, myFixture.getFile().getVirtualFile());
        myFixture.testHighlighting(paths);
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        System.out.println(highlights);
      }
      finally {
        LanguageAnnotators.INSTANCE.removeExplicitExtension(HaxeLanguage.INSTANCE, annotator);
      }
    } finally {
      LanguageAnnotators.INSTANCE.removeExplicitExtension(HaxeLanguage.INSTANCE, annotator);
    }


  }

  public void testIDEA_ForIn () throws Throwable {
    doTest();
  }


}
