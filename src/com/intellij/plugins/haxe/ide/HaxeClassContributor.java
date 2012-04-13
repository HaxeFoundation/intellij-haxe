package com.intellij.plugins.haxe.ide;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.index.HaxeComponentIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeClassContributor implements ChooseByNameContributor {
  @NotNull
  @Override
  public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
    final GlobalSearchScope scope = includeNonProjectItems ? GlobalSearchScope.allScope(project) : GlobalSearchScope.projectScope(project);
    final Collection<NavigationItem> result = HaxeComponentIndex.getItemsByName(name, project, scope);
    return result.toArray(new NavigationItem[result.size()]);
  }

  @NotNull
  @Override
  public String[] getNames(Project project, boolean includeNonProjectItems) {
    final Collection<String> result = HaxeComponentIndex.getNames(project);
    return ArrayUtil.toStringArray(result);
  }
}
