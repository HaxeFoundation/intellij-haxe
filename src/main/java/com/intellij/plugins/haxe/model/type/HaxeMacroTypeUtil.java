package com.intellij.plugins.haxe.model.type;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeGenericListPart;
import com.intellij.plugins.haxe.lang.psi.HaxeGenericParam;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ParameterizedCachedValue;
import org.jetbrains.annotations.NotNull;

public class HaxeMacroTypeUtil {

  private static final  Key<ParameterizedCachedValue<HaxeClass, PsiElement>> EXPR_OF_KEY = Key.create("EXPR_OF_KEY");
  private static final  Key<ParameterizedCachedValue<HaxeClass, PsiElement>> EXPR_KEY = Key.create("EXPR_KEY");
  private static final  Key<ParameterizedCachedValue<HaxeClass, PsiElement>> COMPLEX_TYPE_KEY = Key.create("COMPLEX_TYPE_KEY");
  private static final  Key<ParameterizedCachedValue<HaxeClass, PsiElement>> TYPE_DEFINITION_KEY = Key.create("TYPE_DEFINITION_KEY");

  public static final String EXPR_OF = "haxe.macro.Expr.ExprOf";
  public static final String EXPR = "haxe.macro.Expr";
  public static final String COMPLEX_TYPE = "haxe.macro.Expr.ComplexType";
  public static final String TYPE_DEFINITION = "haxe.macro.Expr.TypeDefinition";


  public static SpecificTypeReference extractTypeFromExprOf(SpecificHaxeClassReference haxeClassReference) {
    if(haxeClassReference == null) return null;
    HaxeClass aClass = haxeClassReference.getHaxeClass();
    if(aClass != null) {
      if (aClass.getQualifiedName().equals(HaxeMacroTypeUtil.EXPR_OF)
          // TODO : TEMP hack since typeDef is resolved and `ExprOf` is typedef of `Expr`
          || aClass.getQualifiedName().equals(HaxeMacroTypeUtil.EXPR)) {
        HaxeGenericResolver resolver = haxeClassReference.getGenericResolver();
        HaxeGenericParam genericParam = aClass.getGenericParam();
        if(genericParam != null) {
          HaxeGenericListPart part = genericParam.getGenericListPartList().get(0);
          ResultHolder resolve = resolver.resolve(part);
          if (resolve != null && !resolve.isUnknown()) {
            return resolve.getType();
          }
        }
      }
    }
    return null;
  }

  public static SpecificTypeReference getExpr(@NotNull PsiElement context) {
    HaxeClass classByQName = getCachedExpr(context, context.getProject());
    HaxeClassReference reference = classByQName != null
                                   ? new HaxeClassReference(classByQName.getModel(), context)
                                   : new HaxeClassReference(EXPR, context);

    return SpecificHaxeClassReference.withoutGenerics(reference);
  }

  public static SpecificTypeReference getExprOf(@NotNull PsiElement context, @NotNull ResultHolder specific) {
    HaxeClass classByQName = getCachedExprOf(context, context.getProject());
    HaxeClassReference reference = classByQName != null
                                   ? new HaxeClassReference(classByQName.getModel(), context)
                                   : new HaxeClassReference(EXPR_OF, context);

    return SpecificHaxeClassReference.withGenerics(reference, new ResultHolder[]{specific});
  }


  public static SpecificTypeReference getComplexType(@NotNull PsiElement context) {
    HaxeClass classByQName = getCachedComplexType(context, context.getProject());
    HaxeClassReference reference = classByQName != null
                                   ? new HaxeClassReference(classByQName.getModel(), context)
                                   : new HaxeClassReference(COMPLEX_TYPE, context);

    return SpecificHaxeClassReference.withoutGenerics(reference);
  }

  public static SpecificTypeReference getTypeDefinition(@NotNull PsiElement context) {
    HaxeClass classByQName = getCachedTypeDefinition(context, context.getProject());
    HaxeClassReference reference = classByQName != null
                                   ? new HaxeClassReference(classByQName.getModel(), context)
                                   : new HaxeClassReference(TYPE_DEFINITION, context);

    return SpecificHaxeClassReference.withoutGenerics(reference);
  }




  private static HaxeClass getCachedExprOf(@NotNull PsiElement context, Project project) {
    return CachedValuesManager.getManager(project).getParameterizedCachedValue(project, EXPR_OF_KEY, c -> {
      HaxeClass haxeClass = HaxeResolveUtil.findClassByQName(EXPR_OF, c);
      return new CachedValueProvider.Result<>(haxeClass, ModificationTracker.EVER_CHANGED);
    }, false, context);
  }


  private static HaxeClass getCachedExpr(@NotNull PsiElement context, Project project) {
    return CachedValuesManager.getManager(project).getParameterizedCachedValue(project, EXPR_KEY, c -> {
      HaxeClass haxeClass = HaxeResolveUtil.findClassByQName(EXPR, c);
      return new CachedValueProvider.Result<>(haxeClass, ModificationTracker.EVER_CHANGED);
    }, false, context);
  }

  private static HaxeClass getCachedComplexType(@NotNull PsiElement context, Project project) {
    return CachedValuesManager.getManager(project).getParameterizedCachedValue(project, COMPLEX_TYPE_KEY, c -> {
      HaxeClass haxeClass = HaxeResolveUtil.findClassByQName(COMPLEX_TYPE, c);
      return new CachedValueProvider.Result<>(haxeClass, ModificationTracker.EVER_CHANGED);
    }, false, context);
  }
  private static HaxeClass getCachedTypeDefinition(@NotNull PsiElement context, Project project) {
    return CachedValuesManager.getManager(project).getParameterizedCachedValue(project, TYPE_DEFINITION_KEY, c -> {
      HaxeClass haxeClass = HaxeResolveUtil.findClassByQName(TYPE_DEFINITION, c);
      return new CachedValueProvider.Result<>(haxeClass, ModificationTracker.EVER_CHANGED);
    }, false, context);
  }


}
