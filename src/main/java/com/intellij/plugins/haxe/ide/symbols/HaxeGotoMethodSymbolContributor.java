package com.intellij.plugins.haxe.ide.symbols;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HaxeGotoMethodSymbolContributor implements ChooseByNameContributor {

  @NotNull
  @Override
  public String[] getNames(@NotNull final Project project, final boolean includeNonProjectItems) {
    if (DumbService.isDumb(project)) return ArrayUtil.EMPTY_STRING_ARRAY;
    final Set<String> result = new LinkedHashSet<>();

    for (String name : HaxeClassMethodNameUnifiedIndex.getAllKeys(project)) {
      // ignoring constructors
      if (!"new".equals(name)) {
        result.add(name);
      }
    }

    result.addAll(HaxeStaticMethodNameUnifiedIndex.getAllKeys(project));
    result.addAll(HaxeModuleMethodNameUnifiedIndex.getAllKeys(project));

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

    if (!"new".equals(name)) {
      for (HaxeMethod method : HaxeClassMethodNameUnifiedIndex.getByName(name, project, scope)) {
        HaxeComponentName cn = method.getComponentName();
        if (cn != null) result.add(cn);
      }
    }

    for (HaxeMethod method : HaxeStaticMethodNameUnifiedIndex.getByName( name, project, scope)) {
      HaxeComponentName cn = method.getComponentName();
      if (cn != null) result.add(cn);
    }

    for (HaxeMethod method : HaxeModuleMethodNameUnifiedIndex.getByName( name, project, scope)) {
      HaxeComponentName cn = method.getComponentName();
      if (cn != null) result.add(cn);
    }

    return result.toArray(NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY);
  }
}
