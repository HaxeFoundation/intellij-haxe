package com.intellij.plugins.haxe.lang.psi.fakes;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class HaxeFakeNamedComponent  extends HaxeFakePsiElement implements HaxeNamedComponent {

    private static final HaxeMetadataList EMPTY_META = new HaxeMetadataList();

    public abstract HaxeComponentType componentType();

    @Override
    public @Nullable HaxeNamedComponent getTypeComponent() {
        return null;
    }

    @Override
    public boolean isPublic() {
        return false;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isOverride() {
        return false;
    }

    @Override
    public boolean isOverload() {
        return false;
    }

    @Override
    public boolean isInline() {
        return false;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public String filterName() {
        return "";
    }

    @Override
    public PsiElement getModiferPsi(IElementType tokenType) {
        return null;
    }

    @Override
    public HaxeComponentType getComponentType() {
        return null;
    }

    @Override
    public IElementType getTokenType() {
        return null;
    }

    @Override
    public String getDocs() {
        return "";
    }

    @Override
    public @NotNull HaxeMetadataList getMetadataList(@Nullable Class<? extends HaxeMeta> metadataType) {
        return EMPTY_META;
    }

    @Override
    public boolean hasMetadata(HaxeMetadataTypeName name, @Nullable Class<? extends HaxeMeta> metadataType) {
        return false;
    }

}
