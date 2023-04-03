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
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.index.HaxeInheritanceDefinitionsSearcher;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Worker/implementation class for {@link HaxeLineMarkerProvider}.
 *
 * Extracted so that HaxeLineMarker can be minimally tweaked for version changes.
 * IDEA 2020.2 changed the signature for {@link LineMarkerProvider#collectSlowLineMarkers}.
 *
 * Original @author: Fedor.Korotkov
 */
public abstract class HaxeLineMarkerProviderNS implements LineMarkerProvider {
  @Override
  public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
    return null;
  }

  protected void collectSlowLineMarkersWorker(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
    for (PsiElement element : elements) {
      if (element instanceof HaxeClass) {
        HaxeLineMarkerProviderNS.collectClassMarkers(result, (HaxeClass)element);
      }
    }
  }

  private static void collectClassMarkers(Collection<LineMarkerInfo> result, @NotNull HaxeClass haxeClass) {
    final List<HaxeClass> supers = HaxeResolveUtil.tyrResolveClassesByQName(haxeClass.getHaxeExtendsList());
    supers.addAll(HaxeResolveUtil.tyrResolveClassesByQName(haxeClass.getHaxeImplementsList()));
    final List<HaxeNamedComponent> superItems = HaxeResolveUtil.findNamedSubComponents(null, supers.toArray(new HaxeClass[supers.size()]));

    final List<HaxeClass> subClasses = HaxeInheritanceDefinitionsSearcher.getItemsByQName(haxeClass);
    final List<HaxeNamedComponent> subItems = new ArrayList<>();
    for (HaxeClass subClass : subClasses) {
      subItems.addAll(HaxeResolveUtil.getNamedSubComponents(subClass));
    }

    final boolean isInterface = HaxeComponentType.typeOf(haxeClass) == HaxeComponentType.INTERFACE;
    for (HaxeNamedComponent haxeNamedComponent : HaxeResolveUtil.getNamedSubComponents(haxeClass)) {
      final HaxeComponentType type = HaxeComponentType.typeOf(haxeNamedComponent);
      if (type == HaxeComponentType.METHOD || type == HaxeComponentType.FIELD) {
        LineMarkerInfo item = HaxeLineMarkerProviderNS.tryCreateOverrideMarker(haxeNamedComponent, superItems);
        if (item != null) {
          result.add(item);
        }
        item = HaxeLineMarkerProviderNS.tryCreateImplementationMarker(haxeNamedComponent, subItems, isInterface);
        if (item != null) {
          result.add(item);
        }
      }
    }

    if (!subClasses.isEmpty()) {
      final LineMarkerInfo marker = HaxeLineMarkerProviderNS.createImplementationMarker(haxeClass, subClasses);
      if (marker != null) {
        result.add(marker);
      }
    }
  }

  @Nullable
  private static LineMarkerInfo tryCreateOverrideMarker(final HaxeNamedComponent namedComponent,
                                                        List<HaxeNamedComponent> superItems) {

    final HaxeComponentName componentName = namedComponent.getComponentName();
    final String methodName = namedComponent.getName();
    if (componentName == null || methodName == null || methodName.isEmpty()) {
      return null;
    }

    final List<HaxeNamedComponent> filteredSuperItems = ContainerUtil.filter(superItems, item -> methodName.equals(item.getName()));
    if (filteredSuperItems.isEmpty()) {
      return null;
    }
    final PsiElement element = componentName.getIdentifier().getFirstChild();
    HaxeMethodDeclaration methodDeclaration = namedComponent instanceof HaxeMethodDeclaration ?
                                              (HaxeMethodDeclaration)namedComponent : null;
    final boolean overrides = methodDeclaration != null &&
                              HaxeResolveUtil.getDeclarationTypes(methodDeclaration.getMethodModifierList()).
                                contains(HaxeTokenTypes.KOVERRIDE);
    final Icon icon = overrides ? AllIcons.Gutter.OverridingMethod : AllIcons.Gutter.ImplementingMethod;
    Supplier<String> accessibleNameProvider = () -> overrides ? "Overriding Method" : "Implementing Method";
    if (null == element) {
      return null;
    }
    return new LineMarkerInfo<>(
      element,
      element.getTextRange(),
      icon,
      new Function<PsiElement, String>() {
        @Override
        public String fun(PsiElement element) {
          final HaxeClass superHaxeClass = PsiTreeUtil.getParentOfType(namedComponent, HaxeClass.class);
          if (superHaxeClass == null) return "null";
          if (overrides) {
            return HaxeBundle.message("overrides.method.in", namedComponent.getName(), superHaxeClass.getQualifiedName());
          }
          return HaxeBundle.message("implements.method.in", namedComponent.getName(), superHaxeClass.getQualifiedName());
        }
      },
      new GutterIconNavigationHandler<PsiElement>() {
        @Override
        public void navigate(MouseEvent e, PsiElement elt) {
          PsiElementListNavigator.openTargets(
            e,
            HaxeResolveUtil.getComponentNames(filteredSuperItems).toArray(new NavigatablePsiElement[filteredSuperItems.size()]),
            DaemonBundle.message("navigation.title.super.method", namedComponent.getName()),
            DaemonBundle.message("navigation.findUsages.title.super.method", namedComponent.getName()),
            new DefaultPsiElementCellRenderer());
        }
      },
      GutterIconRenderer.Alignment.LEFT,
      accessibleNameProvider
    );
  }

  @Nullable
  private static LineMarkerInfo tryCreateImplementationMarker(final HaxeNamedComponent namedComponent,
                                                              List<HaxeNamedComponent> subItems,
                                                              final boolean isInterface) {
    final HaxeComponentName componentName = namedComponent.getComponentName();
    final String methodName = namedComponent.getName();
    if (componentName == null || methodName == null || methodName.isEmpty()) {
      return null;
    }

    final List<HaxeNamedComponent> filteredSubItems = ContainerUtil.filter(subItems, item -> methodName.equals(item.getName()));
    if (filteredSubItems.isEmpty()) {
      return null;
    }
    final PsiElement element = componentName.getIdentifier().getFirstChild();
    Supplier<String> accessibleNameProvider = () -> isInterface ? "Implemented Method" : "Overriden Method";
    return new LineMarkerInfo<>(
      element,
      element.getTextRange(),
      isInterface ? AllIcons.Gutter.ImplementedMethod : AllIcons.Gutter.OverridenMethod,
      new Function<PsiElement, String>() {
        @Override
        public String fun(PsiElement element) {
          return isInterface
                 ? DaemonBundle.message("method.is.implemented.too.many")
                 : DaemonBundle.message("method.is.overridden.too.many");
        }
      },
      new GutterIconNavigationHandler<PsiElement>() {
        @Override
        public void navigate(MouseEvent e, PsiElement elt) {
          PsiElementListNavigator.openTargets(
            e, HaxeResolveUtil.getComponentNames(filteredSubItems).toArray(new NavigatablePsiElement[filteredSubItems.size()]),
            isInterface ?
            DaemonBundle.message("navigation.title.implementation.method", namedComponent.getName(), filteredSubItems.size())
                        :
            DaemonBundle.message("navigation.title.overrider.method", namedComponent.getName(), filteredSubItems.size()),
            "Implementations of " + namedComponent.getName(),
            new DefaultPsiElementCellRenderer()
          );
        }
      },
      GutterIconRenderer.Alignment.RIGHT,
      accessibleNameProvider
    );
  }

  @Nullable
  private static LineMarkerInfo createImplementationMarker(final HaxeClass componentWithDeclarationList,
                                                           final List<HaxeClass> items) {
    final HaxeComponentName componentName = componentWithDeclarationList.getComponentName();
    if (componentName == null) {
      return null;
    }
    final PsiElement element = componentName.getIdentifier().getFirstChild();
    Supplier<String> accessibleNameProvider = () -> componentWithDeclarationList instanceof HaxeInterfaceDeclaration ? "Implemented Method" : "Overriden Method";
    return new LineMarkerInfo<>(
      element,
      element.getTextRange(),
      componentWithDeclarationList instanceof HaxeInterfaceDeclaration
      ? AllIcons.Gutter.ImplementedMethod
      : AllIcons.Gutter.OverridenMethod,
      item -> DaemonBundle.message("method.is.implemented.too.many"),
      new GutterIconNavigationHandler<PsiElement>() {
        @Override
        public void navigate(MouseEvent e, PsiElement elt) {
          PsiElementListNavigator.openTargets(
            e, HaxeResolveUtil.getComponentNames(items).toArray(new NavigatablePsiElement[items.size()]),
            DaemonBundle.message("navigation.title.subclass", componentWithDeclarationList.getName(), items.size()),
            "Subclasses of " + componentWithDeclarationList.getName(),
            new DefaultPsiElementCellRenderer()
          );
        }
      },
      GutterIconRenderer.Alignment.RIGHT,
      accessibleNameProvider
    );
  }
}
