package com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubableFileService;
import com.intellij.plugins.haxe.model.HaxeAnonymousTypeModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexDataUtil.createIndexData;
import static com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeInheritanceIndexUtil.superTypeSimpleName;

/**
 * Keys are the supertypes' SIMPLE names: an indexer may not read outside the
 * indexed file, and resolving a supertype's qualified name would. Supertypes
 * sharing a simple name are told apart at lookup, which checks each
 * candidate's qualified name.
 */
public class HaxeTypedefInheritanceIndexer implements DataIndexer<String, List<HaxeComponentIndexData>, FileContent> {

    @Override
    public @NotNull Map<String, List<HaxeComponentIndexData>>  map(@NonNull FileContent inputData) {
        if (inputData.getPsiFile() instanceof HaxeFile haxeFile) {

            if(HaxeStubableFileService.skipFilebasedIndex(haxeFile)) {
                return Map.of();
            }
            if (HaxeIndexUtil.fileBelongToPlatformSpecificStd(haxeFile)) {
                return Map.of();
            }

            final List<HaxeClass> classes = HaxeResolveUtil.findComponentDeclarations(haxeFile);
            if (classes.isEmpty()) {
                return Map.of();
            }

            return collectSuperClasses(classes);
        }
        return Map.of();
    }

    private static @NonNull Map<String, List<HaxeComponentIndexData>> collectSuperClasses(List<HaxeClass> classes) {
        final Map<String, List<HaxeComponentIndexData>> result = new HashMap<>(classes.size());

        for (HaxeClass haxeClass : classes) {
            if (!haxeClass.isTypeDef()) continue;

            if (haxeClass instanceof HaxeTypedefDeclaration haxeTypeDef) {
                HaxeClassModel model = haxeClass.getModel();
                HaxeComponentIndexData value = createIndexData(model);

                final HaxeTypeOrAnonymous haxeTypeOrAnonymous = haxeTypeDef.getTypeOrAnonymous();
                final HaxeType type = haxeTypeOrAnonymous == null ? null : haxeTypeOrAnonymous.getType();
                final HaxeAnonymousType anonymousType = haxeTypeOrAnonymous == null ? null : haxeTypeOrAnonymous.getAnonymousType();
                if (anonymousType != null) {
                    if (anonymousType.getModel() instanceof HaxeAnonymousTypeModel anonymousTypeModel) {

                        // handles anonymous structures extends (`{> TypeA, > TypeB, ...fields}`)
                        for (HaxeType haxeType : anonymousTypeModel.getExtensionTypesPsi()) {
                            insert(result, superTypeSimpleName(haxeType), value);
                        }

                        // composite types ( TypeA & TypeB & { ... })
                        for (HaxeType haxeType : anonymousTypeModel.getCompositeTypesPsi()) {
                            insert(result, superTypeSimpleName(haxeType), value);
                        }
                    }
                } else if (type != null) {

                    // handles normal types (`typedef TD = String`)
                    insert(result, superTypeSimpleName(type), value);
                }
            }
        }
        return result;
    }

    private static void insert(Map<String, List<HaxeComponentIndexData>> map, String superClassKey, HaxeComponentIndexData value) {
        List<HaxeComponentIndexData> infos = map.computeIfAbsent(superClassKey, k -> new ArrayList<>());
        infos.add(value);
    }

}
