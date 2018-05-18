/*
 * Copyright 2017 Eric Bishton
 * Copyright 2018 Ilya Malanin
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
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.ReadActionProcessor;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeNewExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeFindUsagesHandler extends FindUsagesHandler {

  private static final String SETTER_PREFIX = "set_";
  private static final String GETTER_PREFIX = "get_";

  private final PsiElement[] extraElementsToSearch;

  protected HaxeFindUsagesHandler(@NotNull PsiElement psiElement) {
    this(psiElement, PsiElement.EMPTY_ARRAY);
  }

  protected HaxeFindUsagesHandler(@NotNull PsiElement psiElement, @Nullable PsiElement[] others) {
    super(psiElement);
    extraElementsToSearch = others != null ? others : PsiElement.EMPTY_ARRAY;
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
      ReadActionProcessor<PsiReference> searchProcessor = null;
      PsiElement searchElement = element;
      boolean fastTrack = true;

      if (element instanceof HaxeMethodDeclaration) {
        final HaxeMethodDeclaration method = (HaxeMethodDeclaration)element;
        final HaxeMethodModel methodModel = method.getModel();
        final HaxeClassModel declaringClass = methodModel.getDeclaringClass();

        if (method.isConstructor()) {
          searchElement = declaringClass.haxeClass;
          searchProcessor = getConstructorSearchProcessor(processor);
          fastTrack = false;
        }
      }

      if (searchProcessor == null) {
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
  private static ReadActionProcessor<PsiReference> getSimpleSearchProcessor(@NotNull Processor<UsageInfo> processor) {
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
  private static ReadActionProcessor<PsiReference> getConstructorSearchProcessor(@NotNull Processor<UsageInfo> processor) {
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
    PsiElement[] secondaryElements = ReadAction.compute(() -> {
      if (getPsiElement() instanceof HaxeMethodDeclaration) {
        return tryGetProperty((HaxeMethodDeclaration)getPsiElement());
      } else if (getPsiElement() instanceof HaxeFieldDeclaration &&
                 ((HaxeFieldDeclaration)getPsiElement()).getPropertyDeclaration() != null) {
        return tryGetPropertyAccessMethods((HaxeFieldDeclaration)getPsiElement());
      }
      return null;
    });

    return secondaryElements == null ? super.getSecondaryElements() : secondaryElements;
  }

  @Nullable
  private static PsiElement[] tryGetProperty(HaxeMethodDeclaration method) {
    final HaxeMethodModel methodModel = method.getModel();
    final HaxeClassModel declaringClass = methodModel.getDeclaringClass();
    final String methodName = method.getName();

    HaxeMemberModel classField = null;

    if (StringUtils.startsWith(methodName, GETTER_PREFIX)) {
      classField = declaringClass.getField(methodName.substring(GETTER_PREFIX.length()));
    } else if (StringUtils.startsWith(methodName, SETTER_PREFIX)) {
      classField = declaringClass.getField(methodName.substring(SETTER_PREFIX.length()));
    }
    return classField == null ? null : new PsiElement[]{classField.getBasePsi()};
  }

  @Nullable
  private static PsiElement[] tryGetPropertyAccessMethods(HaxeFieldDeclaration field) {
    final HaxeFieldModel model = (HaxeFieldModel)field.getModel();
    final List<PsiElement> methods = new SmartList<>();
    final HaxeMethodModel setterMethod = model.getSetterMethod();
    final HaxeMethodModel getterMethod = model.getGetterMethod();

    if (setterMethod != null) methods.add(setterMethod.getBasePsi());
    if (getterMethod != null) methods.add(getterMethod.getBasePsi());

    return methods.size() == 0 ? null : methods.toArray(PsiElement.EMPTY_ARRAY);
  }
}
