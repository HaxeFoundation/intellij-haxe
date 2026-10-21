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
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Created by as3boyan on 07.10.14.
 */
@DisplayName("Editor: smart enter")
public class HaxeSmartEnterTest extends HaxeLightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/smartEnter/";
  }

  public void doTest() {
    myFixture.configureByFile(getTestName(false) + ".hx");
    setTestStyleSettings(2);
    final List<SmartEnterProcessor> processors = SmartEnterProcessors.INSTANCE.forKey(HaxeLanguage.INSTANCE);
    WriteCommandAction.writeCommandAction(myFixture.getProject()).run(() -> {
      final Editor editor = myFixture.getEditor();
      for (SmartEnterProcessor processor : processors) {
        processor.process(myFixture.getProject(), editor, myFixture.getFile());
      }
    });
    myFixture.checkResultByFile(getTestName(false) + "_after.hx", true);
  }

  @Test
  @DisplayName("missing class body")
  public void testMissingClassBody() {
    doTest();
  }

  @Test
  @DisplayName("partial class body 1 - unopened class body completed")
  public void testPartialClassBody1() {
    doTest();
  }

  @Test
  @DisplayName("partial class body 2 - unclosed brace completed")
  public void testPartialClassBody2() {
    doTest();
  }

  @Test
  @DisplayName("if fixer")
  public void testIfFixer() {
      doTest();
  }

  @Test
  @DisplayName("if fixer in dead branch")
  public void testIfFixerInDeadBranch() {
    doTest();
  }

  @Test
  @DisplayName("semicolon fixer fix return")
  public void testSemicolonFixerFixReturn() {
    doTest();
  }

  @Test
  @DisplayName("semicolon fixer fix after last valid element")
  public void testSemicolonFixerFixAfterLastValidElement() {
    doTest();
  }
}
