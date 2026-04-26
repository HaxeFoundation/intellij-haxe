package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeUsingStatement;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeUsingStatementImpl;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeUsingStub;
import com.intellij.psi.stubs.*;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HaxeUsingStubElementType extends IStubElementType<HaxeUsingStub, HaxeUsingStatement> {

  public HaxeUsingStubElementType() {
    super("USING_STATEMENT", HaxeLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "haxe.statement.using";
  }

  @Override
  public HaxeUsingStatement createPsi(@NotNull HaxeUsingStub stub) {
    return new HaxeUsingStatementImpl(stub, this);
  }

  @NotNull
  @Override
  public HaxeUsingStub createStub(@NotNull HaxeUsingStatement psi, StubElement parentStub) {
    HaxeReferenceExpression refExpr = psi.getReferenceExpression();
    String usingPath = refExpr != null ? refExpr.getText() : null;
    return new HaxeUsingStub(parentStub, this, usingPath);
  }

  @Override
  public void serialize(@NotNull HaxeUsingStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getUsingPath());
  }

  @NotNull
  @Override
  public HaxeUsingStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef pathRef = dataStream.readName();
    String path = pathRef != null ? pathRef.getString() : null;
    return new HaxeUsingStub(parentStub, this, path);
  }

  @Override
  public void indexStub(@NotNull HaxeUsingStub stub, @NotNull IndexSink sink) {
  }
}

