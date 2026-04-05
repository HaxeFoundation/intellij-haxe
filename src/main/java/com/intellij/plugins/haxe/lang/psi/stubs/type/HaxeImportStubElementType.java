package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeImportAlias;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeImportStatementImpl;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeImportStub;
import com.intellij.psi.stubs.*;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HaxeImportStubElementType extends IStubElementType<HaxeImportStub, HaxeImportStatement> {

  public HaxeImportStubElementType() {
    super("IMPORT_STATEMENT", HaxeLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "haxe.statement.import";
  }

  @Override
  public HaxeImportStatement createPsi(@NotNull HaxeImportStub stub) {
    return new HaxeImportStatementImpl(stub, this);
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
    return new HaxeImportStub(parentStub, this, importPath, hasWildcard, alias);
  }

  @Override
  public void serialize(@NotNull HaxeImportStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getImportPath());
    dataStream.writeBoolean(stub.hasWildcard());
    dataStream.writeName(stub.getAlias());
  }

  @NotNull
  @Override
  public HaxeImportStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef pathRef = dataStream.readName();
    boolean hasWildcard = dataStream.readBoolean();
    StringRef aliasRef = dataStream.readName();
    String path = pathRef != null ? pathRef.getString() : null;
    String alias = aliasRef != null ? aliasRef.getString() : null;
    return new HaxeImportStub(parentStub, this, path, hasWildcard, alias);
  }

  @Override
  public void indexStub(@NotNull HaxeImportStub stub, @NotNull IndexSink sink) {
   // NOOP
  }
}

