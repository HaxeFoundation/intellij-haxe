package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.ide.lookup.HaxeStaticMemberLookupElement;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeFieldNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeMethodNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeStaticFieldNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeStaticMethodNameStubIndex;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.identifierInNewExpression;

public class HaxeStaticMemberCompletionContributor extends CompletionContributor {
  public HaxeStaticMemberCompletionContributor() {
    extend(CompletionType.BASIC, psiElement().inside(HaxeIdentifier.class),
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

    // Static public methods
    StubIndex stubIndex = StubIndex.getInstance();
    Collection<String> methodKeys = stubIndex.getAllKeys(HaxeMethodNameStubIndex.KEY, project);

    methodKeys.forEach(name -> {
      stubIndex.processElements(HaxeStaticMethodNameStubIndex.KEY, name, project, scope, HaxeMethod.class, (method -> {
          if (method.isStatic() && method.isPublic()) {
            addMemberElement(resultSet, method, filterText);
          }
          return true;
        }));
    });

    // TODO mlo: might want to split this into 2 different CompletionContributors if it means we can do this in parallel

    // Static public fields (includes enum value fields and regular fields)
    Collection<String> fieldKeys = stubIndex.getAllKeys(HaxeFieldNameStubIndex.KEY, project);

    fieldKeys.forEach(name ->
      stubIndex.processElements(HaxeStaticFieldNameStubIndex.KEY, name, project, scope, HaxePsiField.class, (field -> {
        if (field.isStatic() && field.isPublic()) {
          addMemberElement(resultSet, field, filterText);
        }
        return true;
      })));

  }



  private static void addMemberElement(CompletionResultSet resultSet, HaxeNamedComponent member, @NlsSafe String filterText) {
    if(member instanceof HaxeModelTarget modelTarget) {
      HaxeModel model = modelTarget.getModel();
      if (model instanceof HaxeMemberModel memberModel) {

        HaxeClassModel possibleClass = memberModel.getDeclaringClass();
        String className = possibleClass != null ? possibleClass.getName() : "";
        if (!className.startsWith(filterText))  return;

        HaxeModule module = memberModel.getModule();
        String moduleName = module != null ? module.getName() : "";
        if (moduleName == null || !moduleName.startsWith(filterText)) return;


        resultSet.addElement(new HaxeStaticMemberLookupElement(memberModel));
      }
    }
  }
}
