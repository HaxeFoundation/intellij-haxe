package com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubableFileService;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexDataUtil.createIndexData;

public class HaxeStaticMethodNameIndexer implements DataIndexer<String, HaxeComponentIndexData, FileContent> {

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

            return collectStaticMethods(classes);


        }
        return Map.of();
    }

    private static @NonNull Map<String, HaxeComponentIndexData> collectStaticMethods(List<HaxeClass> classes) {
        final Map<String, HaxeComponentIndexData> result = new HashMap<>();
        for (HaxeClass haxeClass : classes) {
            if (haxeClass.getName() == null
                    || haxeClass.isTypeDef()
                    || haxeClass.isAnonymousType()
                    || haxeClass.isAbstractType()
                    || haxeClass.isInterface()) {
                continue;
            }

            HaxeClassModel classModel = haxeClass.getModel();
            List<HaxeMethodModel> classMethods = classModel.getMethodsSelf(null);

            for (HaxeMethodModel classMethod : classMethods) {
                if (classMethod.isStatic() && classMethod.isPublic()) {
                    FullyQualifiedInfo qualifiedInfo = classMethod.getQualifiedInfo();
                    result.put(qualifiedInfo.getMemberName(), createIndexData(classMethod));
                }
            }
        }
        return result;
    }



}
