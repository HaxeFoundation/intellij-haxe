package com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
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

public class HaxeModuleMethodNameIndexer implements DataIndexer<String, HaxeComponentIndexData, FileContent> {

    @Override
    public @NotNull Map<String, HaxeComponentIndexData> map(@NonNull FileContent inputData) {
        if (inputData.getPsiFile() instanceof HaxeFile haxeFile) {

            if (HaxeStubableFileService.skipFilebasedIndex(haxeFile)) {
                return Map.of();
            }
            if (HaxeIndexUtil.fileBelongToPlatformSpecificStd(haxeFile)) {
                return Map.of();
            }

            HaxeModule module = haxeFile.getModule();
            if (module != null && module.getModel() instanceof HaxeModuleModel model) {
                return collectModuleMethods(model);
            }


        }
        return Map.of();
    }

    private static @NonNull Map<String, HaxeComponentIndexData> collectModuleMethods(HaxeModuleModel model) {
        final Map<String, HaxeComponentIndexData> result = new HashMap<>();
        List<HaxeNamedComponent> namedComponents = model.getAllHaxeNamedComponents(HaxeComponentType.METHOD);
        for (HaxeNamedComponent component : namedComponents) {

            if (component instanceof HaxeModuleMethodDeclaration methodDeclaration) {
                HaxeMethodModel methodModel = methodDeclaration.getModel();
                FullyQualifiedInfo qualifiedInfo = methodModel.getQualifiedInfo();
                result.put(qualifiedInfo.getMemberName(), createIndexData(methodModel));
            }
        }
        return result;
    }


}
