/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.refactoring.*;
import com.intellij.refactoring.classMembers.DelegatingMemberInfoModel;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.refactoring.extractSuperclass.ExtractSuperBaseProcessor;
import com.intellij.refactoring.extractSuperclass.JavaExtractSuperBaseDialog;
import com.intellij.refactoring.ui.MemberSelectionPanel;
import com.intellij.refactoring.util.DocCommentPolicy;
import com.intellij.refactoring.util.RefactoringMessageUtil;
import com.intellij.refactoring.util.classMembers.MemberInfo;
import com.intellij.util.ArrayUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

class ExtractInterfaceDialog extends JavaExtractSuperBaseDialog {

  public ExtractInterfaceDialog(Project project, PsiClass sourceClass) {
    super(project, sourceClass, collectMembers(sourceClass), ExtractInterfaceHandler.REFACTORING_NAME);
    for (MemberInfo memberInfo : myMemberInfos) {
      memberInfo.setToAbstract(true);
    }
    init();
  }

  private static List<MemberInfo> collectMembers(PsiClass c) {
    return MemberInfo.extractClassMembers(c, new MemberInfoBase.Filter<PsiMember>() {
      public boolean includeMember(PsiMember element) {
        if (element instanceof PsiMethod) {
          return element.hasModifierProperty(PsiModifier.PUBLIC)
                 && !element.hasModifierProperty(PsiModifier.STATIC);
        }
        else if (element instanceof PsiField) {
          return element.hasModifierProperty(PsiModifier.FINAL)
                 && element.hasModifierProperty(PsiModifier.STATIC)
                 && element.hasModifierProperty(PsiModifier.PUBLIC);
        }
        else if (element instanceof PsiClass) {
          return ((PsiClass)element).isInterface() || element.hasModifierProperty(PsiModifier.STATIC);
        }
        return false;
      }
    }, true);
  }

  protected String getClassNameLabelText() {
    return isExtractSuperclass()
           ? RefactoringBundle.message("interface.name.prompt")
           : RefactoringBundle.message("rename.implementation.class.to");
  }

  @Override
  protected String getPackageNameLabelText() {
    return isExtractSuperclass()
           ? RefactoringBundle.message("package.for.new.interface")
           : RefactoringBundle.message("package.for.original.class");
  }

  protected String getEntityName() {
    return RefactoringBundle.message("extractSuperInterface.interface");
  }

  @Override
  protected String getTopLabelText() {
    return RefactoringBundle.message("extract.interface.from");
  }

  @Override
  protected void preparePackage() throws OperationFailedException {
    final String targetPackageName = getTargetPackageName();
    final PsiFile containingFile = mySourceClass.getContainingFile();
    final boolean fromDefaultPackage = containingFile instanceof PsiClassOwner && ((PsiClassOwner)containingFile).getPackageName().isEmpty();
    if (!(fromDefaultPackage && StringUtil.isEmpty(targetPackageName)) && !PsiNameHelper.getInstance(myProject).isQualifiedName(targetPackageName)) {
      throw new OperationFailedException("Invalid package name: " + targetPackageName);
    }
    final PsiPackage aPackage = JavaPsiFacade.getInstance(myProject).findPackage(targetPackageName);
    if (aPackage != null) {
      final PsiDirectory[] directories = aPackage.getDirectories(mySourceClass.getResolveScope());
      if (directories.length >= 1) {
        myTargetDirectory = getDirUnderSameSourceRoot(directories);
      }
    }
    final MoveDestination moveDestination =
      myDestinationFolderComboBox.selectDirectory(new PackageWrapper(PsiManager.getInstance(myProject), targetPackageName), false);
    if (moveDestination == null) return;
    myTargetDirectory = myTargetDirectory != null ? ApplicationManager.getApplication().runWriteAction(new Computable<PsiDirectory>() {
      @Override
      public PsiDirectory compute() {
        return moveDestination.getTargetDirectory(myTargetDirectory);
      }
    }) : null;
    if (myTargetDirectory == null) {
      throw new OperationFailedException(""); // message already reported by PackageUtil
    }
    String error = RefactoringMessageUtil.checkCanCreateClass(myTargetDirectory, getExtractedSuperName());
    if (error != null) {
      throw new OperationFailedException(error);
    }
  }

  private PsiDirectory getDirUnderSameSourceRoot(final PsiDirectory[] directories) {
    final VirtualFile sourceFile = mySourceClass.getContainingFile().getVirtualFile();
    if (sourceFile != null) {
      final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
      final VirtualFile sourceRoot = fileIndex.getSourceRootForFile(sourceFile);
      if (sourceRoot != null) {
        for (PsiDirectory dir : directories) {
          if (Comparing.equal(fileIndex.getSourceRootForFile(dir.getVirtualFile()), sourceRoot)) {
            return dir;
          }
        }
      }
    }
    return directories[0];
  }

  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    final MemberSelectionPanel memberSelectionPanel = new MemberSelectionPanel(RefactoringBundle.message("members.to.form.interface"),
                                                                               myMemberInfos, RefactoringBundle.message("make.abstract"));
    memberSelectionPanel.getTable()
      .setMemberInfoModel(new DelegatingMemberInfoModel<PsiMember, MemberInfo>(memberSelectionPanel.getTable().getMemberInfoModel()) {
        public Boolean isFixedAbstract(MemberInfo member) {
          return Boolean.TRUE;
        }
      });
    panel.add(memberSelectionPanel, BorderLayout.CENTER);

    panel.add(myDocCommentPanel, BorderLayout.EAST);

    return panel;
  }

  @Override
  protected String getDocCommentPanelName() {
    return RefactoringBundle.message("extractSuperInterface.javadoc");
  }

  @Override
  protected String getExtractedSuperNameNotSpecifiedMessage() {
    return RefactoringBundle.message("no.interface.name.specified");
  }

  @Override
  protected int getDocCommentPolicySetting() {
    return JavaRefactoringSettings.getInstance().EXTRACT_INTERFACE_JAVADOC;
  }

  @Override
  protected void setDocCommentPolicySetting(int policy) {
    JavaRefactoringSettings.getInstance().EXTRACT_INTERFACE_JAVADOC = policy;
  }

  @Override
  protected ExtractSuperBaseProcessor createProcessor() {
    return new ExtractInterfaceProcessor(myProject, false, getTargetDirectory(), getExtractedSuperName(),
                                         mySourceClass, ArrayUtil.toObjectArray(getSelectedMemberInfos(), MemberInfo.class),
                                         new DocCommentPolicy(getDocCommentPolicy()));
  }

  @Override
  protected String getHelpId() {
    return HelpID.EXTRACT_INTERFACE;
  }
}
