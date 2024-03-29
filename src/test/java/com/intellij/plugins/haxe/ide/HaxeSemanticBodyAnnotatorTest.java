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

import com.intellij.lang.LanguageAnnotators;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorConfig;
import com.intellij.plugins.haxe.ide.annotator.HaxeUnresolvedTypeAnnotator;
import org.junit.Ignore;
import org.junit.Test;
//TODO mlo:
// This part of the semantics has been disabled for quite a while in the built version of the plugin and it looks like the code
// causes side effects for other code inspections (static imports seems to be an issue)
// current idea is to replace this logic with something like use "find usages" to resolve type parameters
// when those are not part of the resolved declaration
@Ignore
public class HaxeSemanticBodyAnnotatorTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/annotation.semantic.body/";
  }

  private void doTestNoFix(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings) throws Exception {
    boolean old = HaxeSemanticAnnotatorConfig.ENABLE_EXPERIMENTAL_BODY_CHECK;
    HaxeSemanticAnnotatorConfig.ENABLE_EXPERIMENTAL_BODY_CHECK = true;
    myFixture.configureByFiles(getTestName(false) + ".hx");
    final HaxeUnresolvedTypeAnnotator annotator = new HaxeUnresolvedTypeAnnotator();
    LanguageAnnotators.INSTANCE.addExplicitExtension(HaxeLanguage.INSTANCE, annotator);
    myFixture.enableInspections(getAnnotatorBasedInspection());
    myFixture.testHighlighting(true, false, false);
    HaxeSemanticAnnotatorConfig.ENABLE_EXPERIMENTAL_BODY_CHECK = old;
  }

  private void doTestNoFixWithWarnings() throws Exception {
    doTestNoFix(true, false, false);
  }

  private void doTestNoFixWithoutWarnings() throws Exception {
    doTestNoFix(false, false, false);
  }

  @Test
  public void testAssignUnknownTwice() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testUpdateUnknownInGenerics() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testUpdateUnknownOnLambdas() throws Exception {
    myFixture.configureByFiles("std/StdTypes.hx", "std/String.hx");
    doTestNoFixWithWarnings();
  }
}
