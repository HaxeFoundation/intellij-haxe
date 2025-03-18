package com.intellij.plugins.haxe.model.evaluator.assign;

import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CustomLog
public class HaxeTypeCompatible {

    //TODO mlo: add some kind of recursion guard  (implicit cast can loop around)?

    private static final AssignEvaluationSettings DEFAULT_SETTINGS = new AssignEvaluationSettings(false, true,true, false, false, false);
    private static final AssignEvaluationSettings DEFAULT_STRICT_SETTINGS = new AssignEvaluationSettings(true, false, false, false, false, false);
    private static final AssignEvaluationSettings CONTRAVARIANCE_SETTINGS = new AssignEvaluationSettings(false, true, true, true, false, false);
    private static final AssignEvaluationSettings CALL_EXPRESSION_SETTINGS = new AssignEvaluationSettings(false, true, true, false, true, false);

    /**
     * Regular can assign will allow Dynamic to be assigned to anything and also check implicit casts (@:to/@From) and handle special annotation rules
     * like <code>@:multiType</code>,  <code>@:transient</code> and <code>@:forward</code> in the case of anonymous types.
     */
    static public boolean canAssignToFromReference(@Nullable SpecificTypeReference to, @Nullable SpecificTypeReference from) {
        if (to == null || from == null) return false;
        return canAssignToFromReference(to.createHolder(), from.createHolder());
    }
    static public boolean canAssignToFromReference(HaxeAssignEvaluation context, @Nullable SpecificTypeReference to, @Nullable SpecificTypeReference from) {
        if (to == null || from == null) return false;
        return canAssignToFromEvaluation(to.createHolder(), from.createHolder(),DEFAULT_SETTINGS, context).result;
    }

    static public boolean canAssignToFromReference(@Nullable SpecificTypeReference to, @Nullable SpecificTypeReference from, boolean checkDirectCasts, boolean checkImplicitCasts) {
        if (to == null || from == null) return false;
        AssignEvaluationSettings settings = new AssignEvaluationSettings(false, checkDirectCasts, checkImplicitCasts, false, false, false);
        return canAssignToFromEvaluation(to.createHolder(), from.createHolder(), settings, null).result;
    }


    static public boolean canAssignToFromReference(@Nullable ResultHolder to, @Nullable ResultHolder from) {
        if (to == null || from == null) return false;
        return canAssignToFromEvaluation(to, from, DEFAULT_SETTINGS, null).result;
    }

    /**
     *  Used to perform canAssign in parameters when functionTypes are assigned.
     *  - flips the to/from order when performing assign operation
     *  - constraints checks are still performed in normal order.
     */
    static public boolean canAssignToFromContravariance(@Nullable ResultHolder to, @Nullable ResultHolder from) {
        if (to == null || from == null) return false;
        return canAssignToFromEvaluation(to, from, CONTRAVARIANCE_SETTINGS, null).result;
    }

    static public boolean canAssignToFromContravariance(@Nullable ResultHolder to, @Nullable ResultHolder from, boolean checkDirectCasts, boolean checkImplicitCasts, boolean implicitTypeMustMatchUnderlying) {
        if (to == null || from == null) return false;
        AssignEvaluationSettings settings = new AssignEvaluationSettings(false, checkDirectCasts, checkImplicitCasts, true, false, implicitTypeMustMatchUnderlying);
        return canAssignToFromEvaluation(to, from, settings, null).result;
    }

    /**
     *  Check is arguments matches parameters but makes sure generics can be inherited from parameter type.
     *  - ignores any constraints on argument type (if type parameter then we try to inherit from parameter type)
     */
    static public HaxeAssignEvaluation evaluateAssignToFromForNewAndCallExpression(@NotNull ResultHolder to, @NotNull ResultHolder from) {
        return canAssignToFromEvaluation(to, from, CALL_EXPRESSION_SETTINGS, null);
    }

    static public boolean canAssignToFromReference(@Nullable ResultHolder to, @Nullable ResultHolder from, boolean checkDirectCasts, boolean checkImplicitCasts, boolean contravariance) {
        if (to == null || from == null) return false;
        AssignEvaluationSettings settings = new AssignEvaluationSettings(false, checkDirectCasts, checkImplicitCasts, contravariance, false, false);
        return canAssignToFromEvaluation(to, from, settings, null).result;
    }
    static public boolean canAssignToFromReference(@Nullable ResultHolder to, @Nullable ResultHolder from, boolean checkDirectCasts, boolean checkImplicitCasts, boolean contravariance, boolean ignoreFromConstraints) {
        if (to == null || from == null) return false;
        AssignEvaluationSettings settings = new AssignEvaluationSettings(false, checkDirectCasts, checkImplicitCasts, contravariance, ignoreFromConstraints, false);
        return canAssignToFromEvaluation(to, from, settings, null).result;
    }

