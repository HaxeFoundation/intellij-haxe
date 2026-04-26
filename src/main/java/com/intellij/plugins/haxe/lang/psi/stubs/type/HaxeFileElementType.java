package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFileStub;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubVersions;
import com.intellij.psi.*;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IStubFileElementType;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@CustomLog
public class HaxeFileElementType extends IStubFileElementType<HaxeFileStub> {

  public HaxeFileElementType() {
    super("HAXEFILE", HaxeLanguage.INSTANCE);
  }

  @Override
  public int getStubVersion() {
    return HaxeStubVersions.STUB_VERSION;
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "haxe.file";
  }

  @Override
  public void serialize(@NotNull HaxeFileStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    // Package name is now stored in the child HaxePackageStatementStub — nothing to serialize here.
  }

  @NotNull
  @Override
  public HaxeFileStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    return new HaxeFileStub(null);
  }

  @Override
  public StubBuilder getBuilder() {
    return new DefaultStubBuilder() {
      @NotNull
      @Override
      protected PsiFileStub<?> createStubForFile(@NotNull PsiFile file) {
        if (file instanceof HaxeFile haxeFile) {
          return new HaxeFileStub(haxeFile);
        }
        return new HaxeFileStub(null);
      }

      /**
       * attempt at reducing stub creation for anything inside a method/function body.
       */
      @Override
      public boolean skipChildProcessingWhenBuildingStubs(@NotNull ASTNode parent, @NotNull ASTNode node) {
        IElementType parentType = parent.getElementType();
        IElementType currentType = node.getElementType();

        if(docsConditionalOrSpacing(currentType)) return true;

        // skip method blocks (Note that methods can be created without a block as body)
        if (isElementTypeToSkip(parentType) || isElementTypeToSkip(currentType)) return true;

        return false;
      }
    };
  }

  private static boolean isElementTypeToSkip(IElementType parentType) {
    return parentType == HaxeTokenTypes.VAR_INIT // Skip field inits
           || parentType == HaxeTokenTypes.BLOCK_STATEMENT
           //
           // Skip method "bodies" that are not blocks
           || parentType == HaxeTokenTypes.RETURN_STATEMENT
           || parentType == HaxeTokenTypes.IF_STATEMENT
           || parentType == HaxeTokenTypes.TRY_STATEMENT
           || parentType == HaxeTokenTypes.SWITCH_STATEMENT
           || parentType == HaxeTokenTypes.WHILE_STATEMENT
           || parentType == HaxeTokenTypes.DO_WHILE_STATEMENT
           || parentType == HaxeTokenTypes.FOR_STATEMENT
           || parentType == HaxeTokenTypes.THROW_STATEMENT
          //
           || parentType == HaxeTokenTypes.THIS_EXPRESSION
           || parentType == HaxeTokenTypes.SUPER_EXPRESSION
           || parentType == HaxeTokenTypes.ASSIGN_EXPRESSION;
  }

  private boolean docsConditionalOrSpacing(IElementType type) {
    if (HaxeTokenTypeSets.WHITESPACES.contains(type)) return true;
    if (HaxeTokenTypeSets.ONLY_COMMENTS.contains(type)) return true;
    if (HaxeTokenTypeSets.CONDITIONALLY_NOT_COMPILED.contains(type)) return true;
    return false;
  }

}
