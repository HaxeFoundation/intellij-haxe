/*
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018 Eric Bishton
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
package com.intellij.plugins.haxe.ide.completion;

import com.intellij.patterns.*;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.plugins.haxe.util.HaxeDebugPsiUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.apache.log4j.Level;
import org.jetbrains.annotations.Nullable;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class HaxeCommonCompletionPattern {
  public static final PsiElementPattern.Capture<PsiElement> idInExpression =
    elementPattern("idInExpression")
      .withSuperParent(1, HaxeIdentifier.class)
      .withSuperParent(2, HaxeReference.class);
  public static final PsiElementPattern.Capture<PsiElement> inComplexExpression =
    elementPattern("inComplexExpression")
      .withSuperParent(2,
         psiElement()
           .withFirstChild(StandardPatterns.instanceOf(HaxeReference.class)));
  public static final PsiElementPattern.Capture<PsiElement> isSimpleIdentifier =
    elementPattern("isSimpleIdentifier")
      .andOr(StandardPatterns.instanceOf(HaxeType.class),
             idInExpression.andNot(inComplexExpression));

  public static final PsiElementPattern.Capture<PsiElement> matchUsingAndImport =
    elementPattern("matchUsingAndImport")
      .andOr(StandardPatterns.instanceOf(HaxeUsingStatement.class),
             StandardPatterns.instanceOf(HaxeImportStatement.class));

  public static final PsiElementPattern.Capture<PsiElement> inImportOrUsing =
    elementPattern("inImportOrUsing")
      .withSuperParent(3, matchUsingAndImport);

  public static final PsiElementPattern.Capture<PsiElement> skippableWhitespace =
    elementPattern("skippableWhitespace")
      .andOr(psiElement().withText(" "),
             psiElement().withText("\t"));
  public static final PsiElementPattern.Capture<PsiElement> inSwitchStatement =
    elementPattern("inSwitchStatement")
      .andOr(
        psiElement().withSuperParent(3,HaxeSwitchBlock.class),
        psiElement().withSuperParent(3,HaxeSwitchCase.class),
        psiElement().withSuperParent(3,HaxeDefaultCase.class),
        psiElement().withSuperParent(3,HaxeSwitchCaseBlock.class),
        psiElement().afterSiblingSkipping(skippableWhitespace,psiElement(HaxeSwitchStatement.class)) // Syntax error in stmt.
      );
  public static final PsiElementPattern.Capture<PsiElement> inFunctionTypeTag =
    elementPattern("inFunctionTypeTag")
      .afterLeafSkipping(skippableWhitespace, psiElement().withText(":"))
      .andNot(inSwitchStatement)
      ;

  /**
   * Create a new capture rule that requires the matched token to be a
   * PsiElement.
   *
   * Use this as the root capture rule instead of psiElement() when creating
   * rules that may need debugging.
   *
   * Note that you may have to cast the result of a chained rule to a
   * HaxeCapturePattern.
   *
   * @return a pattern that ensures the token is a PsiElement.
   */
  private static HaxeElementPattern<PsiElement> elementPattern(String name) {
    return pattern(name, new InitialPatternCondition<PsiElement>(PsiElement.class){
      @Override
      public boolean accepts(@Nullable Object o, ProcessingContext context) {
        return o instanceof PsiElement;
      }
    });
  }

  private static <T extends PsiElement> HaxeElementPattern<T> pattern(
    String name,
    InitialPatternCondition<T> capture) {
    return new HaxeElementPattern<>(name, capture);
  }

  /**
   * A wrapper around PsiElementPattern.Capture<> that can print the actual element,
   * context, and match pattern to which it applies.
   * @param <T>
   */
  private static class HaxeElementPattern<T extends PsiElement> extends PsiElementPattern.Capture<T> {

    static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
    static {
      // Use when you want to see the match pattern and parents.
      // LOG.setLevel(Level.DEBUG);

      // Use when you want the full local PSI tree (two parents up)
      // LOG.setLevel(Level.TRACE);
    }

    String myName;

    public HaxeElementPattern(String name, final InitialPatternCondition<T> pattern) {
      super(pattern);
      myName = name;
    }

    @Override
    public boolean accepts(@Nullable Object o, ProcessingContext context) {
      boolean matches = super.accepts(o, context);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Pattern " + myName + " " + (matches ? "matched" : "failed")
                  + ": " + toString());
        LOG.debug("For " + o.getClass() + "\n" +
                  (o instanceof PsiElement
                   ? "with path\n" + HaxeDebugPsiUtil.formatElementPath((PsiElement)o, false)
                   : ""));
        if (o instanceof PsiElement && LOG.isTraceEnabled()) {
          PsiElement element = (PsiElement)o;
          int dumplevel = "IntellijIdeaRulezzz".equals(element.getText()) ? 5 : 2;
          PsiElement superParent = HaxeDebugPsiUtil.getParents((PsiElement)o, dumplevel, false);
          LOG.trace(HaxeDebugPsiUtil.printElementTree(superParent));
        }
      }
      return matches;
    }
  }
}
