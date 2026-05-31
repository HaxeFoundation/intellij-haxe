package com.intellij.plugins.haxe.lang.psi.indexes.unified;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.haxelib.definitions.HaxeDefineDetectionManager;
import com.intellij.plugins.haxe.ide.lookup.indexed.data.HaxeMemberLookupData;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.HaxeStaticFieldNameFileIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeStaticFieldNameStubIndex;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.HaxeModelTarget;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeStaticFieldNameUnifiedIndex {


    public static Collection<String> getAllKeys(@NotNull Project project) {

        Collection<String> stubKeys = StubIndex.getInstance().getAllKeys(HaxeStaticFieldNameStubIndex.KEY, project);
        Collection<String> fileKeys = FileBasedIndex.getInstance().getAllKeys(HaxeStaticFieldNameFileIndex.INDEX, project);

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
        Collection<HaxePsiField> stubResults = HaxeStaticFieldNameStubIndex.getByName(name, project, searchScope);
        Collection<HaxePsiField> fileResults = HaxeStaticFieldNameFileIndex.getByName(name, project, searchScope);

        ArrayList<HaxePsiField> result = new ArrayList<>(stubResults);
        result.addAll(fileResults);

        return result;
    }



    public static List<HaxeMemberLookupData> getCompletionData(@NotNull String name,
                                                               @NotNull Project project,
                                                               @Nullable GlobalSearchScope scope
    ) {

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);

        Collection<HaxePsiField> stubResults = HaxeStaticFieldNameStubIndex.getByName(name, project, scope);
        Collection<HaxeComponentIndexData> values = HaxeStaticFieldNameFileIndex.getValues(name, project, scope);

        List<HaxeMemberLookupData> listA = stubResults.stream()
                .filter(LookupUtil::isActiveTarget)
                .filter(m ->  m.isPublic())
                .map(HaxePsiField::getModel)
                .map(HaxeMemberLookupData::new)
                .toList();

        List<HaxeMemberLookupData> listB = values.stream()
                .filter(m -> m.isPublic())
                .filter(indexData ->  LookupUtil.isActiveTarget(indexData, project))
                .map( indexData -> new  HaxeMemberLookupData(indexData, ()->{
                    return resolveModel(indexData.getFqn(), project, scope);
                }))
                .toList();

        ArrayList<HaxeMemberLookupData> result = new ArrayList<>(listA);
        result.addAll(listB);

        return result;
    }



    public static @Nullable HaxeMemberModel resolveModel(@NotNull FullyQualifiedInfo qualifiedInfo, @NotNull Project project, @Nullable GlobalSearchScope scope) {
        PsiManager instance = PsiManager.getInstance(project);

        String qualifiedName = qualifiedInfo.getQualifiedName(false);
        PsiElement classOrMemberByQName = HaxeResolveUtil.findClassOrMemberByQName(qualifiedName, instance, scope);
        if (classOrMemberByQName instanceof HaxeModelTarget modelTarget) {
            if(modelTarget.getModel() instanceof HaxeMemberModel model) {
                return model;
            }
        }
        return null;
    }
}
