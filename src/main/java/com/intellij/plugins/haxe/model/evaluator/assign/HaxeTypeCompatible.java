package com.intellij.plugins.haxe.model.evaluator.assign;

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.PsiElement;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CustomLog
public class HaxeTypeCompatible {

        //TODO mlo: add some kind of recursion guard  (implicit cast can loop around)?

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
        return canAssignToFromEvaluation(to.createHolder(), from.createHolder(), false,true, true, context).result;
    }

    static public boolean canAssignToFromReference(@Nullable SpecificTypeReference to, @Nullable SpecificTypeReference from, boolean checkExplicitCasts, boolean checkImplicitCasts) {
        if (to == null || from == null) return false;
        return canAssignToFromReference(to.createHolder(), from.createHolder(), checkExplicitCasts, checkImplicitCasts);
    }


    static public boolean canAssignToFromReference(@Nullable ResultHolder to, @Nullable ResultHolder from) {
        if (to == null || from == null) return false;
        return canAssignToFromReference(to, from, true, true);
    }

    static public boolean canAssignToFromReference(@Nullable ResultHolder to, @Nullable ResultHolder from, boolean checkExplicitCasts, boolean checkImplicitCasts) {
        if (to == null || from == null) return false;
        return canAssignToFromEvaluation(to, from, false, checkExplicitCasts, checkImplicitCasts).result;
    }


    static public HaxeAssignEvaluation evaluateAssignToFrom(@NotNull ResultHolder to, @NotNull ResultHolder from) {
        return canAssignToFromEvaluation(to, from, false, true, true);
    }

    static public HaxeAssignEvaluation evaluateAssignToFrom(@NotNull ResultHolder to, @NotNull ResultHolder from, boolean checkExplicitCasts, boolean checkImplicitCasts) {
        return canAssignToFromEvaluation(to, from, false, checkExplicitCasts, checkImplicitCasts);
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
        return canAssignToFromStrictEvaluation(null, to, from).result;
    }

    static public boolean canAssignToFromTypeParameter(@Nullable SpecificTypeReference to, @Nullable SpecificTypeReference from) {
        if (to == null || from == null) return false;
        return canAssignToFromTypeParameter(to.createHolder(), from.createHolder());
    }

    static boolean canAssignToFromTypeParameter(HaxeAssignEvaluation context, @Nullable ResultHolder to, @Nullable ResultHolder from) {
        if (to == null || from == null) return false;
        return canAssignToFromStrictEvaluation(context, to, from).result;
    }


    /**
     * Evaluates normal assign operations (ex. references).
     * Used when implicit casts are allowed, ex. object references, parameters etc
     */
    private static HaxeAssignEvaluation canAssignToFromRegularEvaluation(@NotNull ResultHolder to, @NotNull ResultHolder from) {
        return canAssignToFromEvaluation(to, from, false, true, true);
    }

    /**
     * Evaluates strict assign operations (ex. typeParameters/generics).
     * Used for typeParameters and situations where implicit casts are not allowed
     */
    private static HaxeAssignEvaluation canAssignToFromStrictEvaluation(HaxeAssignEvaluation context, @NotNull ResultHolder to, @NotNull ResultHolder from) {
        return canAssignToFromEvaluation(to, from, true, true, false, context);
    }


    /**
     * Evaluates assign operations
     * - checking basic assign operations (Dynamic, Any, Unknown, Expr, enums)
     * - checking if class objects can be assigned (interfaces and inheritance)
     * - checking if enum objects can be assigned (EnumValue, and enum members)
     * - checking if functions types and references can be assigned (checking sugnatures)
     * - checking if anonymous structures can be assigned (comparing members)
     * - checking if abstracts can be explicit or implicit casted to and from other types
     */
    static public HaxeAssignEvaluation canAssignToFromEvaluation(@NotNull ResultHolder to, @NotNull ResultHolder from,
                                                                 boolean strictBasicCheck,
                                                                 boolean checkExplicitCasts,
                                                                 boolean checkImplicitCasts) {
        return canAssignToFromEvaluation(to, from, strictBasicCheck, checkExplicitCasts, checkImplicitCasts, null);
    }


    private static final RecursionGuard<Object> canAssignRecursionGuard = RecursionManager.createGuard("canAssignRecursionGuard");

    static public HaxeAssignEvaluation canAssignToFromEvaluation(@NotNull ResultHolder to, @NotNull ResultHolder from,
                                                                 boolean strictBasicCheck,
                                                                 boolean checkExplicitCasts,
                                                                 boolean checkImplicitCasts,
                                                                 @Nullable HaxeAssignEvaluation parent
    ) {
        HaxeAssignEvaluation evaluation = new HaxeAssignEvaluation(to, from);
        evaluation.fullyResolveTypes();

        if(parent != null){
            parent.addChild(evaluation);
        }

        evaluation.testBasicAssignRules(strictBasicCheck);
        // prevent recursion in the case of typedef and class hierarchy loops, casting loops etc
        if(!evaluation.completed) {
            // NOTE: memoize can not be used as the context elements does not necessarily represent the type
            // (could maybe do some tricks with fully qualified names but recursive typeParameter constraints will be problematic)
            Boolean done = canAssignRecursionGuard.doPreventingRecursion(evaluation.recursionGuardKey(), false, () -> {
                if (!evaluation.completed) evaluation.testClassAssignRules();
                if (!evaluation.completed) evaluation.testEnumAssignRules();
                if (!evaluation.completed) evaluation.testFunctionAssignRules();
                if (!evaluation.completed) evaluation.testAnonymousAssignRules();
                if (!evaluation.completed) evaluation.testAbstractAssignRules(checkExplicitCasts, checkImplicitCasts);
                if (!evaluation.completed) evaluation.testTypeParameterConstraints(checkExplicitCasts, checkImplicitCasts);
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
                evaluation.complete(isRecursiveTypeParameter, "Stopped by recursion guard");
            }
        }
        return evaluation;
    }


}
