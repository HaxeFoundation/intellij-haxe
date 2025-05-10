/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2019-2020 Eric Bishton
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
 *
 * ================================================================
 *
 * Primarily extracted from HaxeLineMarkerProvider, provided under the same license.
 */
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.project.DumbService;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.index.HaxeInheritanceDefinitionsUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.HaxeNamedSubComponentUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
  public LineMarkerInfo<PsiElement> getLineMarkerInfo(@NotNull PsiElement element) {
    return null;
  }

  protected void collectSlowLineMarkersWorker(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
    for (PsiElement element : elements) {
      if (element instanceof HaxeClass haxeClass) {
        if (haxeClass.isObjectLiteralType()) continue;// ignore object literals as they do not inherit)
        if (haxeClass instanceof HaxeGenericListPart) continue;// ignore typeParameter definitions
        HaxeLineMarkerProviderNS.collectClassMarkers(result, haxeClass);
      }
    }
  }

  private static void collectClassMarkers(@NotNull Collection<? super LineMarkerInfo<?>> result, @NotNull HaxeClass haxeClass) {
    DumbService dumbService = DumbService.getInstance(haxeClass.getProject());
    if (dumbService.isDumb()) {
      dumbService.waitForSmartMode();
    }

    final List<HaxeClass> supers = HaxeResolveUtil.tryResolveClassesByQName(haxeClass.getHaxeExtendsList());
    supers.addAll(HaxeResolveUtil.tryResolveClassesByQName(haxeClass.getHaxeImplementsList()));
    final List<HaxeNamedComponent> superItems =  HaxeNamedSubComponentUtil.uniqueNamedSubComponents(HaxeNamedSubComponentUtil.getAllNamedSubComponentsFromClassTypes(supers));

    final Collection<HaxeClass> subs = HaxeInheritanceDefinitionsUtil.getItemsByQNameFirstLevelChildrenOnly(haxeClass);
    final List<HaxeClass> subClasses = subs.stream().filter(c -> !(c instanceof  HaxeTypedefDeclaration)).toList();;
    final List<HaxeClass> typeDefs = subs.stream().filter(c -> c instanceof  HaxeTypedefDeclaration).toList();

    final List<HaxeNamedComponent> subItems = new ArrayList<>();
    for (HaxeClass subClass : subClasses) {
        subItems.addAll(HaxeNamedSubComponentUtil.getNamedSubComponentsFromClassType(subClass));
    }

    final boolean isInterface = haxeClass.getComponentType() == HaxeComponentType.INTERFACE;
    if (!haxeClass.isTypeDef()) {
      for (HaxeNamedComponent haxeNamedComponent : HaxeNamedSubComponentUtil.getNamedSubComponentsFromClassType(haxeClass)) {
        final HaxeComponentType type = haxeNamedComponent.getComponentType();
        if (type == HaxeComponentType.METHOD || type == HaxeComponentType.FIELD) {
          LineMarkerInfo<PsiElement> item = HaxeLineMarkerUtil.tryCreateMemberOverrideMarker(haxeNamedComponent, superItems);
          if (item != null) {
            result.add(item);
          }
          item = HaxeLineMarkerUtil.tryCreateMemberImplementationMarker(haxeNamedComponent, subItems, isInterface);
          if (item != null) {
            result.add(item);
          }
        }
      }
    }else {
      HaxeTypeOrAnonymous typeOrAnonymous = ((HaxeTypedefDeclaration)haxeClass).getTypeOrAnonymous();
      if(typeOrAnonymous != null) {
        HaxeType type = typeOrAnonymous.getType();
        if (type != null) {
          HaxeResolveResult resolveResult = type.getReferenceExpression().resolveHaxeClass();
          HaxeClass resolved = resolveResult.getHaxeClass();
          if (resolved != null) {
            LineMarkerInfo<PsiElement> marker = HaxeLineMarkerUtil.createTypedefMarker(haxeClass, true);
            if (marker != null) {
              result.add(marker);
            }
          }
        }
      }
    }

    if (!subClasses.isEmpty()) {
      final LineMarkerInfo<PsiElement> marker = HaxeLineMarkerUtil.createTypeImplementationMarker(haxeClass, subClasses);
      if (marker != null) {
        result.add(marker);
      }
    }

    if (!typeDefs.isEmpty()) {
      final LineMarkerInfo<PsiElement> marker = HaxeLineMarkerUtil.createTypedefMarker(haxeClass,  false);
      if (marker != null) {
        result.add(marker);
      }
    }
  }

}
