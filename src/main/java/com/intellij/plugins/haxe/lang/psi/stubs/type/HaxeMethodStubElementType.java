package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
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
    return "haxe.method." + getDebugName();
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
    int flags = buildFlags(psi);
    int metaFlags = buildMetaFlags(psi);

    return new HaxeMethodStub(parentStub, this, psi.getName(), flags, metaFlags);
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
    }
    return flags;
  }

  private static int buildFlags(@NotNull HaxeMethod psi) {
    int flags = 0;
    if (psi.isStatic())       flags |= HaxeMethodStub.IS_STATIC;
    if (psi.isPublic())       flags |= HaxeMethodStub.IS_PUBLIC;
    if (psi.isOverride())     flags |= HaxeMethodStub.IS_OVERRIDE;
    if (psi.isAbstract())     flags |= HaxeMethodStub.IS_ABSTRACT;
    if (psi.isInline())       flags |= HaxeMethodStub.IS_INLINE;
    if (psi.hasParameters())  flags |= HaxeMethodStub.HAS_PARAMETERS;
    return flags;
  }


  @Override
  public void serialize(@NotNull HaxeMethodStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    dataStream.writeVarInt(stub.getFlags());
    dataStream.writeVarInt(stub.getMetaFlags());
  }

  @NotNull
  @Override
  public HaxeMethodStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef nameRef = dataStream.readName();
    int flags = dataStream.readVarInt();
    int metaFlags = dataStream.readVarInt();
    String name = nameRef != null ? nameRef.getString() : null;
    return new HaxeMethodStub(parentStub, this, name, flags, metaFlags);
  }

  @Override
  public void indexStub(@NotNull HaxeMethodStub stub, @NotNull IndexSink sink) {
    String name = stub.getName();
    if(stub.isConstructor()) {
      sink.occurrence(HaxeConstructorStubIndex.KEY, name);
    }else if(stub.isStatic()) {
      sink.occurrence(HaxeStaticMethodNameStubIndex.KEY, name);
      //TODO mlo: get Qname (test if separation between class FQN and static members FQN index gains  performance)
//    sink.occurrence(com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedNameStubIndex.KEY, qualifiedName);
    }

    if (name != null) {
      sink.occurrence(com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeMethodNameStubIndex.KEY, name);
    }
  }
}

