// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeClassDeclaration extends PsiIdentifiedElement {

  @Nullable
  public HaxeClassBody getClassBody();

  @Nullable
  public HaxeIdentifier getIdentifier();

  @Nullable
  public HaxeInheritList getInheritList();

  @Nullable
  public HaxeTypeParam getTypeParam();

}
