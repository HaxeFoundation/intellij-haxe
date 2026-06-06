package com.intellij.plugins.haxe.lang.psi.stubs.index.fqn;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubVersions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.psi.stubs.StringStubIndexExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class HaxeFullyQualifiedClassNameStubIndex extends StringStubIndexExtension<HaxeClass> {

  public static final StubIndexKey<String, HaxeClass> KEY = StubIndexKey.createIndexKey("haxe.class.fqn");

  @Override
  public int getVersion() {
    return HaxeStubVersions.STUB_VERSION;
  }

  @NotNull
  @Override
  public StubIndexKey<String, HaxeClass> getKey() {
    return KEY;
  }

  /**
   * Returns all {@link HaxeClass} elements whose fully qualified name matches {@code fqn}.
   * Returns an empty collection if the index has not been built yet or if called during dumb mode
   * (e.g., from within a file-based index builder).
   */
  @NotNull
  public static Collection<HaxeClass> getByFqn(@NotNull String fqn,
                                                @NotNull Project project,
                                                @Nullable GlobalSearchScope scope) {
    if (DumbService.isDumb(project)) return Collections.emptyList();
    GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
    return StubIndex.getElements(KEY, fqn, project, searchScope, HaxeClass.class);
  }
}

