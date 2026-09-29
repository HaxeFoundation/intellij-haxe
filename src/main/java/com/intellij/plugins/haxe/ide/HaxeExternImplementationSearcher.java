package com.intellij.plugins.haxe.ide;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeProjectModel;
import com.intellij.plugins.haxe.model.HaxeSourceRootModel;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService;
import com.intellij.plugins.haxe.v2.display.HaxeGeneratedPreviewTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Feeds Go to Implementation for extern classes. An extern is the
 * declaration; the per-target {@code _std} override in the same std library
 * is the implementation the compiler swaps in (it prepends
 * {@code std/<target>/_std} to the classpath), so each target's override is
 * offered as an implementation of the extern class or member. When compiler
 * completion mode is on, the compiler's generated preview of the type is
 * offered as an additional target — it covers externs with no {@code _std}
 * source and shows the post-macro truth.
 */
public class HaxeExternImplementationSearcher extends QueryExecutorBase<PsiElement, DefinitionsScopedSearch.SearchParameters> {

  private static final String STD_OVERRIDE_DIR = "_std";

  public HaxeExternImplementationSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull DefinitionsScopedSearch.SearchParameters queryParameters,
                           @NotNull Processor<? super PsiElement> consumer) {
    HaxeNamedComponent queried = queriedComponent(queryParameters.getElement());
    if (queried == null) return;

    HaxeClass externClass;
    String memberName;
    if (queried instanceof HaxeClass haxeClass) {
      externClass = haxeClass;
      memberName = null;
    }
    else {
      externClass = PsiTreeUtil.getStubOrPsiParentOfType(queried, HaxeClass.class);
      memberName = queried.getName();
    }
    if (externClass == null || !externClass.isExtern()) return;

    if (!addStdImplementations(externClass, memberName, consumer)) return;
    addGeneratedPreview(externClass, memberName, consumer);
  }

  /** The search element arrives as the class, the member's whole declaration, or its component name. */
  @Nullable
  private static HaxeNamedComponent queriedComponent(@NotNull PsiElement element) {
    if (element instanceof HaxeNamedComponent namedComponent) return namedComponent;
    if (element instanceof HaxeComponentName componentName
        && componentName.getParent() instanceof HaxeNamedComponent namedComponent) {
      return namedComponent;
    }
    return null;
  }

  /** Offers each target's {@code _std} override; false when the consumer stopped the search. */
  private static boolean addStdImplementations(@NotNull HaxeClass externClass,
                                               @Nullable String memberName,
                                               @NotNull Processor<? super PsiElement> consumer) {
    HaxeSourceRootModel sdkRoot = HaxeProjectModel.fromElement(externClass).getSdkRoot();
    if (sdkRoot.root == null) return true;
    PsiFile externPsiFile = externClass.getContainingFile();
    VirtualFile externFile = externPsiFile != null ? externPsiFile.getVirtualFile() : null;
    if (externFile == null) return true;
    // the _std mechanism is std-only: a third-party extern never matches
    String modulePath = VfsUtilCore.getRelativePath(externFile, sdkRoot.root);
    if (modulePath == null) return true;

    String externQName = externClass.getQualifiedName();
    PsiManager psiManager = externPsiFile.getManager();
    for (VirtualFile targetDir : sdkRoot.root.getChildren()) {
      VirtualFile overridesDir = targetDir.isDirectory() ? targetDir.findChild(STD_OVERRIDE_DIR) : null;
      if (overridesDir == null) continue;
      VirtualFile candidate = overridesDir.findFileByRelativePath(modulePath);
      if (candidate == null) continue;
      PsiElement implementation = implementationIn(psiManager, candidate, externQName, memberName);
      if (implementation != null && !consumer.process(implementation)) return false;
    }
    return true;
  }

  /**
   * The implementation the candidate override file declares, or null. A
   * matching path is not a matching type — externs for other integrations
   * can reuse a name — so the candidate must declare the extern's exact
   * qualified name (packages come from the package statement, so a
   * same-named type of another package never matches).
   */
  @Nullable
  private static PsiElement implementationIn(@NotNull PsiManager psiManager,
                                             @NotNull VirtualFile candidate,
                                             @Nullable String externQName,
                                             @Nullable String memberName) {
    if (!(psiManager.findFile(candidate) instanceof HaxeFile haxeFile)) return null;
    for (HaxeClass haxeClass : haxeFile.getClassList()) {
      if (!Objects.equals(haxeClass.getQualifiedName(), externQName)) continue;
      if (memberName == null) return haxeClass;
      List<HaxeNamedComponent> members = haxeClass.findHaxeMemberByName(memberName, null);
      // a member the override lacks yields nothing - a class entry in a member
      // search reads as an implementation but navigates to the class header
      return members.isEmpty() ? null : members.getFirst();
    }
    return null;
  }

  private static void addGeneratedPreview(@NotNull HaxeClass externClass,
                                          @Nullable String memberName,
                                          @NotNull Processor<? super PsiElement> consumer) {
    Project project = externClass.getProject();
    if (!HaxeCompilerSettings.getInstance(project).getCompletionMode().usesCompiler()) return;
    String dotPath = externClass.getQualifiedName();
    if (dotPath == null || dotPath.isEmpty()) return;

    // the queried extern belongs to the SDK, not a module, so any module's
    // build context serves as the dump context
    // TODO: one preview entry per container, labeled by its target, instead of the first context found
    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    for (Module module : ModuleManager.getInstance(project).getSortedModules()) {
      HaxeCompilerDisplayService.DisplayContext context = displayService.contextFor(module);
      if (context != null) {
        consumer.process(HaxeGeneratedPreviewTarget.createElement(externClass, context, dotPath, memberName));
        return;
      }
    }
  }
}
