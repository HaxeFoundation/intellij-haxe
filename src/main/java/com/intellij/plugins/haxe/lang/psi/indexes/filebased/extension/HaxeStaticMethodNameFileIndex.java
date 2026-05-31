package com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer.HaxeStaticMethodNameIndexer;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeModuleModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeStaticMethodNameFileIndex extends HaxeComponentBaseIndex {
    public static final ID<String, HaxeComponentIndexData> INDEX = ID.create("HaxeStaticMethodNameIndex");
    private static final int INDEX_VERSION = HaxeIndexUtil.BASE_INDEX_VERSION;


    @Override
    public @NotNull ID<String, HaxeComponentIndexData> getName() {
        return INDEX;
    }

    @Override
    public int getVersion() {
        return INDEX_VERSION;
    }


    @Override
    public @NotNull DataIndexer<String, HaxeComponentIndexData, FileContent> getIndexer() {
        return new HaxeStaticMethodNameIndexer();
    }

    public static Collection<HaxeComponentIndexData> getValues(@NotNull String name,
                                                               @NotNull Project project,
                                                               @Nullable GlobalSearchScope scope) {
        return FileBasedIndex.getInstance().getValues(INDEX, name, scope);
    }

    public static Collection<HaxeMethod> getByName(@NotNull String name,
                                                   @NotNull Project project,
                                                   @Nullable GlobalSearchScope scope) {

//        List<HaxeMethod> elements = new ArrayList<>();
//        PsiManager instance = PsiManager.getInstance(project);
//        List<HaxeComponentIndexData> values = FileBasedIndex.getInstance().getValues(INDEX, name, scope);
//
//        FileBasedIndex.getInstance().getFilesWithKey(INDEX, Set.of(name),
//                new Processor<VirtualFile>() {
//                    @Override
//                    public boolean process(VirtualFile virtualFile) {
//                        PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
//                        HaxeComponentIndexData data = FileBasedIndex.getInstance().getFileData(INDEX, virtualFile, project).get(name);
//                        String className = data.getFqn().getClassName();
//                        if (file instanceof HaxeFile haxeFile) {
//                            HaxeModule module = haxeFile.getModule();
//                            if (module != null && module.getModel() instanceof HaxeModuleModel model) {
//                                HaxeClassModel aClass = model.getClass(className);
//                                HaxeMethodModel member = aClass.getMethodSelf(name);
//                                if (member != null) {
//                                    elements.add(member.getMethod());
//                                }
//                            }
//
//                        }
//                        return false;
//                    }
//                }, scope);

        List<HaxeMethod> elements = new ArrayList<>();
        PsiManager instance = PsiManager.getInstance(project);
        List<HaxeComponentIndexData> values = FileBasedIndex.getInstance().getValues(INDEX, name, scope);
        for (HaxeComponentIndexData value : values) {
            String qualifiedName = value.getFqn().getQualifiedName(false);
            PsiElement classOrMemberByQName = HaxeResolveUtil.findClassOrMemberByQName(qualifiedName, instance, scope);
            if(classOrMemberByQName instanceof HaxeMethod method) {
                elements.add(method);
            }
        }

        return elements;
    }


}
