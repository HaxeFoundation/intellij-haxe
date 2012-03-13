// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeExternClassDeclarationBodyPart;
import com.intellij.plugins.haxe.lang.psi.HaxeExternFunctionDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxePp;
import com.intellij.plugins.haxe.lang.psi.HaxeVarDeclaration;
import org.jetbrains.annotations.Nullable;

public class HaxeExternClassDeclarationBodyPartImpl extends HaxePsiCompositeElementImpl implements HaxeExternClassDeclarationBodyPart {

  public HaxeExternClassDeclarationBodyPartImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeExternFunctionDeclaration getExternFunctionDeclaration() {
    return findChildByClass(HaxeExternFunctionDeclaration.class);
  }

  @Override
  @Nullable
  public HaxePp getPp() {
    return findChildByClass(HaxePp.class);
  }

  @Override
  @Nullable
  public HaxeVarDeclaration getVarDeclaration() {
    return findChildByClass(HaxeVarDeclaration.class);
  }
}
