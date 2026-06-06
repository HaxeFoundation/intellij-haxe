package com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer;

import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubableFileService;
import com.intellij.plugins.haxe.model.*;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexDataUtil.createIndexData;

public class HaxeFullyQualifiedNameIndexer implements DataIndexer<String, HaxeComponentIndexData, FileContent> {

    @Override
    public @NotNull Map<String, HaxeComponentIndexData> map(@NonNull FileContent inputData) {
        if (inputData.getPsiFile() instanceof HaxeFile haxeFile) {

            if(HaxeStubableFileService.skipFilebasedIndex(haxeFile)) {
                return Map.of();
            }
            if (HaxeIndexUtil.fileBelongToPlatformSpecificStd(haxeFile)) {
                return Map.of();
            }

            Map<String, HaxeComponentIndexData> indexDataMap = new HashMap<>();

            HaxeModule module = haxeFile.getModule();
            if(module != null && module.getModel() instanceof HaxeModuleModel moduleModel ) {
                List<HaxeClassModel> classes = moduleModel.getClasses();
                indexDataMap.putAll(collectAllClasses(classes));
                indexDataMap.putAll(collectAllClassMembers(classes));
                indexDataMap.putAll(collectAllModuleMembers(moduleModel));
            }

        }
        return Map.of();
    }

    private Map<String, ? extends HaxeComponentIndexData> collectAllModuleMembers(HaxeModuleModel moduleModel) {
        Map<String, HaxeComponentIndexData> indexDataMap = new HashMap<>();
        List<HaxeModel> exposedMembers = moduleModel.getExposedMembers();
        for (HaxeModel exposedMember : exposedMembers) {
            if (exposedMember instanceof HaxeMemberModel memberModel && memberModel.isModuleMember()) {
                if (exposedMember instanceof HaxeFieldModel fieldModel) {
                    indexDataMap.put(fieldModel.getQualifiedInfo().getQualifiedName(true), createIndexData(fieldModel));
                } else if (exposedMember instanceof HaxeMethodModel methodModel) {
                    indexDataMap.put(methodModel.getQualifiedInfo().getQualifiedName(true), createIndexData(methodModel));
                }
            }
        }
        return indexDataMap;
    }

    private static Map<String, HaxeComponentIndexData> collectAllClasses(List<HaxeClassModel> classes) {
        Map<String, HaxeComponentIndexData> indexDataMap = new HashMap<>();
        for (HaxeClassModel model : classes) {
            indexDataMap.put(model.getQualifiedInfo().getQualifiedName(true), createIndexData(model));
        }
        return indexDataMap;
    }



    private static @NonNull Map<String, HaxeComponentIndexData> collectAllClassMembers(List<HaxeClassModel> classes) {
        final Map<String, HaxeComponentIndexData> result = new HashMap<>();
        for (HaxeClassModel classModel : classes) {
            if (classModel.getName() == null
                    || classModel.isTypedef()
                    || classModel.isAnonymous()
                    || classModel.isAbstractType()
                    || classModel.isInterface()) {
                continue;
            }

            List<HaxeFieldModel> classFields = classModel.getFieldsSelf(null);
            List<HaxeMethodModel> classMethods = classModel.getMethodsSelf(null);

            for (HaxeFieldModel classField : classFields) {
                FullyQualifiedInfo qualifiedInfo = classField.getQualifiedInfo();
                result.put(qualifiedInfo.getQualifiedName(true), createIndexData(classField));
            }

            for (HaxeMethodModel classMethod : classMethods) {
                FullyQualifiedInfo qualifiedInfo = classMethod.getQualifiedInfo();
                result.put(qualifiedInfo.getQualifiedName(true), createIndexData(classMethod));
            }
        }
        return result;
    }




}
