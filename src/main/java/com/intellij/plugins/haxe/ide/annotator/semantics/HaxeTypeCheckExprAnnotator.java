package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeCheckExpr;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeOrAnonymous;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections.INCOMPATIBLE_TYPE_CHECKS;

public class HaxeTypeCheckExprAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxeTypeCheckExpr typeCheckExpr) {
      check(typeCheckExpr, holder);
    }
  }

  public static void check(final HaxeTypeCheckExpr expr, final AnnotationHolder holder) {
    if (!INCOMPATIBLE_TYPE_CHECKS.isEnabled(expr)) return;

    final PsiElement[] children = expr.getChildren();
    if (children.length == 2) {
      final HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(expr);
      final ResultHolder statementResult = HaxeTypeResolver.getPsiElementType(children[0], expr, resolver);
      ResultHolder assertionResult = SpecificTypeReference.getUnknown(expr).createHolder();
      if (children[1] instanceof HaxeTypeOrAnonymous) {
        assertionResult = HaxeTypeResolver.getTypeFromTypeOrAnonymous((HaxeTypeOrAnonymous)children[1]);
        ResultHolder resolveResult = resolver.resolve(assertionResult.getType().toStringWithoutConstant());
        if (null != resolveResult) {
          assertionResult = resolveResult;
        }
      }
      if (!assertionResult.canAssign(statementResult)) {
        final HaxeDocumentModel document = HaxeDocumentModel.fromElement(expr);
        holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.statement.does.not.unify.with.asserted.type",
                                                                         statementResult.getType().toStringWithoutConstant(),
                                                                         assertionResult.getType().toStringWithoutConstant()))
          .range(children[0])
          .withFix(new HaxeFixer(HaxeBundle.message("haxe.quickfix.remove.type.check")) {
            @Override
            public void run() {
              document.replaceElementText(expr, children[0].getText());
            }
          })
          .withFix(
            new HaxeFixer(HaxeBundle.message("haxe.quickfix.change.type.check.to.0", statementResult.toStringWithoutConstant())) {
              @Override
              public void run() {
                document.replaceElementText(children[1], statementResult.toStringWithoutConstant());
              }
            })
          .create();
        // TODO: Add type conversion fixers. (eg. Wrap with Std.int(), wrap with Std.toString())
      }
    }
  }
}
