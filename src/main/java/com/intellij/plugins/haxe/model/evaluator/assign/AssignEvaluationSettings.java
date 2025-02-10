package com.intellij.plugins.haxe.model.evaluator.assign;

public record AssignEvaluationSettings(
        boolean strictBasicCheck,
        boolean checkExplicitCasts,
        boolean checkImplicitCasts,
        boolean contravariance,
        boolean ignoreFromConstraints
) {
}
