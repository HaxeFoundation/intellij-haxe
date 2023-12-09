/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.ide.refactoring.extractInterface;

import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import com.intellij.lang.findUsages.DescriptiveNameUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil;
import com.intellij.plugins.haxe.ide.refactoring.HaxeRefactoringUtil;
import com.intellij.plugins.haxe.ide.refactoring.extractSuperclass.ExtractSuperClassUtil;
import com.intellij.plugins.haxe.ide.refactoring.memberPullUp.PullUpProcessor;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.psi.*;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.lang.ElementsHandler;
import com.intellij.refactoring.listeners.RefactoringEventListener;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.DocCommentPolicy;
import com.intellij.refactoring.util.classMembers.MemberInfo;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.MultiMap;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.intellij.plugins.haxe.util.HaxeElementGenerator.*;

@CustomLog
public class ExtractInterfaceHandler implements RefactoringActionHandler, ElementsHandler {

  public static final String REFACTORING_NAME = RefactoringBundle.message("extract.interface.title");


  private Project myProject;
  private PsiClass myClass;

  private String myInterfaceName;
  private MemberInfo[] mySelectedMembers;
  private PsiDirectory myTargetDir;
  private String packageName;
  private DocCommentPolicy myJavaDocPolicy;

  public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    int offset = editor.getCaretModel().getOffset();
    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    PsiElement element = file.findElementAt(offset);
    while (true) {
      if (element == null || element instanceof PsiFile) {
        String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("error.wrong.caret.position.class"));
        CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.EXTRACT_INTERFACE);
        return;
      }
      if (element instanceof PsiClass && !(element instanceof PsiAnonymousClass)) {
        invoke(project, new PsiElement[]{element}, dataContext);
        return;
      }
      element = element.getParent();
    }
  }

  public void invoke(@NotNull final Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
    if (elements.length != 1) return;

    myProject = project;
    myClass = (PsiClass)elements[0];


    if (!CommonRefactoringUtil.checkReadOnlyStatus(project, myClass)) return;

    final ExtractInterfaceDialog dialog = new ExtractInterfaceDialog(myProject, myClass);
    if (!dialog.showAndGet() || !dialog.isExtractSuperclass()) {
      return;
    }
    final MultiMap<PsiElement, String> conflicts = new MultiMap<PsiElement, String>();
    ExtractSuperClassUtil.checkSuperAccessible(dialog.getTargetDirectory(), conflicts, myClass);
    if (!ExtractSuperClassUtil.showConflicts(dialog, conflicts, myProject)) return;
    CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          public void run() {
            myInterfaceName = dialog.getExtractedSuperName();
            mySelectedMembers = ArrayUtil.toObjectArray(dialog.getSelectedMemberInfos(), MemberInfo.class);
            myTargetDir = dialog.getTargetDirectory();
            packageName = dialog.getTargetPackage();
            myJavaDocPolicy = new DocCommentPolicy(dialog.getDocCommentPolicy());
            try {
              doRefactoring();
            }
            catch (IncorrectOperationException e) {
              log.error(e);
            }
          }
        });
      }
    }, REFACTORING_NAME, null);
  }


  private void doRefactoring() throws IncorrectOperationException {
    LocalHistoryAction a = LocalHistory.getInstance().startAction(getCommandName());
    final PsiClass anInterface;
    try {
      anInterface = extractInterface(myTargetDir, myClass, myInterfaceName, packageName, mySelectedMembers, myJavaDocPolicy);
    }
    finally {
      a.finish();
    }

    if (anInterface != null) {
      final SmartPsiElementPointer<PsiClass> classPointer = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(myClass);
      final SmartPsiElementPointer<PsiClass> interfacePointer = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(anInterface);
      final Runnable turnRefsToSuperRunnable = new Runnable() {
        @Override
        public void run() {
          ExtractClassUtil.askAndTurnRefsToSuper(myProject, classPointer, interfacePointer);
        }
      };
      SwingUtilities.invokeLater(turnRefsToSuperRunnable);
    }
  }

  static PsiClass extractInterface(PsiDirectory targetDir,
                                   PsiClass aClass,
                                   String interfaceName,
                                   String packageName,
                                   MemberInfo[] selectedMembers,
                                   DocCommentPolicy javaDocPolicy) throws IncorrectOperationException {
    aClass.getProject().getMessageBus().syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
      .refactoringStarted(ExtractSuperClassUtil.REFACTORING_EXTRACT_SUPER_ID, ExtractSuperClassUtil.createBeforeData(aClass, selectedMembers));
    //final PsiClass anInterface = JavaDirectoryService.getInstance().createInterface(targetDir, interfaceName);
    HaxeClass haxeInterface = null;
    try {
      HaxeFile newFile =
        (HaxeFile)HaxeFileTemplateUtil.createClass(interfaceName, packageName, targetDir, "HaxeInterface", ExtractInterfaceHandler.class.getClassLoader());
      HaxeClassModel model = newFile.getModel().getClassModel(interfaceName);
      haxeInterface = model.getPsi();

      if (aClass instanceof HaxeClass haxeClass) {
        final PsiReferenceList referenceList = haxeClass.isInterface() ? haxeClass.getExtendsList() : haxeClass.getImplementsList();
        // if the  class / interface does not have extends/ implements lists we need to create these now.
        if (referenceList == null) {
          if (haxeClass.isInterface()) {
            haxeClass.getModel().addExtends(interfaceName);
          }else {
            haxeClass.getModel().addImplements(interfaceName);
          }
          if (!packageName.trim().isEmpty() && !packageName.equalsIgnoreCase(StringUtil.getPackageName(haxeClass.getQualifiedName()))) {
            HaxeFile containingFile = (HaxeFile)haxeClass.getContainingFile();
            containingFile.getModel().addImport(haxeInterface.getQualifiedName());
          }
        }else {
          referenceList.add(haxeInterface);
        }
        boolean move = aClass.isInterface();
        if (move) {
          PullUpProcessor pullUpHelper = new PullUpProcessor(aClass, haxeInterface, selectedMembers, javaDocPolicy);
          pullUpHelper.moveMembersToBase();
        }else {
          copySignatures(haxeInterface, selectedMembers);

        }
        HaxeRefactoringUtil.reformat(haxeInterface);
        return haxeInterface;
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally{
        aClass.getProject().getMessageBus().syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
          .refactoringDone(ExtractSuperClassUtil.REFACTORING_EXTRACT_SUPER_ID, ExtractSuperClassUtil.createAfterData(haxeInterface));
      }
    return null;
  }

  private static void copySignatures(HaxeClass anInterface, MemberInfo[] members) {
    HaxeDocumentModel document = anInterface.getModel().getDocument();
    PsiElement rBrace = anInterface.getRBrace();
    PsiElement braceParent = rBrace.getParent();

    for (MemberInfo member : members) {
      PsiMember psiMember = member.getMember();
      if (psiMember instanceof HaxeFieldDeclaration fieldDeclaration) {
        anInterface.add(fieldDeclaration);
      }
      if (psiMember instanceof HaxeMethodDeclaration methodDeclaration) {
        HaxeMethodDeclaration workCopy = (HaxeMethodDeclaration)methodDeclaration.copy();
        if(workCopy.getBody() != null)workCopy.getBody().delete();
        workCopy.getMethodModifierList().forEach(PsiElement::delete);
        PsiElement semi = createSemi(document.getFile().getProject());
        workCopy.add(semi);
        braceParent.addBefore(workCopy.copy(), rBrace);
      }
    }
  }

  private String getCommandName() {
    return RefactoringBundle.message("extract.interface.command.name", myInterfaceName, DescriptiveNameUtil.getDescriptiveName(myClass));
  }

  public boolean isEnabledOnElements(PsiElement[] elements) {
    return elements.length == 1 && elements[0] instanceof PsiClass;
  }
}
