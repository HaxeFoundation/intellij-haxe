package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.StandardPatterns;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;

import static com.intellij.codeInsight.completion.CompletionUtil.DUMMY_IDENTIFIER_TRIMMED;
import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.*;
import static com.intellij.plugins.haxe.ide.completion.HaxeCompletionUtil.isPreviousTextHash;
import static com.intellij.plugins.haxe.ide.completion.HaxeKeywordCompletionUtil.*;


public class HaxePPKeywordCompletionContributor extends CompletionContributor {

  public HaxePPKeywordCompletionContributor() {
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

    List<LookupElement> lookupElements = new ArrayList<>();


    if (isPreviousTextHash(position)) {
      addKeywords(lookupElements, PP_KEYWORDS, 2.0f);
      String prefix = createLookupString(position);
      CompletionResultSet set = result.withPrefixMatcher(prefix);
      set.addAllElements(lookupElements);
    }else {
      addKeywords(lookupElements, PP_KEYWORDS, -0.2f);
      result.addAllElements(lookupElements);
    }
  }

  private static @NonNull String createLookupString(PsiElement position) {
    return "#" + position.getText().replace(DUMMY_IDENTIFIER_TRIMMED, "");
  }


}
