package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeMutabilityModifier;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.lang.psi.HaxePropertyDeclaration;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeStaticFieldNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFieldStub;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.psi.stubs.*;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.BiFunction;

/**
 * Shared IStubElementType for field-like declarations (field, enumValueField, moduleField, optionalField).
 */
public class HaxeFieldStubElementType extends IStubElementType<HaxeFieldStub, HaxePsiField> {

  private final BiFunction<HaxeFieldStub, HaxeFieldStubElementType, ? extends HaxePsiField> myPsiCreator;

  public HaxeFieldStubElementType(@NotNull String debugName,
                                   @NotNull BiFunction<HaxeFieldStub, HaxeFieldStubElementType, ? extends HaxePsiField> psiCreator) {
    super(debugName, HaxeLanguage.INSTANCE);
    myPsiCreator = psiCreator;
  }

  @NotNull
  @Override
  public String getExternalId() {
    //want to use getDebugName here instead of "this", but it's marked as internal;
    // however, toString returns the value from getDebugName so "+ this" gives us the same result.
    return "haxe.field." + this;
  }

  @Override
  public HaxePsiField createPsi(@NotNull HaxeFieldStub stub) {
    return myPsiCreator.apply(stub, this);
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

    // Capture relevant/frequently used compile-time metadata
    int metaFlags = buildMetaFlags(psi);

    return new HaxeFieldStub(parentStub, this,
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
      if (meta.isType(HaxeMeta.NATIVE))        flags |= HaxeFieldStub.META_NATIVE;
      if (meta.isType(HaxeMeta.IS_VAR))        flags |= HaxeFieldStub.META_IS_VAR;
      if (meta.isType(HaxeMeta.DEPRECATED))    flags |= HaxeFieldStub.META_DEPRECATED;
      if (meta.isType(HaxeMeta.NO_COMPLETION)) flags |= HaxeFieldStub.META_NO_COMPLETION;
      if (meta.isType(HaxeMeta.KEEP))          flags |= HaxeFieldStub.META_KEEP;
    }
    return flags;
  }

  @Override
  public void serialize(@NotNull HaxeFieldStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    dataStream.writeVarInt(stub.getFlags());
    dataStream.writeVarInt(stub.getMetaFlags());
    dataStream.writeName(stub.getGetter());
    dataStream.writeName(stub.getSetter());
  }

  @NotNull
  @Override
  public HaxeFieldStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef nameRef   = dataStream.readName();
    int flags           = dataStream.readVarInt();
    int metaFlags       = dataStream.readVarInt();
    StringRef getterRef = dataStream.readName();
    StringRef setterRef = dataStream.readName();
    String name   = nameRef   != null ? nameRef.getString()   : null;
    String getter = getterRef != null ? getterRef.getString() : null;
    String setter = setterRef != null ? setterRef.getString() : null;
    return new HaxeFieldStub(parentStub, this, name, flags, metaFlags, getter, setter);
  }

  @Override
  public void indexStub(@NotNull HaxeFieldStub stub, @NotNull IndexSink sink) {
    String name = stub.getName();
    if(stub.isStatic()) {
      sink.occurrence(HaxeStaticFieldNameStubIndex.KEY, name);
    }
    if (name != null) {
      sink.occurrence(com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeFieldNameStubIndex.KEY, name);
    }
  }
}

