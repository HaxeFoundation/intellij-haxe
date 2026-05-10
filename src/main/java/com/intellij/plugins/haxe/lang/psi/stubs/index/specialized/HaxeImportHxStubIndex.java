package com.intellij.plugins.haxe.lang.psi.stubs.index.specialized;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
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
 * Specialized index for import.hx files, key is package, and value is file
 */
public class HaxeImportHxStubIndex extends StringStubIndexExtension<HaxeFile> {

    public static final StubIndexKey<String, HaxeFile> KEY = StubIndexKey.createIndexKey("haxe.import_hx.name");

    @Override
    public int getVersion() {
        return HaxeStubVersions.STUB_VERSION;
    }

    @NotNull
    @Override
    public StubIndexKey<String, HaxeFile> getKey() {
        return KEY;
    }

    public static @NotNull @Unmodifiable Collection<HaxeFile> getImportHxForPackage(@NotNull String packageName,
                                                                                    @NotNull Project project,
                                                                                    @Nullable GlobalSearchScope scope) {
        if (DumbService.isDumb(project)) return Collections.emptyList();
        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
        return StubIndex.getElements(HaxeImportHxStubIndex.KEY, packageName, project, scope, HaxeFile.class);
    }
}
