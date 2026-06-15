package com.intellij.plugins.haxe.lang.psi.indexes.unified.fqn;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.fqn.HaxeFullyQualifiedClassNameIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.fqn.HaxeFullyQualifiedMemberNameIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedClassNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedMemberNameStubIndex;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
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

public class HaxeFullyQualifiedMemberNameUnifiedIndex {


    public static List<PsiElement> getByFqn(@NotNull String name,
                                                          @NotNull Project project,
                                                          @Nullable GlobalSearchScope scope) {
        if (DumbService.isDumb(project)) return Collections.emptyList();

        GlobalSearchScope searchScope = scope != null ? scope : GlobalSearchScope.allScope(project);
        Collection<PsiElement> stubResults = HaxeFullyQualifiedMemberNameStubIndex.getByFqn(name, project, searchScope);
        Collection<PsiElement> fileResults = HaxeFullyQualifiedMemberNameIndex.getByFqn(name, project, searchScope);

        ArrayList<PsiElement> results = new ArrayList<>(stubResults);
        results.addAll(fileResults);

        return results;
    }


}
