package com.intellij.plugins.haxe.lang.psi.indexes.unified;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.lookup.indexed.data.HaxeMemberLookupData;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.HaxeConstructorFileIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeConstructorStubIndex;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class HaxeConstrcutorUnifiedIndex {


    public static List<HaxeMethod> getConstructors(@NotNull Project project, @Nullable GlobalSearchScope scope) {
        if (DumbService.isDumb(project)) return Collections.emptyList();

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
        Collection<HaxeMethod> stubResults = HaxeConstructorStubIndex.getConstructors(project, searchScope);
        Collection<HaxeMethod> fileResults = HaxeConstructorFileIndex.getConstructors(project, searchScope);

        ArrayList<HaxeMethod> results = new ArrayList<>(stubResults);
        results.addAll(fileResults);

        return results;
    }


    public static List<HaxeMemberLookupData> getCompletionData(Project project, GlobalSearchScope scope) {

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);

        Collection<HaxeMethod> stubResults = HaxeConstructorStubIndex.getConstructors(project, searchScope);
        Collection<HaxeComponentIndexData> values = HaxeConstructorFileIndex.getAllValues(project, searchScope);

        List<HaxeMemberLookupData> listA = stubResults.stream()
                .filter(LookupUtil::isActiveTarget)
                .filter(m -> m.isPublic())
                .map(HaxeMethod::getModel)
                .map(HaxeMemberLookupData::new)
                .toList();

        List<HaxeMemberLookupData> listB = values.stream()
                .filter(m ->  m.isPublic())
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
        HaxeClass haxeClass = HaxeResolveUtil.findClassByQName(qualifiedName, instance, scope);
        if (haxeClass != null) {
            HaxeClassModel model = haxeClass.getModel();
            HaxeMethodModel constructor = model.getConstructor(null);
            if (constructor != null) {
                return constructor;
            }
        }

        return null;
    }

    public static HaxeMethod getConstructor(FullyQualifiedInfo qualifiedInfo, Project project, @Nullable GlobalSearchScope scope) {

        HaxeMethod stubResult = HaxeConstructorStubIndex.getConstructor(qualifiedInfo, project, scope);
        if(stubResult != null) return stubResult;

        HaxeMemberModel model = resolveModel(qualifiedInfo, project, scope);
        if(model instanceof HaxeMethodModel methodModel) {
            return methodModel.getMethod();
        }

        return null;
    }
}
