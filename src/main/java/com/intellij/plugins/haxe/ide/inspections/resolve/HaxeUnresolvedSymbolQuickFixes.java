package com.intellij.plugins.haxe.ide.inspections.resolve;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.plugins.haxe.ide.inspections.intentions.*;
import com.intellij.plugins.haxe.ide.quickfix.HaxeIntroduceTypeInModuleQuickFix;
import com.intellij.plugins.haxe.ide.quickfix.HaxeIntroduceTypeNewFileQuickFix;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeAbstractClassModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HaxeUnresolvedSymbolQuickFixes {


  public static LocalQuickFix createFunctionQuickfix(@NotNull HaxeCallExpression expression) {
    return new HaxeIntroduceFunctionIntention(expression);
  }

  public static LocalQuickFix createMethodQuickfix(@NotNull HaxeCallExpression expression,  @NotNull HaxeClass targetClass) {
    return new HaxeIntroduceMethodIntention(expression, targetClass);
  }
  public static LocalQuickFix createMethodQuickfix(@NotNull SpecificFunctionReference functionReference, HaxeReferenceExpression referenceExpression, @NotNull HaxeClass targetClass) {
    return new HaxeIntroduceMethodFromTypeIntention(functionReference,referenceExpression, targetClass);
  }
  public static LocalQuickFix createEnumValueQuickfix(HaxeReferenceExpression reference, HaxeClass targetClass) {
    if(reference.getParent() instanceof HaxeType) return null;
    return new HaxeIntroduceEnumValueIntention(reference, targetClass);
  }

  public static List<LocalQuickFix> createMethodQuickfixesForCallable(HaxeReferenceExpression reference, @NotNull HaxeClassModel abstractType, HaxeClass targetClass) {
    List<LocalQuickFix> localQuickFixes = new ArrayList<>();
    if(abstractType instanceof HaxeAbstractClassModel model) {
      HaxeGenericResolver genericResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(reference);
      List<SpecificTypeReference> fromTypes = model.getDirectCastFromTypes(genericResolver);
      for (SpecificTypeReference fromType : fromTypes) {
        if(fromType instanceof SpecificHaxeClassReference classReference) {
          if(classReference.isTypeDef()) {
            fromType = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
          }
        }
        if(fromType instanceof SpecificFunctionReference functionReference) {
          localQuickFixes.add(new HaxeIntroduceMethodFromTypeIntention(functionReference, reference, targetClass));
        }
      }
    }

    if(localQuickFixes.isEmpty()) {
      localQuickFixes.add(new HaxeIntroduceMethodForCallableIntention(reference, targetClass));
    }
    return localQuickFixes;
  }


  public static LocalQuickFix createLocalVarQuickfix(@NotNull HaxeReferenceExpression expression) {
    // do not suggest local var if reference is a chained expression
    if(expression.getParent() instanceof HaxeReferenceExpression) return null;
    if(expression.textContains('.')) return null;
    // prevent in init expressions that are not inside code blocks
    if (PsiTreeUtil.getParentOfType(expression, HaxeFieldDeclaration.class) != null) return null;
    if (PsiTreeUtil.getParentOfType(expression, HaxePropertyDeclaration.class) != null) return null;

    return new HaxeIntroduceVariableIntention(expression);
  }

  public static LocalQuickFix createFieldQuickfix(@NotNull HaxeReferenceExpression expression,  @NotNull HaxeClass targetClass) {
    if(expression.getParent() instanceof HaxeType) return null;
    return new HaxeIntroduceFieldIntention(expression, targetClass);

  }

  public static List<LocalQuickFix> createTypeQuickFixes(@NotNull HaxeReferenceExpression expression) {
    return List.of(
            new HaxeIntroduceTypeInModuleQuickFix(expression.getIdentifier()),
            new HaxeIntroduceTypeNewFileQuickFix(expression.getIdentifier())
    );
  }


  public static  LocalQuickFix createMethodParameterQuickfix(HaxeReferenceExpression reference) {
    // prevent if not in Method declaration
    HaxeMethodDeclaration declaration = PsiTreeUtil.getParentOfType(reference, HaxeMethodDeclaration.class);
    if(declaration == null) return null;

    return new HaxeIntroduceMethodParameterIntention(reference, declaration);
  }


}
