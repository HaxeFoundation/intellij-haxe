package com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.specialized;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.HaxeComponentBaseIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer.HaxeConstructorNameIndexer;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeModuleModel;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public class HaxeConstructorFileIndex extends HaxeComponentBaseIndex {
    // Key is FQN string of parent class
    private static final ID<String, HaxeComponentIndexData> INDEX = ID.create("HaxeConstructorIndex");
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
        return new HaxeConstructorNameIndexer();
    }

    public static Collection<HaxeMethod> getConstructors(@NotNull Project project, GlobalSearchScope searchScope) {
        List<HaxeMethod> elements = new ArrayList<>();

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(INDEX, project);
        for (String classFqn : allKeys) {
            FileBasedIndex.getInstance().getFilesWithKey(INDEX, Set.of(classFqn),
                    new Processor<VirtualFile>() {
                        @Override
                        public boolean process(VirtualFile virtualFile) {
                            PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
                            HaxeComponentIndexData data = FileBasedIndex.getInstance().getFileData(INDEX, virtualFile, project).get(classFqn);
                            String className = data.getFqn().getClassName();
                            if (file instanceof HaxeFile haxeFile) {
                                HaxeModule module = haxeFile.getModule();
                                if (module != null && module.getModel() instanceof HaxeModuleModel model) {
                                    HaxeClassModel aClass = model.getClass(className);
                                    List<HaxeMethodModel> constructors = aClass.getConstructors(null);
                                    for (HaxeMethodModel constructor : constructors) {
                                        elements.add(constructor.getMethod());
                                    }
                                }
                            }
                            return false;
                        }
                    }, searchScope);
        }
        return elements;
    }

}