    static public HaxeAssignEvaluation evaluateAssignToFrom(@NotNull ResultHolder to, @NotNull ResultHolder from) {
        return canAssignToFromEvaluation(to, from, false, true, true);
    }

    static public HaxeAssignEvaluation evaluateAssignToFrom(@NotNull ResultHolder to, @NotNull ResultHolder from, boolean checkDirectCasts, boolean checkImplicitCasts) {
        return canAssignToFromEvaluation(to, from, false, checkDirectCasts, checkImplicitCasts);
    }


    /**
     * When checking if typeParameters can be assign we have to be stricter than object reference assign.
     * When doing normal assign Dynamic can be assigned to anything while when checking assign for typeParameter you are not allowed
     * to assign Dynamic</>(or Any) to more specific types, implicit cast for abstracts are also not allowed
     * <pre>
     * {@code
     *  var x:String = null;
     *  var y:Dynamic = null;
     *  x = y;// allowed
     * }
     * </pre>
     * <pre>
     * {@code
     *  var x:Array<String> = null;
     *  var y:Array<Dynamic> = null;
     *  x = y // not allowed;
     * }
     * </pre>
     */
    static public boolean canAssignToFromTypeParameter(@Nullable ResultHolder to, @Nullable ResultHolder from) {
        if (to == null || from == null) return false;
        return canAssignToFromTypeParameter(null, to, from, false, false);
    }

    static public boolean canAssignToFromTypeParameter(HaxeAssignEvaluation context, @Nullable ResultHolder to, @Nullable ResultHolder from, boolean ignoreFromConstraints,  boolean implicitTypeMustMatchUnderlying) {
        if (to == null || from == null) return false;
        return canAssignToFromStrictEvaluation(context, to, from, ignoreFromConstraints, implicitTypeMustMatchUnderlying).result;
    }

    /**
     * Evaluates strict assign operations (ex. typeParameters/generics).
     * Used for typeParameters and situations where implicit and implicit direct casts are not allowed
     */
    private static HaxeAssignEvaluation canAssignToFromStrictEvaluation(HaxeAssignEvaluation context, @NotNull ResultHolder to, @NotNull ResultHolder from) {
        // Note: There's a hack in abstract canAssign that allow  assign when abstracts underlying type is Dynamic and it got direct "from Dynamic" cast
        return canAssignToFromEvaluation(to, from, DEFAULT_STRICT_SETTINGS, context);
    }

    private static HaxeAssignEvaluation canAssignToFromStrictEvaluation(HaxeAssignEvaluation context, @NotNull ResultHolder to, @NotNull ResultHolder from, boolean ignoreFromConstraints, boolean implicitTypeMustMatchUnderlying) {
        // Note: There's a hack in abstract canAssign that allow  assign when abstracts underlying type is Dynamic and it got direct "from Dynamic" cast
        AssignEvaluationSettings settings = new AssignEvaluationSettings(true, true, false, false,  ignoreFromConstraints, implicitTypeMustMatchUnderlying);
        return canAssignToFromEvaluation(to, from, settings, context);
    }


    /**
     * Evaluates assign operations
     * - checking basic assign operations (Dynamic, Any, Unknown, Expr, enums)
     * - checking if class objects can be assigned (interfaces and inheritance)
     * - checking if enum objects can be assigned (EnumValue, and enum members)
     * - checking if functions types and references can be assigned (checking sugnatures)
     * - checking if anonymous structures can be assigned (comparing members)
     * - checking if abstracts can be  direct or implicit casted to and from other types
     */
    static public HaxeAssignEvaluation canAssignToFromEvaluation(@NotNull ResultHolder to, @NotNull ResultHolder from,
                                                                 boolean strictBasicCheck,
                                                                 boolean checkDirectCasts,
                                                                 boolean checkImplicitCasts,
                                                                 boolean contravariance
    ) {
        return canAssignToFromEvaluation(to, from, strictBasicCheck, checkDirectCasts, checkImplicitCasts, contravariance, null);
    }

