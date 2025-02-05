package com.intellij.plugins.haxe.model.evaluator.assign;

import com.intellij.plugins.haxe.model.HaxeMethodModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeFunctionCompatible {

    static public boolean canImplement(@Nullable HaxeMethodModel implementModel, @Nullable HaxeMethodModel interfaceModel) {
        if (interfaceModel == null || implementModel == null) return false;
        return checkThisImplementsThat(interfaceModel, implementModel, false).result;
    }

    static public boolean canOverride(@Nullable HaxeMethodModel baseModel, @Nullable HaxeMethodModel overrideModel) {
        if (baseModel == null || overrideModel == null) return false;
        return checkThisOverrideThat(baseModel, overrideModel, false).result;
    }

    @NotNull
    static public HaxeOverrideOrImplementEvaluation checkThisImplementsThat(@NotNull HaxeMethodModel thisModel, @NotNull HaxeMethodModel thatModel, boolean makeAnnotations) {
        return new HaxeOverrideOrImplementEvaluation(thisModel, thatModel, makeAnnotations).evaluateImplementation();
    }

    @NotNull
    static public HaxeOverrideOrImplementEvaluation checkThisOverrideThat(@NotNull HaxeMethodModel thisModel, @NotNull HaxeMethodModel thatModel, boolean makeAnnotations) {
        return new HaxeOverrideOrImplementEvaluation(thisModel, thatModel, makeAnnotations)
                .evaluateOverride();
    }
}

