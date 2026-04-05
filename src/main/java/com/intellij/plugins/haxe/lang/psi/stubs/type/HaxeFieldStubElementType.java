package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeMutabilityModifier;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeStaticFieldNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFieldStub;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
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
    return "haxe.field." + getDebugName();
  }

  @Override
  public HaxePsiField createPsi(@NotNull HaxeFieldStub stub) {
    return myPsiCreator.apply(stub, this);
  }

  @NotNull
  @Override
  public HaxeFieldStub createStub(@NotNull HaxePsiField psi, StubElement parentStub) {
    boolean isFinal = false;
    if (psi instanceof HaxeFieldDeclaration fieldDecl) {

      // TODO add FQN to stub and index for FQN lookup of static members
//      FullyQualifiedInfo qualifiedInfo = fieldDecl.getModel().getQualifiedInfo();

      HaxeMutabilityModifier mutabilityPsi = fieldDecl.getMutabilityModifier();

      if (mutabilityPsi != null) {
        isFinal = mutabilityPsi.getText().equals(HaxePsiModifier.FINAL);
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
                              metaFlags);
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
  }

  @NotNull
  @Override
  public HaxeFieldStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef nameRef = dataStream.readName();
    int flags = dataStream.readVarInt();
    int metaFlags = dataStream.readVarInt();
    String name = nameRef != null ? nameRef.getString() : null;
    return new HaxeFieldStub(parentStub, this, name, flags, metaFlags);
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

