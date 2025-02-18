package com.intellij.plugins.haxe.model.evaluator.callexpression;

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.model.evaluator.assign.AssignExplanation;
import com.intellij.plugins.haxe.model.evaluator.assign.HaxeAssignEvaluation;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeTypeCompatible.evaluateAssignToFrom;
import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeTypeCompatible.evaluateAssignToFromForNewAndCallExpression;

public class HaxeCallExpressionContext {

    private static final RecursionGuard<RecursionKey> canAssignRecursionGuard = RecursionManager.createGuard("canAssignRecursionGuard");


    private record RecursionKey(PsiElement argumentContext, PsiElement parameterContext){}

    @NotNull
    final List<CallExpressionArgumentModel> arguments;
    @NotNull
    final List<CallExpressionParameterModel> parameters;
    @Nullable
    final ResultHolder returnType;

    @NotNull
    final HaxeGenericResolver callExpressionScopeResolver;
    final HaxeGenericResolver methodResolver;

    @Setter
    @Nullable
    SpecificHaxeClassReference callie;
    SpecificTypeReference assignHint;

    @Nullable
    private PsiElement sourceExpression;

    public boolean isConstructor = false;
    public boolean isMacroFunction = false;
    public boolean isStaticExtension = false;

    /**
     *
     * @param argumentList  list of arguments passed to the function/method beeing called
     * @param parameterList list of parameters that the function /method has declared
     * @param returnType returntype for the method/function
     * @param callExpressionScopeResolver should contain typeParameters from the callExpression surroundings
     * @param methodResolver should contain typeParameters for the method/function being called (if any)
     */
    public HaxeCallExpressionContext(@NotNull List<CallExpressionArgumentModel> argumentList,
                                     @NotNull List<CallExpressionParameterModel> parameterList,
                                     @Nullable ResultHolder returnType,
                                     @Nullable HaxeGenericResolver callExpressionScopeResolver,
                                     @Nullable HaxeGenericResolver methodResolver
    ) {
        this.arguments = argumentList;
        this.parameters = parameterList;
        this.returnType = returnType;
        this.methodResolver = methodResolver != null ? methodResolver : new HaxeGenericResolver();
        this.callExpressionScopeResolver = callExpressionScopeResolver != null ? callExpressionScopeResolver : new HaxeGenericResolver();
    }

    @NotNull
    public HaxeCallExpressionEvaluation evaluate() {
        return evaluate(false, null);
    }

    @NotNull
    public HaxeCallExpressionEvaluation evaluateWithAnnotationData(@NotNull PsiElement callExpression) {
        return evaluate(true, callExpression);
    }



