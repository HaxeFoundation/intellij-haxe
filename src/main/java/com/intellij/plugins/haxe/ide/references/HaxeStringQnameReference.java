package com.intellij.plugins.haxe.ide.references;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.util.HaxeQnameResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A string literal whose VALUE is a resolvable fully-qualified name —
 * navigates to the class or member, like the debugger's value links but in
 * source. Soft: prose that happens to be dot-shaped is never an error — the
 * contributor only attaches this reference when the name resolves.
 */
public class HaxeStringQnameReference extends PsiReferenceBase<HaxeStringLiteralExpression> {

  private final String qname;

  public HaxeStringQnameReference(@NotNull HaxeStringLiteralExpression literal, @NotNull TextRange range, @NotNull String qname) {
    super(literal, range, true);
    this.qname = qname;
  }

  @Override
  @Nullable
  public PsiElement resolve() {
    return HaxeQnameResolveUtil.findClassOrMember(qname, getElement().getProject());
  }
}
