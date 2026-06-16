package com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.specialized;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.HaxeComponentBaseIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer.HaxeModuleInPackageIndexer;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class HaxeModulesInPackageIndex extends HaxeComponentBaseIndex {

    private static final ID<String, HaxeComponentIndexData> INDEX = ID.create("HaxeModulesInPackageIndex");
    private static final int INDEX_VERSION = HaxeIndexUtil.BASE_INDEX_VERSION;

    @Override
    public int getVersion() {
        return INDEX_VERSION;
    }


    @Override
    public @NotNull ID<String, HaxeComponentIndexData> getName() {
        return INDEX;
    }


    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }



    @Override
    public @NotNull DataIndexer<String, HaxeComponentIndexData, FileContent> getIndexer() {
        return new HaxeModuleInPackageIndexer();
    }


    public static Collection<HaxeComponentIndexData> getModulesInPackage(@NotNull String name, @NotNull Project project, @Nullable GlobalSearchScope scope) {
        return FileBasedIndex.getInstance().getValues(INDEX, name, scope);
    }


}
