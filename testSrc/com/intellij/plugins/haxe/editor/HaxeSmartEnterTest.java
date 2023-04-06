/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2020 Eric Bishton
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
package com.intellij.plugins.haxe.editor;

import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessor;
import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessors;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.build.IdeaTarget;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

/**
 * Created by as3boyan on 07.10.14.
 */
public class HaxeSmartEnterTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/smartEnter/";
  }

  public void doTest() {
    myFixture.configureByFile(getTestName(false) + ".hx");
    setTestStyleSettings(2);
    final List<SmartEnterProcessor> processors = SmartEnterProcessors.INSTANCE.forKey(HaxeLanguage.INSTANCE);
    new WriteCommandAction(myFixture.getProject()) {
      @Override
      protected void run(@NotNull Result result) throws Throwable {
        final Editor editor = myFixture.getEditor();
        for (SmartEnterProcessor processor : processors) {
          processor.process(myFixture.getProject(), editor, myFixture.getFile());
        }
      }
    }.execute();
    myFixture.checkResultByFile(getTestName(false) + "_after.hx", true);
  }

  @Test
  public void testMissingClassBody() {
    doTest();
  }

  @Test
  public void testPartialClassBody1() {
    doTest();
  }

  @Test
  public void testPartialClassBody2() {
    doTest();
  }

  @Test
  public void testIfFixer() {
    // Pre 18.3 versions don't format the code in the same way.  The spacing differs.
    if (IdeaTarget.IS_VERSION_18_3_COMPATIBLE) {
      doTest();
    }
  }

  @Test
  public void testSemicolonFixerFixReturn() {
    doTest();
  }

  @Test
  public void testSemicolonFixerFixAfterLastValidElement() {
    doTest();
  }
}
