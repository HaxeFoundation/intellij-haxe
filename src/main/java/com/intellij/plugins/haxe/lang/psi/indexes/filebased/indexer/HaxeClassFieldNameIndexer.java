package com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubableFileService;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexDataUtil.createIndexData;

public class HaxeClassFieldNameIndexer implements DataIndexer<String, HaxeComponentIndexData, FileContent> {

    @Override
    public @NotNull Map<String, HaxeComponentIndexData> map(@NonNull FileContent inputData) {
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

            return collectClassFields(classes);


        }
        return Map.of();
    }

    private static @NonNull Map<String, HaxeComponentIndexData> collectClassFields(List<HaxeClass> classes) {
        final Map<String, HaxeComponentIndexData> result = new HashMap<>();
        for (HaxeClass haxeClass : classes) {
            if (haxeClass.getName() == null || haxeClass.isTypeDef() || haxeClass.isAnonymousType()) {
                continue;
            }

            HaxeClassModel classModel = haxeClass.getModel();
            List<HaxeFieldModel> classFields = classModel.getFieldsSelf(null);

            for (HaxeFieldModel classField : classFields) {
                if (!classField.isStatic()) {
                    FullyQualifiedInfo qualifiedInfo = classField.getQualifiedInfo();
                    result.put(qualifiedInfo.getMemberName(), createIndexData(classField));
                }
            }
        }
        return result;
    }



}