    /**
     * @param trackErrors      makes a list of elements errors and the psi elements causing them
     *                         default is  false as its unnecessary overhead for most evaluations
     * @param sourceExpression the CallExpression or NewExpression  is required for correct error ranges
     */
    @NotNull
    private HaxeCallExpressionEvaluation evaluate(boolean trackErrors, PsiElement sourceExpression) {
        HaxeCallExpressionEvaluation evaluation = new HaxeCallExpressionEvaluation();
        this.sourceExpression = sourceExpression;
        evaluation.returnType =  returnType;
        evaluation.callie =  callie != null ? callie.createHolder() : null;
        evaluation.callExpressionResolver = new HaxeGenericResolver();
        evaluation.callExpressionResolver.addAll(callExpressionScopeResolver);

        boolean firstArgIsThisReference = isStaticExtension || isMacroFunction;
        boolean hasRestParam = hasRestParameter(parameters);

        int minArgRequired = countRequiredArguments(parameters) - (firstArgIsThisReference ? 1 : 0);
        int maxArgAllowed = hasRestParam ? Integer.MAX_VALUE : parameters.size() - (firstArgIsThisReference ? 1 : 0);
        int argumentCount = arguments.size();


        // min arg check
        if (argumentCount < minArgRequired) {
            if (trackErrors) addToFewArgumentError(evaluation, minArgRequired);
            return evaluation.validationFailed();
        }
        //max arg check
        if (argumentCount > maxArgAllowed) {
            if (trackErrors) addToManyArgumentError(evaluation, maxArgAllowed);
            return evaluation.validationFailed();
        }


        SpecificTypeReference resolvedCallie = callie != null?  callie.fullyResolveTypeDefAndUnwrapNullTypeReference() : null;
        evaluation.callieResolver = getCallieResolver(resolvedCallie);

        // we use 2 different resolvers here because the callExpression might be inside the same class as the callie
        // but the instance might have different values for its class typeParameters and we dont want to overwrite
        HaxeGenericResolver argumentResolver = new HaxeGenericResolver();
        HaxeGenericResolver parameterResolver = new HaxeGenericResolver();

        argumentResolver.addAll(callExpressionScopeResolver);

        parameterResolver.addAll(methodResolver);
        parameterResolver.addAll(evaluation.callieResolver);
        parameterResolver.addAll(callExpressionScopeResolver);

        applyAssignHint(argumentResolver, parameterResolver);

        // we use a third resolver that combines callie and method parameter values to correctly resolve parameter type
        // we do this because we want the parameter resolver to only keep track the values used in the callExpression
        // and not be affected by callie values, this is important to correctly resolve returnType.
        HaxeGenericResolver combinedResolver = new HaxeGenericResolver();
        combinedResolver.addAll(parameterResolver);
        combinedResolver.addAll(evaluation.callieResolver);

        boolean reachedRestParameter = false;

        int parameterCounter = 0;
        int argumentCounter = 0;

        // validate extension method and macro method criteria
        if (firstArgIsThisReference) {
            if (parameters.isEmpty()) {
                // TODO better error message
                if (trackErrors)
                    evaluation.addError("Extension methods require at least one parameter", sourceExpression);
                return evaluation.validationFailed();
            }
            SpecificTypeReference expectedCallieType = parameters.get(parameterCounter++).getType();
            if (!expectedCallieType.canAssign(callie)) {
                // todo better error message, use bundle and show types
                if (trackErrors) evaluation.addError("Can not use extension method, wrong type", sourceExpression);
                return evaluation.validationFailed();
            }
        }

        CallExpressionArgumentModel argumentModel = null;
        CallExpressionParameterModel parameterModel = null;

        HaxeAssignEvaluation assignEvaluation = null;
        SpecificTypeReference argumentType = null;
        SpecificTypeReference parameterType = null;

        // loop through all arguments and match them to parameters
        // Note: argument and parameter index  can deviate a lot (optional parameters, rest values, extension method etc)
        while (true) {
            if (arguments.size() > argumentCounter) {
                argumentModel = arguments.get(argumentCounter++);
            } else {
                // out of arguments (normal behavior when all arguments have been checked)
                break;
            }

            // if we reach rest parameter then there should not be any parameterModel updates as this is the last one
            if (!reachedRestParameter) {
                if (parameters.size() > parameterCounter) {
                    parameterModel = parameters.get(parameterCounter++);
                    if (parameterModel.isRest()){
                        // sanity check for MacroVarArgOrRest, aka Array<Expr>
                        // if argument is array then this might be a normal parameter and not a rest-parameter
                        SpecificTypeReference typeFromModel = parameterModel.getType();
                        if (HaxeMacroTypeUtil.isMacroVarArgOrRestType(typeFromModel)) {
                            if (!typeFromModel.canAssign(argumentModel.getType())) {
                                reachedRestParameter = true;
                            }
                        }else {
                            reachedRestParameter = true;
                        }
                    }
                } else {
                    // out of parameters and last is not var arg, must mean that ve have skipped optionals and still had arguments left
                    if (parameterModel != null && argumentModel != null) {
                        if (trackErrors) {
                            addTypeMismatchError(evaluation,
                                    argumentType,
                                    parameterType,
                                    assignEvaluation.explanations,
                                    argumentModel.psiElement);
                        }
                        return evaluation.validationFailed();
                    }
                    break;
                }
            }
            //
            SpecificTypeReference originalParameterType = reachedRestParameter ? parameterModel.getRestType() : parameterModel.getType();
            parameterType = tryResolve(combinedResolver, originalParameterType, null);
            argumentType = tryResolve(argumentResolver, argumentModel.getType(), isConstructor? null : parameterType);

            //making final instances so we can use them in  recursion-guard lambda.
            final SpecificTypeReference finalParameterType = parameterType;
            final SpecificTypeReference finalArgumentType = argumentType;

            RecursionKey recursionKey = new RecursionKey(argumentType.getElementContext(), parameterType.getElementContext());
            assignEvaluation = canAssignRecursionGuard.doPreventingRecursion(recursionKey, true,
                    isConstructor
                            ? () -> evaluateAssignToFromForNewAndCallExpression(finalParameterType.createHolder(), finalArgumentType.createHolder())
                            : () -> evaluateAssignToFrom(finalParameterType.createHolder(), finalArgumentType.createHolder()));




            if (assignEvaluation == null) {
                // Recursion guard
                return evaluation.validationFailed();
//        break;
            } else if (assignEvaluation.result) {
                //assign OK, add to evaluation result
                evaluation.addArgumentToParameterMapping(
                        // note: we do -1 here because we did  ++ when getting arguments and parameters
                        argumentCounter-1,
                        parameterCounter-1,
                        argumentType,
                        parameterType,
                        parameterModel.getName()
                );
                //Note: we are using the original (unresolved) parameter type to find typeParameters to inherit
                // if we use the resolved value we would be trying to update values for a different class.
                TypeConstraintMismatch constraintMismatch = updateResolverIfNecessary(argumentType, argumentResolver, originalParameterType, parameterResolver);
                if(constraintMismatch != null) {
                    addConstraintMismatchError(evaluation, argumentType, constraintMismatch, argumentModel.psiElement);
                    evaluation.validationFailed();
                }
                combinedResolver.addAll(parameterResolver);// update commbined resolver
            } else if (parameterModel.isOptional()) {
                // optional parameter did not match argument, continue
                argumentCounter--;  //prevent loop from picking next argument
            } else {
                // argument did not match parameter
                if(trackErrors) {
                    if (assignEvaluation.explanations.hasMissingModel()) {
                        addMissingModelWarning(assignEvaluation, evaluation, argumentModel);
                    } else {
                        addTypeMismatchError(evaluation,
                                argumentType,
                                parameterType,
                                assignEvaluation.explanations,
                                argumentModel.psiElement);
                    }
                }
                evaluation.validationFailed();
//        break;
            }

        }
        // update callExpressionResolver with any new resolve values from argument-parameter types
        evaluation.callExpressionResolver.addAll(combinedResolver);
        evaluation.setCompleted(true);
        return evaluation;
    }



