package com.intellij.plugins.haxe.lang.psi.indexes.filebased.data;

import org.jetbrains.annotations.Nullable;

public interface HaxeIndexMemberData {
    @Nullable public String getTarget();
    @Nullable public String getType();
    @Nullable public String getName();
    @Nullable public String getFqn();
}
