// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeAnonymousTypeBodyImpl extends HaxePsiCompositeElementImpl implements HaxeAnonymousTypeBody {

  public HaxeAnonymousTypeBodyImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeAnonymousTypeFieldList getAnonymousTypeFieldList() {
    return findChildByClass(HaxeAnonymousTypeFieldList.class);
  }

  @Override
  @Nullable
  public HaxeInterfaceBody getInterfaceBody() {
    return findChildByClass(HaxeInterfaceBody.class);
  }

  @Override
  @Nullable
  public HaxeTypeExtends getTypeExtends() {
    return findChildByClass(HaxeTypeExtends.class);
  }

}
