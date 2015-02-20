// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeSwitchBlock extends HaxePsiCompositeElement {

  @Nullable
  HaxeDefaultCase getDefaultCase();

  @NotNull
  List<HaxeSwitchCase> getSwitchCaseList();

}
