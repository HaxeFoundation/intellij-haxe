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
package com.intellij.plugins.haxe.ide;

import com.intellij.find.findUsages.AbstractFindUsagesDialog;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.application.ReadActionProcessor;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeNewExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeFindUsagesHandler extends FindUsagesHandler {

  public static final PsiElement[] NO_ELEMENTS = {};

  private final PsiElement[] extraElementsToSearch;

  protected HaxeFindUsagesHandler(@NotNull PsiElement psiElement) {
    this(psiElement, NO_ELEMENTS);
  }

  protected HaxeFindUsagesHandler(@NotNull PsiElement psiElement, @Nullable PsiElement[] others) {
    super(psiElement);
    extraElementsToSearch = others != null ? others : NO_ELEMENTS;
  }

  @NotNull
  @Override
  public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
    return super.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab);
  }

  @NotNull
  @Override
  public PsiElement[] getPrimaryElements() {
    // Return the element.  In Java, this checks for the named parameter in overriding methods (and lamdas) and adds them to to search scope.
    // See @{JavaFindUsagesProvider#getPrimaryElements}

    // We check for the presence of overriding methods, et cetera, before we create this handler.
    return ArrayUtil.mergeArrays(super.getPrimaryElements(), extraElementsToSearch);
  }

  @Override
  public boolean processElementUsages(@NotNull PsiElement element,
                                      @NotNull Processor<UsageInfo> processor,
                                      @NotNull FindUsagesOptions options) {
    final SearchScope scope = options.searchScope;

    final boolean searchText = options.isSearchForTextOccurrences && scope instanceof GlobalSearchScope;

    if (options.isUsages) {
      ReadActionProcessor<PsiReference> searchProcessor;
      PsiElement searchElement;
      boolean fastTrack = true;

      if (element instanceof HaxeMethodDeclaration && ((HaxeMethodDeclaration)element).isConstructor()) {
        searchElement = ((HaxeMethodDeclaration)element).getModel().getDeclaringClass().haxeClass;
        searchProcessor = getConstructorSearchProcessor(processor);
        fastTrack = false;
      } else {
        searchElement = element;
        searchProcessor = getSimpleSearchProcessor(processor);
      }

      final ReferencesSearch.SearchParameters parameters =
        new ReferencesSearch.SearchParameters(searchElement, scope, false, fastTrack ? options.fastTrack : null);
      final boolean success = ReferencesSearch.search(parameters).forEach(searchProcessor);

      if (!success) return false;
    }

    if (searchText) {
      if (options.fastTrack != null) {
        options.fastTrack.searchCustom(consumer -> processUsagesInText(element, processor, (GlobalSearchScope)scope));
      } else {
        return processUsagesInText(element, processor, (GlobalSearchScope)scope);
      }
    }
    return true;
  }

  @NotNull
  public ReadActionProcessor<PsiReference> getSimpleSearchProcessor(@NotNull Processor<UsageInfo> processor) {
    return new ReadActionProcessor<PsiReference>() {
      @Override
      public boolean processInReadAction(final PsiReference ref) {
        TextRange rangeInElement = ref.getRangeInElement();
        return processor
          .process(new UsageInfo(ref.getElement(), rangeInElement.getStartOffset(), rangeInElement.getEndOffset(), false));
      }
    };
  }

  @NotNull
  public ReadActionProcessor<PsiReference> getConstructorSearchProcessor(@NotNull Processor<UsageInfo> processor) {
    return new ReadActionProcessor<PsiReference>() {
      @Override
      public boolean processInReadAction(PsiReference reference) {
        PsiElement refElement = reference.getElement();
        final PsiElement parent = refElement.getParent();
        if (parent instanceof HaxeType && parent.getParent() instanceof HaxeNewExpression) {
          TextRange rangeInElement = reference.getRangeInElement();
          processor.process(new UsageInfo(refElement, rangeInElement.getStartOffset(), rangeInElement.getEndOffset(), false));
        }
        return true;
      }
    };
  }

  @NotNull
  @Override
  public PsiElement[] getSecondaryElements() {
    // In Java, if the element is a property, return getters and setters that reference it.
    return super.getSecondaryElements();
  }
}
