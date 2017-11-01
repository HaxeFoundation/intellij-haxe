/*
 * Copyright 2017 Eric Bishton
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

import com.intellij.lang.ASTNode;
import com.intellij.lang.LighterLazyParseableNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.impl.PsiBuilderImpl;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.SharedImplUtil;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class HaxePsiBuilder extends PsiBuilderImpl {

  private static final Logger LOG = Logger.getLogger("#HaxePsiBuilder");
  static { LOG.setLevel(Level.DEBUG); } // Remove when finished debugging.

  private final PsiFile psiFile;

  public HaxePsiBuilder(@NotNull ParserDefinition parserDefinition,
                        @NotNull Lexer lexer,
                        @NotNull final CharSequence text) {
    super(null, null, parserDefinition, lexer, null, text, null, null);
    psiFile = null;
  }

  public HaxePsiBuilder(@NotNull Project project,
                        @NotNull ParserDefinition parserDefinition,
                        @NotNull Lexer lexer,
                        @NotNull ASTNode chameleon,
                        @NotNull CharSequence text) {
    super(project, parserDefinition, lexer, chameleon, text);
    psiFile = SharedImplUtil.getContainingFile(chameleon);
  }

  public HaxePsiBuilder(@NotNull Project project,
                        @NotNull ParserDefinition parserDefinition,
                        @NotNull Lexer lexer,
                        @NotNull LighterLazyParseableNode chameleon,
                        @NotNull CharSequence text) {
    super(project, parserDefinition, lexer, chameleon, text);
    psiFile = chameleon.getContainingFile();
  }


    @Override
  public void error(String messageText) {
    if (LOG.isDebugEnabled()) {
      PsiDocumentManager mgr = PsiDocumentManager.getInstance(getProject());
      Document doc = null != mgr ? mgr.getDocument(psiFile) : null;
      int offset = getCurrentOffset();
      int line = null != doc ? doc.getLineNumber(offset) : StringUtil.offsetToLineNumber(getOriginalText(), offset);

      StringBuilder s = new StringBuilder();
      s.append("Parsing error at ");
      s.append(null != psiFile ? psiFile.getName() : "<text>");
      s.append(", ");
      s.append(line);
      s.append(": '");
      s.append(getTokenText());
      s.append("'.  ");
      s.append(messageText);

      LOG.debug(s.toString());
    }

    super.error(messageText);
  }

}
