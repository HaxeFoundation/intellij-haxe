package com.intellij.plugins.haxe.lang.psi.indexes.filebased.data;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HaxeComponentIndexData {
    List<String> targets = new ArrayList<>();
    String name;
    HaxeComponentType type;
    FullyQualifiedInfo fqn;
    /**
     * Visibility as DECLARED in the indexed file. When
     * {@link #isVisibilityInherited()} this is only a placeholder: a bare
     * {@code override} inherits the overridden method's visibility, which an
     * indexer cannot resolve (index data must come from the file alone) —
     * consumers that need an instance method's real visibility must resolve
     * the parent chain at query time.
     */
    boolean isPublic;

    /** True for a bare {@code override} (no explicit public/private): the real visibility lives in the parent. */
    boolean visibilityInherited;
}
