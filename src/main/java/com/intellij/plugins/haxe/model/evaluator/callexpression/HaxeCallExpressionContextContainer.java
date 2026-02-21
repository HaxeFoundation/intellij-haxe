package com.intellij.plugins.haxe.model.evaluator.callexpression;

import com.intellij.psi.PsiElement;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeCallExpressionContextContainer {
    private final List<HaxeCallExpressionContext> contexts;

    @Nullable @Getter private HaxeCallExpressionEvaluation evaluation;
    @Nullable @Getter private HaxeCallExpressionContext context;

    public HaxeCallExpressionContextContainer(@NotNull List<HaxeCallExpressionContext> contexts) {
        this.contexts = contexts;
    }

    public static HaxeCallExpressionContextContainer create(List<HaxeCallExpressionContext> list) {
        return new HaxeCallExpressionContextContainer(list);
    }

    @Nullable
    public HaxeCallExpressionEvaluation evaluateContexts() {
        for (HaxeCallExpressionContext ctx : contexts) {
            HaxeCallExpressionEvaluation evaluate = ctx.evaluate();
            context = ctx;
            evaluation = evaluate;
            if (evaluate.isValid()) {
                return evaluate;
            }
        }
        return evaluation;
    }
    @Nullable
    public HaxeCallExpressionEvaluation evaluateContextsWithAnnotationData(PsiElement element) {
        for (HaxeCallExpressionContext ctx : contexts) {
            HaxeCallExpressionEvaluation evaluate = ctx.evaluateWithAnnotationData(element);
            context = ctx;
            evaluation = evaluate;
            if (evaluate.isValid()) {
                return evaluate;
            }
        }
        return evaluation;
    }

    public boolean canCache() {
        return contexts.stream().allMatch(context -> context.canCache);
    }

}
