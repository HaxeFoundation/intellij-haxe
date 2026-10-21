package com.intellij.plugins.haxe.lang.psi.stubs.factories;

import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeMethodStub;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;


public class HaxeMethodStubFactory implements StubElementFactory<HaxeMethodStub, HaxeMethod> {

  private final HaxeElementType myElementType;
  private final BiFunction<HaxeMethodStub, HaxeElementType, ? extends HaxeMethod> myPsiCreator;

  public HaxeMethodStubFactory(@NotNull HaxeElementType elementType,
                               @NotNull BiFunction<HaxeMethodStub, HaxeElementType, ? extends HaxeMethod> psiCreator) {
    myElementType = elementType;
    myPsiCreator = psiCreator;
  }

  @Override
  public HaxeMethod createPsi(@NotNull HaxeMethodStub stub) {
    return myPsiCreator.apply(stub, myElementType);
  }


  @NotNull
  @Override
  public HaxeMethodStub createStub(@NotNull HaxeMethod psi, StubElement parentStub) {
    int keywordFlags = buildKeywordFlags(psi);
    int metaFlags = buildMetaFlags(psi);
    int propertyFlags = buildPropertyFlags(psi);

    return new HaxeMethodStub(parentStub, myElementType, psi.getName(), keywordFlags, metaFlags, propertyFlags);
  }

  /** Walks preceding sibling metadata and packs the result into a metaFlags bitmask. */
  private static int buildMetaFlags(@NotNull HaxeMethod psi) {
    var metas = HaxeMetadataUtils.getMetadataList(psi, HaxeMeta.COMPILE_TIME);
    int flags = 0;
    for (HaxeMeta meta : metas) {
      if (meta.isType(HaxePsiModifier.ABSTRACT))    flags |= HaxeMethodStub.META_ABSTRACT;
      if (meta.isType(HaxeMeta.FINAL))              flags |= HaxeMethodStub.META_FINAL;
      if (meta.isType(HaxeMeta.NATIVE))             flags |= HaxeMethodStub.META_NATIVE;
      if (meta.isType(HaxeMeta.INLINE))             flags |= HaxeMethodStub.META_INLINE;
      if (meta.isType(HaxeMeta.MACRO))              flags |= HaxeMethodStub.META_MACRO;
      if (meta.isType(HaxeMeta.DEPRECATED))         flags |= HaxeMethodStub.META_DEPRECATED;
      if (meta.isType(HaxeMeta.NO_COMPLETION))      flags |= HaxeMethodStub.META_NO_COMPLETION;
      if (meta.isType(HaxeMeta.KEEP))               flags |= HaxeMethodStub.META_KEEP;
      if (meta.isType(HaxeMeta.NO_USING))           flags |= HaxeMethodStub.META_NO_USING;
      if (meta.isType(HaxeMeta.OVERLOAD))           flags |= HaxeMethodStub.META_OVERLOAD;
    }
    return flags;
  }

  private static int buildKeywordFlags(@NotNull HaxeMethod psi) {
    int flags = 0;
    if (psi.isStatic())         flags |= HaxeMethodStub.KEYWORD_STATIC;
    // only the DECLARED visibility: indexing cannot read other files
    if (psi.isDeclaredPublic()) flags |= HaxeMethodStub.KEYWORD_PUBLIC;
    if (psi.isOverride())       flags |= HaxeMethodStub.KEYWORD_OVERRIDE;
    if (psi.isAbstract())       flags |= HaxeMethodStub.KEYWORD_ABSTRACT;
    if (psi.isInline())         flags |= HaxeMethodStub.KEYWORD_INLINE;
    if (psi.isOverload())       flags |= HaxeMethodStub.KEYWORD_OVERLOAD;
    if (psi.isMacro())          flags |= HaxeMethodStub.KEYWORD_MACRO;
    if (psi.isDynamic())        flags |= HaxeMethodStub.KEYWORD_DYNAMIC;
    return flags;
  }

  private static int buildPropertyFlags(@NotNull HaxeMethod psi) {
    int flags = 0;
    if (psi.isConstructor())  flags |= HaxeMethodStub.IS_CONSTRUCTOR;
    if (psi.hasParameters())  flags |= HaxeMethodStub.HAS_PARAMETERS;
    if (psi.isVarArgs())      flags |= HaxeMethodStub.HAS_VARARG_PARAMETERS;
    // a bare `override` inherits the overridden method's visibility, which
    // indexing cannot resolve (it may not read other files); the flag defers
    // that check to query time
    if (psi.getModel() instanceof HaxeMemberModel member && member.isVisibilityInheritedFromParent()) {
      flags |= HaxeMethodStub.VISIBILITY_INHERITED;
    }
    return flags;
  }
}