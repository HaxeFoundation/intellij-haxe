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
package com.intellij.plugins.haxe.ide.hierarchy.method;

import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.method.MethodHierarchyBrowser;
import com.intellij.ide.hierarchy.method.MethodHierarchyTreeStructure;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

/**
 * Created by ebishton on 11/1/14.
 */
public class HaxeMethodHierarchyBrowser extends MethodHierarchyBrowser {

  Logger LOG = Logger.getInstance("#" + HaxeMethodHierarchyBrowser.class.getName());

  public HaxeMethodHierarchyBrowser(final Project project, final PsiMethod method) {
    super(project, method);
  }

  @Override
  protected HierarchyTreeStructure createHierarchyTreeStructure(@NotNull String typeName, @NotNull PsiElement psiElement) {
    if (!METHOD_TYPE.equals(typeName)) {
      LOG.error("unexpected type: " + typeName);
      return null;
    }
    return new HaxeMethodHierarchyTreeStructure(myProject, (PsiMethod)psiElement);
  }
}
