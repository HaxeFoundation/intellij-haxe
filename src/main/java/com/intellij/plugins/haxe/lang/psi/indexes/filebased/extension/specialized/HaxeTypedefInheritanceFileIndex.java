package com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.specialized;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.externalizer.HaxeComponentListExternalizer;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer.HaxeClassInheritanceIndexer;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer.HaxeTypedefInheritanceIndexer;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeSdkInputFilter;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HaxeTypedefInheritanceFileIndex extends  FileBasedIndexExtension<String, List<HaxeComponentIndexData>> {

    private static final ID<String, List<HaxeComponentIndexData>> INDEX = ID.create("HaxeTypedefClassIndex");
    private static final int INDEX_VERSION = HaxeIndexUtil.BASE_INDEX_VERSION;

    @Override
    public @NotNull ID<String, List<HaxeComponentIndexData>> getName() {
        return INDEX;
    }

    @Override
    public int getVersion() {
        return INDEX_VERSION;
    }

    @Override
    public FileBasedIndex.@NotNull InputFilter getInputFilter() {
        return HaxeSdkInputFilter.INSTANCE;
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public @NotNull DataIndexer<String, List<HaxeComponentIndexData>, FileContent> getIndexer() {
        return new HaxeTypedefInheritanceIndexer();
    }

    @Override
    public @NotNull DataExternalizer<List<HaxeComponentIndexData>> getValueExternalizer() {
        return new HaxeComponentListExternalizer();
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    public static Collection<HaxeClass> getBySuper(@NotNull String name, @NotNull Project project, GlobalSearchScope searchScope) {

        List<HaxeClass> results = new ArrayList<>();

        List<List<HaxeComponentIndexData>> files = FileBasedIndex.getInstance().getValues(INDEX, name, searchScope);
        PsiManager psiManager = PsiManager.getInstance(project);
        for (List<HaxeComponentIndexData> infoList : files) {
            for (HaxeComponentIndexData info : infoList) {
                String qualifiedName = info.getFqn().getQualifiedName(true);
                final HaxeClass subClass = HaxeResolveUtil.findClassByQName( qualifiedName , psiManager, searchScope);
                if (subClass != null) {
                    results.add(subClass);
                }
            }
        }

        return results;
    }
}