    private static void addMissingModelWarning(HaxeAssignEvaluation assignEvaluation,
                                               HaxeCallExpressionEvaluation evaluation,
                                               CallExpressionArgumentModel argumentModel) {

        String typeName = assignEvaluation.explanations.getMissingModel().getFirst();
        String message = HaxeBundle.message("haxe.semantic.method.parameter.type.not.found", typeName);
        evaluation.addWarning(message, argumentModel.psiElement);
    }

    private void applyAssignHint(HaxeGenericResolver argumentResolver, HaxeGenericResolver parameterResolver) {
        if (assignHint != null && returnType != null) {
            SpecificTypeReference _assignHint = assignHint;
            if(_assignHint instanceof  SpecificHaxeClassReference classReference) {
                _assignHint = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
            }

            SpecificTypeReference _returnType = returnType.getType();
            if(_returnType instanceof  SpecificHaxeClassReference classReference) {
                _returnType = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
            }

            updateResolverIfNecessary(_assignHint, argumentResolver,_returnType, parameterResolver);
        }
    }

    private static @NotNull HaxeGenericResolver getCallieResolver(SpecificTypeReference resolvedCallie) {
        return Optional.ofNullable(resolvedCallie)
                .filter(s -> s instanceof SpecificHaxeClassReference)
                .map(SpecificHaxeClassReference.class::cast)
                .map(SpecificHaxeClassReference::getGenericResolver)
                .orElse(new HaxeGenericResolver());
    }




