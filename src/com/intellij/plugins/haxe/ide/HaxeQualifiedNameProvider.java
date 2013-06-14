/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.ide;

import com.intellij.ide.actions.QualifiedNameProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeQualifiedNameProvider implements QualifiedNameProvider {
  @Override
  public PsiElement adjustElementToCopy(PsiElement element) {
    if (element instanceof HaxeCallExpression) {
      element = ((HaxeCallExpression)element).getExpression();
    }
    if (element instanceof HaxeReference) {
      element = ((HaxeReference)element).resolve();
    }
    if (element instanceof HaxeComponentName) {
      return element.getParent();
    }
    return element;
  }

  @Override
  public String getQualifiedName(PsiElement element) {
    if (element instanceof HaxeClass) {
      return ((HaxeClass)element).getQualifiedName();
    }
    final HaxeComponentType componentType = HaxeComponentType.typeOf(element);
    if (componentType == HaxeComponentType.METHOD || componentType == HaxeComponentType.FIELD) {
      final String name = ((HaxeComponent)element).getName();
      final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(element, HaxeClass.class, true);
      if (name != null && haxeClass != null) {
        return haxeClass.getQualifiedName() + "#" + name;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public PsiElement qualifiedNameToElement(String fqn, Project project) {
    final int index = fqn.indexOf("#");
    if (index == -1) {
      final HaxeClass haxeClass =
        HaxeResolveUtil.findClassByQName(fqn, PsiManager.getInstance(project), GlobalSearchScope.projectScope(project));
      return haxeClass == null ? null : haxeClass.getComponentName();
    }
    final HaxeClass haxeClass =
      HaxeResolveUtil.findClassByQName(fqn.substring(0, index), PsiManager.getInstance(project), GlobalSearchScope.projectScope(project));
    if (haxeClass == null) {
      return null;
    }
    final String memberName = fqn.substring(index + 1);
    HaxeNamedComponent namedComponent = haxeClass.findMethodByName(memberName);
    if (namedComponent == null) {
      namedComponent = haxeClass.findFieldByName(memberName);
    }
    return namedComponent == null ? null : namedComponent.getComponentName();
  }

  @Override
  public void insertQualifiedName(String fqn, PsiElement element, Editor editor, Project project) {
    EditorModificationUtil.insertStringAtCaret(editor, fqn);
  }
}
