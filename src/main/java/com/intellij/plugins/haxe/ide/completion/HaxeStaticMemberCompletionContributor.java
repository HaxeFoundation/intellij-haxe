package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.plugins.haxe.ide.lookup.indexed.data.HaxeClassLookupData;
import com.intellij.plugins.haxe.ide.lookup.indexed.data.HaxeMemberLookupData;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.ide.lookup.indexed.HaxeIndexedStaticMemberLookupElement;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeStaticFieldNameUnifiedIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeStaticMethodNameUnifiedIndex;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.ProcessingContext;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.identifierInNewExpression;
import static com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil.belongToPlatformNotTargeted;

public class HaxeStaticMemberCompletionContributor extends CompletionContributor {

    private static final PsiElementPattern.Capture<PsiElement> ELEMENT_CAPTURE = psiElement()
            .inside(HaxeIdentifier.class)
            .andNot(psiElement().inside(HaxeType.class))
            // - avoid chained refs (MyClass.startComplet.. / myVar.startComplet... should not show static suggestions)
            // current = ID token
            // parent 1: HaxeIdentifier
            // parent 2: HaxeReference
            // parent 3: should not be a refrence as that would be a chain
            .andNot(psiElement().withSuperParent(3, HaxeReferenceExpression.class));

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
               if(!newExpression) {
                 position = position != null ? position : parameters.getPosition();
                 addVariantsFromIndex(result, file, position.getText());
               }
             }
           });
  }

  private static void addVariantsFromIndex(final CompletionResultSet resultSet,
                                            final PsiFile targetFile,
                                            @NlsSafe String filterText) {
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
            addMemberElement(resultSet, lookupDatum, filterText);
        }
    });

    // TODO mlo: might want to split this into 2 different CompletionContributors if it means we can do this in parallel

    // Static public fields (includes enum value fields and regular fields)
    Collection<String> fieldKeys = HaxeStaticFieldNameUnifiedIndex.getAllKeys(project);

      fieldKeys.forEach(name -> {
                  ProgressIndicatorProvider.checkCanceled();
                  List<HaxeMemberLookupData> lookupData = HaxeStaticFieldNameUnifiedIndex.getCompletionData(name, project, scope);
                  for (HaxeMemberLookupData lookupDatum : lookupData) {
                      addMemberElement(resultSet, lookupDatum, filterText);
                  }
              }
      );
  }

    private static void addMemberElement(CompletionResultSet resultSet, HaxeMemberLookupData lookupData, @NlsSafe String filterText) {
        FullyQualifiedInfo qualifiedInfo = lookupData.qualifiedInfo;

        String possibleClass = qualifiedInfo.getClassName();
        String className = possibleClass != null ? possibleClass : "";
        if (!className.startsWith(filterText)) return;

        String moduleName = qualifiedInfo.getModuleName();
        if (moduleName == null || !moduleName.startsWith(filterText)) return;

        resultSet.addElement(new HaxeIndexedStaticMemberLookupElement(lookupData));
    }

}
