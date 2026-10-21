package com.intellij.plugins.haxe.lang.psi.indexes.compiler;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerTypeCatalogService;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The compiler leg of the unified FQN class lookup: macro-generated types the
 * source indexes cannot know, served from the type catalog and materialized
 * as blueprint-rendered classes. Cache-only — a cold blueprint schedules
 * hydration and contributes nothing this pass; the daemon restart after
 * hydration re-resolves. The scope parameter exists for signature parity with
 * the stub/file legs but cannot apply: catalog entries have no backing file
 * and are already scoped to the project's build contexts.
 */
public class HaxeFullyQualifiedClassNameCompilerIndex {

    public static List<HaxeClass> getByFqn(@NotNull String fqn,
                                           @NotNull Project project,
                                           @SuppressWarnings("unused") @Nullable GlobalSearchScope scope) {
        HaxeCompilerTypeCatalogService catalog = HaxeCompilerTypeCatalogService.getInstance(project);
        List<HaxeClass> result = new ArrayList<>();
        for (HaxeCompilerTypeCatalogService.GeneratedType entry : catalog.byFqn(fqn)) {
            HaxeClassModel model = catalog.renderedClass(entry);
            if (model != null) {
                result.add(model.haxeClass);
            }
        }
        return result;
    }
}
