/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.lang.parser;

import com.intellij.lang.ASTFactory;
import com.intellij.lang.LanguageASTFactory;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLFileType;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLLanguage;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLParserDefinition;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.LazyParseableElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.testFramework.ParsingTestCase;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * Jupiter front for the platform's JUnit3-style {@link ParsingTestCase};
 * same pattern as {@link HaxeParsingTestBase}, for the HXML grammar,
 * including its conditional write-intent context around engine calls.
 */
abstract public class HxmlParsingTestBase {
  private final Engine engine;

  public HxmlParsingTestBase(String... path) {
    engine = new Engine(String.join("/", path));
  }

  @BeforeEach
  final void startParsingEngine(TestInfo info) throws Exception {
    // the JUnit3 test name drives the data-file lookup (testSimple -> Simple.hxml)
    engine.setName(info.getTestMethod().orElseThrow().getName());
    HaxeParsingTestBase.runEngineStep(engine::start);
  }

  @AfterEach
  final void stopParsingEngine() throws Exception {
    engine.stop();
  }

  protected void doTest(boolean checkResult) {
    engine.doTest(checkResult);
  }

  private static final class Engine extends ParsingTestCase {
    Engine(String path) {
      super(path, HXMLFileType.DEFAULT_EXTENSION, new HXMLParserDefinition());
    }

    @Override
    protected void setUp() throws Exception {
      super.setUp();
      addExplicitExtension(LanguageASTFactory.INSTANCE, HXMLLanguage.INSTANCE, new ASTFactory() {
        @Nullable
        @Override
        public LazyParseableElement createLazy(ILazyParseableElementType type, CharSequence sequence) {
          return super.createLazy(type, sequence);
        }

        @Nullable
        @Override
        public CompositeElement createComposite(IElementType type) {
          return super.createComposite(type);
        }

        @Nullable
        @Override
        public LeafElement createLeaf(IElementType type, CharSequence text) {
          // We're making our default token type be a PsiJavaToken so that our
          // PSI tree is more compatible with the Java one, thus, we can use
          // more of the Java code without doing so much work.
          //if (HaxeTokenTypeSets.COMMENTS.contains(type)) {
          //  return new PsiCommentImpl(type, text);
          //}
          //return new PsiJavaTokenImpl(type, text);
          return null;
        }
      });
    }

    @Override
    protected String getTestDataPath() {
      return HaxeTestUtils.BASE_TEST_DATA_PATH;
    }

    @Override
    protected boolean skipSpaces() {
      return true;
    }

    void start() throws Exception {
      setUp();
    }

    void stop() throws Exception {
      tearDown();
    }

    @Override
    public void doTest(boolean checkResult) {
      super.doTest(checkResult);
    }
  }
}
