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
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.plugins.haxe.ide.index.HaxeClassInfo;
import com.intellij.plugins.haxe.ide.index.HaxeComponentIndex;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

               addVariantsFromIndex(result, file, null, CLASS_INSERT_HANDLER);
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

               addVariantsFromIndex(result, file, null, CLASS_INSERT_HANDLER);
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
               if (leftTarget instanceof PsiPackage) {
                 addVariantsFromIndex(result, parameters.getOriginalFile(), ((PsiPackage)leftTarget).getQualifiedName(), null);
               }
             }
           });
  }

  private static void addVariantsFromIndex(final CompletionResultSet resultSet,
                                           final PsiFile targetFile,
                                           @Nullable String prefixPackage,
                                           @Nullable final InsertHandler<LookupElement> insertHandler) {
    final Project project = targetFile.getProject();
    final GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(targetFile);
    final MyProcessor processor = new MyProcessor(resultSet, prefixPackage, insertHandler);
    HaxeComponentIndex.processAll(project, processor, scope);
  }

  private static void addVariantsFromImports(final CompletionResultSet resultSet,
                                             final PsiFile targetFile) {
    targetFile.acceptChildren(new HaxeRecursiveVisitor() {
      @Override
      public void visitImportStatement(@NotNull HaxeImportStatement importStatement) {
        final List<HaxeModel> exposedMembers = importStatement.getModel().getExposedMembers();
        final String alias = importStatement.getAlias() != null ? importStatement.getAlias().getText() : null;

        for (HaxeModel member : exposedMembers) {
          LookupElementBuilder lookupElement = HaxeLookupElementFactory.create(member, alias);
          if (lookupElement != null) resultSet.addElement(lookupElement);
          if (alias != null) return;
        }
      }
    });
  }


  private static final InsertHandler<LookupElement> CLASS_INSERT_HANDLER =
    (context, item) -> addImportForLookupElement(context, item, context.getTailOffset() - 1);

  private static void addImportForLookupElement(final InsertionContext context, final LookupElement item, final int tailOffset) {
    final PsiReference ref = context.getFile().findReferenceAt(tailOffset);
    if (ref == null || ref.resolve() != null) {
      // no import statement needed
      return;
    }
    new WriteCommandAction(context.getProject(), context.getFile()) {
      @Override
      protected void run(@NotNull Result result) throws Throwable {
        final String importPath = (String)item.getObject();
        HaxeAddImportHelper.addImport(importPath, context.getFile());
      }
    }.execute();
  }

  /**
   * Full path insert handler
   **/
  private static final InsertHandler<LookupElement> FULL_PATH_INSERT_HANDLER =
    (context, item) -> replaceElementToFullPath(context, item, context.getTailOffset() - 1);

  private static void replaceElementToFullPath(final InsertionContext context, final LookupElement item, final int tailOffset) {
    new WriteCommandAction(context.getProject(), context.getFile()) {
      @Override
      protected void run(@NotNull Result result) throws Throwable {
        final String importPath = (String)item.getObject();
        final PsiReference currentReference = context.getFile().findReferenceAt(context.getTailOffset() - 1);
        if (currentReference != null && currentReference.getElement() != null) {
          final PsiElement currentElement = currentReference.getElement();
          final HaxeReference fullPathReference = HaxeElementGenerator.createReferenceFromText(context.getProject(), importPath);
          if (fullPathReference != null) {
            currentElement.replace(fullPathReference);
          }
        }
      }
    }.execute();
  }

  private static class MyProcessor implements Processor<Pair<String, HaxeClassInfo>> {
    private final CompletionResultSet myResultSet;
    @Nullable private final InsertHandler<LookupElement> myInsertHandler;
    @Nullable private final String myPrefixPackage;

    private MyProcessor(CompletionResultSet resultSet,
                        @Nullable String prefixPackage,
                        @Nullable InsertHandler<LookupElement> insertHandler) {
      myResultSet = resultSet;
      myPrefixPackage = prefixPackage;
      myInsertHandler = insertHandler;
    }

    @Override
    public boolean process(Pair<String, HaxeClassInfo> pair) {
      HaxeClassInfo info = pair.getSecond();
      if (myPrefixPackage == null || myPrefixPackage.equalsIgnoreCase(info.getValue())) {
        String name = pair.getFirst();
        final String qName = HaxeResolveUtil.joinQName(info.getValue(), name);
        myResultSet.addElement(LookupElementBuilder.create(qName, name)
                                 .withIcon(info.getCompletionIcon())
                                 .withTailText(" " + info.getValue(), true)
                                 .withInsertHandler(myInsertHandler));
      }
      return true;
    }
  }
}
