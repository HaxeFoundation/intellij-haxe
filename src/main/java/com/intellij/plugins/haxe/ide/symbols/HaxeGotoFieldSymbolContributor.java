package com.intellij.plugins.haxe.ide.symbols;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HaxeGotoFieldSymbolContributor implements ChooseByNameContributor {

  @NotNull
  @Override
  public String[] getNames(@NotNull final Project project, final boolean includeNonProjectItems) {
    if (DumbService.isDumb(project)) return ArrayUtil.EMPTY_STRING_ARRAY;
    List<String> allFields = new ArrayList<>();

    allFields.addAll(HaxeClassFieldNameUnifiedIndex.getAllKeys(project));
    allFields.addAll(HaxeStaticFieldNameUnifiedIndex.getAllKeys(project));
    allFields.addAll(HaxeModuleFieldNameUnifiedIndex.getAllKeys(project));

    return ArrayUtil.toStringArray(allFields);
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

    for (HaxePsiField field : HaxeClassFieldNameUnifiedIndex.getByName( name, project, scope)) {
      HaxeComponentName cn = field.getComponentName();
      if (cn != null) result.add(cn);
    }

    for (HaxePsiField field : HaxeStaticFieldNameUnifiedIndex.getByName( name, project, scope)) {
      HaxeComponentName cn = field.getComponentName();
      if (cn != null) result.add(cn);
    }

    for (HaxePsiField field : HaxeModuleFieldNameUnifiedIndex.getByName( name, project, scope)) {
      HaxeComponentName cn = field.getComponentName();
      if (cn != null) result.add(cn);
    }

    return result.toArray(NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY);
  }
}
