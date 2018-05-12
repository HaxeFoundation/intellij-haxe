/*
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
package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.model.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.inImportOrUsing;
import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.isSimpleIdentifier;

public class HaxeEnumValuesCompletionContributor extends CompletionContributor {
  public HaxeEnumValuesCompletionContributor() {
    extend(CompletionType.BASIC,
           isSimpleIdentifier.andNot(inImportOrUsing),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               addEnumValuesFromCurrentFile(result, parameters.getOriginalPosition(), parameters.getOriginalFile());
             }
           });
  }

  private static void addEnumValuesFromCurrentFile(CompletionResultSet resultSet, PsiElement contextElement, PsiFile targetFile) {
    if (!(targetFile instanceof HaxeFile)) return;

    final HaxeFileModel fileModel = ((HaxeFile)targetFile).getModel();
    final HaxeClass contextClass = PsiTreeUtil.getParentOfType(contextElement, HaxeClass.class);
    fileModel.getClassModelsStream()
      .filter(model -> model.haxeClass != contextClass)
      .filter(HaxeClassModel::isEnum)
      .flatMap(model -> ((HaxeEnumModel)model).getValuesStream())
      .forEach(enumValueModel -> {
        LookupElement lookupElement = HaxeLookupElementFactory.create(enumValueModel);
        if (lookupElement != null) resultSet.addElement(lookupElement);
      });
  }
}
