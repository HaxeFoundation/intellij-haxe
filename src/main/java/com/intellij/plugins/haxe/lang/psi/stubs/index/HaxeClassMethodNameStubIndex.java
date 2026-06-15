package com.intellij.plugins.haxe.lang.psi.stubs.index;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubVersions;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class HaxeClassMethodNameStubIndex extends StringStubIndexExtension<HaxeMethod> {

  public static final StubIndexKey<String, HaxeMethod> KEY = StubIndexKey.createIndexKey("haxe.class.method.name");

  @Override
  public int getVersion() {
    return HaxeStubVersions.STUB_VERSION;
  }

  @NotNull
  @Override
  public StubIndexKey<String, HaxeMethod> getKey() {
    return KEY;
  }

  @NotNull
  public static Collection<HaxeMethod> getByName(@NotNull String name,
                                                   @NotNull Project project,
                                                   @Nullable GlobalSearchScope scope) {
    if (DumbService.isDumb(project)) return Collections.emptyList();
    GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
    return StubIndex.getElements(KEY, name, project, searchScope, HaxeMethod.class);
  }
}

