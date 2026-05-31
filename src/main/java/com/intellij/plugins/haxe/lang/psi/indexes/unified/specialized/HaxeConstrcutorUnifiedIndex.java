package com.intellij.plugins.haxe.lang.psi.indexes.unified.specialized;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.specialized.HaxeConstructorFileIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeConstructorStubIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class HaxeConstrcutorUnifiedIndex {



    //TODO use HaxeComponentIndexData for  faster constructor suggestion completion

    public static List<HaxeMethod> getConstructors(@NotNull Project project, @Nullable GlobalSearchScope scope) {
        if (DumbService.isDumb(project)) return Collections.emptyList();

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
        Collection<HaxeMethod> stubResults = HaxeConstructorStubIndex.getConstructors(project, searchScope);
        Collection<HaxeMethod> fileResults = HaxeConstructorFileIndex.getConstructors(project, searchScope);

        ArrayList<HaxeMethod> results = new ArrayList<>(stubResults);
        results.addAll(fileResults);

        return results;
    }


}
