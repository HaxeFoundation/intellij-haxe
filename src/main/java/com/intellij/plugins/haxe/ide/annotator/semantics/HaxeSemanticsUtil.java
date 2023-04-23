package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.fixer.HaxeExpressionConversionFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeRemoveElementFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeTypeTagChangeFixer;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeAbstractEnumUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation.typeMismatch;

public class HaxeSemanticsUtil {
  public static class TypeTagChecker {
    public static void check(
      final PsiElement erroredElement,
      final HaxeTypeTag tag,
      final HaxeVarInit initExpression,
      boolean requireConstant,
      final AnnotationHolder holder
    ) {
      final ResultHolder varType = HaxeTypeResolver.getTypeFromTypeTag(tag, erroredElement);
      final ResultHolder initType = getTypeFromVarInit(initExpression);

      if (!varType.canAssign(initType)) {

        AnnotationBuilder builder =
          typeMismatch(holder, erroredElement, initType.toStringWithoutConstant(), varType.toStringWithoutConstant());
        if (null != initType.getClassType()) {
          builder.withFix(
            new HaxeTypeTagChangeFixer(HaxeBundle.message("haxe.quickfix.change.variable.type"), tag, initType.getClassType()));
        }

        List<HaxeExpressionConversionFixer> fixes =
          HaxeExpressionConversionFixer.createStdTypeFixers(initExpression.getExpression(), initType.getType(), varType.getType());
        builder.withFix(new HaxeRemoveElementFixer(HaxeBundle.message("haxe.quickfix.remove.initializer"), initExpression));
        fixes.forEach(builder::withFix);
        builder.create();
      }
      else if (requireConstant && initType.getType().getConstant() == null) {
        holder.newAnnotation(HighlightSeverity.ERROR,
                             HaxeBundle.message("haxe.semantic.parameter.default.type.should.be.constant", initType))
          .range(erroredElement)
          .create();
      }
    }

    @NotNull
    private static ResultHolder getTypeFromVarInit(@NotNull HaxeVarInit init) {
      HaxeExpression initExpression = init.getExpression();
      HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(initExpression);

      final ResultHolder abstractEnumFieldInitType = HaxeAbstractEnumUtil.getStaticMemberExpression(initExpression, resolver);
      if (abstractEnumFieldInitType != null) {
        return abstractEnumFieldInitType;
      }

      // fallback to simple init expression
      return null != initExpression ? HaxeTypeResolver.getPsiElementType(initExpression, init, resolver)
                                    : SpecificTypeReference.getInvalid(init).createHolder();
    }
  }


}
