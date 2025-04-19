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
import com.intellij.ide.hierarchy.type.TypeHierarchyBrowser;
import com.intellij.openapi.project.Project;

import com.intellij.plugins.haxe.ide.hierarchy.type.treestructures.HaxeSubtypesHierarchyTreeStructure;
import com.intellij.plugins.haxe.ide.hierarchy.type.treestructures.HaxeSupertypesHierarchyTreeStructure;
import com.intellij.plugins.haxe.ide.hierarchy.type.treestructures.HaxeTypeHierarchyTreeStructure;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.ide.hierarchy.type.treestructures.HaxeSubtypesHierarchyTreeStructure.getSuperTypesAsArray;


/**
 * Created by srikanthg on 10/23/14.
 */
@CustomLog
public class HaxeTypeHierarchyBrowser extends TypeHierarchyBrowser {
  PsiClass[] superTypesAsArray = new PsiClass[0];

  public HaxeTypeHierarchyBrowser(final Project project, final PsiClass psiClass) {
    super(project, psiClass);
  }

  protected HierarchyTreeStructure createHierarchyTreeStructure(@NotNull final String typeName, @NotNull final PsiElement psiElement) {
    HierarchyTreeStructure currentActiveTree = null;
    if (getSupertypesHierarchyType().equals(typeName)) {
      currentActiveTree = new HaxeSupertypesHierarchyTreeStructure(myProject, (PsiClass) psiElement);
    }
    else if (getSubtypesHierarchyType().equals(typeName)) {
      currentActiveTree = new HaxeSubtypesHierarchyTreeStructure(myProject, (PsiClass) psiElement, getCurrentScopeType());
    }
    else if (getTypeHierarchyType().equals(typeName)) {
      currentActiveTree = new HaxeTypeHierarchyTreeStructure(myProject, (PsiClass) psiElement, getCurrentScopeType(), superTypesAsArray);
    }
    else {
      log.error("unexpected type: " + typeName);
    }
    return currentActiveTree;
  }

  // --------
  // this is essential for auto-scrolling to source
  //
  protected PsiElement getElementFromDescriptor(@NotNull HierarchyNodeDescriptor descriptor) {
    if (!(descriptor instanceof HaxeTypeHierarchyNodeDescriptor)) return null;
    return ((HaxeTypeHierarchyNodeDescriptor) descriptor).getHaxeClass();
  }


  // HACK
  // getSuperTypes is too slow and we get exceptions thrown complaining about slow EDT thread.
  //(Slow operations are prohibited on EDT. See SlowOperations.assertSlowOperationsAreAllowed javadoc.)
  //
  // We need to know the super type root because it has to be the first TypeHierarchyNodeDescriptor in TypeHierarchy
  // and so  in order to find the root/base type without interfering with the EDT thread we try to calculate it  in
  // method calls that are in a background thread.
  public void collectInfo(PsiClass psiClass) {
    superTypesAsArray = getSuperTypesAsArray(psiClass);
  }

  @Override
  protected void doRefresh(boolean currentBuilderOnly) {
    PsiElement element =getHierarchyBase();
    if(element instanceof  PsiClass psiClass) {
      collectInfo(psiClass);
    }else {
      superTypesAsArray = new PsiClass[0];
    }
    super.doRefresh(currentBuilderOnly);
  }


}
