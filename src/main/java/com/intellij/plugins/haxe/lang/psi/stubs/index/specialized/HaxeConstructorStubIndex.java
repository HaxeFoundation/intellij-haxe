package com.intellij.plugins.haxe.lang.psi.stubs.index.specialized;

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
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;

/**
 * Specialized index with only constructor methods used for Completion after "new" keywords
 */
public class HaxeConstructorStubIndex extends StringStubIndexExtension<HaxeMethod> {

    // Key is FQN string of parent class
    public static final StubIndexKey<String, HaxeMethod> KEY = StubIndexKey.createIndexKey("haxe.constructors.name");

    @Override
    public int getVersion() {
        return HaxeStubVersions.STUB_VERSION;
    }

    @NotNull
    @Override
    public StubIndexKey<String, HaxeMethod> getKey() {
        return KEY;
    }

    public static @NotNull @Unmodifiable Collection<HaxeMethod> getConstructors(@NotNull Project project, @Nullable GlobalSearchScope scope) {
        if (DumbService.isDumb(project)) return Collections.emptyList();
        Collection<String> allKeys = StubIndex.getInstance().getAllKeys(KEY, project);
        return allKeys.stream()
                .flatMap( classFqn ->  StubIndex.getElements(KEY, classFqn, project, scope, HaxeMethod.class).stream())
                .toList();
    }
}
