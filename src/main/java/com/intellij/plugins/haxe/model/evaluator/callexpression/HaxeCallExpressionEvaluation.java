package com.intellij.plugins.haxe.model.evaluator.callexpression;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.PsiElement;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    ResultHolder callie;
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
        return callExpressionResolver.resolve(returnType);
    }
    public ResultHolder getCallie() {
        return callExpressionResolver.resolve(callie);
    }

    public HaxeCallExpressionEvaluation validationFailed() {
        valid = false;
        return this;
    }



}
