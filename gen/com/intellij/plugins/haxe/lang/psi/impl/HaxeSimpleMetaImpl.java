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

public class HaxeSimpleMetaImpl extends HaxePsiCompositeElementImpl implements HaxeSimpleMeta {

  public HaxeSimpleMetaImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitSimpleMeta(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public HaxeBindMeta getBindMeta() {
    return findChildByClass(HaxeBindMeta.class);
  }

  @Override
  @Nullable
  public HaxeCoreApiMeta getCoreApiMeta() {
    return findChildByClass(HaxeCoreApiMeta.class);
  }

  @Override
  @Nullable
  public HaxeFinalMeta getFinalMeta() {
    return findChildByClass(HaxeFinalMeta.class);
  }

  @Override
  @Nullable
  public HaxeHackMeta getHackMeta() {
    return findChildByClass(HaxeHackMeta.class);
  }

  @Override
  @Nullable
  public HaxeKeepMeta getKeepMeta() {
    return findChildByClass(HaxeKeepMeta.class);
  }

  @Override
  @Nullable
  public HaxeMacroMeta getMacroMeta() {
    return findChildByClass(HaxeMacroMeta.class);
  }

  @Override
  @Nullable
  public HaxeUnreflectiveMeta getUnreflectiveMeta() {
    return findChildByClass(HaxeUnreflectiveMeta.class);
  }

}
