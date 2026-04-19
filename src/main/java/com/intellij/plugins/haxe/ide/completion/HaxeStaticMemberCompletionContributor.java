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
    Collection<String> methodKeys = StubIndex.getInstance().getAllKeys(HaxeMethodNameStubIndex.KEY, project);
    List<String> methodKeyList = new ArrayList<>(methodKeys);

    JobLauncher.getInstance().invokeConcurrentlyUnderProgress(
            methodKeyList,
            ProgressManager.getInstance().getProgressIndicator(),
            name -> ReadAction.compute(() -> {
              StubIndex.getInstance().processElements(HaxeStaticMethodNameStubIndex.KEY, name, project, scope, HaxeMethod.class, (method -> {
                if (method.isPublic()) {
                  addMemberElement(resultSet, method, name, filterText, targetFile);
                }
                return true;
              }));
              return true; // continue processing
            })
    );


    // Static public fields (includes enum value fields and regular fields)
    Collection<String> fieldKeys = StubIndex.getInstance().getAllKeys(HaxeFieldNameStubIndex.KEY, project);
    List<String> fieldKeyList = new ArrayList<>(fieldKeys);

    JobLauncher.getInstance().invokeConcurrentlyUnderProgress(
            fieldKeyList,
            ProgressManager.getInstance().getProgressIndicator(),
            name -> ReadAction.compute(() -> {
              StubIndex.getInstance().processElements(HaxeStaticFieldNameStubIndex.KEY, name, project, scope, HaxePsiField.class, (field -> {
                if (field.isStatic() && field.isPublic()) {
                  addMemberElement(resultSet, field, name, filterText, targetFile);
                }
                return true;
              }));
              return true; // continue processing
            })
    );
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
