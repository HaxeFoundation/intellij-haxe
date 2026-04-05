package com.intellij.plugins.haxe.lang.psi.stubs.index;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.index.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubVersions;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.psi.stubs.StringStubIndexExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;

public class HaxeClassNameStubIndex extends StringStubIndexExtension<HaxeClass> {

  public static final StubIndexKey<String, HaxeClass> KEY = StubIndexKey.createIndexKey("haxe.class.name");

  @Override
  public int getVersion() {
    return HaxeStubVersions.STUB_VERSION;
  }


  @NotNull
  @Override
  public StubIndexKey<String, HaxeClass> getKey() {
    return KEY;
  }

  @NotNull
  public static Collection<HaxeClass> getByName(@NotNull String name,
                                                 @NotNull Project project,
                                                 @Nullable GlobalSearchScope scope) {
    if (DumbService.isDumb(project)) return Collections.emptyList();
    GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
    return StubIndex.getElements(KEY, name, project, searchScope, HaxeClass.class);
  }

  @NotNull
  public static Collection<HaxeClass> getByNameFiltered(@NotNull String name,
                                                 @NotNull Project project,
                                                 @Nullable GlobalSearchScope scope) {
    if (DumbService.isDumb(project)) return Collections.emptyList();
    GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
    return StubIndex.getElements(KEY, name, project, searchScope, HaxeClass.class).stream()
            .filter(HaxeClassNameStubIndex::removeNotTargeted)
            .toList();
  }

  private static boolean removeNotTargeted(@NonNull HaxeClass haxeClass) {
    PsiFile containingFile = haxeClass.getContainingFile();
    if (HaxeIndexUtil.fileBelongToPlatformSpecificStd(containingFile)) {
      return false;
    }

    if (HaxeIndexUtil.belongToPlatformNotTargeted(containingFile)) {
      return false;
    }

    return true;
  }
}

