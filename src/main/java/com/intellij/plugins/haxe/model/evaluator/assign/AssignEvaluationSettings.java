package com.intellij.plugins.haxe.model.evaluator.assign;

public record AssignEvaluationSettings(
        boolean strictBasicCheck,
        boolean checkDirectCasts,
        boolean checkImplicitCasts,
        boolean contravariance,
        boolean ignoreFromConstraints,
        boolean implicitTypeMustMatchUnderlying
) {
}
