package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Key;
import com.intellij.patterns.StandardPatterns;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.PsiIdentifiedElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeLocalReferenceCompletionContributor extends CompletionContributor {
  public HaxeLocalReferenceCompletionContributor() {
    extend(CompletionType.BASIC, psiElement().inFile(StandardPatterns.instanceOf(HaxeFile.class)),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               final Set<String> suggestedKeywords = new THashSet<String>();
               PsiTreeUtil.treeWalkUp(new MyPsiScopeProcessor(suggestedKeywords), parameters.getPosition(), null, new ResolveState());
               for (String keyword : suggestedKeywords) {
                 result.addElement(LookupElementBuilder.create(keyword));
               }
             }
           });
  }

  private static class MyPsiScopeProcessor implements PsiScopeProcessor {
    private final Set<String> result;

    private MyPsiScopeProcessor(Set<String> result) {
      this.result = result;
    }

    @Override
    public boolean execute(PsiElement element, ResolveState state) {
      if (element instanceof PsiIdentifiedElement) {
        final PsiIdentifiedElement psiIdentifiedElement = (PsiIdentifiedElement)element;
        if (psiIdentifiedElement.getIdentifier() != null) {
          result.add(psiIdentifiedElement.getIdentifier().getText());
        }
      }
      return true;
    }

    @Override
    public <T> T getHint(Key<T> hintKey) {
      return null;
    }

    @Override
    public void handleEvent(Event event, @Nullable Object associated) {
    }
  }
}
