// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxePpIfValue extends HaxePsiCompositeElement {

  @NotNull
  List<HaxeExpression> getExpressionList();

  @NotNull
  HaxePpElse getPpElse();

  @NotNull
  List<HaxePpElseIf> getPpElseIfList();

  @Nullable
  HaxePpEnd getPpEnd();

  @NotNull
  HaxePpIf getPpIf();

}
