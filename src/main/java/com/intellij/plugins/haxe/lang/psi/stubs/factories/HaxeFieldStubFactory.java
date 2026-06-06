package com.intellij.plugins.haxe.lang.psi.stubs.factories;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeMutabilityModifier;
import com.intellij.plugins.haxe.lang.psi.HaxePropertyDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFieldStub;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.function.BiFunction;


public class HaxeFieldStubFactory implements StubElementFactory<HaxeFieldStub, HaxePsiField> {

  private final HaxeElementType myElementType;
  private final BiFunction<HaxeFieldStub, HaxeElementType, ? extends HaxePsiField> myPsiCreator;

  public HaxeFieldStubFactory(@NotNull HaxeElementType elementType,
                              @NotNull BiFunction<HaxeFieldStub, HaxeElementType, ? extends HaxePsiField> psiCreator) {
    myElementType = elementType;
    myPsiCreator = psiCreator;
  }


  @Override
  public HaxePsiField createPsi(@NotNull HaxeFieldStub stub) {
    return myPsiCreator.apply(stub, myElementType);
  }

  @NotNull
  @Override
  public HaxeFieldStub createStub(@NotNull HaxePsiField psi, StubElement parentStub) {
    boolean isFinal = false;
    String getter = null;
    String setter = null;

    if (psi instanceof HaxeFieldDeclaration fieldDecl) {
      HaxeMutabilityModifier mutabilityPsi = fieldDecl.getMutabilityModifier();
      if (mutabilityPsi != null) {
        isFinal = mutabilityPsi.getText().equals(HaxePsiModifier.FINAL);
      }

      HaxePropertyDeclaration prop = fieldDecl.getPropertyDeclaration();
      if (prop != null) {
        var accessors = prop.getPropertyAccessorList();
        if (accessors.size() >= 1) getter = accessors.get(0).getText();
        if (accessors.size() >= 2) setter = accessors.get(1).getText();
      }
    }

    int metaFlags = buildMetaFlags(psi);

    return new HaxeFieldStub(parentStub, myElementType,
                             psi.getName(),
                             psi.isStatic(),
                             psi.isPublic(),
                             isFinal,
                             psi.isInline(),
                             metaFlags,
                             getter,
                             setter);
  }

  /** Walks preceding sibling metadata and packs the result into a metaFlags bitmask. */
  private static int buildMetaFlags(@NotNull HaxePsiField psi) {
    var metas = HaxeMetadataUtils.getMetadataList(psi, HaxeMeta.COMPILE_TIME);
    int flags = 0;
    for (HaxeMeta meta : metas) {
      if (meta.isType(HaxeMeta.FINAL))         flags |= HaxeFieldStub.META_FINAL;
      if (meta.isType(HaxeMeta.IS_VAR))        flags |= HaxeFieldStub.META_IS_VAR;
      if (meta.isType(HaxeMeta.INLINE))        flags |= HaxeFieldStub.META_INLINE;
      if (meta.isType(HaxeMeta.NATIVE))        flags |= HaxeFieldStub.META_NATIVE;
      if (meta.isType(HaxeMeta.DEPRECATED))    flags |= HaxeFieldStub.META_DEPRECATED;
      if (meta.isType(HaxeMeta.NO_COMPLETION)) flags |= HaxeFieldStub.META_NO_COMPLETION;
      if (meta.isType(HaxeMeta.KEEP))          flags |= HaxeFieldStub.META_KEEP;
    }
    return flags;
  }
}