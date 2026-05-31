package com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubableFileService;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeAnonymousTypeModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexDataUtil.createIndexData;
import static com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeInheritanceIndexUtil.containsDotSeparator;
import static com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeInheritanceIndexUtil.getClassNameCandidate;

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

            return collectSuperClasses(classes, haxeFile);


        }
        return Map.of();
    }

    private static @NonNull Map<String, List<HaxeComponentIndexData>>  collectSuperClasses(List<HaxeClass> classes, HaxeFile haxeFile) {
        final Map<String, List<HaxeComponentIndexData>> result = new HashMap<String, List<HaxeComponentIndexData>>(classes.size());
        final Map<String, String> qNameCache = new HashMap<String, String>();

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
                            final String classNameCandidate = haxeType.getText();
                            final String key = containsDotSeparator(classNameCandidate) ?
                                    classNameCandidate :
                                    getQNameAndCache(qNameCache, haxeFile, classNameCandidate, haxeType);
                            insert(result, key, value);
                        }

                        // composite types ( TypeA & TypeB & { ... })
                        for (HaxeType haxeType : anonymousTypeModel.getCompositeTypesPsi()) {
                            final String classNameCandidate = haxeType.getText();
                            final String key = containsDotSeparator(classNameCandidate) ?
                                    classNameCandidate :
                                    getQNameAndCache(qNameCache, haxeFile, classNameCandidate, haxeType);
                            insert(result, key, value);
                        }
                    }
                } else if (type != null) {

                    // handles normal types (`typedef TD = String`)
                    final String classNameCandidate = getClassNameCandidate(type);
                    final String qName = containsDotSeparator(classNameCandidate) ?
                            classNameCandidate :
                            getQNameAndCache(qNameCache, haxeFile, classNameCandidate, type);
                    insert(result, qName, value);
                }
            }
        }
        return result;
    }

    private static String getQNameAndCache(Map<String, String> qNameCache, PsiFile psiFile, String classNameCandidate, HaxeType haxeType) {
        String result = qNameCache.get(classNameCandidate);
        if (result == null) {
            result = HaxeResolveUtil.getQName(psiFile, classNameCandidate, true, true, haxeType);
            if (result == null) result = classNameCandidate;// fallback so key wont be null
            qNameCache.put(classNameCandidate, result);
        }
        return result;
    }

    private static void insert(Map<String, List<HaxeComponentIndexData>> map, String superClassQname, HaxeComponentIndexData value) {
        List<HaxeComponentIndexData> infos = map.computeIfAbsent(superClassQname, k -> new ArrayList<>());
        infos.add(value);
    }

}
