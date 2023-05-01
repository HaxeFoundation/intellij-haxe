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

abstract public class HxmlParsingTestBase extends ParsingTestCase {

  public HxmlParsingTestBase(String... path) {
    super(getPath(path), HXMLFileType.DEFAULT_EXTENSION, new HXMLParserDefinition());
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

  private static String getPath(String... args) {
    final StringBuilder result = new StringBuilder();
    for (String folder : args) {
      if (result.length() > 0) {
        result.append("/");
      }
      result.append(folder);
    }
    return result.toString();
  }

  @Override
  protected String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH;
  }

  @Override
  protected boolean skipSpaces() {
    return true;
  }
}
