package com.intellij.plugins.haxe.ide.refactoring.move;

import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;

import java.util.List;
import java.util.Map;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeFileMoveHandler extends MoveFileHandler {
  public static final Key<String> destinationPackageKey = Key.create("haxe.destination.package.key");

  @Override
  public boolean canProcessElement(PsiFile element) {
    return element instanceof HaxeFile;
  }

  @Override
  public void prepareMovedFile(PsiFile file, PsiDirectory moveDestination, Map<PsiElement, PsiElement> oldToNewMap) {
    file.putUserData(destinationPackageKey, DirectoryIndex.getInstance(file.getProject()).getPackageName(moveDestination.getVirtualFile()));
  }

  @Override
  public List<UsageInfo> findUsages(PsiFile psiFile, PsiDirectory newParent, boolean searchInComments, boolean searchInNonJavaFiles) {
    return null;
  }

  @Override
  public void retargetUsages(List<UsageInfo> usageInfos, Map<PsiElement, PsiElement> oldToNewMap) {
  }

  @Override
  public void updateMovedFile(PsiFile file) throws IncorrectOperationException {
    final HaxeFile haxeFile = (HaxeFile)file;
    final PsiElement firstChild = haxeFile.getFirstChild();
    final HaxePackageStatement packageStatement = PsiTreeUtil.getChildOfType(haxeFile, HaxePackageStatement.class);
    final HaxePackageStatement newPackageStatement =
      HaxeElementGenerator.createPackageStatementFromPath(haxeFile.getProject(), file.getUserData(destinationPackageKey));
    assert newPackageStatement != null;
    if (packageStatement == null && firstChild == null) {
      haxeFile.add(newPackageStatement);
    }
    else if (packageStatement == null && firstChild != null) {
      haxeFile.addBefore(newPackageStatement, firstChild);
    }
    else {
      packageStatement.replace(newPackageStatement);
    }
  }
}
