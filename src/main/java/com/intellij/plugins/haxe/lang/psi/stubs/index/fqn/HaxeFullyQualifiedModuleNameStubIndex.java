package com.intellij.plugins.haxe.lang.psi.stubs.index.fqn;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubVersions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class HaxeFullyQualifiedModuleNameStubIndex extends StringStubIndexExtension<HaxeModule> {

  public static final StubIndexKey<String, HaxeModule> KEY = StubIndexKey.createIndexKey("haxe.module.fqn");

  @Override
  public int getVersion() {
    return HaxeStubVersions.STUB_VERSION;
  }

  @NotNull
  @Override
  public StubIndexKey<String, HaxeModule> getKey() {
    return KEY;
  }


  @NotNull
  public static Collection<HaxeModule> getByFqn(@NotNull String fqn,
                                                @NotNull Project project,
                                                @Nullable GlobalSearchScope scope) {
    if (DumbService.isDumb(project)) return Collections.emptyList();
    GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
    return StubIndex.getElements(KEY, fqn, project, searchScope, HaxeModule.class);
  }
}

