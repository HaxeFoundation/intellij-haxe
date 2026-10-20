package com.intellij.plugins.haxe.lang.psi.indexes.compiler;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.lookup.indexed.data.HaxeClassLookupData;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.LookupUtil;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerTypeCatalogService;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The compiler leg of the unified class-name lookup: macro-generated types
 * the source indexes cannot know, served from the type catalog. Completion
 * entries carry only a name and FQN up front — the model materializes lazily
 * from the type's blueprint; PSI results ({@link #getByNameFiltered}) only
 * include entries whose blueprint already hydrated, and a cold entry
 * schedules its hydration. The scope parameter exists for signature parity
 * with the stub/file legs but cannot apply: catalog entries have no backing
 * file and are already scoped to the project's build contexts.
 */
public class HaxeClassNameCompilerIndex {

    public static Collection<String> getAllKeys(@NotNull Project project) {
        return HaxeCompilerTypeCatalogService.getInstance(project).allNames();
    }

    public static List<HaxeClassLookupData> getCompletionData(@NotNull String name,
                                                              @NotNull Project project,
                                                              @SuppressWarnings("unused") @Nullable GlobalSearchScope scope) {
        HaxeCompilerTypeCatalogService catalog = HaxeCompilerTypeCatalogService.getInstance(project);
        List<HaxeClassLookupData> result = new ArrayList<>();
        for (HaxeCompilerTypeCatalogService.GeneratedType entry : catalog.byName(name)) {
            FullyQualifiedInfo qualifiedInfo = new FullyQualifiedInfo(entry.fqn());
            if (!LookupUtil.isActiveTargetPackage(qualifiedInfo.getPackageName(), project)) continue;
            // the blueprint knows the real kind; until it hydrates, CLASS is the display default
            HaxeClassLookupData lookupData =
              new HaxeClassLookupData(qualifiedInfo, entry.name(), HaxeComponentType.CLASS, () -> catalog.renderedClass(entry));
            result.add(lookupData);
        }
        return result;
    }

    public static Collection<HaxeClass> getByNameFiltered(@NotNull String name,
                                                          @NotNull Project project,
                                                          @SuppressWarnings("unused") @Nullable GlobalSearchScope scope) {
        HaxeCompilerTypeCatalogService catalog = HaxeCompilerTypeCatalogService.getInstance(project);
        List<HaxeClass> result = new ArrayList<>();
        for (HaxeCompilerTypeCatalogService.GeneratedType entry : catalog.byName(name)) {
            HaxeClassModel model = catalog.renderedClass(entry);
            if (model != null) {
                result.add(model.haxeClass);
            }
        }
        return result;
    }
}
