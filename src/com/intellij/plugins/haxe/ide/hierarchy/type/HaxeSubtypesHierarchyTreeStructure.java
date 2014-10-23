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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
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
import com.intellij.psi.search.searches.AllClassesSearch;
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
  protected final Object[] buildChildren(@NotNull final HierarchyNodeDescriptor descriptor) {

    final Object element = ((TypeHierarchyNodeDescriptor)descriptor).getPsiClass();
    if (!(element instanceof PsiClass)) return ArrayUtil.EMPTY_OBJECT_ARRAY;

    final HaxeClass theHaxeClass = (HaxeClass) element;

    if (theHaxeClass instanceof HaxeAnonymousType) return ArrayUtil.EMPTY_OBJECT_ARRAY;
    if (theHaxeClass.hasModifierProperty(HaxePsiModifier.FINAL)) return ArrayUtil.EMPTY_OBJECT_ARRAY;

    final Project inClassPsiProject = theHaxeClass.getProject();
    final PsiFile inClassPsiFile = theHaxeClass.getContainingFile();

    final List<PsiClass> subTypeList = new ArrayList<PsiClass>(); // add the sub-types to this list, as they are found

    // ----
    // search current file for sub-types that extend/implement from this class/type
    //
    final HaxeClass[] allHaxeClassesInFile = PsiTreeUtil.getChildrenOfType(inClassPsiFile, HaxeClass.class);
    for (HaxeClass aClassInFile : allHaxeClassesInFile) {
      if (isThisTypeASubTypeOfTheSuperType(aClassInFile, theHaxeClass)) {
        subTypeList.add(aClassInFile);
      }
    }

    // if private class, scope ends there
    if (theHaxeClass.hasModifierProperty(HaxePsiModifier.PRIVATE)) { // XXX: how about @:allow occurrences?
      return typeListToObjArray(descriptor, subTypeList);
    }

    // ----
    // search current package for sub-types that extend/implement from this class/type
    //
    final HaxePackageStatement packageStatement = PsiTreeUtil.getChildOfType(inClassPsiFile, HaxePackageStatement.class);
    final String packageStatementStr = HaxeResolveUtil.getPackageName(packageStatement);
    final VirtualFile[] packageVirtualDirs = UsefulPsiTreeUtil.getVirtualDirectoriesForPackage(packageStatementStr, inClassPsiProject);
    for (VirtualFile file : packageVirtualDirs) {
      final VirtualFile[] files = file.getChildren();
      for (VirtualFile virtualFile : files)
        if (virtualFile.getFileType().equals(HaxeFileType.HAXE_FILE_TYPE)) {
          final PsiFile psiFile = PsiManager.getInstance(inClassPsiProject).findFile(virtualFile);
          final HaxeClass[] allHaxeClassesInThisFile = PsiTreeUtil.getChildrenOfType(psiFile, HaxeClass.class);
          for (HaxeClass aHaxeClassInFile : allHaxeClassesInThisFile)
            if (isThisTypeASubTypeOfTheSuperType(aHaxeClassInFile, theHaxeClass)) {
              subTypeList.add(aHaxeClassInFile);
            }
        }
    }


    // TODO: Must be fixed - see below description and comments with "//XXX: FIX:" prefix (in this method)
    // AllClassesSearch.search calls are returning empty list/array.
    // Without that fixed, sub-types that are not in the same package (as the type in focus) will not be listed...
    // even though they are in same module or in a module that's directly listed as being dependent its module.
    // It will mislead as being functional because, the sub-types within same package continue to be listed.


    // ----
    // search the module (this class belongs to) for sub-types that extend/implement from this class/type
    //
    Module theModule = ModuleUtil.findModuleForFile(inClassPsiFile.getVirtualFile(), theHaxeClass.getProject());
    final PsiClass[] allClassesInTheModule = ArrayUtil.toObjectArray(
      AllClassesSearch.search(theModule.getModuleScope(), inClassPsiProject).findAll(), PsiClass.class); //XXX: FIX: always '0' sized !
    for (PsiClass aClassInFile : allClassesInTheModule) {
      final HaxeClass hxClassInFile = (HaxeClass) aClassInFile;
      if (isThisTypeASubTypeOfTheSuperType(hxClassInFile, theHaxeClass)) {
        subTypeList.add(aClassInFile);
      }
    }

    // ----
    // search the modules dependent on module (one that, this class belongs to) for sub-types that extend/implement from this class/type
    //
    List<Module> dependentModules = ModuleManager.getInstance(theHaxeClass.getProject()).getModuleDependentModules(theModule);
    for (Module module : dependentModules) {
      final PsiClass[] allClassesInModule = ArrayUtil.toObjectArray(
        AllClassesSearch.search(module.getModuleScope(), inClassPsiProject).findAll(), PsiClass.class); //XXX: FIX: always '0' sized !
      for (PsiClass aClassInFile : allClassesInModule) {
        final HaxeClass hxClassInFile = (HaxeClass) aClassInFile;
        if (isThisTypeASubTypeOfTheSuperType(hxClassInFile, theHaxeClass)) {
          subTypeList.add(aClassInFile);
        }
      }
    }

    return typeListToObjArray(descriptor, subTypeList);
  }


  @NotNull
  protected final Object[] typeListToObjArray(@NotNull final HierarchyNodeDescriptor descriptor, @NotNull final List<PsiClass> classes) {
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

  public static boolean isThisTypeASubTypeOfTheSuperType(PsiClass thisType, PsiClass theSuperType) {
    if (!thisType.isValid()) return false;
    final String tcfqn = thisType.getQualifiedName();
    final String pscfqn = theSuperType.getQualifiedName();
    if (pscfqn.equals(tcfqn)) return false; // it's the same class in LHS & RHS
    final ArrayList<PsiClass> allSuperTypes = getSuperTypesAsList(thisType);
    for (PsiClass aSuperType : allSuperTypes) {
      if (pscfqn.equals(aSuperType.getQualifiedName())) {
        return true;
      }
    }
    return false;
  }

  public static PsiClass[] getSuperTypesAsArray(PsiClass theClass) {
    if (!theClass.isValid()) return PsiClass.EMPTY_ARRAY;
    final ArrayList<PsiClass> allSuperClasses = getSuperTypesAsList(theClass);
    return allSuperClasses.toArray(new PsiClass[allSuperClasses.size()]);
  }

  public static ArrayList<PsiClass> getSuperTypesAsList(PsiClass theClass) {
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
