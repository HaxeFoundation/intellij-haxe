package com.intellij.plugins.haxe.lang.psi.indexes.unified.specialized;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.fqn.HaxeFullyQualifiedClassNameIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.specialized.HaxeImportHxFileIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedClassNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeImportHxStubIndex;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
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

public class HaxeImportHxFileUnifiedIndex {


    public static @Nullable HaxeClassModel resolveModel(@NotNull FullyQualifiedInfo qualifiedInfo, @NotNull Project project, @Nullable GlobalSearchScope scope) {
        PsiManager instance = PsiManager.getInstance(project);

        String qualifiedName = qualifiedInfo.getQualifiedName(false);
        PsiElement classOrMemberByQName = HaxeResolveUtil.findClassOrMemberByQName(qualifiedName, instance, scope);
        if (classOrMemberByQName instanceof HaxeClass haxeClass) {
            return haxeClass.getModel();
        }
        return null;
    }

    public static List<HaxeFile> getImportHxForPackage(@NotNull String name,
                                          @NotNull Project project,
                                          @Nullable GlobalSearchScope scope) {
        if (DumbService.isDumb(project)) return Collections.emptyList();

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
        Collection<HaxeFile> stubResults = HaxeImportHxStubIndex.getImportHxForPackage(name, project, searchScope);
        Collection<HaxeFile> fileResults = HaxeImportHxFileIndex.getImportHxForPackage(name, project, searchScope);

        ArrayList<HaxeFile> results = new ArrayList<>(stubResults);
        results.addAll(fileResults);

        return results;
    }


}
