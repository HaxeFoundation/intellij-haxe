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
package com.intellij.plugins.haxe.ide.refactoring;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.refactoring.memberPullUp.PullUpConflictsUtil;
import com.intellij.plugins.haxe.ide.refactoring.memberPullUp.PullUpProcessor;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.refactoring.util.DocCommentPolicy;
import com.intellij.refactoring.util.classMembers.InterfaceContainmentVerifier;
import com.intellij.refactoring.util.classMembers.MemberInfo;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Boch on 06.04.2015.
 */
public class HaxePullUpMultifileTest extends MultiFileTestCase {
  @Override
  protected String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH;
  }

  @NotNull
  @Override
  protected String getTestRoot() {
    return "/refactoring/pullUp/";
  }

  private void doTestMethod(final String... conflicts) throws Exception {
    final MultiMap<PsiElement, String> conflictsMap = new MultiMap<PsiElement, String>();
    doTest(new PerformAction() {
      @Override
      public void performAction(final VirtualFile rootDir, final VirtualFile rootAfter) throws Exception {
        final PsiClass srcClass = HaxeResolveUtil.findClassByQName("a.A", getPsiManager(), GlobalSearchScope.allScope(myProject));
        assertTrue("Source class not found", srcClass != null);
        final PsiClass targetClass = HaxeResolveUtil.findClassByQName("b.B", getPsiManager(), GlobalSearchScope.allScope(myProject));
        assertTrue("Target class not found", targetClass != null);
        final PsiMethod[] methods = srcClass.getMethods();
        assertTrue("No methods found", methods.length > 0);
        final MemberInfo[] membersToMove = new MemberInfo[1];
        final MemberInfo memberInfo = new MemberInfo(methods[0]);
        memberInfo.setChecked(true);
        membersToMove[0] = memberInfo;
        final PsiDirectory targetDirectory = targetClass.getContainingFile().getContainingDirectory();
        final PsiPackage targetPackage = targetDirectory != null ? JavaDirectoryService.getInstance().getPackage(targetDirectory) : null;
        conflictsMap.putAllValues(
          PullUpConflictsUtil
            .checkConflicts(membersToMove, srcClass, targetClass, targetPackage, targetDirectory, new InterfaceContainmentVerifier() {
              @Override
              public boolean checkedInterfacesContain(PsiMethod psiMethod) {
                return PullUpProcessor.checkedInterfacesContain(Arrays.asList(membersToMove), psiMethod);
              }
            }));
        new PullUpProcessor(srcClass, targetClass, membersToMove, new DocCommentPolicy(DocCommentPolicy.ASIS)).run();
      }
    });
    if (conflicts.length != 0 && conflictsMap.isEmpty()) {
      fail("Conflict was not detected");
    }
    final HashSet<String> values = new HashSet<String>(conflictsMap.values());
    final HashSet<String> expected = new HashSet<String>(Arrays.asList(conflicts));
    assertEquals(expected.size(), values.size());
    for (String value : values) {
      if (!expected.contains(value)) {
        fail("Conflict: " + value + " is unexpectedly reported");
      }
    }
  }

  private void doTestField(final String... conflicts) throws Exception {
    final MultiMap<PsiElement, String> conflictsMap = new MultiMap<PsiElement, String>();
    doTest(new PerformAction() {
      @Override
      public void performAction(final VirtualFile rootDir, final VirtualFile rootAfter) throws Exception {
        final PsiClass srcClass = HaxeResolveUtil.findClassByQName("a.A", getPsiManager(), GlobalSearchScope.allScope(myProject));
        assertTrue("Source class not found", srcClass != null);
        final PsiClass targetClass = HaxeResolveUtil.findClassByQName("b.B", getPsiManager(), GlobalSearchScope.allScope(myProject));
        assertTrue("Target class not found", targetClass != null);
        final PsiField[] fields = srcClass.getFields();
        assertTrue("No methods found", fields.length > 0);
        final MemberInfo[] membersToMove = new MemberInfo[1];
        final MemberInfo memberInfo = new MemberInfo(fields[0]);
        memberInfo.setChecked(true);
        membersToMove[0] = memberInfo;
        final PsiDirectory targetDirectory = targetClass.getContainingFile().getContainingDirectory();
        final PsiPackage targetPackage = targetDirectory != null ? JavaDirectoryService.getInstance().getPackage(targetDirectory) : null;
        conflictsMap.putAllValues(
          PullUpConflictsUtil
            .checkConflicts(membersToMove, srcClass, targetClass, targetPackage, targetDirectory, new InterfaceContainmentVerifier() {
              @Override
              public boolean checkedInterfacesContain(PsiMethod psiMethod) {
                return PullUpProcessor.checkedInterfacesContain(Arrays.asList(membersToMove), psiMethod);
              }
            }));
        new PullUpProcessor(srcClass, targetClass, membersToMove, new DocCommentPolicy(DocCommentPolicy.ASIS)).run();
      }
    });
    if (conflicts.length != 0 && conflictsMap.isEmpty()) {
      fail("Conflict was not detected");
    }
    final HashSet<String> values = new HashSet<String>(conflictsMap.values());
    final HashSet<String> expected = new HashSet<String>(Arrays.asList(conflicts));
    assertEquals(expected.size(), values.size());
    for (String value : values) {
      if (!expected.contains(value)) {
        fail("Conflict: " + value + " is unexpectedly reported");
      }
    }
  }


  public void testFromClassToSuperClass() throws Exception {
    doTestMethod();
  }

  public void testMoveFieldFromClassToSuperClass() throws Exception {
    doTestField();
  }

  public void testMoveFieldWithInitializerFromClassToSuperClass() throws Exception {
    doTestField();
  }

  public void testFromClassToInterface() throws Exception {
    doTestMethod();
  }

  public void testFromInterfaceToInterface() throws Exception {
    doTestMethod();
  }
}
