package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeModifierList;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFieldStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeMethodStub;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.light.LightModifierList;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A lightweight stub-based implementation of {@link HaxeModifierList} that avoids
 * forcing AST loading (Returned by {@code getModifierList()}) when the owning element
 * (class, method, or field) has a stub available.

 */
public class HaxeModifierListFromStub extends LightModifierList implements HaxeModifierList {

  private final PsiElement parent;

  public HaxeModifierListFromStub(@NotNull PsiElement parent) {
    super(parent.getManager(), HaxeLanguage.INSTANCE);
    this.parent = parent;
  }

  @Override
  public IElementType getTokenType() {
    return HaxeTokenTypes.CLASS_MODIFIER_LIST;
  }

  @NotNull
  @Override
  public HaxeMetadataList getMetadataList(@Nullable Class<? extends HaxeMeta> metadataType) {
    // Metadata lives on the owner element, not the modifier list node itself.
    return HaxeMetadataUtils.getMetadataList(parent, metadataType);
  }

  @Override
  public boolean hasMetadata(HaxeMetadataTypeName name, @Nullable Class<? extends HaxeMeta> metadataType) {
    // Try the parent's green stub before falling back to PSI tree traversal.
    Boolean fromStub = queryParentStubForMeta(name);
    if (fromStub != null) return fromStub;
    return HaxeMetadataUtils.hasMeta(parent, metadataType, name);
  }

  /**
   * Queries the parent element's green stub (if any) for the given metadata type.
   *
   * @return {@code true}/{@code false} if the stub tracks this metadata type,
   *         or {@code null} if the metadata type is not tracked by the stub
   *         (caller should fall back to PSI traversal).
   */
  @Nullable
  private Boolean queryParentStubForMeta(@NotNull HaxeMetadataTypeName name) {
    if (!(parent instanceof StubBasedPsiElementBase<?> stubBase)) return null;
    Object stub = stubBase.getGreenStub();
    if (stub == null) return null;

    String modifierKey = "@:" + name.name;

    if (stub instanceof HaxeClassStub classStub) {
      return classStub.hasMetadata(modifierKey);
    }
    if (stub instanceof HaxeMethodStub methodStub) {
      return methodStub.hasMetadata(modifierKey);
    }
    if (stub instanceof HaxeFieldStub fieldStub) {
      return fieldStub.hasMetadata(modifierKey);
    }
    return null;
  }
}
