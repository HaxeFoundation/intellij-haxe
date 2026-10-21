package com.intellij.plugins.haxe.lang.psi.indexes.unified;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.lookup.indexed.data.HaxeClassLookupData;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.HaxeClassNameFileIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeClassNameStubIndex;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.lang.psi.indexes.compiler.HaxeClassNameCompilerIndex;
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

public class HaxeClassNameUnifiedIndex {


    public static Collection<String> getAllKeys(@NotNull Project project) {

        Collection<String> stubKeys = StubIndex.getInstance().getAllKeys(HaxeClassNameStubIndex.KEY, project);
        Collection<String> fileKeys = FileBasedIndex.getInstance().getAllKeys(HaxeClassNameFileIndex.INDEX, project);

        Set<String> result = new HashSet<>(stubKeys);
        result.addAll(fileKeys);
        result.addAll(HaxeClassNameCompilerIndex.getAllKeys(project));

        return result;
    }


    public static List<HaxeClassLookupData> getCompletionData(@NotNull String name,
                                                              @NotNull Project project,
                                                              @Nullable GlobalSearchScope scope
    ) {

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
        Collection<HaxeClass> stubResults = HaxeClassNameStubIndex.getByNameFiltered(name, project, scope);
        Collection<HaxeComponentIndexData> values = HaxeClassNameFileIndex.getValues(name, project, scope);

        // the stub filter only covers sdk-root files; library-shipped target
        // externs need the package check on both paths
        List<HaxeClassLookupData> stubList = stubResults.stream()
                .map(HaxeClass::getModel)
                .filter(model -> LookupUtil.isActiveTargetPackage(model.getQualifiedInfo().getPackageName(), project))
                .map(HaxeClassLookupData::new)
                .toList();

        List<HaxeClassLookupData> fileList = values.stream()
                .filter(indexData -> LookupUtil.isActiveTargetPackage(indexData.getFqn().getPackageName(), project))
                .map( indexData -> new  HaxeClassLookupData(indexData, ()->{
                    return resolveModel(indexData.getFqn(), project, scope);
                }))
                .toList();

        List<HaxeClassLookupData> compilerList = HaxeClassNameCompilerIndex.getCompletionData(name, project, scope);

        ArrayList<HaxeClassLookupData> result = new ArrayList<>(stubList);
        result.addAll(fileList);
        result.addAll(compilerList);

        return result;
    }


    public static @Nullable HaxeClassModel resolveModel(@NotNull FullyQualifiedInfo qualifiedInfo, @NotNull Project project, @Nullable GlobalSearchScope scope) {
        PsiManager instance = PsiManager.getInstance(project);

        String qualifiedName = qualifiedInfo.getQualifiedName(true);
        PsiElement classOrMemberByQName = HaxeResolveUtil.findClassOrMemberByQName(qualifiedName, instance, scope);
        if (classOrMemberByQName instanceof HaxeClass haxeClass) {
            return haxeClass.getModel();
        }
        return null;
    }
    public static void processElements(@NotNull String name,
                                       @NotNull Project project,
                                       @Nullable GlobalSearchScope scope,
                                       @NotNull Processor<HaxeClass> processor
    ) {
        Collection<HaxeClass> byNameFiltered = getByNameFiltered(name, project, scope);
        for (HaxeClass haxeClass : byNameFiltered) {
            ProgressManager.checkCanceled();
            if (!processor.process(haxeClass)) {
                return;
            }
        }
    }

    public static Collection<HaxeClass> getByNameFiltered(@NotNull String name,
                                                          @NotNull Project project,
                                                          @Nullable GlobalSearchScope scope) {
        if (DumbService.isDumb(project)) return Collections.emptyList();

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);

        Collection<HaxeClass> stubResults = HaxeClassNameStubIndex.getByNameFiltered(name, project, searchScope);
        Collection<HaxeClass> fileResults = HaxeClassNameFileIndex.getByNameFiltered(name, project, searchScope);
        Collection<HaxeClass> compilerResults = HaxeClassNameCompilerIndex.getByNameFiltered(name, project, searchScope);

        ArrayList<HaxeClass> haxeClasses = new ArrayList<>(stubResults);
        haxeClasses.addAll(fileResults);
        haxeClasses.addAll(compilerResults);

        // the stub-side filter only covers sdk-root files, not library-shipped target externs
        return haxeClasses.stream()
          .filter(haxeClass -> LookupUtil.isActiveTargetPackage(packageNameOf(haxeClass), project))
          .toList();
    }

    @Nullable
    private static String packageNameOf(@NotNull HaxeClass haxeClass) {
        HaxeClassModel model = haxeClass.getModel();
        return model != null ? model.getQualifiedInfo().getPackageName() : null;
    }


}
