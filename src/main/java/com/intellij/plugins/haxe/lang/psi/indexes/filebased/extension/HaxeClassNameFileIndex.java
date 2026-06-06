package com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer.HaxeClassNameIndexer;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeModel;
import com.intellij.plugins.haxe.model.HaxeModuleModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class HaxeClassNameFileIndex extends HaxeComponentBaseIndex {
    public static final ID<String, HaxeComponentIndexData> INDEX = ID.create("HaxeClassNameIndex");
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
        return new HaxeClassNameIndexer();
    }

    public static Collection<HaxeComponentIndexData> getValues(@NotNull String name,
                                                                       @NotNull Project project,
                                                                       @Nullable GlobalSearchScope scope) {
        return FileBasedIndex.getInstance().getValues(INDEX, name, scope);
    }

    public static Collection<HaxeClass> getByNameFiltered(@NotNull String name,
                                                          @NotNull Project project,
                                                          @Nullable GlobalSearchScope scope) {

        List<HaxeClass> classes = new ArrayList<>();

        FileBasedIndex.getInstance().getFilesWithKey(INDEX, Set.of(name),
                new Processor<VirtualFile>() {
                    @Override
                    public boolean process(VirtualFile virtualFile) {
                        PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
                        if (file instanceof HaxeFile haxeFile) {
                            HaxeModule module = haxeFile.getModule();
                            if (module != null && module.getModel() instanceof HaxeModuleModel model) {
                                HaxeClassModel aClass = model.getClass(name);
                                if (aClass != null) {
                                    classes.add(aClass.haxeClass);
                                }
                            }

                        }
                        return false;
                    }
                }, scope);


//        List<HaxeClass> classes = new ArrayList<>();
//        PsiManager instance = PsiManager.getInstance(project);
//        List<HaxeComponentIndexData> values = FileBasedIndex.getInstance().getValues(INDEX, name, scope);
//        for (HaxeComponentIndexData value : values) {
//            String qualifiedName = value.getFqn().getQualifiedName(false);
//            PsiElement classOrMemberByQName = HaxeResolveUtil.findClassOrMemberByQName(qualifiedName, instance, scope);
//            if(classOrMemberByQName instanceof HaxeClass haxeClass) {
//                classes.add(haxeClass);
//            }
//        }

        return classes;
    }

}
