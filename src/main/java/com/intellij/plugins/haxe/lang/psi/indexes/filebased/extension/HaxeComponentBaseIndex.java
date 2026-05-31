package com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension;

import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeSdkInputFilter;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.externalizer.HaxeComponentExternalizer;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

public abstract class HaxeComponentBaseIndex  extends FileBasedIndexExtension<String, HaxeComponentIndexData> {

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public FileBasedIndex.@NotNull InputFilter getInputFilter() {
        return HaxeSdkInputFilter.INSTANCE;
    }

    @Override
    public @NotNull DataExternalizer<HaxeComponentIndexData> getValueExternalizer() {
        return HaxeComponentExternalizer.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    public boolean traceKeyHashToVirtualFileMapping() {
        return true;
    }
}