    public TypeConstraintMismatch updateResolverIfNecessary(SpecificTypeReference argumentType, HaxeGenericResolver argumentResolver,
                                           SpecificTypeReference parameterType, HaxeGenericResolver parameterResolver) {

        TypeConstraintMismatch mismatch = null;
        // check if parameter type is a typeParameter and update resolver if missing resolve value
        if (parameterType instanceof  SpecificHaxeClassReference parameterClassReference) {
            if (parameterClassReference.getHaxeClass() instanceof HaxeTypeParameterDeclaration typeParameter) {
                if(parameterResolver.containsConstraint(typeParameter)) {
                    ResultHolder resolve = parameterResolver.resolve(typeParameter);
                    if (resolve == null || resolve.isUnknown()  || resolve.isTypeParameter()) {
                        if (parameterClassReference.canAssign(argumentType)) {
                            parameterResolver.add(typeParameter, argumentType.createHolder());
                        }else {
                            mismatch = new TypeConstraintMismatch(parameterClassReference, argumentType);
                        }
                    }
                }
            }
            else if (parameterClassReference.createHolder().containsUnknownTypeParameters()) {
                if(argumentType instanceof  SpecificHaxeClassReference argumentClassReference) {
                    SpecificHaxeClassReference downCastedType = argumentClassReference.tryCastToClass(parameterClassReference);
                    if (downCastedType != null) {
                        @NotNull ResultHolder[] parameterSpecifics = parameterClassReference.getSpecifics();
                        @NotNull ResultHolder[] argumentSpecifics = downCastedType.getSpecifics();
                        int argumentsToCheck = Math.min(argumentSpecifics.length, parameterSpecifics.length);
                        for (int i = 0; i < argumentsToCheck; i++) {
                            ResultHolder parameterSpecific = tryUnwrapNull(parameterSpecifics[i]);
                            ResultHolder argumentSpecific = tryUnwrapNull(argumentSpecifics[i]);
                            ResultHolder argumentSpecificResolved = tryUnwrapNull(argumentResolver.resolve(argumentSpecific));
                            if (argumentSpecificResolved != null) argumentSpecific = argumentSpecificResolved;

                            if (argumentSpecific != null && parameterSpecific.canAssign(argumentSpecific)) {
                                TypeConstraintMismatch newMiss = updateResolverIfNecessary(
                                        argumentSpecific.getType(), argumentResolver,
                                        parameterSpecific.getType(), parameterResolver);
                                if(newMiss != null) {
                                    mismatch = newMiss;
                                }
                            }
                        }
                    } else {
                        // note that parameterClassReference is the one with unknown typeParameters,
                        // we dont give it new generics here, we just make sure that it matches arguments type
                        SpecificHaxeClassReference upCastedType = parameterClassReference.tryCastToClass(argumentClassReference);
                        if (upCastedType != null) {
                            @NotNull ResultHolder[] parameterSpecifics = upCastedType.getSpecifics();
                            @NotNull ResultHolder[] argumentSpecifics = argumentClassReference.getGenericResolver().getSpecifics();
                            int argumentsToCheck = Math.min(argumentSpecifics.length, parameterSpecifics.length);
                            for (int i = 0; i < argumentsToCheck; i++) {
                                ResultHolder parameterSpecific = parameterSpecifics[i];
                                ResultHolder argumentSpecific = argumentResolver.resolve(argumentSpecifics[i]);
                                if (argumentSpecific != null) {
                                    TypeConstraintMismatch newMiss = updateResolverIfNecessary(
                                            argumentSpecific.getType(), argumentResolver,
                                            parameterSpecific.getType(), parameterResolver);

                                    if(newMiss != null) {
                                        mismatch = newMiss;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // todo if argument or parameter is abstract of function,  cast?
        if(parameterType instanceof SpecificFunctionReference parameterFunctionReference) {
            if(argumentType instanceof  SpecificFunctionReference argumentFunctionReference) {
                List<HaxeArgument> argumentsArguments = argumentFunctionReference.arguments;
                List<HaxeArgument> parameterArguments = parameterFunctionReference.arguments;
                int argumentsToCheck = Math.min(argumentsArguments.size(), parameterArguments.size());
                for (int i = 0; i < argumentsToCheck; i++) {
                    HaxeArgument argumentA = argumentsArguments.get(i);
                    HaxeArgument argumentP = parameterArguments.get(i);
                    TypeConstraintMismatch newMiss = updateResolverIfNecessary(
                            argumentA.getType().getType(), argumentResolver,
                            argumentP.getType().getType(), parameterResolver);
                    if(newMiss != null) {
                        mismatch = newMiss;
                    }

                }
                ResultHolder argumentReturnType = argumentFunctionReference.getReturnType();
                ResultHolder parameterReturnType = parameterFunctionReference.getReturnType();
                TypeConstraintMismatch newMiss = updateResolverIfNecessary(
                        argumentReturnType.getType(), argumentResolver,
                        parameterReturnType.getType(), parameterResolver);
                if(newMiss != null) {
                    mismatch = newMiss;
                }
            }

        }
        return mismatch;
    }

    private static ResultHolder tryUnwrapNull(@Nullable ResultHolder holder) {
        if(holder == null) return null;
        if(holder.isNullWrappedType()) {
            return holder.getClassType().unwrapNullType().createHolder();
        }
        return holder;
    }

    private static @NotNull SpecificTypeReference tryResolve(HaxeGenericResolver callExpressionResolver, SpecificTypeReference type, @Nullable SpecificTypeReference hint) {

        ResultHolder resolvedParameterType = callExpressionResolver.resolve(type);
        if (resolvedParameterType != null && !resolvedParameterType.isUnknown()) {
            type = resolvedParameterType.getType();
            if(hint != null) {
                type = addHintValuesToResolver(hint, type);
            }

        }

        return type;
    }
    //when we have arguments that do not contain all typeParameters  for instance other callExpressions
    // we try to inherit the "expected" values from the parameter type (monomorphism i guess).
    //
    // This sometimes necessary when doing the argument-parameter assign check (DisplayObjectRecycler in feathersUI is a good example)
    // if one of our parameters got typeParameters with constraints and the input arguments typeParameters
    // does not fulfill these constraints.
    private static SpecificTypeReference addHintValuesToResolver(@Nullable SpecificTypeReference hint, SpecificTypeReference expected) {
        //TODO solve this for functions (and other types?)
        if (expected instanceof SpecificHaxeClassReference currentClass) {
            if (hint instanceof SpecificHaxeClassReference hintClass) {
                SpecificHaxeClassReference castedHint = hintClass.tryCastToClass(currentClass);
                if (castedHint != null) {
                    @NotNull ResultHolder[] currentSpecifics = currentClass.getSpecifics();
                    @NotNull ResultHolder[] hintSpecifics = castedHint.getSpecifics();
                    @NotNull ResultHolder[] newSpecifics = new ResultHolder[currentSpecifics.length];
                    for (int i = 0; i < currentSpecifics.length; i++) {
                        ResultHolder currentSpecific = currentSpecifics[i];
                        // TODO should probably traverse types instead of  substituting when containsTypeParameters is true
                        if(currentSpecific.isTypeParameter() || currentSpecific.containsTypeParameters()) {
                            if (hintSpecifics.length> i) {
                                ResultHolder hintSpecific = hintSpecifics[i];
                                if (currentSpecific.canAssign(hintSpecific)) {
                                    newSpecifics[i] = hintSpecific;
                                } else {
                                    newSpecifics[i] = currentSpecific;
                                }
                            }else {
                                newSpecifics[i] = currentSpecific;
                            }
                        }else {
                            newSpecifics[i] = currentSpecific;
                        }
                    }
                    return SpecificHaxeClassReference.withGenerics(currentClass.getHaxeClassReference(), newSpecifics);
                }
            }
        }
        return expected;
    }


    private static boolean hasRestParameter(List<CallExpressionParameterModel> parametersList) {
        if (parametersList.isEmpty()) return false;
        return parametersList.getLast().isRest();
    }

    private static int countRequiredArguments(List<CallExpressionParameterModel> parametersList) {
        return (int) parametersList.stream()
                .filter(p -> !p.isOptional() && !p.hasIntiValue() && !p.isRest())
                .count();
    }
    private void addConstraintMismatchError(HaxeCallExpressionEvaluation evaluation, SpecificTypeReference argumentType, TypeConstraintMismatch constraintMismatch, PsiElement argumentPsi) {
        if (argumentPsi != null) {
            String message = "Constraint violation want" +constraintMismatch.expected().toPresentationString() + " got " + constraintMismatch.got().toPresentationString();
            evaluation.addError(message, argumentPsi);
        }
    }

    private void addTypeMismatchError(HaxeCallExpressionEvaluation evaluation,
                                      SpecificTypeReference argumentType,
                                      SpecificTypeReference parameterType,
                                      AssignExplanation explanation,
                                      PsiElement argumentPsi
    ) {
        if (explanation != null && argumentPsi != null) {
            TextRange expectedRange = argumentPsi.getTextRange();

            if (explanation.hasMissingMembers()) {
                String message = HaxeBundle.message("haxe.semantic.method.parameter.mismatch.missing.members", explanation.createMissingMembersMessage());
                evaluation.addError(message, argumentPsi);

            } else if (explanation.hasWrongTypeMembers()) {
                // we are not allowed to annotate outside the expression so we check if all are inside before attempting to make them
                // if they are not inside, we fall back to just annotating the entire expression
                Map<PsiElement, String> wrongTypeMap = explanation.getWrongTypeMap();
                boolean allInRange = wrongTypeMap.keySet().stream().allMatch(psi -> expectedRange.contains(psi.getTextRange()));

                if (allInRange) {
                    wrongTypeMap.forEach((key, value) -> evaluation.addError(value, key.getTextRange()));
                } else {
                    String message = HaxeBundle.message("haxe.semantic.method.parameter.mismatch.wrong.type.members",
                            explanation.createWrongTypeMembersMessage());
                    evaluation.addError(message, expectedRange);
                }
            }else if (explanation.hasMissingModel()) {
                String missingModel = explanation.getMissingModel().getFirst();
                String message = HaxeBundle.message("haxe.semantic.method.parameter.type.not.found", missingModel);
                evaluation.addWarning(message, expectedRange);
            }else {
                String message = HaxeBundle.message("haxe.semantic.method.parameter.mismatch",
                        parameterType.toPresentationString(true),
                        argumentType.toPresentationString(true));
                evaluation.addError(message, expectedRange);
            }
        }
    }




    private void addToFewArgumentError(HaxeCallExpressionEvaluation evaluation, int minArgRequired) {
        String message = HaxeBundle.message("haxe.semantic.method.parameter.missing", minArgRequired, arguments.size());
        if (sourceExpression instanceof HaxeCallExpression callExpression) {
            if (!arguments.isEmpty() && callExpression.getExpressionList() != null) {
                evaluation.addError(message, callExpression.getExpressionList().getTextRange());
            } else {
                HaxeExpression expression = callExpression.getExpression();
                if (expression != null) {
                    PsiElement first = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(expression);
                    PsiElement second = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(first);
                    if (first == null || second == null) {
                        TextRange range = expression.getTextRange();
                        evaluation.addError(message, range);
                    } else {
                        TextRange range = TextRange.create(first.getTextOffset(), second.getTextOffset() + 1);
                        evaluation.addError(message, range);
                    }
                }
            }
        } else if (sourceExpression instanceof HaxeNewExpression newExpression) {
            @NotNull PsiElement[] children = newExpression.getChildren();
            if (children.length > 0) {
                PsiElement first = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(children[0]);
                if (first != null) {
                    PsiElement second = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(first);
                    if (second != null) {
                        TextRange range = TextRange.create(first.getTextOffset(), second.getTextOffset() + 1);
                        evaluation.addError(message, range);
                    }
                }
            }
        }
    }

    private void addToManyArgumentError(HaxeCallExpressionEvaluation evaluation, int maxArgAllowed) {
        String message = HaxeBundle.message("haxe.semantic.method.parameter.too.many", maxArgAllowed, arguments.size());
        if (sourceExpression instanceof HaxeCallExpression callExpression) {
            HaxeCallExpressionList expressionList = callExpression.getExpressionList();
            if (expressionList != null) {
                evaluation.addError(message, expressionList.getTextRange());
            } else {
                evaluation.addError(message, sourceExpression.getTextRange());
            }
        } else if (sourceExpression instanceof HaxeNewExpression newExpression) {
            evaluation.addError(message, newExpression.getTextRange());
        }
    }
}

record TypeConstraintMismatch(SpecificTypeReference expected, SpecificTypeReference got){}