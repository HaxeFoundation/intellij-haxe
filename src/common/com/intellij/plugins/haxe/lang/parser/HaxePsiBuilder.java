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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.util.ASTNodeWalker;
import com.intellij.plugins.haxe.util.Lambda;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.SharedImplUtil;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class HaxePsiBuilder extends PsiBuilderImpl {

  private static final Logger LOG = Logger.getLogger("#HaxePsiBuilder");

  private static final HashSet<String> reportedErrors = new HashSet<String>();

  private final PsiFile psiFile;

  public HaxePsiBuilder(@NotNull ParserDefinition parserDefinition,
                        @NotNull Lexer lexer,
                        @NotNull final CharSequence text) {
    super(ProjectManager.getInstance().getDefaultProject(), null, parserDefinition, lexer, null, text, null, null);
    psiFile = null;
    setupDebugTraces();
  }

  public HaxePsiBuilder(@NotNull Project project,
                        @NotNull ParserDefinition parserDefinition,
                        @NotNull Lexer lexer,
                        @NotNull ASTNode chameleon,
                        @NotNull CharSequence text) {
    super(project, parserDefinition, lexer, chameleon, text);
    psiFile = SharedImplUtil.getContainingFile(chameleon);
    setupDebugTraces();
  }

  public HaxePsiBuilder(@NotNull Project project,
                        @NotNull ParserDefinition parserDefinition,
                        @NotNull Lexer lexer,
                        @NotNull LighterLazyParseableNode chameleon,
                        @NotNull CharSequence text) {
    super(project, parserDefinition, lexer, chameleon, text);
    psiFile = chameleon.getContainingFile();
    setupDebugTraces();
  }

  private void setupDebugTraces() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      // The trace output causes unit tests to fail. :/
      setDebugMode(false);
    } else {
      setDebugMode(LOG.isTraceEnabled());
    }
  }

  @NotNull
  @Override
  public ASTNode getTreeBuilt() {
    ASTNode built = super.getTreeBuilt();

    if (LOG.isDebugEnabled() && !ApplicationManager.getApplication().isUnitTestMode()) {
      // Walk the tree, depth first and print out all remaining error elements.
      new ASTNodeWalker().walk(built, new Lambda<ASTNode>() {
        @Override
        public boolean process(ASTNode node) {
          if (node instanceof PsiErrorElement) {
            PsiErrorElement err = (PsiErrorElement)node;
            printErrorInfo(err.getTextRange().getStartOffset(),
                           err.getText(),
                           err.getErrorDescription());
          }
          return true;
        }
      });
    }

    return built;
  }

  private void printErrorInfo(int offset, String token, String message) {

    PsiDocumentManager mgr = PsiDocumentManager.getInstance(getProject());
    Document doc = null != mgr ? mgr.getDocument(psiFile) : null;
    int line = null != doc ? doc.getLineNumber(offset) + 1 : StringUtil.offsetToLineNumber(getOriginalText(), offset);

    StringBuilder s = new StringBuilder();
    s.append("Parsing error at ");
    s.append(null != psiFile ? psiFile.getName() : "<text>");
    s.append(", ");
    s.append(line);
    s.append(": '");
    s.append(null != token ? token : "<unknown token>");
    s.append("'.  ");
    s.append(null != message ? message : "<no message>");

    String error = s.toString();
    boolean needToReport;
    synchronized (this) {
      needToReport = reportedErrors.add(error);
    }
    if (needToReport) {
      LOG.debug(error);
    }
  }
}
