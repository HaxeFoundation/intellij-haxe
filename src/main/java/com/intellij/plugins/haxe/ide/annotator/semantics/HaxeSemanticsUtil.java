package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeAbstractClassModel;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.HaxeEnumModel;
import com.intellij.plugins.haxe.model.evaluator.assign.AssignExplanation;
import com.intellij.plugins.haxe.model.evaluator.assign.HaxeAssignEvaluation;
import com.intellij.plugins.haxe.model.fixer.HaxeExpressionConversionFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeRemoveElementFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeTypeTagChangeFixer;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeAbstractEnumUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation.*;

public class HaxeSemanticsUtil {
  public static class TypeTagChecker {
    public static void check(
      final PsiElement erroredElement,
      final HaxeTypeTag tag,
      final HaxeVarInit initExpression,
      boolean requireConstant,
      final AnnotationHolder holder
    ) {
      if (isTypeFromMacroVar(tag))return; // ignore if  macro variable name

      final ResultHolder varType = HaxeTypeResolver.getTypeFromTypeTag(tag, erroredElement);
      final ResultHolder initType = getTypeFromVarInit(initExpression, varType);
      if (initType.isInvalid()) return;
        checkNullAssignForNonNullableType(holder, initExpression, tag, initType, varType);
        HaxeAssignEvaluation assignEvaluation = varType.canAssignEvaluation(initType);
      if (!assignEvaluation.result) {
        AssignExplanation messages = assignEvaluation.explanations;
        if(messages.hasMissingMembers()) {
          typeMismatchMissingMembers(holder, erroredElement, messages).create();
        }else if(messages.hasWrongTypeMembers()) {
          addtypeMismatchWrongTypeMembersAnnotations(holder, erroredElement, messages);
        }else if (messages.hasMissingModel()) {
          typeModelMissing(holder, erroredElement, messages.getMissingModel().getFirst());
        }else{
          PsiElement  element = Optional.ofNullable((PsiElement)initExpression.getExpression()).orElse(erroredElement);
          AnnotationBuilder builder = typeMismatch(holder, element, initType.toStringWithoutConstant(), varType.toStringWithoutConstant());
          if (null != initType.getClassType()) {
            // TODO this also affects parameters, name should reflect type
            boolean isParameter = tag.getParent() instanceof HaxeParameter;
            String message = isParameter
                    ? HaxeBundle.message("haxe.quickfix.change.parameter.type")
                    : HaxeBundle.message("haxe.quickfix.change.variable.type");

            builder.withFix(new HaxeTypeTagChangeFixer(message, tag, initType.getClassType()));
          }

          List<HaxeExpressionConversionFixer> fixes =
            HaxeExpressionConversionFixer.createStdTypeFixers(initExpression.getExpression(), initType.getType(), varType.getType());
          builder.withFix(new HaxeRemoveElementFixer(HaxeBundle.message("haxe.quickfix.remove.initializer"), initExpression));
          fixes.forEach(builder::withFix);
          builder.create();
        }


      }
      else if (requireConstant && !isConstant(initType, initExpression)) {
        holder.newAnnotation(HighlightSeverity.ERROR,
                             HaxeBundle.message("haxe.semantic.parameter.default.type.should.be.constant", initType))
          .range(erroredElement)
          .create();
      }
    }



      private static boolean isTypeFromMacroVar(HaxeTypeTag tag) {
      if (tag.getTypeOrAnonymous() != null) {
        return tag.getTypeOrAnonymous().getText().startsWith("$");
      }
      return false;
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
        resolver.setAssignHint(assignType);
      }

      // fallback to simple init expression
      return null != initExpression ? HaxeTypeResolver.getPsiElementType(initExpression, init, resolver)
                                    : SpecificTypeReference.getInvalid(init).createHolder();
    }
  }

    public static void checkNullAssignForNonNullableType(AnnotationHolder holder, HaxeVarInit initExpression, HaxeTypeTag tag, ResultHolder initType, ResultHolder varType) {
        if (initType.getConstant() instanceof HaxeNull && initExpression.getExpression() != null) {
            if(varType.getType() instanceof SpecificHaxeClassReference classReference && classReference.isNotNullMeta()) {
                String typePresentationString = varType.toPresentationString();
                String nullabilityWarning = HaxeBundle.message("haxe.semantic.incompatible.type.null.warning", typePresentationString);
                holder.newAnnotation(HighlightSeverity.WEAK_WARNING, nullabilityWarning)
                        .withFix(NullWrapTypeFix("Null<"+typePresentationString+">", tag))
                        .range(initExpression.getExpression())
                        .create();
            }
        }
    }
    public static void checkNullAssignForNonNullableType(AnnotationHolder holder, ResultHolder initType, ResultHolder varType, PsiElement rhs) {
        if (initType.getConstant() instanceof HaxeNull) {
            if(varType.getType() instanceof SpecificHaxeClassReference classReference && classReference.isNotNullMeta()) {
                String typePresentationString = varType.toPresentationString();
                String nullabilityWarning = HaxeBundle.message("haxe.semantic.incompatible.type.null.warning", typePresentationString);

                AnnotationBuilder builder = holder.newAnnotation(HighlightSeverity.WEAK_WARNING, nullabilityWarning).range(rhs);

                if(varType.getContext() instanceof  HaxeReferenceExpression referenceExpression) {
                    PsiElement resolve = referenceExpression.resolve();
                    if(resolve instanceof HaxePsiField field) {
                        HaxeTypeTag typeTag = field.getTypeTag();
                        HaxeFixer fix = NullWrapTypeFix("Null<" + typePresentationString + ">", typeTag);
                        builder.withFix(fix);
                    }
                }
                builder.create();
            }
        }
    }

    private static @NotNull HaxeFixer NullWrapTypeFix(String newValue, HaxeTypeTag typeTag) {
        return new HaxeFixer("Replace type with " + newValue) {
            @Override
            public void run() {
                HaxeDocumentModel.fromElement(typeTag).replaceElementText(typeTag, ":" + newValue);
            }

            @Override
            public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
                int startOffset = typeTag.getTextRange().getStartOffset();
                int endOffset = typeTag.getTextRange().getEndOffset();
                editor.getDocument().replaceString(startOffset,endOffset , ":" + newValue);

                return IntentionPreviewInfo.DIFF;
            }
        };
    }

}
