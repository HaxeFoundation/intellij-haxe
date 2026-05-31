package com.intellij.plugins.haxe.lang.psi.indexes.unified.specialized;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.specialized.HaxeClassInheritanceFileIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeClassInheritanceStubIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class HaxeClassInheritanceUnifiedIndex {



    public static List<HaxeClass> getBySuper(@NotNull String name,
                                          @NotNull Project project,
                                          @Nullable GlobalSearchScope scope) {
        if (DumbService.isDumb(project)) return Collections.emptyList();

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
        Collection<HaxeClass> stubResults = HaxeClassInheritanceStubIndex.getBySuper(name, project, searchScope);
        Collection<HaxeClass> fileResults = HaxeClassInheritanceFileIndex.getBySuper(name, project, searchScope);

        ArrayList<HaxeClass> results = new ArrayList<>(stubResults);
        results.addAll(fileResults);

        return results;
    }


}
