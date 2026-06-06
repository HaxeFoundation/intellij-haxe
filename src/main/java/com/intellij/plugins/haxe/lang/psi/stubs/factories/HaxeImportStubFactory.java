package com.intellij.plugins.haxe.lang.psi.stubs.factories;

import com.intellij.plugins.haxe.lang.psi.HaxeImportAlias;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeImportStatementImpl;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeImportStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import org.jetbrains.annotations.NotNull;

public class HaxeImportStubFactory implements StubElementFactory<HaxeImportStub, HaxeImportStatement> {

  private final HaxeElementType myElementType;

  public HaxeImportStubFactory(@NotNull HaxeElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public HaxeImportStatement createPsi(@NotNull HaxeImportStub stub) {
    return new HaxeImportStatementImpl(stub, myElementType);
  }

  @NotNull
  @Override
  public HaxeImportStub createStub(@NotNull HaxeImportStatement psi, StubElement parentStub) {
    HaxeReferenceExpression refExpr = psi.getReferenceExpression();
    String importPath = refExpr != null ? refExpr.getText() : null;
    boolean hasWildcard = psi.getWildcard() != null;
    HaxeImportAlias aliasElement = psi.getAlias();
    String alias = null;
    if (aliasElement != null) {
      var identifier = aliasElement.getIdentifier();
      if (identifier != null) {
        alias = identifier.getText();
      }
    }
    return new HaxeImportStub(parentStub, myElementType, importPath, hasWildcard, alias);
  }
}