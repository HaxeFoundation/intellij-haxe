package com.intellij.plugins.haxe.lang.psi.stubs.index.specialized;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubVersions;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Specialized used to find inheritance faster. (see HaxeInheritanceDefinitionsSearcher)
 */
public class HaxeTypedefInheritanceStubIndex extends StringStubIndexExtension<HaxeClass> {

  public static final StubIndexKey<String, HaxeClass> KEY = StubIndexKey.createIndexKey("haxe.typedef.super");

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
  public static Collection<HaxeClass> getBySuper(@NotNull String superName,
                                                  @NotNull Project project,
                                                  @Nullable GlobalSearchScope scope) {
    if (DumbService.isDumb(project)) return Collections.emptyList();
    GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
    return StubIndex.getElements(KEY, superName, project, searchScope, HaxeClass.class);
  }
}

