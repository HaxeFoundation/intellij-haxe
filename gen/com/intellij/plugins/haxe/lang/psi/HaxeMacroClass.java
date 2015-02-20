// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeMacroClass extends HaxePsiCompositeElement {

  @Nullable
  HaxeAutoBuildMacro getAutoBuildMacro();

  @Nullable
  HaxeBitmapMeta getBitmapMeta();

  @Nullable
  HaxeBuildMacro getBuildMacro();

  @Nullable
  HaxeCustomMeta getCustomMeta();

  @Nullable
  HaxeFakeEnumMeta getFakeEnumMeta();

  @Nullable
  HaxeJsRequireMeta getJsRequireMeta();

  @Nullable
  HaxeMetaMeta getMetaMeta();

  @Nullable
  HaxeNativeMeta getNativeMeta();

  @Nullable
  HaxeNsMeta getNsMeta();

  @Nullable
  HaxeRequireMeta getRequireMeta();

  @Nullable
  HaxeSimpleMeta getSimpleMeta();

}
