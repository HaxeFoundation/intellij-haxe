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
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Created by srikanthg on 10/23/14.
 */
public class HaxeTypeHierarchyBrowser extends TypeHierarchyBrowser {
  private static final HaxeDebugLogger LOG = HaxeDebugLogger.getInstance("#com.intellij.plugins.haxe.ide.hierarchy.type.HaxeTypeHierarchyBrowser");

  public HaxeTypeHierarchyBrowser(final Project project, final PsiClass psiClass) {
    super(project, psiClass);
  }

  protected HierarchyTreeStructure createHierarchyTreeStructure(@NotNull final String typeName, @NotNull final PsiElement psiElement) {
    HierarchyTreeStructure currentActiveTree = null;
    if (getSupertypesHierarchyType().equals(typeName)) {
      currentActiveTree = new HaxeSupertypesHierarchyTreeStructure(myProject, (PsiClass) psiElement);
    }
    else if (getSubtypesHierarchyType().equals(typeName)) {
      currentActiveTree = new HaxeSubtypesHierarchyTreeStructure(myProject, (PsiClass) psiElement);
    }
    else if (getTypeHierarchyType().equals(typeName)) {
      currentActiveTree = new HaxeTypeHierarchyTreeStructure(myProject, (PsiClass) psiElement);
    }
    else {
      LOG.error("unexpected type: " + typeName);
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
}
