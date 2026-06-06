package com.intellij.plugins.haxe.lang.psi.stubs.factories;

import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeUsingStatement;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeUsingStatementImpl;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeUsingStub;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import org.jetbrains.annotations.NotNull;

public class HaxeUsingStubFactory implements StubElementFactory<HaxeUsingStub, HaxeUsingStatement> {

  private final HaxeElementType myElementType;

  public HaxeUsingStubFactory(@NotNull HaxeElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public HaxeUsingStatement createPsi(@NotNull HaxeUsingStub stub) {
    return new HaxeUsingStatementImpl(stub, myElementType);
  }

  @NotNull
  @Override
  public HaxeUsingStub createStub(@NotNull HaxeUsingStatement psi, StubElement parentStub) {
    HaxeReferenceExpression refExpr = psi.getReferenceExpression();
    String usingPath = refExpr != null ? refExpr.getText() : null;
    return new HaxeUsingStub(parentStub, myElementType, usingPath);
  }
}