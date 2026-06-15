/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.lookup.indexed.HaxeIndexedClassLookupElement;
import com.intellij.plugins.haxe.ide.lookup.indexed.data.HaxeClassLookupData;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeClassNameCompletionContributor extends CompletionContributor {
  public HaxeClassNameCompletionContributor() {

    extend(CompletionType.BASIC,
           psiElement().and(inImportOrUsing),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               HaxeReference reference = PsiTreeUtil.getParentOfType(parameters.getPosition(), HaxeReference.class);
               String packagePrefix = reference != null && reference.isQualified() ? reference.getQualifier().getText() : null;
               addVariantsFromIndex(result, parameters.getOriginalFile(), packagePrefix, FULL_PATH_INSERT_HANDLER);
             }
           });

    extend(CompletionType.BASIC,
            isSimpleIdentifier.andNot(inImportOrUsing),
            new CompletionProvider<CompletionParameters>() {
                @Override
                protected void addCompletions(@NotNull CompletionParameters parameters,
                                              ProcessingContext context,
                                              @NotNull CompletionResultSet result) {
                    final PsiFile file = parameters.getOriginalFile();
                    addVariantsFromIndex(result, file, null, null);
                    addVariantsFromImports(result, file);
                }
            });

      extend(CompletionType.SMART,
              inFunctionTypeTag,
              new CompletionProvider<CompletionParameters>() {
                  @Override
                  protected void addCompletions(@NotNull CompletionParameters parameters,
                                                ProcessingContext context,
                                                @NotNull CompletionResultSet result) {
                      final PsiFile file = parameters.getOriginalFile();
                      addVariantsFromIndex(result, file, null, null);
                      addVariantsFromImports(result, file);
                  }
              });

    extend(CompletionType.BASIC,
           inComplexExpression.andNot(inImportOrUsing),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               HaxeReference leftReference =
                 HaxeResolveUtil.getLeftReference(PsiTreeUtil.getParentOfType(parameters.getPosition(), HaxeReference.class));
               PsiElement leftTarget = leftReference != null ? leftReference.resolve() : null;
               if (leftTarget instanceof PsiPackage aPackage) {
                 addVariantsFromIndex(result, parameters.getOriginalFile(), aPackage.getQualifiedName(), null);
               }
             }
           });
  }

  private static void addVariantsFromIndex(final CompletionResultSet resultSet,
                                           final PsiFile targetFile,
                                           @Nullable String prefixPackage,
                                           @Nullable final InsertHandler<HaxeIndexedClassLookupElement> insertHandler) {
    final Project project = targetFile.getProject();
    final GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(targetFile);
    final PrefixMatcher matcher = resultSet.getPrefixMatcher();

    // Collecting matching keys first as processElements cannot be called inside
    // a processAllKeys callback (same index lock -> deadlock assertion).
    StubIndex stubIndex = StubIndex.getInstance();

    final List<String> matchingKeys = new ArrayList<>();
      HaxeClassNameUnifiedIndex.getAllKeys(project).forEach(key -> {
          if (matcher.prefixMatches(key)) matchingKeys.add(key);
      });


      for (String key : matchingKeys) {
          List<HaxeClassLookupData> lookupData = HaxeClassNameUnifiedIndex.getCompletionData(key, project, scope);
          for (HaxeClassLookupData lookupDatum : lookupData) {
              String packageName = lookupDatum.qualifiedInfo.getPackageName();
              if (prefixPackage == null || prefixPackage.equalsIgnoreCase(packageName)) {
                  resultSet.addElement(new HaxeIndexedClassLookupElement(lookupDatum, insertHandler));
//                }
              }
          }
      }
  }



  private static void addVariantsFromImports(final CompletionResultSet resultSet,
                                             final PsiFile targetFile) {
      targetFile.acceptChildren(new HaxeRecursiveVisitor() {
          @Override
          public void visitImportStatement(@NotNull HaxeImportStatement importStatement) {
              final List<HaxeModel> exposedMembers = new ArrayList<>();
              // we want to skip HaxeClassModels here as we already got these from index.
              for (HaxeModel haxeModel : importStatement.getModel().getExposedMembers()) {
                  if (haxeModel instanceof HaxeAliasModel aliasModel) {
                      String alias = aliasModel.getName();
                      HaxeModel aliasFor = aliasModel.getAliasForModel();
                      if(alias != null &&  aliasFor != null) {
                          LookupElementBuilder lookupElement = HaxeLookupElementFactory.create(aliasFor, alias);
                          resultSet.addElement(lookupElement);
                          return;
                      }
                  }
                  else if(haxeModel instanceof HaxeModuleModel moduleModel) {
                      LookupElementBuilder lookupElement = HaxeLookupElementFactory.create(moduleModel);
                      resultSet.addElement(lookupElement);
                  }
                  else if(haxeModel instanceof HaxeMethodModel methodModel) {
                      LookupElementBuilder lookupElement = HaxeLookupElementFactory.create(methodModel);
                      resultSet.addElement(lookupElement);
                  }
                  else if(haxeModel instanceof HaxeEnumValueModel enumValueModel) {
                      LookupElementBuilder lookupElement = HaxeLookupElementFactory.create(enumValueModel);
                      resultSet.addElement(lookupElement);
                  }
              }
          }
      });
  }


  private static final InsertHandler<HaxeIndexedClassLookupElement> FULL_PATH_INSERT_HANDLER = HaxeClassNameCompletionContributor::replaceElementToFullPath;

  private static void replaceElementToFullPath(final InsertionContext context, final HaxeIndexedClassLookupElement item) {
    WriteCommandAction.writeCommandAction(context.getProject(), context.getFile()).run(() -> {
      FullyQualifiedInfo qualifiedInfo = item.getQualifiedInfo();
      if (qualifiedInfo != null) {
        String importPath = qualifiedInfo.toShortendImportReferenceString();
        final PsiReference currentReference = context.getFile().findReferenceAt(context.getTailOffset() - 1);
        if (currentReference != null && currentReference.getElement() != null) {
          final PsiElement currentElement = currentReference.getElement();
          final HaxeReference fullPathReference = HaxeElementGenerator.createReferenceFromText(context.getProject(), importPath);
          if (fullPathReference != null) {
            currentElement.replace(fullPathReference);
          }
        }
      }
    });
  }
}

