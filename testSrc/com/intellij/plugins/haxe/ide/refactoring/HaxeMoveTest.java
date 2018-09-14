/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Aleksandr Kuzmenko
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
package com.intellij.plugins.haxe.ide.refactoring;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.lang.psi.HaxeClassDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.refactoring.PackageWrapper;
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
    return HaxeTestUtils.BASE_TEST_DATA_PATH;
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
          new MoveFilesOrDirectoriesProcessor(myProject, PsiUtilCore.toPsiElementArray(files), targetDirectory,
                                              false, false, null, null).run();
        }
        else if (files.iterator().next() instanceof PsiPackage) {
          PsiPackage newParentPackage = JavaPsiFacade.getInstance(myPsiManager.getProject()).findPackage(targetDirName);
          assertNotNull(newParentPackage);
          final PsiDirectory[] dirs = newParentPackage.getDirectories();
          assertEquals(dirs.length, 1);

          new MoveClassesOrPackagesProcessor(myProject, PsiUtilCore.toPsiElementArray(files),
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

  public void testMoveClass() throws Exception {
    final String testHx = "pack1/Moved.hx";
    final String targetDirName = "pack2";
    doTest((rootDir, rootAfter) -> {
      final VirtualFile src = VfsUtil.findRelativeFile(testHx, rootDir);
      assertNotNull("Class pack1.Moved not found", src);
      PsiElement file = myPsiManager.findFile(src);
      assertNotNull("Psi for " + testHx + " not found", file);
      PsiElement cls = file.getNode().getPsi(HaxeFile.class).findChildByClass(HaxeClassDeclaration.class);

      PsiPackage newParentPackage = JavaPsiFacade.getInstance(myPsiManager.getProject()).findPackage(targetDirName);
      assertNotNull(newParentPackage);

      ArrayList<PsiElement> list = new ArrayList<>();
      list.add(cls);
      new MoveClassesOrPackagesProcessor(myProject, PsiUtilCore.toPsiElementArray(list),
                                         new SingleSourceRootMoveDestination(PackageWrapper.create(newParentPackage),
                                                                             newParentPackage.getDirectories()[0]),
                                         true, true, null).run();
      FileDocumentManager.getInstance().saveAllDocuments();
    });
  }
}
