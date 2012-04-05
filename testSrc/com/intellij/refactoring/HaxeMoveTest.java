package com.intellij.refactoring;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesProcessor;
import com.intellij.refactoring.move.moveClassesOrPackages.SingleSourceRootMoveDestination;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeMoveTest extends MultiFileTestCase {
  @Override
  protected String getTestDataPath() {
    return PathManager.getHomePath() + FileUtil.toSystemDependentName("/plugins/haxe/testData/");
  }

  @Override
  protected String getTestRoot() {
    return "/move/";
  }

  @Override
  protected ModuleType getModuleType() {
    return HaxeModuleType.getInstance();
  }

  //Both names are relative to root directory
  private void doTest(String toMove, final String targetDirName) throws Exception {
    doTest(new String[]{toMove}, targetDirName);
  }

  //Both names are relative to root directory
  private void doTest(final String[] toMove, final String targetDirName) throws Exception {
    doTest(new PerformAction() {
      @Override
      public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
        Collection<PsiElement> files = new ArrayList<PsiElement>();
        for (String s : toMove) {
          final VirtualFile child = VfsUtil.findRelativeFile(s, rootDir);
          assertNotNull("Neither class nor file " + s + " not found", child);
          PsiElement file = myPsiManager.findFile(child);
          if (file == null) file = JavaPsiFacade.getInstance(myProject).findPackage(s);
          files.add(file);
        }
        final VirtualFile child1 = VfsUtil.findRelativeFile(targetDirName, rootDir);
        assertNotNull("Target dir " + targetDirName + " not found", child1);
        final PsiDirectory targetDirectory = myPsiManager.findDirectory(child1);
        assertNotNull(targetDirectory);

        if (files.iterator().next() instanceof PsiFile) {
          new MoveFilesOrDirectoriesProcessor(myProject, PsiUtilBase.toPsiElementArray(files), targetDirectory,
                                              false, false, null, null).run();
        }
        else if (files.iterator().next() instanceof PsiPackage) {
          PsiPackage newParentPackage = JavaPsiFacade.getInstance(myPsiManager.getProject()).findPackage(targetDirName);
          assertNotNull(newParentPackage);
          final PsiDirectory[] dirs = newParentPackage.getDirectories();
          assertEquals(dirs.length, 1);

          new MoveClassesOrPackagesProcessor(myProject, PsiUtilBase.toPsiElementArray(files),
                                             new SingleSourceRootMoveDestination(PackageWrapper.create(newParentPackage),
                                                                                 newParentPackage.getDirectories()[0]),
                                             true, true, null).run();
        }
        FileDocumentManager.getInstance().saveAllDocuments();
      }
    });
  }

  public void testMoveFile1() throws Exception {
    doTest("util/ArrayUtils.hx", "bar");
  }

  public void testMoveFile2() throws Exception {
    doTest("util/ArrayUtils.hx", "");
  }

  public void testMovePackage() throws Exception {
    doTest("util", "foo");
  }
}
