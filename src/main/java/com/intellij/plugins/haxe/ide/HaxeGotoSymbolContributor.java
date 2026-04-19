/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeClassNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeFieldNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeMethodNameStubIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HaxeGotoSymbolContributor implements ChooseByNameContributor {
  @NotNull
  @Override
  public String[] getNames(@NotNull final Project project, final boolean includeNonProjectItems) {
    if (DumbService.isDumb(project)) return ArrayUtil.EMPTY_STRING_ARRAY;
    StubIndex stubIndex = StubIndex.getInstance();

    final Set<String> result = new LinkedHashSet<>();
    result.addAll(stubIndex.getAllKeys(HaxeClassNameStubIndex.KEY, project));
    for (String name : stubIndex.getAllKeys(HaxeMethodNameStubIndex.KEY, project)) {
      if (!"new".equals(name)) result.add(name);
    }
    result.addAll(stubIndex.getAllKeys(HaxeFieldNameStubIndex.KEY, project));
    return ArrayUtil.toStringArray(result);
  }

  @NotNull
  @Override
  public NavigationItem[] getItemsByName(@NotNull final String name,
                                         @NotNull final String pattern,
                                         @NotNull final Project project,
                                         final boolean includeNonProjectItems) {
    if (DumbService.isDumb(project)) return NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY;
    final GlobalSearchScope scope = includeNonProjectItems ? GlobalSearchScope.allScope(project) : GlobalSearchScope.projectScope(project);

    final List<HaxeComponentName> result = new ArrayList<>();
    Collection<HaxeClass> haxeClasses = HaxeClassNameStubIndex.getByName(name, project, scope);
    for (HaxeClass cls : haxeClasses) {
      HaxeComponentName cn = cls.getComponentName();
      if (cn != null) result.add(cn);
    }
    if (!"new".equals(name)) {
      for (HaxeMethod method : StubIndex.getElements(HaxeMethodNameStubIndex.KEY, name, project, scope, HaxeMethod.class)) {
        HaxeComponentName cn = method.getComponentName();
        if (cn != null) result.add(cn);
      }
    }
    for (HaxePsiField field : StubIndex.getElements(HaxeFieldNameStubIndex.KEY, name, project, scope, HaxePsiField.class)) {
      HaxeComponentName cn = field.getComponentName();
      if (cn != null) result.add(cn);
    }
    return result.toArray(NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY);
  }
}
