package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeAbstractClassModel;
import com.intellij.plugins.haxe.model.HaxeEnumModel;
import com.intellij.plugins.haxe.model.fixer.HaxeExpressionConversionFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeRemoveElementFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeTypeTagChangeFixer;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.model.type.resolver.ResolveSource;
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
      final ResultHolder initType = getTypeFromVarInit(initExpression, varType);

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
      else if (requireConstant && !isConstant(initType, initExpression)) {
        holder.newAnnotation(HighlightSeverity.ERROR,
                             HaxeBundle.message("haxe.semantic.parameter.default.type.should.be.constant", initType))
          .range(erroredElement)
          .create();
      }
    }

    private static boolean isConstant(ResultHolder initType, HaxeVarInit initExpression) {
      SpecificTypeReference specificTypeReference = initType.getType();

      if (specificTypeReference.isEnumValue()) {
        return true; // enum values are constants
      }
      if (initExpression.getExpression() instanceof HaxeReference reference) {
        PsiElement resolve = reference.resolve();
        if (resolve instanceof  HaxeFieldDeclaration fieldDeclaration) {
          if(fieldDeclaration.getModel()!= null  ) {
            // any member inside abstracts/enums seems to be constants afaik.
            if (fieldDeclaration.getModel().getExhibitor() instanceof HaxeAbstractClassModel) return true;
            if (fieldDeclaration.getModel().getExhibitor() instanceof HaxeEnumModel) return true;
          }
          if (fieldDeclaration.getVarInit() == null) return false; // no constant

          List<HaxeFieldModifier> list = fieldDeclaration.getFieldModifierList();
          return  fieldDeclaration.getMutabilityModifier().textMatches(HaxePsiModifier.FINAL) ||
            list.stream().anyMatch(modifier -> modifier.textMatches(HaxePsiModifier.INLINE));

          // since the compiler seems to do some evaluation of expressions we can use a simple const check here
          // ex :
          //  static inline var myConstantA = "123" + "456" + "789"; // WONT work (tested on haxe 4.3)
          //  static inline var myConstantB = ((Std.int(2.0) + 10 / 3) - (2 * 1)); // works even though using "Std.int" (tested on haxe 4.3)

          //ResultHolder init = getTypeFromVarInit(fieldDeclaration.getVarInit());
          //return canBeConstant && init.getType().getConstant() != null;
        }
      }
      return specificTypeReference.getConstant() != null;
    }

    @NotNull
    public static ResultHolder getTypeFromVarInit(@NotNull HaxeVarInit init, ResultHolder assignType) {
      HaxeExpression initExpression = init.getExpression();
      HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(initExpression);

      final ResultHolder abstractEnumFieldInitType = HaxeAbstractEnumUtil.getStaticMemberExpression(initExpression, resolver);
      if (abstractEnumFieldInitType != null) {
        return abstractEnumFieldInitType;
      }
      if (assignType != null) {
        resolver.add("", assignType, ResolveSource.ASSIGN_TYPE);
      }

      // fallback to simple init expression
      return null != initExpression ? HaxeTypeResolver.getPsiElementType(initExpression, init, resolver)
                                    : SpecificTypeReference.getInvalid(init).createHolder();
    }
  }


}
