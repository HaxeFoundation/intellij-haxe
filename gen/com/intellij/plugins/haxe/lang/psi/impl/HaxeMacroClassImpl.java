// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeMacroClassImpl extends HaxePsiCompositeElementImpl implements HaxeMacroClass {

  public HaxeMacroClassImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitMacroClass(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public HaxeAutoBuildMacro getAutoBuildMacro() {
    return findChildByClass(HaxeAutoBuildMacro.class);
  }

  @Override
  @Nullable
  public HaxeBitmapMeta getBitmapMeta() {
    return findChildByClass(HaxeBitmapMeta.class);
  }

  @Override
  @Nullable
  public HaxeBuildMacro getBuildMacro() {
    return findChildByClass(HaxeBuildMacro.class);
  }

  @Override
  @Nullable
  public HaxeCustomMeta getCustomMeta() {
    return findChildByClass(HaxeCustomMeta.class);
  }

  @Override
  @Nullable
  public HaxeFakeEnumMeta getFakeEnumMeta() {
    return findChildByClass(HaxeFakeEnumMeta.class);
  }

  @Override
  @Nullable
  public HaxeJsRequireMeta getJsRequireMeta() {
    return findChildByClass(HaxeJsRequireMeta.class);
  }

  @Override
  @Nullable
  public HaxeMetaMeta getMetaMeta() {
    return findChildByClass(HaxeMetaMeta.class);
  }

  @Override
  @Nullable
  public HaxeNativeMeta getNativeMeta() {
    return findChildByClass(HaxeNativeMeta.class);
  }

  @Override
  @Nullable
  public HaxeNsMeta getNsMeta() {
    return findChildByClass(HaxeNsMeta.class);
  }

  @Override
  @Nullable
  public HaxeRequireMeta getRequireMeta() {
    return findChildByClass(HaxeRequireMeta.class);
  }

  @Override
  @Nullable
  public HaxeSimpleMeta getSimpleMeta() {
    return findChildByClass(HaxeSimpleMeta.class);
  }

}
