package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub for import statements.
 * Stores the qualified path text, whether it has a wildcard, and the alias name (if any).
 */
public class HaxeImportStub extends StubBase<HaxeImportStatement> {

  private final String importPath;
  private final boolean hasWildcard;
  private final String alias;

  public HaxeImportStub(StubElement parent,
                         @NotNull IElementType elementType,
                         @Nullable String importPath,
                         boolean hasWildcard,
                         @Nullable String alias) {
    super(parent, elementType);
    this.importPath = importPath;
    this.hasWildcard = hasWildcard;
    this.alias = alias;
  }

  @Nullable
  public String getImportPath() {
    return importPath;
  }

  public boolean hasWildcard() {
    return hasWildcard;
  }

  @Nullable
  public String getAlias() {
    return alias;
  }
}

