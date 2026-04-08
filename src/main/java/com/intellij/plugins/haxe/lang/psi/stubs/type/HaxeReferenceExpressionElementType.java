package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeStaticFieldNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFieldStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeReferenceExpressionStub;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.*;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.function.BiFunction;

/**
 * Shared IStubElementType for field-like declarations (field, enumValueField, moduleField, optionalField).
 */
public class HaxeReferenceExpressionElementType extends IStubElementType<HaxeReferenceExpressionStub, HaxeReferenceExpression> {

  private final BiFunction<HaxeReferenceExpressionStub, HaxeReferenceExpressionElementType, ? extends HaxeReferenceExpression> myPsiCreator;

  public HaxeReferenceExpressionElementType(@NotNull String debugName,
                                            @NotNull BiFunction<HaxeReferenceExpressionStub, HaxeReferenceExpressionElementType, ? extends HaxeReferenceExpression> psiCreator) {
    super(debugName, HaxeLanguage.INSTANCE);
    myPsiCreator = psiCreator;
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "haxe.field." + getDebugName();
  }

  @Override
  public HaxeReferenceExpression createPsi(@NotNull HaxeReferenceExpressionStub stub) {
    return myPsiCreator.apply(stub, this);
  }

  @Override
  public @NonNull HaxeReferenceExpressionStub createStub(@NonNull HaxeReferenceExpression psi,
                                                         StubElement<? extends PsiElement> parentStub) {

    return new HaxeReferenceExpressionStub(parentStub, this, psi.getText());
  }

  @Override
  public void serialize(@NotNull HaxeReferenceExpressionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getText());
  }

  @NotNull
  @Override
  public HaxeReferenceExpressionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef text   = dataStream.readName();
    return new HaxeReferenceExpressionStub(parentStub, this, text.getString());
  }

  @Override
  public void indexStub(@NotNull HaxeReferenceExpressionStub stub, @NotNull IndexSink sink) {

  }
}

