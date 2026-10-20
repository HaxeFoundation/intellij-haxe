package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.plugins.haxe.ide.lookup.indexed.data.HaxeMemberLookupData;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.ide.lookup.indexed.HaxeIndexedStaticMemberLookupElement;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeStaticFieldNameUnifiedIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeStaticMethodNameUnifiedIndex;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.identifierInNewExpression;
import static com.intellij.plugins.haxe.ide.completion.HaxeCompletionUtil.isInReferenceChain;

public class HaxeStaticMemberCompletionContributor extends CompletionContributor {

    private static final PsiElementPattern.Capture<PsiElement> ELEMENT_CAPTURE = psiElement()
            .inside(HaxeIdentifier.class)
            .andNot(psiElement().inside(HaxeType.class))
            // current = ID token
            // parent 1: HaxeIdentifier
            // parent 2: HaxeReference
            .and(psiElement().withSuperParent(2, HaxeReferenceExpression.class));

    public HaxeStaticMemberCompletionContributor() {
    extend(CompletionType.BASIC, ELEMENT_CAPTURE,
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               final PsiFile file = parameters.getOriginalFile();
               var position = parameters.getOriginalPosition();
               boolean newExpression = identifierInNewExpression.accepts(position);
               boolean inChain = isInReferenceChain(parameters.getPosition());
               if(!newExpression && !inChain) {
                 position = position != null ? position : parameters.getPosition();
                 FullyQualifiedInfo expectedEnum = expectedEnumInfo(parameters);
                 addVariantsFromIndex(result, file, position.getText(), expectedEnum);
               }
             }
           });
  }

  /** The expected enum's class info, whose values HaxeExpectedEnumValueCompletionContributor already offers prioritized. */
  @Nullable
  private static FullyQualifiedInfo expectedEnumInfo(CompletionParameters parameters) {
    HaxeClass expectedEnum = HaxeExpectedEnumValueCompletionContributor.expectedEnumClass(parameters);
    return expectedEnum == null ? null : expectedEnum.getModel().getQualifiedInfo();
  }

  private static void addVariantsFromIndex(final CompletionResultSet resultSet,
                                            final PsiFile targetFile,
                                            @NlsSafe String filterText,
                                            @Nullable FullyQualifiedInfo suppressedClass) {
    final Project project = targetFile.getProject();
    final GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(targetFile);
    final PrefixMatcher matcher = resultSet.getPrefixMatcher();


    // Static public methods
    StubIndex stubIndex = StubIndex.getInstance();
    Collection<String> methodKeys = HaxeStaticMethodNameUnifiedIndex.getAllKeys(project);

    methodKeys.forEach(name -> {
        ProgressIndicatorProvider.checkCanceled();
        List<HaxeMemberLookupData> lookupData = HaxeStaticMethodNameUnifiedIndex.getCompletionData(name, project, scope);
        for (HaxeMemberLookupData lookupDatum : lookupData) {
            addMemberElement(resultSet, lookupDatum, filterText, suppressedClass);
        }
    });

    // TODO mlo: might want to split this into 2 different CompletionContributors if it means we can do this in parallel

    // Static public fields (includes enum value fields and regular fields)
    Collection<String> fieldKeys = HaxeStaticFieldNameUnifiedIndex.getAllKeys(project);

      fieldKeys.forEach(name -> {
                  ProgressIndicatorProvider.checkCanceled();
                  List<HaxeMemberLookupData> lookupData = HaxeStaticFieldNameUnifiedIndex.getCompletionData(name, project, scope);
                  for (HaxeMemberLookupData lookupDatum : lookupData) {
                      addMemberElement(resultSet, lookupDatum, filterText, suppressedClass);
                  }
              }
      );
  }

    private static void addMemberElement(CompletionResultSet resultSet,
                                         HaxeMemberLookupData lookupData,
                                         @NlsSafe String filterText,
                                         @Nullable FullyQualifiedInfo suppressedClass) {
        FullyQualifiedInfo qualifiedInfo = lookupData.qualifiedInfo;
        // the expected-enum contributor already offers this class's values at the top; a second copy would scatter below
        if (suppressedClass != null && qualifiedInfo.withMemberName(null).equals(suppressedClass)) return;

        String possibleClass = qualifiedInfo.getClassName();
        String className = possibleClass != null ? possibleClass : "";
        if (!className.startsWith(filterText)) return;

        String moduleName = qualifiedInfo.getModuleName();
        if (moduleName == null || !moduleName.startsWith(filterText)) return;

        resultSet.addElement(new HaxeIndexedStaticMemberLookupElement(lookupData));
    }

}
