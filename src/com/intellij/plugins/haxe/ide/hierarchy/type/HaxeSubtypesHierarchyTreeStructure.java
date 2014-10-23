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
package com.intellij.plugins.haxe.ide.hierarchy.type;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.type.TypeHierarchyNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.HaxeAnonymousType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by srikanthg on 10/23/14.
 */
public class HaxeSubtypesHierarchyTreeStructure extends HierarchyTreeStructure {

  private final String myCurrentScopeType;

  protected HaxeSubtypesHierarchyTreeStructure(final Project project, final HierarchyNodeDescriptor descriptor, String currentScopeType) {
    super(project, descriptor);
    myCurrentScopeType = currentScopeType;
  }

  public HaxeSubtypesHierarchyTreeStructure(Project project, PsiClass psiClass, String currentScopeType) {
    super(project, new TypeHierarchyNodeDescriptor(project, null, psiClass, true));
    myCurrentScopeType = currentScopeType;
  }

  @NotNull
  protected final Object[] buildChildren(@NotNull final HierarchyNodeDescriptor descriptor) { // FIX THIS

    final Object element = ((TypeHierarchyNodeDescriptor)descriptor).getPsiClass();
    if (!(element instanceof PsiClass)) return ArrayUtil.EMPTY_OBJECT_ARRAY;

    final HaxeClass inClass = (HaxeClass) element;

    if (inClass instanceof HaxeAnonymousType) return ArrayUtil.EMPTY_OBJECT_ARRAY;
    if (inClass.hasModifierProperty(HaxePsiModifier.FINAL)) return ArrayUtil.EMPTY_OBJECT_ARRAY;

    final Project inClassPsiProject = inClass.getProject();
    final PsiFile inClassPsiFile = inClass.getContainingFile();

    final List<PsiClass> classes = new ArrayList<PsiClass>(); // add the sub-types to this list, as they are found

    // ----
    // search current file for sub-types that extend/implement from this one
    //
    final HaxeClass[] allClassesInFile = PsiTreeUtil.getChildrenOfType(inClassPsiFile, HaxeClass.class);
    for (HaxeClass aClassInFile : allClassesInFile) {
      if (isA(aClassInFile, inClass)) {
        classes.add(aClassInFile);
      }
    }

    // if private class, scope ends there
    if (inClass.hasModifierProperty(HaxePsiModifier.PRIVATE)) { // XXX: how about @:allow occurrences?
      return clsList2ObjArray(descriptor, classes);
    }

    // ----
    // search current package for sub-types that extend/implement from this one
    //
    final HaxePackageStatement packageStatement = PsiTreeUtil.getChildOfType(inClassPsiFile, HaxePackageStatement.class);
    final String packageStatementStr = HaxeResolveUtil.getPackageName(packageStatement);
    final VirtualFile[] packageVirtualDirs = UsefulPsiTreeUtil.getVirtualDirectoriesForPackage(packageStatementStr, inClassPsiProject);
    for (VirtualFile file : packageVirtualDirs) {
      final VirtualFile[] files = file.getChildren();
      for (VirtualFile virtualFile : files)
        if (virtualFile.getFileType().equals(HaxeFileType.HAXE_FILE_TYPE)) {
          final PsiFile psiFile = PsiManager.getInstance(inClassPsiProject).findFile(virtualFile);
          final HaxeClass[] classesInFile = PsiTreeUtil.getChildrenOfType(psiFile, HaxeClass.class);
          for (HaxeClass aClassInFile : classesInFile)
            if (isA(aClassInFile, inClass)) {
              classes.add(aClassInFile);
            }
        }
    }

    // ----
    // search all files - this is expensive
    //
    /* TODO: optimize - because, below code is searching all files (expensive). */
    final List<PsiFile> psiRoots = inClassPsiFile.getViewProvider().getAllFiles();
    for (PsiFile f : psiRoots) {
      if (f != null) {
        final VirtualFile virtualFile = f.getVirtualFile();
        if (virtualFile.getFileType().equals(HaxeFileType.HAXE_FILE_TYPE)) {
          final PsiFile psiFile = PsiManager.getInstance(inClassPsiProject).findFile(virtualFile);
          final HaxeClass[] classesInFile = PsiTreeUtil.getChildrenOfType(psiFile, HaxeClass.class);
          for (HaxeClass aClassInFile : classesInFile)
            if (isA(aClassInFile, inClass)) {
              classes.add(aClassInFile);
            }
        }
      }
    }

    /* XXX: optimize tip: use helper methods in PsiTreeUtil, UsefulPsiTreeUtil, HaxeResolveutil etc to narrow search to below list: */
    /* find current module & then its dependent modules and narrow scope to it. */
    //Module module = ModuleUtil.findModuleForFile(virtualFile, inClass.getProject()); // module that the class/file belongs to
    //List<Module> modules = ModuleManager.getInstance(inClass.getProject()).getModuleDependentModules(module); // dependent modules

    return clsList2ObjArray(descriptor, classes);
  }


  @NotNull
  protected final Object[] clsList2ObjArray(@NotNull final HierarchyNodeDescriptor descriptor, @NotNull final List<PsiClass> classes) {
    final int size = classes.size();
    if (size > 0) {
      final List<HierarchyNodeDescriptor> descriptors = new ArrayList<HierarchyNodeDescriptor>(size);
      for (PsiClass aClass : classes) {
        descriptors.add(new TypeHierarchyNodeDescriptor(myProject, descriptor, aClass, false));
      }
      return descriptors.toArray(new HierarchyNodeDescriptor[descriptors.size()]);
    }
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  protected static boolean isA(PsiClass theClass, PsiClass possibleSuperClass) {
    if (!theClass.isValid()) return false;
    //if (aClass.isInterface()) return false; // XXX: uncomment ?
    final String tcfqn = theClass.getQualifiedName();
    final String pscfqn = possibleSuperClass.getQualifiedName();
    if (pscfqn.equals(tcfqn)) return false; // it's the same class in LHS & RHS
    final ArrayList<PsiClass> allSuperClasses = getSuperClassList(theClass);
    for (PsiClass aSuperClass : allSuperClasses) {
      if (pscfqn.equals(aSuperClass.getQualifiedName())) {
        return true;
      }
    }
    return false;
  }

  protected static PsiClass[] createSuperClasses(PsiClass theClass) {
    if (!theClass.isValid()) return PsiClass.EMPTY_ARRAY;
    //if (aClass.isInterface()) return PsiClass.EMPTY_ARRAY; // XXX: uncomment ?
    final ArrayList<PsiClass> allSuperClasses = getSuperClassList(theClass);
    return allSuperClasses.toArray(new PsiClass[allSuperClasses.size()]);
  }

  protected static ArrayList<PsiClass> getSuperClassList(PsiClass theClass) {
    final ArrayList<PsiClass> allSuperClasses = new ArrayList<PsiClass>();
    while (true) {
      final PsiClass aClass1 = theClass;
      final PsiClass[] superTypes = aClass1.getSupers();
      PsiClass superType = null;
      for (int i = 0; i < superTypes.length; i++) {
        final PsiClass type = superTypes[i];
        if (!type.isInterface()) {
          superType = type;
          break;
        }
      }
      if (superType == null) break;
      if (allSuperClasses.contains(superType)) break;
      allSuperClasses.add(superType);
      theClass = superType;
    }
    return allSuperClasses;
  }
}
