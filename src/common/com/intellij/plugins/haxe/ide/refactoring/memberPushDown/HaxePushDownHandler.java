/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.ide.refactoring.memberPushDown;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClassDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeFunctionDeclarationWithAttributes;
import com.intellij.plugins.haxe.lang.psi.HaxeFunctionPrototypeDeclarationWithAttributes;
import com.intellij.plugins.haxe.lang.psi.HaxeVarDeclaration;
import com.intellij.psi.*;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.refactoring.memberPushDown.JavaPushDownHandler;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.classMembers.MemberInfo;
import com.intellij.refactoring.util.classMembers.MemberInfoStorage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by Boch on 14.03.2015.
 */
public class HaxePushDownHandler extends JavaPushDownHandler {
  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext context) {
    int offset = editor.getCaretModel().getOffset();
    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    PsiElement element = file.findElementAt(offset);
    while (true) {
      if (element == null || element instanceof PsiFile) {
        String message = RefactoringBundle.getCannotRefactorMessage(
          RefactoringBundle.message("the.caret.should.be.positioned.inside.a.class.to.push.members.from"));
        CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.MEMBERS_PUSH_DOWN);
        return;
      }

      if (element instanceof HaxeClassDeclaration || element instanceof HaxeVarDeclaration || element instanceof HaxeFunctionDeclarationWithAttributes
        || element instanceof HaxeFunctionPrototypeDeclarationWithAttributes) {
        //if (element instanceof JspClass) {
        //  RefactoringMessageUtil.showNotSupportedForJspClassesError(project, editor, REFACTORING_NAME, HelpID.MEMBERS_PUSH_DOWN);
        //  return;
        //}
        invoke(project, new PsiElement[]{element}, context);
        return;
      }
      element = element.getParent();
    }
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext context) {
    if (elements.length != 1) return;
    PsiElement element = elements[0];
    PsiClass aClass;
    PsiElement aMember = null;
    if (element instanceof HaxeClassDeclaration) {
      aClass = (HaxeClassDeclaration) element;
    } else if (element instanceof HaxeFunctionDeclarationWithAttributes) {
      aClass = ((HaxeFunctionDeclarationWithAttributes) element).getContainingClass();
      aMember = element;
    } else if (element instanceof HaxeFunctionPrototypeDeclarationWithAttributes) {
      aClass = ((HaxeFunctionPrototypeDeclarationWithAttributes) element).getContainingClass();
      aMember = element;
    } else if (element instanceof HaxeVarDeclaration) {
      aClass = ((HaxeVarDeclaration)element).getContainingClass();
      aMember = element;
    }
    else {
      return;
    }
    if (!CommonRefactoringUtil.checkReadOnlyStatus(project, aClass)) return;
    MemberInfoStorage memberInfoStorage = new MemberInfoStorage(aClass, new MemberInfo.Filter<PsiMember>() {
      public boolean includeMember(PsiMember element) {
        return !(element instanceof PsiEnumConstant);
      }
    });
    List<MemberInfo> members = memberInfoStorage.getClassMemberInfos(aClass);
    PsiManager manager = aClass.getManager();
    for (MemberInfoBase<PsiMember> member : members) {
      if (manager.areElementsEquivalent(member.getMember(), aMember)) {
        member.setChecked(true);
        break;
      }
    }
    HaxePushDownDialog dialog = new HaxePushDownDialog(
      project,
      members.toArray(new MemberInfo[members.size()]),
      aClass);
    dialog.show();
  }
}
