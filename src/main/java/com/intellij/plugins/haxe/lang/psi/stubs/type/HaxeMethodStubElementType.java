package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeMethodNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeConstructorStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeStaticMethodNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeMethodStub;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.psi.stubs.*;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.BiFunction;

/**
 * Shared IStubElementType for method-like declarations (method, constructor, enumValueConstructor, moduleMethod).
 */
public class HaxeMethodStubElementType extends IStubElementType<HaxeMethodStub, HaxeMethod> {

  private final BiFunction<HaxeMethodStub, HaxeMethodStubElementType, ? extends HaxeMethod> myPsiCreator;

  public HaxeMethodStubElementType(@NotNull String debugName,
                                    @NotNull BiFunction<HaxeMethodStub, HaxeMethodStubElementType, ? extends HaxeMethod> psiCreator) {
    super(debugName, HaxeLanguage.INSTANCE);
    myPsiCreator = psiCreator;
  }

  @NotNull
  @Override
  public String getExternalId() {
    //want to use getDebugName here instead of "this", but it's marked as internal;
    // however, toString returns the value from getDebugName so "+ this" gives us the same result.
    return "haxe.method." + this;
  }

  @Override
  public HaxeMethod createPsi(@NotNull HaxeMethodStub stub) {
    return myPsiCreator.apply(stub, this);
  }

  @NotNull
  @Override
  public HaxeMethodStub createStub(@NotNull HaxeMethod psi, StubElement parentStub) {
    boolean hasParameters = psi.getParameterList().getParametersCount() > 0;

    // Capture relevant/frequently used compile-time metadata
    int keywordFlags = buildKeywordFlags(psi);
    int metaFlags = buildMetaFlags(psi);
    int propertyFlags = buildPropertyFlags(psi);

    return new HaxeMethodStub(parentStub, this, psi.getName(), keywordFlags, metaFlags, propertyFlags);
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
    if (psi.isStatic())       flags |= HaxeMethodStub.IS_STATIC;
    if (psi.isPublic())       flags |= HaxeMethodStub.IS_PUBLIC;
    if (psi.isOverride())     flags |= HaxeMethodStub.IS_OVERRIDE;
    if (psi.isAbstract())     flags |= HaxeMethodStub.IS_ABSTRACT;
    if (psi.isInline())       flags |= HaxeMethodStub.IS_INLINE;
    if (psi.isOverload())     flags |= HaxeMethodStub.IS_OVERLOAD;
    if (psi.isMacro())        flags |= HaxeMethodStub.IS_MACRO;
    if (psi.isDynamic())      flags |= HaxeMethodStub.IS_DYNAMIC;
    return flags;
  }
  private static int buildPropertyFlags(@NotNull HaxeMethod psi) {
    int flags = 0;
    if (psi.isConstructor())  flags |= HaxeMethodStub.IS_CONSTRUCTOR;
    if (psi.hasParameters())  flags |= HaxeMethodStub.HAS_PARAMETERS;
    if (psi.isVarArgs())      flags |= HaxeMethodStub.HAS_VARARG_PARAMETERS;
    return flags;
  }


  @Override
  public void serialize(@NotNull HaxeMethodStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    dataStream.writeVarInt(stub.getKeywordFlags());
    dataStream.writeVarInt(stub.getMetaFlags());
    dataStream.writeVarInt(stub.getPropertyFlags());
  }

  @NotNull
  @Override
  public HaxeMethodStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef nameRef = dataStream.readName();
    int keywordFlags = dataStream.readVarInt();
    int metaFlags = dataStream.readVarInt();
    int propertyFlags = dataStream.readVarInt();
    String name = nameRef != null ? nameRef.getString() : null;
    return new HaxeMethodStub(parentStub, this, name, keywordFlags, metaFlags, propertyFlags);
  }

  @Override
  public void indexStub(@NotNull HaxeMethodStub stub, @NotNull IndexSink sink) {
    String name = stub.getName();
    if(stub.isConstructor()) {
      sink.occurrence(HaxeConstructorStubIndex.KEY, name);
    }else if(stub.isStatic()) {
      sink.occurrence(HaxeStaticMethodNameStubIndex.KEY, name);
      //TODO mlo: get method Qname (test if separation between class FQN and static members FQN index improves performance)
//    sink.occurrence(HaxeFullyQualifiedNameStubIndex.KEY, qualifiedName);
    }

    if (name != null) {
      sink.occurrence(HaxeMethodNameStubIndex.KEY, name);
    }
  }
}

