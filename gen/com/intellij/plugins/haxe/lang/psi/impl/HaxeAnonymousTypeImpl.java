// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeAnonymousTypeImpl extends HaxeFunctionTypeImpl implements HaxeAnonymousType {

  public HaxeAnonymousTypeImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeAnonymousTypeFieldList getAnonymousTypeFieldList() {
    return findChildByClass(HaxeAnonymousTypeFieldList.class);
  }

  @Override
  @Nullable
  public HaxeTypeExtends getTypeExtends() {
    return findChildByClass(HaxeTypeExtends.class);
  }

  @Override
  @Nullable
  public HaxeVarDeclarationList getVarDeclarationList() {
    return findChildByClass(HaxeVarDeclarationList.class);
  }

}
