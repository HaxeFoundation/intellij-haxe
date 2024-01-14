/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
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

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.StandardPatterns;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.*;
import static com.intellij.plugins.haxe.ide.completion.HaxeKeywordCompletionPatterns.*;
import static com.intellij.plugins.haxe.ide.completion.HaxeKeywordCompletionUtil.*;
import static com.intellij.plugins.haxe.ide.completion.KeywordCompletionData.keywordOnly;
import static com.intellij.plugins.haxe.ide.completion.KeywordCompletionData.keywordWithSpace;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeKeywordCompletionContributor extends CompletionContributor {
  private static final Set<String> allowedKeywords = new HashSet<String>() {
    {
      for (IElementType elementType : HaxeTokenTypeSets.KEYWORDS.getTypes()) {
        add(elementType.toString());
      }
    }
  };

  public HaxeKeywordCompletionContributor() {


    // foo.b<caret> - bad
    // i<caret> - good
    extend(CompletionType.BASIC,
           psiElement().inFile(StandardPatterns.instanceOf(HaxeFile.class))
             .andNot(idInExpression.and(inComplexExpression)),
           new CompletionProvider<>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               suggestKeywords(parameters.getPosition(), result, context);
             }
           });
  }

  private static void suggestKeywords(PsiElement position, @NotNull CompletionResultSet result, ProcessingContext context) {
    List<String> keywordsFromParser = new ArrayList<>();
    final HaxeFile cloneFile = createCopyWithFakeIdentifierAsComment(position, keywordsFromParser);
    PsiElement completionElementAsComment = cloneFile.findElementAt(position.getTextOffset());

    List<LookupElement> lookupElements = new ArrayList<>();

    boolean isPPExpression = psiElement().withElementType(PPEXPRESSION).accepts(position);
    // avoid showing keyword suggestions when not relevant
    if (allowLookupPattern.accepts(completionElementAsComment)) {
      if (!isPPExpression) {
        if (dotFromIterator.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, Set.of(keywordOnly(OTRIPLE_DOT)));
          return;
        }

        if (packageExpected.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, PACKAGE_KEYWORD);
          return;
        }

        if (toplevelScope.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, TOP_LEVEL_KEYWORDS);
        }

        if (moduleScope.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, MODULE_STRUCTURES_KEYWORDS);
          addKeywords(lookupElements, VISIBILITY_KEYWORDS);
        }

        if (classDeclarationScope.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, CLASS_DEFINITION_KEYWORDS);
        }
        if (interfaceDeclarationScope.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, INTERFACE_DEFINITION_KEYWORDS);
        }

        if (abstractTypeDeclarationScope.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, ABSTRACT_DEFINITION_KEYWORDS);
        }

        if (interfaceBodyScope.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, INTERFACE_BODY_KEYWORDS);
        }

        if (classBodyScope.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, CLASS_BODY_KEYWORDS);
          addKeywords(lookupElements, VISIBILITY_KEYWORDS);
          addKeywords(lookupElements, ACCESSIBILITY_KEYWORDS);
        }


        if (functionBodyScope.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, METHOD_BODY_KEYWORDS);
          addKeywords(lookupElements, VALUE_KEYWORDS);
        }
        if (initScope.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, VALUE_KEYWORDS);
        }


        if (insideSwitchCase.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, SWITCH_BODY_KEYWORDS);
        }

        if (isAfterIfStatement.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, Set.of(keywordWithSpace(KELSE)));
        }

        if (isInsideLoopBlock.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, LOOP_BODY_KEYWORDS);
        }

        if (isInsideForIterator.accepts(completionElementAsComment)) {
          addKeywords(lookupElements, LOOP_ITERATOR_KEYWORDS);
        }

        HaxePropertyAccessor propertyAccessor = PsiTreeUtil.getParentOfType(position, HaxePropertyAccessor.class);
        if (isPropertyGetterValue.accepts(propertyAccessor)) {
          result.stopHere();
          lookupElements.clear();
          addKeywords(lookupElements, PROPERTY_KEYWORDS, 1.1f);
          addKeywords(lookupElements, Set.of(keywordOnly(PROPERTY_GET)), 1.2f);
        }
        if (isPropertySetterValue.accepts(propertyAccessor)) {
          result.stopHere();
          lookupElements.clear();
          addKeywords(lookupElements, PROPERTY_KEYWORDS, 1.1f);
          addKeywords(lookupElements, Set.of(keywordOnly(PROPERTY_SET)), 1.2f);
        }
      }
      addKeywords(lookupElements, PP_KEYWORDS, -0.2f);
      addKeywords(lookupElements, MISC_KEYWORDS, -0.1f);

    }
    result.addAllElements(lookupElements);
  }



  private static HaxeFile createCopyWithFakeIdentifierAsComment(PsiElement position, List<String> keywordsFromParser) {

    final HaxeFile posFile = (HaxeFile)position.getContainingFile();
    final TextRange posRange = position.getTextRange();

    // clone original content
    HaxeFile clonedFile = (HaxeFile)posFile.copy();
    int offset = posRange.getStartOffset();

    // replace dummy identifier with comment so it does not affect the parsing and psi structure
    PsiElement dummyIdentifier = clonedFile.findElementAt(offset);
    PsiElement comment = HaxeElementGenerator.createDummyComment(posFile.getProject(), dummyIdentifier.getTextLength());
    PsiElement elementToReplace = dummyIdentifier;

    //make sure we replace the "root" element of the identifier
    // we dont want to replace identifier inside a reference and keep the reference etc.
    while (elementToReplace.getPrevSibling() == null && elementToReplace.getParent() != null) {
      PsiElement parent = elementToReplace.getParent();
      if (parent == clonedFile) break;
      elementToReplace = parent;
    }
    elementToReplace.replace(comment);

    // reparse content
    HaxeFile file = (HaxeFile)PsiFileFactory.getInstance(posFile.getProject()).createFileFromText("a.hx", HaxeLanguage.INSTANCE, clonedFile.getText(), true, false);
    TreeUtil.ensureParsed(file.getNode());
    return file;
  }


}
