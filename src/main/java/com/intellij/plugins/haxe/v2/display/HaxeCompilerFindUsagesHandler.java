package com.intellij.plugins.haxe.v2.display;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.FindReferencesKind;
import com.intellij.plugins.haxe.display.protocol.Location;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.SearchScope;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Find Usages answered by the compilation server: one
 * {@code display/references} request at the searched element, whose
 * locations become the usages. No static search runs. The search scope
 * still applies; text occurrences are not searched. For a method the
 * request includes the base and the overriding declarations, matching what
 * the static handler offers in its dialog.
 */
public final class HaxeCompilerFindUsagesHandler extends FindUsagesHandler {

  /** What to ask the compiler: the file, the position of the searched name, and the name itself (null when the element has none). */
  private record Query(@NotNull VirtualFile file, int offset, @NotNull FindReferencesKind kind, @Nullable String name) {
  }

  public HaxeCompilerFindUsagesHandler(@NotNull PsiElement element) {
    super(element);
  }

  @Override
  public boolean processElementUsages(@NotNull PsiElement element,
                                      @NotNull Processor<? super UsageInfo> processor,
                                      @NotNull FindUsagesOptions options) {
    if (!options.isUsages) return true;
    // the search runs on a progress thread; every PSI touch takes the read lock
    Project project = HaxeReadActions.compute(element::getProject);
    Query query = HaxeReadActions.compute(() -> queryFor(element));
    if (query == null) return true;

    List<Location> locations = HaxeCompilerNavigationService.getInstance(project).references(query.file(), query.offset(), query.kind());
    for (Location location : locations) {
      UsageInfo usage = HaxeReadActions.compute(() -> usageFor(project, location, query.name(), options.searchScope));
      if (usage != null && !processor.process(usage)) return false;
    }
    return true;
  }

  @Nullable
  private static Query queryFor(@NotNull PsiElement element) {
    VirtualFile file = HaxeCompilerDisplayService.physicalFileOf(element);
    if (file == null) return null;
    HaxeNamedComponent named = element instanceof HaxeNamedComponent component ? component : null;
    HaxeComponentName name = named == null ? null : named.getComponentName();
    int offset = (name != null ? name : element).getTextRange().getStartOffset();
    FindReferencesKind kind = named instanceof HaxeMethod ? FindReferencesKind.WITH_BASE_AND_DESCENDANTS : FindReferencesKind.DIRECT;
    return new Query(file, offset, kind, nameOf(element, name));
  }

  @Nullable
  private static String nameOf(@NotNull PsiElement element, @Nullable HaxeComponentName name) {
    if (name != null) return name.getText();
    return element instanceof HaxeReference reference ? reference.getReferenceName() : null;
  }

  /** The usage at the location, or null when the location lies outside the search scope. The compiler never lists the declaration itself. */
  @Nullable
  private static UsageInfo usageFor(@NotNull Project project, @NotNull Location location, @Nullable String name, @NotNull SearchScope scope) {
    VirtualFile file = HaxeCompilerLocations.fileOf(location);
    if (file == null || !scope.contains(file)) return null;
    PsiFile psiFile = HaxeCompilerLocations.psiFileOf(project, location);
    return psiFile == null ? null : HaxeCompilerLocations.usageIn(psiFile, location.range(), name);
  }
}
