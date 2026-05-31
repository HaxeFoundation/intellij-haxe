package com.intellij.plugins.haxe.lang.psi.indexes.unified;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.lookup.indexed.data.HaxeMemberLookupData;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.HaxeModuleMethodNameFileIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeModuleMethodNameStubIndex;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.HaxeModelTarget;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeModuleMethodNameUnifiedIndex {


    public static Collection<String> getAllKeys(@NotNull Project project) {

        Collection<String> stubKeys = StubIndex.getInstance().getAllKeys(HaxeModuleMethodNameStubIndex.KEY, project);
        Collection<String> fileKeys = FileBasedIndex.getInstance().getAllKeys(HaxeModuleMethodNameFileIndex.INDEX, project);

        Set<String> result = new HashSet<>(stubKeys);
        result.addAll(stubKeys);
        result.addAll(fileKeys);

        return result;
    }


    public static Collection<HaxeMethod> getByName(@NotNull String name,
                                                   @NotNull Project project,
                                                   @Nullable GlobalSearchScope scope) {

        if (DumbService.isDumb(project)) return Collections.emptyList();

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
        Collection<HaxeMethod> stubResults = HaxeModuleMethodNameStubIndex.getByName(name, project, searchScope);
        Collection<HaxeMethod> fileResults = HaxeModuleMethodNameFileIndex.getByName(name, project, searchScope);

        ArrayList<HaxeMethod> result = new ArrayList<>(stubResults);
        result.addAll(fileResults);

        return result;
    }



    public static List<HaxeMemberLookupData> getCompletionData(@NotNull String name,
                                                               @NotNull Project project,
                                                               @Nullable GlobalSearchScope scope
    ) {

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);

        Collection<HaxeMethod> stubResults = HaxeModuleMethodNameStubIndex.getByName(name, project, scope);
        Collection<HaxeComponentIndexData> values = HaxeModuleMethodNameFileIndex.getValues(name, project, scope);

        List<HaxeMemberLookupData> listA = stubResults.stream()
                .filter(LookupUtil::isActiveTarget)
                .filter(m -> m.isPublic())
                .map(HaxeMethod::getModel)
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
