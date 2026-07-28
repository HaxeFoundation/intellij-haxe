/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe.ide.refactoring;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeMultiFileTestBase;
import com.intellij.plugins.haxe.lang.psi.HaxeClassDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.file.PsiDirectoryImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.PackageWrapper;
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesProcessor;
import com.intellij.refactoring.move.moveClassesOrPackages.SingleSourceRootMoveDestination;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author: Fedor.Korotkov
 */
@DisplayName("Refactoring: move")
public class HaxeMoveTest extends HaxeMultiFileTestBase {

  @Override
  protected void tearDown() throws Exception {
    HaxeTestUtils.cleanupUnexpiredAppleUITimers(this::addSuppressedException);
    super.tearDown();
  }

  @Override
  protected String getTestRoot() {
    return "/move/";
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
          assertNotNull(child, "Neither class nor file " + s + " not found");
          PsiElement file = myPsiManager.findFile(child);
          if (file == null) file = JavaPsiFacade.getInstance(myProject).findPackage(s);
          files.add(file);
        }
        final VirtualFile child1 = VfsUtil.findRelativeFile(targetDirName, rootDir);
        assertNotNull(child1, "Target dir " + targetDirName + " not found");
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

  @Test
  @DisplayName("move file 1")
  public void testMoveFile1() throws Exception {
    doTest("util/ArrayUtils.hx", "bar");
  }

  @Test
  @DisplayName("move file 2")
  public void testMoveFile2() throws Exception {
    doTest("util/ArrayUtils.hx", "");
  }

  @Test
  @DisplayName("move package")
  public void testMovePackage() throws Exception {
    doTest("util", "foo");
  }

  @Test
  @DisplayName("move class")
  public void testMoveClass() throws Exception {
    final String testHx = "pack1/Moved.hx";
    final String targetDirName = "pack2";
    doTest((rootDir, rootAfter) -> {
      final VirtualFile src = VfsUtil.findRelativeFile(testHx, rootDir);
      assertNotNull(src, "Class pack1.Moved not found");


      PsiElement file = myPsiManager.findFile(src);
      assertNotNull(file, "Psi for " + testHx + " not found");
      HaxeModule haxeModule = PsiTreeUtil.getStubChildOfType(file, HaxeModule.class);
      PsiElement cls = PsiTreeUtil.getStubChildOfType(haxeModule, HaxeClassDeclaration.class);

      PackageWrapper pack = new PackageWrapper(myPsiManager, targetDirName);
      VirtualFile targetDir = VfsUtil.findRelativeFile(targetDirName, rootDir);
      PsiDirectoryImpl dir = new PsiDirectoryImpl((PsiManagerImpl)myPsiManager, targetDir);

      ArrayList<PsiElement> list = new ArrayList<>();
      list.add(cls);
      new MoveClassesOrPackagesProcessor(myProject, PsiUtilCore.toPsiElementArray(list),
                                         new SingleSourceRootMoveDestination(pack, dir),
                                         true, true, null).run();
      FileDocumentManager.getInstance().saveAllDocuments();
    });
  }
}
