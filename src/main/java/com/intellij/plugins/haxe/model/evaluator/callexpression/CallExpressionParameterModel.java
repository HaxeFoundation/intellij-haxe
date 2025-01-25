package com.intellij.plugins.haxe.model.evaluator.callexpression;

import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.HaxeArgument;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.PsiElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.model.type.HaxeMacroTypeUtil.getTypeFromMacroVarArgOrRestType;

@Getter
@AllArgsConstructor
public class CallExpressionParameterModel {
  @NotNull PsiElement psiElement;
  boolean rest;// vararg
  boolean optional;

  @Nullable SpecificTypeReference initValue;
  @Nullable SpecificTypeReference restType;
  @NotNull SpecificTypeReference type;

  @Nullable String name;

  @Nullable HaxeParameterModel parameterModel;

  public  boolean hasIntiValue() {
    return initValue != null;
  }

  @NotNull
  public static CallExpressionParameterModel fromParameter(HaxeParameterModel model) {
    HaxeParameter psi = model.getParameterPsi();
    boolean rest = model.isRest() || model.isMacroVarArg();
    boolean optional = model.isOptional();

    SpecificTypeReference init = model.hasInit() ? HaxeExpressionEvaluator.evaluate(model.getVarInitPsi()).result.getType() : null;
    SpecificTypeReference type = model.getType().getType();
    SpecificTypeReference restType  = rest ? getTypeFromMacroVarArgOrRestType(type) : null;

    // if parameter does not have a typeTag or init expression we must resolve type from usage
    if(model.isUntyped()) {
      type = HaxeExpressionEvaluator.evaluate(psi).result.getType();
    }



    return new CallExpressionParameterModel(psi, rest, optional, init, restType, type,  model.getName(), model);
  }

  @NotNull
  public static CallExpressionParameterModel fromFunctionArgument(HaxeArgument argument) {
    PsiElement psi = argument.getElement();
    boolean rest = argument.isRest();
    boolean optional = argument.isOptional();

    SpecificTypeReference type = argument.getType().getType();
    SpecificTypeReference restType  = rest ?  getTypeFromMacroVarArgOrRestType(type) : null;

    return new CallExpressionParameterModel(psi, rest, optional, null, restType, type, argument.getName(), null);
  }


}
