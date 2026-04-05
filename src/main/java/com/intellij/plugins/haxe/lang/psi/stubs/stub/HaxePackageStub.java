package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub for package statements.
 * Stores the fully qualified package name so it can be retrieved without parsing.
 */
public class HaxePackageStub extends StubBase<HaxePackageStatement> {

  private final String packageName;

  public HaxePackageStub(StubElement parent,
                         @NotNull IStubElementType elementType,
                         @Nullable String packageName) {
    super(parent, elementType);
    this.packageName = packageName;
  }

  @Nullable
  public String getPackageName() {
    return packageName;
  }
}

