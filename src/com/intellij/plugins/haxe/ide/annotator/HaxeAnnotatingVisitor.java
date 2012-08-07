package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.HaxeVisitor;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeAnnotatingVisitor extends HaxeVisitor implements Annotator {
  private static final Set<String> BUILTIN = new THashSet<String>(Arrays.asList(
    "trace", "__call__", "__vmem_set__", "__vmem_get__", "__vmem_sign__", "__global__", "_global", "__foreach__"
  ));
  private AnnotationHolder myHolder = null;

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    assert myHolder == null;
    myHolder = holder;
    try {
      element.accept(this);
    }
    finally {
      myHolder = null;
    }
  }

  @Override
  public void visitReference(@NotNull HaxeReference reference) {
    if (reference.getTokenType() != HaxeTokenTypes.HAXE_REFERENCE_EXPRESSION) {
      return; // call, array access, this, literal, etc
    }
    final HaxeReference leftSiblingReference = HaxeResolveUtil.getLeftReference(reference);
    final PsiElement referenceTarget = reference.resolve();
    if (referenceTarget != null) {
      return; // OK
    }

    if (BUILTIN.contains(reference.getText()) &&
        reference.getParent() instanceof HaxeCallExpression &&
        !(reference.getParent().getParent() instanceof HaxeReference)) {
      return;
    }

    if (!(reference.getParent() instanceof HaxeReference) && !(reference.getParent() instanceof HaxePackageStatement)) {
      // whole reference expression
      myHolder.createErrorAnnotation(reference, HaxeBundle.message("cannot.resolve.reference"));
    }
    final PsiElement leftSiblingReferenceTarget = leftSiblingReference == null ? null : leftSiblingReference.resolve();
    if (leftSiblingReference != null && leftSiblingReferenceTarget == null) {
      return; // already bad
    }

    // check all parents (ex. com.reference.Bar)
    PsiElement parent = reference.getParent();
    while (parent instanceof HaxeReference) {
      if (((HaxeReference)parent).resolve() != null) return;
      parent = parent.getParent();
    }

    if (parent instanceof HaxePackageStatement) {
      return; // package
    }

    myHolder.createErrorAnnotation(reference, HaxeBundle.message("cannot.resolve.reference"));
  }
}
