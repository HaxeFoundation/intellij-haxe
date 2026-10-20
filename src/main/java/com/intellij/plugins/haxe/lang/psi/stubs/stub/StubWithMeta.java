package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.model.HaxeCompilerMetadata;
import org.jetbrains.annotations.Nullable;

/** A stub recording compile-time metadata flags, keyed the way the stubs store metadata ({@code @:name}). */
public interface StubWithMeta {
    /** True or false when the stub records the metadata, null when it does not. */
    Boolean hasMetadata(@HaxeCompilerMetadata.CompilerMetadata String modifier);

    /**
     * The stub's answer to a PSI metadata query; null when the stub does not
     * record that metadata or the query is about run-time metadata.
     */
    default Boolean compileTimeMetadata(HaxeMetadataTypeName name, @Nullable Class<? extends HaxeMeta> metadataType) {
        return metadataType == HaxeMetadataCompileTimeMeta.class ? hasMetadata(name.asCompileTimeText()) : null;
    }
}
