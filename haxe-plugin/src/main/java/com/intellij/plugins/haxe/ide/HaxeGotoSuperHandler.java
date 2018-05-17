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
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeGotoSuperHandler implements LanguageCodeInsightActionHandler {
  @Override
  public boolean isValidFor(Editor editor, PsiFile file) {
    return file.getLanguage() == HaxeLanguage.INSTANCE;
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    final PsiElement at = file.findElementAt(editor.getCaretModel().getOffset());
    final HaxeComponentName componentName = PsiTreeUtil.getParentOfType(at, HaxeComponentName.class);

    final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(at, HaxeClass.class);
    final HaxeNamedComponent namedComponent = componentName == null ? haxeClass : (HaxeNamedComponent)componentName.getParent();
    if (at == null || haxeClass == null || namedComponent == null) return;

    final List<HaxeClass> supers = HaxeResolveUtil.tyrResolveClassesByQName(haxeClass.getHaxeExtendsList());
    supers.addAll(HaxeResolveUtil.tyrResolveClassesByQName(haxeClass.getHaxeImplementsList()));
    final List<HaxeNamedComponent> superItems = HaxeResolveUtil.findNamedSubComponents(false, supers.toArray(new HaxeClass[supers.size()]));

    final HaxeComponentType type = HaxeComponentType.typeOf(namedComponent);
    if (type == HaxeComponentType.METHOD) {
      final HaxeMethod methodDeclaration = (HaxeMethod)namedComponent;
      tryNavigateToSuperMethod(editor, methodDeclaration, superItems);
    }
    else if (!supers.isEmpty() && namedComponent instanceof HaxeClass) {
      PsiElementListNavigator.openTargets(
        editor,
        HaxeResolveUtil.getComponentNames(supers).toArray(new NavigatablePsiElement[supers.size()]),
        DaemonBundle.message("navigation.title.subclass", namedComponent.getName(), supers.size()),
        "Subclasses of " + namedComponent.getName(),
        new DefaultPsiElementCellRenderer()
      );
    }
  }

  private static void tryNavigateToSuperMethod(Editor editor,
                                               HaxeMethod methodDeclaration,
                                               List<HaxeNamedComponent> superItems) {
    final String methodName = methodDeclaration.getName();
    if (methodName == null) {
      return;
    }
    final List<HaxeNamedComponent> filteredSuperItems = ContainerUtil.filter(superItems, new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        return methodName.equals(component.getName());
      }
    });
    if (!filteredSuperItems.isEmpty()) {
      PsiElementListNavigator.openTargets(editor, HaxeResolveUtil.getComponentNames(filteredSuperItems)
        .toArray(new NavigatablePsiElement[filteredSuperItems.size()]),
                                          DaemonBundle.message("navigation.title.super.method", methodName),
                                          null,
                                          new DefaultPsiElementCellRenderer());
    }
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
