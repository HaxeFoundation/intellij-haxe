/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.ide.refactoring.memberPullUp;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.psi.*;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.refactoring.lang.ElementsHandler;
import com.intellij.refactoring.ui.ConflictsDialog;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.classMembers.MemberInfo;
import com.intellij.refactoring.util.classMembers.MemberInfoStorage;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by as3boyan on 10.09.14.
 * Based on https://github.com/JetBrains/intellij-community/blob/master/java/java-impl/src/com/intellij/refactoring/memberPullUp/JavaPullUpHandler.java
 */
public class HaxePullUpHandler implements RefactoringActionHandler, HaxePullUpDialog.Callback, ElementsHandler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.ide.refactoring.memberPullUp.HaxePullUpHandler");
  public static final String REFACTORING_NAME = RefactoringBundle.message("pull.members.up.title");
  private PsiClass mySubclass;
  private Project myProject;

  /*@Override
  public boolean checkConflicts(PullUpDialog dialog) {
    return false;
  }*/

  @Override
  public boolean isEnabledOnElements(PsiElement[] elements) {
    return elements.length == 1 && elements[0] instanceof PsiClass;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext context) {
    int offset = editor.getCaretModel().getOffset();
    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    PsiElement element = file.findElementAt(offset);
    //HaxeClassDeclaration classDeclaration;
    //PsiElement parentElement;

    while (true) {
      if (element == null || element instanceof PsiFile) {
        String message = RefactoringBundle
          .getCannotRefactorMessage(RefactoringBundle.message("the.caret.should.be.positioned.inside.a.class.to.pull.members.from"));
        CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.MEMBERS_PULL_UP);
        return;
      }
      if (!CommonRefactoringUtil.checkReadOnlyStatus(project, element)) return;

      /*classDeclaration = PsiTreeUtil.getParentOfType(element, HaxeClassDeclaration.class, false);

      parentElement = null;
      parentElement = PsiTreeUtil.getParentOfType(element, HaxeVarDeclaration.class, false);
      if (parentElement == null) {
        parentElement = PsiTreeUtil.getParentOfType(element, HaxeFunctionDeclarationWithAttributes.class, false);
      }*/

      if (element instanceof HaxeClassDeclaration || element instanceof HaxeInterfaceDeclaration || element instanceof PsiField || element instanceof PsiMethod) {
        invoke(project, new PsiElement[]{element}, context);
        return;
      }

      //if (classDeclaration != null) {
      //  invoke(project, context, classDeclaration, parentElement);
      //  return;
      //}
      element = element.getParent();
    }
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext context) {
    if (elements.length != 1) return;
    myProject = project;
    PsiElement element = elements[0];
    PsiClass aClass;
    PsiElement aMember = null;
    if (element instanceof HaxeClassDeclaration || element instanceof HaxeInterfaceDeclaration) {
      aClass = (AbstractHaxePsiClass)element;
    }
    else if (element instanceof PsiMethod) {
      aClass = ((PsiMethod)element).getContainingClass();
      aMember = element;
    }
    else if (element instanceof PsiField) {
      aClass = ((PsiField)element).getContainingClass();
      aMember = element;
    }
    else {
      return;
    }
    invoke(project, context, aClass, aMember);
  }

  private void invoke(Project project, DataContext dataContext, PsiClass psiClass, PsiElement aMember) {
    AbstractHaxePsiClass aClass = (AbstractHaxePsiClass)psiClass;
    final Editor editor = dataContext != null ? CommonDataKeys.EDITOR.getData(dataContext) : null;
    if (aClass == null) {
      String message =
        RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("is.not.supported.in.the.current.context", REFACTORING_NAME));
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.MEMBERS_PULL_UP);
      return;
    }
    List<HaxeType> extendsList = aClass.getHaxeExtendsList();
    List<HaxeType> implementsList = aClass.getHaxeImplementsList();
    if (extendsList.isEmpty() && implementsList.isEmpty()) {
      final AbstractHaxePsiClass containingClass = aClass;
      if (containingClass != null) {
        invoke(project, dataContext, containingClass, aClass);
        return;
      }
      String message = RefactoringBundle.getCannotRefactorMessage(
        RefactoringBundle.message("class.does.not.have.base.classes.interfaces.in.current.project", aClass.getQualifiedName()));
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.MEMBERS_PULL_UP);
      return;
    }
    mySubclass = aClass;
    MemberInfoStorage memberInfoStorage = new MemberInfoStorage(mySubclass, new MemberInfo.Filter<PsiMember>() {
      @Override
      public boolean includeMember(PsiMember element) {
        return true;
      }
    });
    List<MemberInfo> members = memberInfoStorage.getClassMemberInfos(mySubclass);
    PsiManager manager = mySubclass.getManager();
    for (MemberInfoBase<PsiMember> member : members) {
      if (manager.areElementsEquivalent(member.getMember(), aMember)) {
        member.setChecked(true);
        break;
      }
    }

    List<PsiClass> psiClasses = new ArrayList<PsiClass>();

    HaxeClassResolveResult result;
    HaxeClass haxeClass;
    for (int i = 0; i < extendsList.size(); i++) {
      result = extendsList.get(i).getReferenceExpression().resolveHaxeClass();
      if (result != null) {
        haxeClass = result.getHaxeClass();
        if (haxeClass != null) {
          psiClasses.add(haxeClass);
        }
      }
    }

    for (int i = 0; i < implementsList.size(); i++) {
      result = implementsList.get(i).getReferenceExpression().resolveHaxeClass();
      if (result != null) {
        haxeClass = result.getHaxeClass();
        if (haxeClass != null) {
          psiClasses.add(haxeClass);
        }
      }
    }

    final HaxePullUpDialog dialog = new HaxePullUpDialog(project, aClass, psiClasses, memberInfoStorage, this);
    dialog.show();
  }

  @Override
  public boolean checkConflicts(final HaxePullUpDialog dialog) {
    final List<MemberInfo> infos = dialog.getSelectedMemberInfos();
    final MemberInfo[] memberInfos = infos.toArray(new MemberInfo[infos.size()]);
    final PsiClass superClass = dialog.getSuperClass();
    if (!checkWritable(superClass, memberInfos)) return false;
    final MultiMap<PsiElement, String> conflicts = new MultiMap<PsiElement, String>();
    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
          @Override
          public void run() {
            //final PsiDirectory targetDirectory = superClass.getContainingFile().getContainingDirectory();
            //final PsiPackage targetPackage = targetDirectory != null ? JavaDirectoryService.getInstance().getPackage(targetDirectory) : null;
            //conflicts
            //  .putAllValues(PullUpConflictsUtil.checkConflicts(memberInfos, mySubclass, superClass, targetPackage, targetDirectory,
            //                                                   dialog.getContainmentVerifier()));
          }
        });
      }
    }, RefactoringBundle.message("detecting.possible.conflicts"), true, myProject)) return false;
    if (!conflicts.isEmpty()) {
      ConflictsDialog conflictsDialog = new ConflictsDialog(myProject, conflicts);
      conflictsDialog.show();
      final boolean ok = conflictsDialog.isOK();
      if (!ok && conflictsDialog.isShowConflicts()) dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
      return ok;
    }
    return true;
  }

  private boolean checkWritable(PsiClass superClass, MemberInfo[] infos) {
    if (!CommonRefactoringUtil.checkReadOnlyStatus(myProject, superClass)) return false;
    for (MemberInfo info : infos) {
      if (info.getMember() instanceof PsiClass && info.getOverrides() != null) continue;
      if (!CommonRefactoringUtil.checkReadOnlyStatus(myProject, info.getMember())) return false;
    }
    return true;
  }


}