// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeTypeList extends HaxePsiCompositeElement {

  @Nullable
  HaxeAnonymousType getAnonymousType();

  @Nullable
  HaxeType getType();

  @Nullable
  HaxeTypeConstraint getTypeConstraint();

  @NotNull
  List<HaxeTypeList> getTypeListList();

}
