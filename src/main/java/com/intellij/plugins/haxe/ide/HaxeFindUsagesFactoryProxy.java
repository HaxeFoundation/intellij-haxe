package com.intellij.plugins.haxe.ide;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerFindUsagesHandler;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerNavigationService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * The registered Find Usages factory: it hands out the compiler-backed
 * handler while "Use compiler for IDE features" is on, and the static
 * handler otherwise. Identifier highlighting asks for a handler on every
 * caret move, so it always gets the static one. A file the compiler cannot
 * serve gets no handler at all, and the notification explains why.
 */
public final class HaxeFindUsagesFactoryProxy extends FindUsagesHandlerFactory {

  private final Project project;
  private final HaxeFindUsagesHandlerFactory staticFactory;

  public HaxeFindUsagesFactoryProxy(@NotNull Project project) {
    this.project = project;
    this.staticFactory = new HaxeFindUsagesHandlerFactory(project);
  }

  @Override
  public boolean canFindUsages(@NotNull PsiElement element) {
    return staticFactory.canFindUsages(element);
  }

  @Override
  public FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element, boolean forHighlightUsages) {
    boolean useCompiler = !forHighlightUsages && HaxeCompilerSettings.getInstance(project).isCompilerIdeFeaturesEnabled();
    if (!useCompiler) return staticFactory.createFindUsagesHandler(element, forHighlightUsages);
    if (!canFindUsages(element)) return FindUsagesHandler.NULL_HANDLER;

    PsiFile file = element.getContainingFile();
    VirtualFile virtualFile = file == null ? null : file.getOriginalFile().getVirtualFile();
    boolean available = virtualFile != null && HaxeCompilerNavigationService.getInstance(project).ensureAvailable(virtualFile);
    if (!available) return FindUsagesHandler.NULL_HANDLER;
    PsiElement target = HaxeFindUsagesUtil.getTargetElement(element);
    return new HaxeCompilerFindUsagesHandler(target != null ? target : element);
  }
}
