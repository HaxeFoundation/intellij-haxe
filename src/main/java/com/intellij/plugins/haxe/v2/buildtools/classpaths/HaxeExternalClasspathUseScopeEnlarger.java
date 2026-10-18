package com.intellij.plugins.haxe.v2.buildtools.classpaths;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UseScopeEnlarger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Widens a declaration's use scope by the modules whose external classpaths
 * hold its file ({@link HaxeExternalClasspathStore}): no module dependency
 * links them, so the platform's own use scope would miss their usages.
 */
public final class HaxeExternalClasspathUseScopeEnlarger extends UseScopeEnlarger {

  @Override
  public @Nullable SearchScope getAdditionalUseScope(@NotNull PsiElement element) {
    PsiFile psiFile = element.getContainingFile();
    VirtualFile file = psiFile == null ? null : psiFile.getVirtualFile();
    if (file == null) return null;

    Project project = element.getProject();
    ModuleManager moduleManager = ModuleManager.getInstance(project);
    List<String> moduleNames = HaxeExternalClasspathStore.getInstance(project).modulesReaching(file);
    List<GlobalSearchScope> moduleScopes = moduleNames.stream()
      .map(moduleManager::findModuleByName)
      .filter(Objects::nonNull)
      .map(GlobalSearchScope::moduleScope)
      .toList();
    return moduleScopes.isEmpty() ? null : GlobalSearchScope.union(moduleScopes);
  }
}
