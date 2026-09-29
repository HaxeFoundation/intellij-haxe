package com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubableFileService;
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

import static com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeInheritanceIndexUtil.superTypeSimpleName;
import static com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexDataUtil.createIndexData;

/**
 * Keys are the supertypes' SIMPLE names: an indexer may not read outside the
 * indexed file, and resolving a supertype's qualified name would. Supertypes
 * sharing a simple name are told apart at lookup, which checks each
 * candidate's qualified name.
 */
public class HaxeClassInheritanceIndexer implements DataIndexer<String, List<HaxeComponentIndexData>, FileContent> {

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
            if (!haxeClass.isTypeDef()) {
                HaxeClassModel classModel = haxeClass.getModel();
                HaxeComponentIndexData value = createIndexData(classModel);

                for (HaxeType haxeType : haxeClass.getHaxeExtendsList()) {
                    processInheritance(haxeType, result, value);
                }
                for (HaxeType haxeType : haxeClass.getHaxeImplementsList()) {
                    processInheritance(haxeType, result, value);
                }
            }
        }
        return result;
    }

    private static void processInheritance(HaxeType haxeType,
                                           Map<String, List<HaxeComponentIndexData>> result,
                                           HaxeComponentIndexData value) {
        if (haxeType == null) return;
        insert(result, superTypeSimpleName(haxeType), value);
    }

    private static void insert(Map<String, List<HaxeComponentIndexData>> map, String superClassKey, HaxeComponentIndexData value) {
        List<HaxeComponentIndexData> infos = map.computeIfAbsent(superClassKey, k -> new ArrayList<>());
        infos.add(value);
    }

}
