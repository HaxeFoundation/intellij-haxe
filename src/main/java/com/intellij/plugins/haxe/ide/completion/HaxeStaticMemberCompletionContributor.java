package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.concurrency.JobLauncher;
import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.ide.lookup.HaxeStaticMemberLookupElement;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeFieldNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeMethodNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeStaticFieldNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeStaticMethodNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.util.PsiTreeUtil;
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



  private static void addMemberElement(CompletionResultSet resultSet,
                                        HaxeNamedComponent member,
                                        String memberName,
                                        String filterText,
                                        PsiFile helperPsi) {
    final HaxeClass cls = PsiTreeUtil.getStubOrPsiParentOfType(member, HaxeClass.class);
    if (cls == null) return;

    // Same exclusions as the original indexer
    if (cls.isTypeDef() || cls.isInterface() || cls.isAbstractType() || cls.isAnonymousType()) return;

    String className = null;
    String packageName = "";
    String moduleName = "";
    FullyQualifiedInfo fqi = null;

    if (cls instanceof StubBasedPsiElementBase<?> stubPsi) {
      StubElement<?> stub = stubPsi.getStub();
      if (stub instanceof HaxeClassStub classStub) {
        className = classStub.getName();
        String qname = classStub.getQualifiedName();
        if (qname != null) {
          fqi = new FullyQualifiedInfo(qname);
          packageName = fqi.packagePath != null ? fqi.packagePath : "";
          moduleName  = fqi.moduleName  != null ? fqi.moduleName  : (className != null ? className : "");
        }
      }
    }
    if (className == null) className = cls.getName();
    if (className == null) return;
    if (fqi == null) {
      fqi = new FullyQualifiedInfo(cls.getQualifiedName());
      if (fqi.packagePath != null) packageName = fqi.packagePath;
      if (fqi.moduleName  != null) moduleName  = fqi.moduleName;
    }

    // Same filter logic as original processAll(): match on className or moduleName prefix
    if (!className.startsWith(filterText) && !moduleName.startsWith(filterText)) return;

    resultSet.addElement(new HaxeStaticMemberLookupElement(
      packageName, moduleName, className, memberName,
      member.getComponentType(), "",
      fqi.withMemberName(memberName), helperPsi));
  }
}
