package com.intellij.plugins.haxe.model.evaluator.callexpression;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeCallExpressionEvaluation {

    @Getter
    @NotNull
    private final List<EvaluationAnnotationData> errors = new ArrayList<>();
    @Getter
    @NotNull
    private final List<EvaluationAnnotationData> warnings = new ArrayList<>();


    @Getter
    @Setter
    private boolean completed = false;
    @Getter
    @Setter
    private boolean valid = true;


    @Getter
    Map<Integer, Integer> argumentToParameterIndex = new HashMap<>();
    @Getter
    Map<Integer, ResultHolder> argumentIndexToType = new HashMap<>();
    Map<Integer, ResultHolder> parameterIndexToType = new HashMap<>();
    Map<Integer, ResultHolder> originalParameterIndexToType = new HashMap<>();

    @Getter List<String> parameterNames = new ArrayList<>();


    // should contain final values after arguments and monomorphs have been evaluated
    @Getter HaxeGenericResolver callExpressionResolver = new HaxeGenericResolver();
    @Getter HaxeGenericResolver callieResolver = new HaxeGenericResolver();
    @Nullable ResultHolder callie;
    ResultHolder returnType;


    public void addError(String message, TextRange textRange) {
        errors.add(new EvaluationAnnotationData(message, textRange));
    }

    public void addError(String message, PsiElement element) {
        errors.add(new EvaluationAnnotationData(message, element.getTextRange()));
    }
    public void addWarning(String message, TextRange textRange) {
        warnings.add(new EvaluationAnnotationData(message, textRange));
    }

    public void addWarning(String message, PsiElement element) {
        warnings.add(new EvaluationAnnotationData(message, element.getTextRange()));
    }

    public void addArgumentToParameterMapping(int argumentIndex,
                                              int parameterIndex,
                                              SpecificTypeReference argumentType,
                                              SpecificTypeReference parameterType,
                                              String parameterName
                                              ) {
        argumentIndexToType.put(argumentIndex, argumentType.createHolder());
        parameterIndexToType.put(parameterIndex, parameterType.createHolder());
        argumentToParameterIndex.put(argumentIndex, parameterIndex);
        parameterNames.add(parameterName);
    }



    public @Nullable ResultHolder getParameterType(int index) {
        return parameterIndexToType.getOrDefault(index, null);
    }

    public @Nullable ResultHolder getArgumentType(int index) {
        return argumentIndexToType.getOrDefault(index, null);
    }

    public List<ResultHolder> getParameterTypes() {
        return List.copyOf(parameterIndexToType.values());
    }

    public Map<Integer, Integer> getArgumentToParameterMapping() {
        return argumentToParameterIndex;
    }

    public int getParameterForArgument(int argumentIndex) {
        return argumentToParameterIndex.getOrDefault(argumentIndex, -1);
    }


    public ResultHolder getReturnType() {
        ResultHolder resolve = callExpressionResolver.resolve(returnType);
        resolve = addMissingTypeParametersIfNecessary(resolve);
        return resolve != null ? resolve : returnType;
    }

    // TODO : HACK
    // when Class<T> is used as arguments and we later use T we might be missing some typeParameters
    // as you do not pass those when you pass a class type. ex with Array `fn<X>(Class<X>){} fn(Array)`
    // when we resolve anything of type X we now only got array and not Array<T>, this method makes sure
    // the return type wont miss any type Params
    // example haxe code that might experience this problem:
    // var newVar = Std.downcast(dynamicValue, Array);

    private ResultHolder addMissingTypeParametersIfNecessary(ResultHolder resolve) {
        if(resolve != null && resolve.getClassType() != null ) {
            SpecificHaxeClassReference classType = resolve.getClassType();
            HaxeClassModel classModel = classType.getHaxeClassModel();
            if(classModel != null) {
                ResultHolder instanceType = classModel.getInstanceType();
                @NotNull ResultHolder[] currentSpecifics = classType.getSpecifics();
                @NotNull ResultHolder[] expectedSpecifics = instanceType.getClassType().getSpecifics();
                if(currentSpecifics.length == 0 &&  expectedSpecifics.length != 0) {
                    return instanceType;
                }else {
                    @NotNull ResultHolder[]  specs = new  ResultHolder[currentSpecifics.length];
                    for (int i = 0; i < currentSpecifics.length; i++) {
                        specs[i] = addMissingTypeParametersIfNecessary(currentSpecifics[i]);
                    }
                    return SpecificHaxeClassReference.withGenerics(classType.getHaxeClassReference(), specs).createHolder();
                }
            }
        }
        return resolve;
    }

    @Nullable
    public ResultHolder getCallie() {
        return callExpressionResolver.resolve(callie);
    }

    public HaxeCallExpressionEvaluation validationFailed() {
        valid = false;
        return this;
    }

    public SpecificFunctionReference getFunctionType(HaxeMethodModel haxeMethod) {
        LinkedList<HaxeArgument> args = new LinkedList<>();
        List<HaxeParameterModel> parameters = haxeMethod.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            HaxeParameterModel param = parameters.get(i);
            args.add(new HaxeArgument(param.getParameterPsi(), i, param.isOptional(), param.isRest(), param.getType(callExpressionResolver), param.getName()));
        }
        return new SpecificFunctionReference(args, getReturnType(),  haxeMethod, haxeMethod.getMethod());
    }
}
