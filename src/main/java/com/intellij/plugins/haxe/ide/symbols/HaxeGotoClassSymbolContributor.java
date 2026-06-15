
package com.intellij.plugins.haxe.ide.symbols;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HaxeGotoClassSymbolContributor implements ChooseByNameContributor {


  @NotNull
  @Override
  public String[] getNames(@NotNull final Project project, final boolean includeNonProjectItems) {
    if (DumbService.isDumb(project)) return ArrayUtil.EMPTY_STRING_ARRAY;
    Collection<String> AllClasses = HaxeClassNameUnifiedIndex.getAllKeys(project);
    return ArrayUtil.toStringArray(AllClasses);
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

    Collection<HaxeClass> haxeClasses = HaxeClassNameUnifiedIndex.getByNameFiltered(name, project, scope);
    for (HaxeClass cls : haxeClasses) {
      HaxeComponentName cn = cls.getComponentName();
      if (cn != null) result.add(cn);
    }

    return result.toArray(NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY);
  }
}
