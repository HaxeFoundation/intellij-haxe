// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeTopLevelDeclarationImpl extends HaxePsiCompositeElementImpl implements HaxeTopLevelDeclaration {

  public HaxeTopLevelDeclarationImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeClassDeclaration getClassDeclaration() {
    return findChildByClass(HaxeClassDeclaration.class);
  }

  @Override
  @Nullable
  public HaxeEnumDeclaration getEnumDeclaration() {
    return findChildByClass(HaxeEnumDeclaration.class);
  }

  @Override
  @Nullable
  public HaxeInterfaceDeclaration getInterfaceDeclaration() {
    return findChildByClass(HaxeInterfaceDeclaration.class);
  }

  @Override
  @Nullable
  public HaxeTypedefDeclaration getTypedefDeclaration() {
    return findChildByClass(HaxeTypedefDeclaration.class);
  }

}
