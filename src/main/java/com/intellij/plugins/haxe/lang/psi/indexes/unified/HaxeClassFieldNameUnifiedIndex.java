package com.intellij.plugins.haxe.lang.psi.indexes.unified;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.HaxeClassFieldNameFileIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeClassFieldNameStubIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeClassFieldNameUnifiedIndex {


    public static Collection<String> getAllKeys(@NotNull Project project) {

        Collection<String> stubKeys = StubIndex.getInstance().getAllKeys(HaxeClassFieldNameStubIndex.KEY, project);
        Collection<String> fileKeys = FileBasedIndex.getInstance().getAllKeys(HaxeClassFieldNameFileIndex.INDEX, project);

        Set<String> result = new HashSet<>(stubKeys);
        result.addAll(stubKeys);
        result.addAll(fileKeys);

        return result;
    }


    public static void processElements(@NotNull String name,
                                       @NotNull Project project,
                                       @Nullable GlobalSearchScope scope,
                                       @NotNull Processor<HaxePsiField> processor
    ) {
        Collection<HaxePsiField> byName = getByName(name, project, scope);
        for (HaxePsiField field : byName) {
            ProgressManager.checkCanceled();
            if (!processor.process(field)) {
                return;
            }
        }

    }

    public static Collection<HaxePsiField> getByName(@NotNull String name,
                                                   @NotNull Project project,
                                                   @Nullable GlobalSearchScope scope) {

        if (DumbService.isDumb(project)) return Collections.emptyList();

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
        Collection<HaxePsiField> stubResults = HaxeClassFieldNameStubIndex.getByName(name, project, searchScope);
        Collection<HaxePsiField> fileResults = HaxeClassFieldNameFileIndex.getByName(name, project, searchScope);

        ArrayList<HaxePsiField> result = new ArrayList<>(stubResults);
        result.addAll(fileResults);

        return result;
    }


}
