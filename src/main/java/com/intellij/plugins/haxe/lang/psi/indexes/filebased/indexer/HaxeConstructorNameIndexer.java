package com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubableFileService;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexDataUtil.createIndexData;

public class HaxeConstructorNameIndexer implements DataIndexer<String, HaxeComponentIndexData, FileContent> {

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

            return collectConstrcutors(classes);


        }
        return Map.of();
    }

    private static @NonNull Map<String, HaxeComponentIndexData> collectConstrcutors(List<HaxeClass> classes) {
        final Map<String, HaxeComponentIndexData> result = new HashMap<>();
        for (HaxeClass haxeClass : classes) {
            if (haxeClass.getName() == null || haxeClass.isTypeDef() || haxeClass.isAnonymousType()) {
                //TODO considder resolving typedef to get constructor if class type?
                continue;
            }

            HaxeClassModel classModel = haxeClass.getModel();
            List<HaxeMethodModel> constructorModels = classModel.getConstructors(null);

            for (HaxeMethodModel constructorModel : constructorModels) {
                FullyQualifiedInfo qualifiedInfo = constructorModel.getQualifiedInfo();
                FullyQualifiedInfo classQname =  qualifiedInfo.toClassQualifiedName();
                result.put(classQname.getQualifiedName(true), createIndexData(constructorModel));
                result.put(classQname.getQualifiedName(false), createIndexData(constructorModel));
            }
        }
        return result;
    }



}
