package com.intellij.plugins.haxe.ide.index;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class HaxeInheritanceDefinitionsUtil {
  public static Collection<HaxeClass> getItemsByQNameFirstLevelChildrenOnly(final HaxeClass haxeClass) {
    Project project = haxeClass.getProject();
    if (project.isDisposed()) {
      throw new ProcessCanceledException(new Throwable("Project disposed"));
    }

    DumbService dumbService = DumbService.getInstance(project);
    if (dumbService.isDumb()) {
      dumbService.waitForSmartMode();
    }
    return ReadAction.nonBlocking(() -> _getItemsByQNameFirstLevelChildrenOnly(haxeClass, project))
            .inSmartMode(project)
            .executeSynchronously();
  }


  public static List<HaxeClass> getItemsByQNameIncludingSubChildren(final HaxeClass haxeClass) {
    Project project = haxeClass.getProject();
    if (project.isDisposed()) {
      throw new ProcessCanceledException(new Throwable("Project disposed"));
    }

    DumbService dumbService = DumbService.getInstance(project);
    if (dumbService.isDumb()) {
      dumbService.waitForSmartMode();
    }

    return ReadAction.nonBlocking(() -> _getItemsByQNameIncludingSubChildren(haxeClass))
            .inSmartMode(project)
            .executeSynchronously();
  }

  private static @NotNull Collection<HaxeClass> _getItemsByQNameFirstLevelChildrenOnly(HaxeClass haxeClass, Project project) {
    GlobalSearchScope scope = GlobalSearchScope.projectScope(project);

    return DefinitionsScopedSearch.search(haxeClass, scope, false)
      .allowParallelProcessing()
      .filtering(element -> element instanceof HaxeClass)
      .mapping(element -> (HaxeClass)element)
      .filtering(aClass -> !aClass.isObjectLiteralType())// ignore object literals as they do not inherit
      .filtering(element ->
                   Arrays.stream(element.getSuperTypes()).map(PsiClassType::resolve).anyMatch(psiClass -> psiClass == haxeClass)
      ).findAll();
  }

  private static @NotNull List<HaxeClass> _getItemsByQNameIncludingSubChildren(HaxeClass haxeClass) {
    final List<HaxeClass> result = new ArrayList<HaxeClass>();
    DefinitionsScopedSearch.search(haxeClass).allowParallelProcessing().forEach(element -> {
      if (element instanceof HaxeClass) {
        result.add((HaxeClass)element);
      }
      return true;
    });
    return result;
  }
}
