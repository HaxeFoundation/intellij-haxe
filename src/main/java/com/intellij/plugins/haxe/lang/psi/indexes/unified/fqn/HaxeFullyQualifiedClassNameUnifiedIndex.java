package com.intellij.plugins.haxe.lang.psi.indexes.unified.fqn;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.indexes.compiler.HaxeFullyQualifiedClassNameCompilerIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.fqn.HaxeFullyQualifiedClassNameIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedClassNameStubIndex;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerTypeCatalogService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeFullyQualifiedClassNameUnifiedIndex {


    public static @Nullable HaxeClassModel resolveModel(@NotNull FullyQualifiedInfo qualifiedInfo, @NotNull Project project, @Nullable GlobalSearchScope scope) {
        PsiManager instance = PsiManager.getInstance(project);

        String qualifiedName = qualifiedInfo.getQualifiedName(false);
        PsiElement classOrMemberByQName = HaxeResolveUtil.findClassOrMemberByQName(qualifiedName, instance, scope);
        if (classOrMemberByQName instanceof HaxeClass haxeClass) {
            return haxeClass.getModel();
        }
        return null;
    }

    /** Every known class FQN across the three legs (stub, file-based, compiler catalog). */
    public static Collection<String> getAllKeys(@NotNull Project project) {
        if (DumbService.isDumb(project)) return Collections.emptyList();
        Set<String> keys = new HashSet<>(StubIndex.getInstance().getAllKeys(HaxeFullyQualifiedClassNameStubIndex.KEY, project));
        keys.addAll(HaxeFullyQualifiedClassNameIndex.getAllKeys(project));
        keys.addAll(HaxeCompilerTypeCatalogService.getInstance(project).allFqns());
        return keys;
    }

    public static List<HaxeClass> getByFqn(@NotNull String name,
                                                          @NotNull Project project,
                                                          @Nullable GlobalSearchScope scope) {
        if (DumbService.isDumb(project)) return Collections.emptyList();

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);

        Collection<HaxeClass> stubResults = HaxeFullyQualifiedClassNameStubIndex.getByFqn(name, project, searchScope);
        Collection<HaxeClass> fileResults = HaxeFullyQualifiedClassNameIndex.getByFqn(name, project, searchScope);
        Collection<HaxeClass> compilerResults = HaxeFullyQualifiedClassNameCompilerIndex.getByFqn(name, project, searchScope);

        ArrayList<HaxeClass> results = new ArrayList<>(stubResults);
        results.addAll(fileResults);
        results.addAll(compilerResults);

        return results;
    }


}