    static public HaxeAssignEvaluation canAssignToFromEvaluation(@NotNull ResultHolder to, @NotNull ResultHolder from,
                                                                 boolean strictBasicCheck,
                                                                 boolean checkDirectCasts,
                                                                 boolean checkImplicitCasts) {
        return canAssignToFromEvaluation(to, from, strictBasicCheck, checkDirectCasts, checkImplicitCasts, false, null);
    }


    private static final RecursionGuard<Object> canAssignRecursionGuard = RecursionManager.createGuard("canAssignRecursionGuard");

    static public HaxeAssignEvaluation canAssignToFromEvaluation(@NotNull ResultHolder to, @NotNull ResultHolder from,
                                                                 boolean strictBasicCheck,
                                                                 boolean checkDirectCasts,
                                                                 boolean checkImplicitCasts,
                                                                 boolean contravariance,
                                                                 @Nullable HaxeAssignEvaluation parent
    ) {
        AssignEvaluationSettings settings = new AssignEvaluationSettings(strictBasicCheck, checkDirectCasts, checkImplicitCasts, contravariance, false, false);
        return canAssignToFromEvaluation(to,from, settings, parent);
    }
    static public HaxeAssignEvaluation canAssignToFromEvaluation(@NotNull ResultHolder to, @NotNull ResultHolder from,
                                                                 AssignEvaluationSettings settings,
                                                                 @Nullable HaxeAssignEvaluation parent
    ) {
        ProgressIndicatorProvider.checkCanceled();

        HaxeAssignEvaluation evaluation = new HaxeAssignEvaluation(to, from, settings);
        evaluation.fullyResolveTypes();

        if(parent != null){
            parent.addChild(evaluation);
        }

        evaluation.testBasicAssignRules(settings.strictBasicCheck());
        // prevent recursion in the case of typedef and class hierarchy loops, casting loops etc
        if(!evaluation.completed) {
            // NOTE: memoize can not be used as the context elements does not necessarily represent the type
            // (could maybe do some tricks with fully qualified names but recursive typeParameter constraints will be problematic)
            Boolean done = canAssignRecursionGuard.doPreventingRecursion(evaluation.recursionGuardKey(), false, () -> {
                if (!evaluation.completed) evaluation.testClassAssignRules();
                if (!evaluation.completed) evaluation.testEnumAssignRules();
                if (!evaluation.completed) evaluation.testFunctionAssignRules();
                if (!evaluation.completed) evaluation.testAnonymousAssignRules();
                if (!evaluation.completed) evaluation.testAbstractAssignRules(settings.checkDirectCasts(), settings.checkImplicitCasts(), settings.implicitTypeMustMatchUnderlying());
                if (!evaluation.completed) evaluation.testTypeParameterConstraints(settings.checkDirectCasts(), settings.checkImplicitCasts());
                return true;
            });
            if (done == null) {
                // stopped by recursion guard.

                // we allow assign when recursion guard is triggered and the recursive types are typeParameter,
                // anything else should fail the assign test. (this might not be the best solution but works for now)

                // we allow type parameters as recursive constraints in typeParameters would always fail the assign test.
                // Ex. the typeParameter for linkedList sort. T:{prev:T, next:T}  (anonymous member check would fail)
                // Note: this can probably fail for other complex cases of anonymous member checks as well.
                boolean isRecursiveTypeParameter = to.isTypeParameter() || from.isTypeParameter();
                if(isRecursiveTypeParameter) {
                    evaluation.complete(true, "Stopped by recursion guard (typeParameter)");
                }else {
                    // allow assign if constraint refers to its parent/owing typeParameter
                    boolean toSelfConstraint = to.isOrContainsTypeParameters() && to.hasNoGenericsOrTheOnlyGenericTypeisItSelf();
                    boolean fromSelfConstraint = from.isOrContainsTypeParameters() && from.hasNoGenericsOrTheOnlyGenericTypeisItSelf();

                    boolean selfConstraint = toSelfConstraint || fromSelfConstraint;
                    if(selfConstraint) {
                        evaluation.complete(true, "Stopped by recursion guard (SelfConstraint)");
                    }else {
                        evaluation.complete(false, "Stopped by recursion guard");
                    }
                }
            }
        }
        return evaluation;
    }


}
