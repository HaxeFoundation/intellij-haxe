package com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer.HaxeClassMethodNameIndexer;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer.HaxeModuleMethodNameIndexer;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeModuleModel;
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

public class HaxeModuleMethodNameFileIndex extends HaxeComponentBaseIndex {
    public static final ID<String, HaxeComponentIndexData> INDEX = ID.create("HaxeModuleMethodNameIndex");
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
        return new HaxeModuleMethodNameIndexer();
    }

    public static Collection<HaxeComponentIndexData> getValues(@NotNull String name,
                                                               @NotNull Project project,
                                                               @Nullable GlobalSearchScope scope) {
        return FileBasedIndex.getInstance().getValues(INDEX, name, scope);
    }


    public static Collection<HaxeMethod> getByName(@NotNull String name,
                                                   @NotNull Project project,
                                                   @Nullable GlobalSearchScope scope) {

        List<HaxeMethod> elements = new ArrayList<>();

        FileBasedIndex.getInstance().getFilesWithKey(INDEX, Set.of(name),
                new Processor<VirtualFile>() {
                    @Override
                    public boolean process(VirtualFile virtualFile) {
                        PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
                        HaxeComponentIndexData data = FileBasedIndex.getInstance().getFileData(INDEX, virtualFile, project).get(name);
                        String className = data.getFqn().getClassName();
                        if (file instanceof HaxeFile haxeFile) {
                            HaxeModule module = haxeFile.getModule();
                            if (module != null && module.getModel() instanceof HaxeModuleModel model) {
                                HaxeMethodModel member = model.getMethod(name, null);
                                if (member != null) {
                                    elements.add(member.getMethod());
                                }
                            }
                        }
                        return false;
                    }
                }, scope);

        return elements;
    }


}
