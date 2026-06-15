package com.intellij.plugins.haxe.lang.psi.indexes.unified;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.HaxeClassMethodNameFileIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeClassMethodNameStubIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeClassMethodNameUnifiedIndex {


    public static Collection<String> getAllKeys(@NotNull Project project) {

        Collection<String> stubKeys = StubIndex.getInstance().getAllKeys(HaxeClassMethodNameStubIndex.KEY, project);
        Collection<String> fileKeys = FileBasedIndex.getInstance().getAllKeys(HaxeClassMethodNameFileIndex.INDEX, project);

        Set<String> result = new HashSet<>(stubKeys);
        result.addAll(stubKeys);
        result.addAll(fileKeys);

        return result;
    }


    public static void processElements(@NotNull String name,
                                       @NotNull Project project,
                                       @Nullable GlobalSearchScope scope,
                                       @NotNull Processor<HaxeMethod> processor
    ) {
        Collection<HaxeMethod> byName = getByName(name, project, scope);
        for (HaxeMethod method : byName) {
            ProgressManager.checkCanceled();
            if (!processor.process(method)) {
                return;
            }
        }

    }

    public static Collection<HaxeMethod> getByName(@NotNull String name,
                                                   @NotNull Project project,
                                                   @Nullable GlobalSearchScope scope) {

        if (DumbService.isDumb(project)) return Collections.emptyList();

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
        Collection<HaxeMethod> stubResults = HaxeClassMethodNameStubIndex.getByName(name, project, searchScope);
        Collection<HaxeMethod> fileResults = HaxeClassMethodNameFileIndex.getByName(name, project, searchScope);

        ArrayList<HaxeMethod> result = new ArrayList<>(stubResults);
        result.addAll(fileResults);

        return result;
    }

}
