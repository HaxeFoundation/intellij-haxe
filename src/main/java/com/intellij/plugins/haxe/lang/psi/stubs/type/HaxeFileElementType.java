package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeImportHxStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFileStub;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubVersions;
import com.intellij.psi.*;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.util.io.StringRef;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

@CustomLog
public class HaxeFileElementType extends IStubFileElementType<HaxeFileStub> {

  public HaxeFileElementType() {
    super("HAXEFILE", HaxeLanguage.INSTANCE);
  }

  /**
   * Pluggable readiness gate invoked once per stub creation. Default is a no-op;
   * production replaces it from {@code HaxelibProjectStartActivity} with a call
   * to {@code HaxeDefineDetectionManager.awaitReady}.
   *
   * <p>Static because the stub builder runs at index time with no convenient
   * Project handle; the gate decides for itself how to look up the relevant
   * manager. Volatile so the production replacement is visible across all
   * indexing pool threads without a happens-before story.
   */
  public static volatile Runnable READINESS_GATE = () -> {};

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
    dataStream.writeName(stub.getFileName());
  }

  @NotNull
  @Override
  public HaxeFileStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef stringRef = dataStream.readName();
    return new HaxeFileStub(null, stringRef.getString());
  }

  @Override
  public void indexStub(@NonNull HaxeFileStub stub, @NotNull IndexSink sink) {
    if("import.hx".equals(stub.getFileName())) {
      sink.occurrence(HaxeImportHxStubIndex.KEY, stub.getPackageName());
    }
  }

  @Override
  public StubBuilder getBuilder() {
    return new DefaultStubBuilder() {
      @NotNull
      @Override
      protected PsiFileStub<?> createStubForFile(@NotNull PsiFile file) {
        READINESS_GATE.run();
        String name = file.getName();

        if (file instanceof HaxeFile haxeFile) {
          return new HaxeFileStub(haxeFile, name);
        }
        return new HaxeFileStub(null, name);
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
