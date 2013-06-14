package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.actions.HaxeTypeAddImportIntentionAction;
import com.intellij.plugins.haxe.ide.index.HaxeComponentIndex;
import com.intellij.plugins.haxe.lang.psi.HaxeComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.lang.psi.HaxeVisitor;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeTypeAnnotator extends HaxeVisitor implements Annotator {
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
  public void visitType(@NotNull HaxeType type) {
    super.visitType(type);
    final HaxeReferenceExpression expression = type.getReferenceExpression();
    if (expression.resolve() != null) {
      return;
    }

    tryCreateAnnotation(expression);
  }

  @Override
  public void visitReferenceExpression(@NotNull HaxeReferenceExpression expression) {
    super.visitReferenceExpression(expression);

    if (expression.resolve() == null) {
      tryCreateAnnotation(expression);
    }
  }

  private void tryCreateAnnotation(HaxeReferenceExpression expression) {
    final GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(expression);
    final List<HaxeComponent> components =
      HaxeComponentIndex.getItemsByName(expression.getText(), expression.getProject(), scope);
    if (!components.isEmpty()) {
      myHolder.createErrorAnnotation(expression, HaxeBundle.message("haxe.unresolved.type"))
        .registerFix(new HaxeTypeAddImportIntentionAction(expression, components));
    }
  }
}
